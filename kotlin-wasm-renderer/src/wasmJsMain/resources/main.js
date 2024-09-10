const BYTES_PER_COLOUR = 4; // RGBA
const WIDTH = 640;
const HEIGHT = 480;
const SCALE = 1;
const SCALE_SQUARED = SCALE * SCALE;
const LOG_DRAW_TIME = false;

const buffer = new ImageData(WIDTH * SCALE, HEIGHT * SCALE);
const utf8Encode = new TextEncoder();

let kotlin = null;

const init = () => {
  console.log("Initialized JS");

  const canvas = document.getElementById("canvas");
  canvas.style.height = HEIGHT + "px";
  canvas.style.width = WIDTH + "px";
  const ctx = canvas.getContext("bitmaprenderer");

  // Disable anti-aliasing.
  ctx.imageSmoothingEnabled = false;

  draw(ctx, buffer);
  setInterval(() => kotlin.update(), 1000);
};

const loadPixels = () => {
  const now = performance.now();
  for (let y = 0; y < HEIGHT; y++) {
    for (let x = 0; x < WIDTH; x++) {
      const pixel = kotlin.getPixel(x, y);
      const pixelOffset = (y * WIDTH + x) * BYTES_PER_COLOUR * SCALE;
      const r = pixel & 0xff;
      const g = (pixel >> 8) & 0xff;
      const b = (pixel >> 16) & 0xff;

      for (let i = 0; i < SCALE; i++) {
        const rowOffset = i * WIDTH * BYTES_PER_COLOUR * SCALE;
        for (let j = 0; j < SCALE; j++) {
          const subpixelOffset = pixelOffset + rowOffset + j * BYTES_PER_COLOUR;
          buffer.data[subpixelOffset] = r;
          buffer.data[subpixelOffset + 1] = g;
          buffer.data[subpixelOffset + 2] = b;
          // Ignore alpha channel.
          buffer.data[subpixelOffset + 3] = 255;
        }
      }
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
};

window["kotlin-wasm-renderer"].then((module) => {
  kotlin = module;
});

document.addEventListener("DOMContentLoaded", () =>
  window["kotlin-wasm-renderer"].then(init)
);
