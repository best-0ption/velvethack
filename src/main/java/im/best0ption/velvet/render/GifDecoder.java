package im.best0ption.velvet.render;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import org.w3c.dom.NodeList;

public final class GifDecoder {

    private static final int DISPOSE_NONE = 0;
    private static final int DISPOSE_BACKGROUND = 1;
    private static final int DISPOSE_PREVIOUS = 2;

    private GifDecoder() {
    }

    public record Decoded(int width, int height, int[][] frames, int[] delaysMs, int loops) {
    }

    public static Decoded decode(byte[] data) throws IOException {
        int loops = readLoopCount(data);
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            if (input == null) {
                throw new IOException("cannot read gif");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IOException("no gif decoder");
            }
            ImageReader reader = readers.next();
            try {
                if (!"gif".equalsIgnoreCase(reader.getFormatName())) {
                    throw new IOException("stream is not a gif");
                }
                reader.setInput(input, false, false);
                int count = reader.getNumImages(true);
                if (count <= 0) {
                    throw new IOException("empty gif");
                }

                int[] left = new int[count];
                int[] top = new int[count];
                int[] width = new int[count];
                int[] height = new int[count];
                int[] disposal = new int[count];
                int[] delay = new int[count];
                int canvasWidth = 0;
                int canvasHeight = 0;

                for (int i = 0; i < count; i++) {
                    IIOMetadata metadata = reader.getImageMetadata(i);
                    IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metadata.getNativeMetadataFormatName());
                    IIOMetadataNode descriptor = find(root, "ImageDescriptor");
                    IIOMetadataNode control = find(root, "GraphicControlExtension");
                    left[i] = attr(descriptor, "imageLeftPosition", 0);
                    top[i] = attr(descriptor, "imageTopPosition", 0);
                    width[i] = reader.getWidth(i);
                    height[i] = reader.getHeight(i);
                    disposal[i] = parseDisposal(control == null ? null : control.getAttribute("disposalMethod"));
                    delay[i] = parseDelay(control == null ? -1 : attr(control, "delayTime", -1));
                    canvasWidth = Math.max(canvasWidth, left[i] + width[i]);
                    canvasHeight = Math.max(canvasHeight, top[i] + height[i]);
                }

                int[] screen = readScreenSize(reader);
                if (screen != null) {
                    canvasWidth = Math.max(canvasWidth, screen[0]);
                    canvasHeight = Math.max(canvasHeight, screen[1]);
                }
                if (canvasWidth <= 0 || canvasHeight <= 0) {
                    throw new IOException("cannot detect gif size");
                }

                int[] canvas = new int[canvasWidth * canvasHeight];
                int[] snapshot = null;
                int[][] frames = new int[count][];
                for (int i = 0; i < count; i++) {
                    if (i > 0 && disposal[i - 1] == DISPOSE_BACKGROUND) {
                        clearRect(canvas, canvasWidth, left[i - 1], top[i - 1], width[i - 1], height[i - 1]);
                    } else if (i > 0 && disposal[i - 1] == DISPOSE_PREVIOUS && snapshot != null) {
                        System.arraycopy(snapshot, 0, canvas, 0, canvas.length);
                    }
                    if (disposal[i] == DISPOSE_PREVIOUS) {
                        if (snapshot == null) {
                            snapshot = new int[canvas.length];
                        }
                        System.arraycopy(canvas, 0, snapshot, 0, canvas.length);
                    }

                    blit(reader.read(i), canvas, canvasWidth, canvasHeight, left[i], top[i]);
                    frames[i] = canvas.clone();
                }
                return new Decoded(canvasWidth, canvasHeight, frames, delay, loops);
            } finally {
                reader.dispose();
            }
        }
    }

    private static void blit(BufferedImage frame, int[] canvas, int canvasWidth, int canvasHeight, int left, int top) {
        int width = frame.getWidth();
        int height = frame.getHeight();
        if (frame.getColorModel() instanceof IndexColorModel colors && frame.getRaster().getNumBands() == 1) {
            int transparent = colors.getTransparentPixel();
            Raster raster = frame.getRaster();
            int[] row = new int[width];
            for (int y = 0; y < height; y++) {
                int cy = top + y;
                if (cy < 0 || cy >= canvasHeight) {
                    continue;
                }
                raster.getSamples(0, y, width, 1, 0, row);
                for (int x = 0; x < width; x++) {
                    int index = row[x];
                    if (index == transparent) {
                        continue;
                    }
                    int cx = left + x;
                    if (cx < 0 || cx >= canvasWidth) {
                        continue;
                    }
                    canvas[cy * canvasWidth + cx] = colors.getRGB(index);
                }
            }
            return;
        }
        int[] argb = frame.getRGB(0, 0, width, height, null, 0, width);
        for (int y = 0; y < height; y++) {
            int cy = top + y;
            if (cy < 0 || cy >= canvasHeight) {
                continue;
            }
            for (int x = 0; x < width; x++) {
                int pixel = argb[y * width + x];
                if ((pixel >>> 24) == 0) {
                    continue;
                }
                int cx = left + x;
                if (cx < 0 || cx >= canvasWidth) {
                    continue;
                }
                canvas[cy * canvasWidth + cx] = pixel;
            }
        }
    }

    private static void clearRect(int[] canvas, int width, int x0, int y0, int w, int h) {
        int height = canvas.length / width;
        int fromX = Math.max(0, x0);
        int toX = Math.min(width, x0 + w);
        if (fromX >= toX) {
            return;
        }
        for (int y = Math.max(0, y0); y < Math.min(height, y0 + h); y++) {
            Arrays.fill(canvas, y * width + fromX, y * width + toX, 0);
        }
    }

    private static int parseDisposal(String value) {
        if ("restoreToBackgroundColor".equals(value)) {
            return DISPOSE_BACKGROUND;
        }
        if ("restoreToPrevious".equals(value)) {
            return DISPOSE_PREVIOUS;
        }
        return DISPOSE_NONE;
    }

    private static int parseDelay(int centiseconds) {
        if (centiseconds <= 0) {
            return 100;
        }
        return centiseconds * 10;
    }

    private static IIOMetadataNode find(IIOMetadataNode root, String name) {
        NodeList nodes = root.getElementsByTagName(name);
        return nodes.getLength() == 0 ? null : (IIOMetadataNode) nodes.item(0);
    }

    private static int attr(IIOMetadataNode node, String name, int fallback) {
        if (node == null) {
            return fallback;
        }
        String value = node.getAttribute(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int[] readScreenSize(ImageReader reader) {
        try {
            return readScreenSize0(reader);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static int[] readScreenSize0(ImageReader reader) throws IOException {
        IIOMetadata metadata = reader.getStreamMetadata();
        if (metadata == null) {
            return null;
        }
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metadata.getNativeMetadataFormatName());
        NodeList nodes = root.getElementsByTagName("LogicalScreenDescriptor");
        if (nodes.getLength() == 0) {
            return null;
        }
        IIOMetadataNode screen = (IIOMetadataNode) nodes.item(0);
        int width = attr(screen, "logicalScreenWidth", 0);
        int height = attr(screen, "logicalScreenHeight", 0);
        return width > 0 && height > 0 ? new int[]{width, height} : null;
    }

    private static int readLoopCount(byte[] data) {
        for (int i = 0; i + 14 <= data.length; i++) {
            if (data[i] != 'N' || data[i + 1] != 'E' || data[i + 2] != 'T' || data[i + 3] != 'S'
                || data[i + 4] != 'C' || data[i + 5] != 'A' || data[i + 6] != 'P' || data[i + 7] != 'E'
                || data[i + 8] != '2' || data[i + 9] != '.' || data[i + 10] != '0') {
                continue;
            }
            int p = i + 11;
            if ((data[p] & 0xFF) == 3 && (data[p + 1] & 0xFF) == 1) {
                return (data[p + 2] & 0xFF) | ((data[p + 3] & 0xFF) << 8);
            }
        }
        return 0;
    }
}
