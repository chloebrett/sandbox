import kotlin.random.Random

const val WIDTH = 640
const val HEIGHT = 480
const val LOG_VERBOSE = false

fun main() {
  println("Loaded Kotlin/WASM module")
}

val renderer = Renderer()

@JsExport
fun getPixel(x: Int, y: Int): Int {
  return renderer.buffer[y * WIDTH + x]
}

@JsExport
fun update() {
  renderer.update()
}

data class Point(val x: Int, val y: Int)

fun makeColour(r: Int, g: Int, b: Int): Int {
  val alpha = 0xFF000000.toInt()
  return alpha or (r shl 16) or (g shl 8) or b
}

class Renderer {
  var buffer = IntArray(WIDTH * HEIGHT)

  init {
    clear()
    drawRandomTriangles()
  }

  fun clear() {
    buffer = IntArray(WIDTH * HEIGHT)
  }

  fun update() {
    clear()
    drawRandomTriangles()
  }

  fun drawRandomTriangles() {
    for (i in 0..5) {
      val a = randomPoint()
      val b = randomPoint()
      val c = randomPoint()
      val triangle = Triangle.sortedFromPoints(a, b, c)
      val colour = randomColour()
      drawFilledTriangle(triangle, colour)
    }
  }

  fun renderTestLines() {
    val centre = 200
    val length = 100
    val white = makeColour(255, 255, 255)

    fun drawTo(point: Point) {
      drawLine(Point(centre, centre), point, white)
    }

    drawTo(Point(centre + length, centre))
    drawTo(Point(centre + length, centre + length / 2))
    drawTo(Point(centre + length, centre + length))
    drawTo(Point(centre + length / 2, centre + length))
    drawTo(Point(centre, centre + length))
    drawTo(Point(centre - length / 2, centre + length))
    drawTo(Point(centre - length, centre + length))
    drawTo(Point(centre - length, centre + length / 2))
    drawTo(Point(centre - length, centre))
    drawTo(Point(centre - length, centre - length / 2))
    drawTo(Point(centre - length, centre - length))
    drawTo(Point(centre - length / 2, centre - length))
    drawTo(Point(centre, centre - length))
    drawTo(Point(centre + length / 2, centre - length))
    drawTo(Point(centre + length, centre - length))
    drawTo(Point(centre + length, centre - length / 2))
  }

  fun randomPoint() =
          Point(
                  (Random.nextDouble() * WIDTH).toInt(),
                  (kotlin.random.Random.nextDouble() * HEIGHT).toInt()
          )

  fun randomColour() =
          makeColour(
                  (Random.nextDouble() * 255).toInt(),
                  (Random.nextDouble() * 255).toInt(),
                  (Random.nextDouble() * 255).toInt()
          )

  fun setPixel(x: Int, y: Int, colour: Int) {
    if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) {
      println("Pixel out of bounds")
      return
    }

    if (LOG_VERBOSE) {
      println("Setting pixel at $x, $y to $colour")
    }

    buffer[y * WIDTH + x] = colour
  }

  fun drawLine(start: Point, end: Point, colour: Int) {
    if (kotlin.math.abs(end.y - start.y) < kotlin.math.abs(end.x - start.x)) {
      if (start.x > end.x) {
        drawLineLow(end, start, colour)
      } else {
        drawLineLow(start, end, colour)
      }
    } else {
      if (start.y > end.y) {
        drawLineHigh(end, start, colour)
      } else {
        drawLineHigh(start, end, colour)
      }
    }
  }

  fun drawLineLow(start: Point, end: Point, colour: Int) {
    val dx = end.x - start.x
    var dy = end.y - start.y
    var D = 2 * dy - dx
    var y = start.y
    var yi = 1
    if (dy < 0) {
      yi = -1
      dy = -dy
    }

    for (x in start.x until end.x) {
      setPixel(x, y, colour)
      if (D > 0) {
        y += yi
        D += 2 * (dy - dx)
      } else {
        D += 2 * dy
      }
    }
  }

  fun drawLineHigh(start: Point, end: Point, colour: Int) {
    var dx = end.x - start.x
    val dy = end.y - start.y
    var D = 2 * dx - dy
    var x = start.x

    var xi = 1
    if (dx < 0) {
      xi = -1
      dx = -dx
    }

    for (y in start.y until end.y) {
      setPixel(x, y, colour)
      if (D > 0) {
        x += xi
        D += 2 * (dx - dy)
      } else {
        D += 2 * dx
      }
    }
  }

  fun drawFilledTriangle(triangle: Triangle, colour: Int) {
    val flatTriangles = splitIntoFlatTriangles(triangle)
    if (LOG_VERBOSE) {
      println("Flat triangles: $flatTriangles")
    }

    drawFlatBottomTriangle(flatTriangles.first, colour)
    drawFlatTopTriangle(flatTriangles.second, colour)
  }

  fun drawFlatTopTriangle(triangle: Triangle, colour: Int) {
    val a = triangle.a
    val b = triangle.b
    val c = triangle.c

    val invslope1 = (c.x - a.x).toDouble() / (c.y - a.y)
    val invslope2 = (c.x - b.x).toDouble() / (c.y - b.y)

    var curx1 = c.x.toDouble()
    var curx2 = c.x.toDouble()

    for (scanlineY in c.y downTo a.y) {
      drawLine(Point(curx1.toInt(), scanlineY), Point(curx2.toInt(), scanlineY), colour)
      curx1 -= invslope1
      curx2 -= invslope2
    }
  }

  fun drawFlatBottomTriangle(triangle: Triangle, colour: Int) {
    val a = triangle.a
    val b = triangle.b
    val c = triangle.c

    val invslope1 = (b.x - a.x).toDouble() / (b.y - a.y)
    val invslope2 = (c.x - a.x).toDouble() / (c.y - a.y)

    var curx1 = a.x.toDouble()
    var curx2 = a.x.toDouble()

    for (scanlineY in a.y until b.y) {
      drawLine(Point(curx1.toInt(), scanlineY), Point(curx2.toInt(), scanlineY), colour)
      curx1 += invslope1
      curx2 += invslope2
    }
  }

  fun drawTriangleOutline(a: Point, b: Point, c: Point, colour: Int) {
    drawLine(a, b, colour)
    drawLine(b, c, colour)
    drawLine(c, a, colour)
  }

  fun splitIntoFlatTriangles(input: Triangle): Pair<Triangle, Triangle> {
    // Intersect the line AC with the horizontal line y = B
    val m = (input.c.y - input.a.y).toDouble() / (input.c.x - input.a.x)
    val x = (input.b.y - input.a.y) / m + input.a.x
    val d = Point(x.toInt(), input.b.y)

    val result = Pair(Triangle(input.a, input.b, d), Triangle(d, input.b, input.c))
    return result
  }
}

data class Triangle(val a: Point, val b: Point, val c: Point) {
  init {
    _require(a.y <= b.y && b.y <= c.y) { "Points must be sorted by y. Got: ${a.y}, ${b.y}, ${c.y}" }
  }

  companion object {
    fun sortedFromPoints(a: Point, b: Point, c: Point): Triangle {
      val sorted = listOf(a, b, c).sortedBy { it.y }
      return Triangle(sorted[0], sorted[1], sorted[2])
    }
  }
}

// We don't get proper logs with the usual `require()` so make a wrapper.
fun _require(condition: Boolean, error: () -> String) {
  if (!condition) {
    println(error())
  }
  require(condition, error)
}
