import kotlin.random.Random

const val WIDTH = 640
const val HEIGHT = 480

fun main() {
  println("Loaded Kotlin/WASM module")
}

val renderer = Renderer()

@JsExport fun getPixel(x: Int, y: Int): Int = renderer.buffer[y * WIDTH + x]

data class Point(val x: Int, val y: Int)

fun makeColour(r: Int, g: Int, b: Int): Int {
  val alpha = 0xFF000000.toInt()
  return alpha or (r shl 16) or (g shl 8) or b
}

class Renderer {
  val buffer = IntArray(WIDTH * HEIGHT)

  init {
    for (y in 0 until HEIGHT) {
      for (x in 0 until WIDTH) {
        val pixel = makeColour(0, 0, 0)
        val offset = y * WIDTH + x
        buffer[offset] = pixel
      }
    }

    // renderTestLines()

    for (i in 0..5) {
      val a = randomPoint()
      val b = randomPoint()
      val c = randomPoint()
      drawTriangleOutline(a, b, c, randomColour())
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
      throw IllegalArgumentException("Pixel out of bounds")
    }

    println("Setting pixel at $x, $y to $colour")

    buffer[y * WIDTH + x] = colour
  }

  fun drawLine(start: Point, end: Point, colour: Int) {
    if (kotlin.math.abs(end.y - start.y) < kotlin.math.abs(end.x - start.x)) {
      if (start.x > end.x) {
        println("DrawLine1")
        drawLineLow(end, start, colour)
      } else {
        println("DrawLine2")
        drawLineLow(start, end, colour)
      }
    } else {
      if (start.y > end.y) {
        println("DrawLine3")
        drawLineHigh(end, start, colour)
      } else {
        println("DrawLine4")
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
    println("DrawLineHigh from $start to $end")

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

  fun drawTriangleOutline(a: Point, b: Point, c: Point, colour: Int) {
    drawLine(a, b, colour)
    drawLine(b, c, colour)
    drawLine(c, a, colour)
  }
}
