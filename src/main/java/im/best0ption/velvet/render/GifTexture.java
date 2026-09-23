package im.best0ption.velvet.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.resources.Identifier;

public final class GifTexture {

    private static final int MAX_SIDE = 512;

    private final GpuTextureView[] frames;
    private final int[] delays;
    private final long totalDelay;
    private final int loops;
    private long startMillis = -1L;

    private GifTexture(GpuTextureView[] frames, int[] delays, int loops) {
        this.frames = frames;
        this.delays = delays;
        long total = 0L;
        for (int delay : delays) {
            total += delay;
        }
        this.totalDelay = Math.max(1L, total);
        this.loops = loops;
    }

    public GpuTextureView currentFrame() {
        if (frames.length == 1) {
            return frames[0];
        }
        long now = System.currentTimeMillis();
        if (startMillis < 0L) {
            startMillis = now;
        }
        long elapsed = now - startMillis;
        if (loops > 0 && elapsed >= (long) (loops + 1) * totalDelay) {
            return frames[frames.length - 1];
        }
        long time = elapsed % totalDelay;
        long passed = 0L;
        for (int i = 0; i < delays.length; i++) {
            passed += delays[i];
            if (time < passed) {
                return frames[i];
            }
        }
        return frames[frames.length - 1];
    }

    public int frameCount() {
        return frames.length;
    }

    static GifTexture load(Identifier id, InputStream stream) throws IOException {
        GifDecoder.Decoded decoded = GifDecoder.decode(stream.readAllBytes());
        int[][] pixels = decoded.frames();
        int width = decoded.width();
        int height = decoded.height();
        GpuTextureView[] frames = new GpuTextureView[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            NativeImage image = toNativeImage(pixels[i], width, height);
            frames[i] = RenderUtils.uploadTextureWithMipmaps(id, RenderUtils.downscale(image, MAX_SIDE));
            pixels[i] = null;
        }
        return new GifTexture(frames, decoded.delaysMs(), decoded.loops());
    }

    private static NativeImage toNativeImage(int[] pixels, int width, int height) {
        NativeImage image = new NativeImage(width, height, true);
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                image.setPixel(x, y, pixels[offset + x]);
            }
        }
        return image;
    }
}
