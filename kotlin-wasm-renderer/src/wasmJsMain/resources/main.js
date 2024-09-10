const BYTES_PER_COLOUR = 4; // RGBA
const WIDTH = 640;
const HEIGHT = 480;
const SCALE = 2;
const LOG_DRAW_TIME = false;

const buffer = new ImageData(WIDTH, HEIGHT);
const utf8Encode = new TextEncoder();

let kotlin = null;

const init = () => {
  console.log("Initialized JS");

  const canvas = document.getElementById("canvas");
  canvas.style.height = HEIGHT * SCALE + "px";
  canvas.style.width = WIDTH * SCALE + "px";
  const ctx = canvas.getContext("bitmaprenderer");

  // Disable anti-aliasing.
  ctx.imageSmoothingEnabled = false;

  draw(ctx, buffer);
};

const loadPixels = () => {
  const now = performance.now();
  for (let y = 0; y < HEIGHT; y++) {
    for (let x = 0; x < WIDTH; x++) {
      const offset = (y * WIDTH + x) * BYTES_PER_COLOUR;
      const pixel = kotlin.getPixel(x, y);
      buffer.data[offset] = pixel & 0xff;
      buffer.data[offset + 1] = (pixel >> 8) & 0xff;
      buffer.data[offset + 2] = (pixel >> 16) & 0xff;
      // Ignore alpha channel.
      buffer.data[offset + 3] = 255;
    }
  }
  if (LOG_DRAW_TIME) {
    console.log("Kt -> JS time:", (performance.now() - now).toFixed(3), "ms");
  }
};

const draw = (ctx, buffer) => {
  loadPixels(buffer);

  const now = performance.now();
  const bitmapPromise = createImageBitmap(buffer);
  bitmapPromise.then((bitmap) => {
    ctx.transferFromImageBitmap(bitmap);
    if (LOG_DRAW_TIME) {
      console.log(
        "JS -> bitmap time:",
        (performance.now() - now).toFixed(3),
        "ms"
      );
    }
  });

  requestAnimationFrame(() => draw(ctx, buffer));
  // setTimeout(() => draw(ctx, buffer), 1000);
};

window["kotlin-wasm-renderer"].then((module) => {
  kotlin = module;
});

document.addEventListener("DOMContentLoaded", () =>
  window["kotlin-wasm-renderer"].then(init)
);
