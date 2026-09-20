package im.best0ption.velvet.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

public class RenderUtils {

    public static final VertexFormat params = VertexFormat.builder(0)
        .addAttribute("Position", GpuFormat.RGB32_FLOAT)
        .addAttribute("Color", GpuFormat.RGBA8_UNORM)
        .addAttribute("UV0", GpuFormat.RG32_FLOAT)
        .addAttribute("UV1", GpuFormat.RG16_SINT)
        .addAttribute("UV2", GpuFormat.RG16_SINT)
        .addAttribute("UV3", GpuFormat.RG32_FLOAT)
        .addAttribute("LineWidth", GpuFormat.R32_FLOAT)
        .build();

    public static final RenderPipeline rect = pipeline("pipeline/rectangle", "velvet:core/rectangle", false);
    public static final RenderPipeline border = pipeline("pipeline/border", "velvet:core/border", false);
    public static final RenderPipeline texture = pipeline("pipeline/texture", "velvet:core/texture", true);
    public static final RenderPipeline kawase = windowPipeline("pipeline/blur_window_kawase", "velvet:core/blur_window", new String[0], new float[0]);
    public static final RenderPipeline gaussian = windowPipeline("pipeline/blur_window_gauss", "velvet:core/blur_window", new String[0], new float[0]);
    public static final RenderPipeline liquid = windowPipeline(
        "pipeline/liquid_glass",
        "velvet:core/liquid_glass",
        new String[]{"EDGE", "REFRACT", "SATURATION"},
        new float[]{18.0F, 20.0F, 1.35F}
    );

    private static final Map<Identifier, GpuTextureView> textures = new Object2ObjectOpenHashMap<>();

    public enum BlurType {
        KAWASE,
        GAUSSIAN
    }

    public static BlurType blurType = BlurType.KAWASE;

    private static boolean blurMarked;
    private static boolean kawaseNeeded;
    private static boolean gaussNeeded;

    public static void drawBlur(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, int color) {
        markBlur(graphics);
        graphics.guiRenderState.addGuiElement(new RectRenderState(
            blurType == BlurType.KAWASE ? kawase : gaussian,
            TextureSetup.singleTexture(BlurEngine.resultView(), BlurEngine.sampler()),
            new Matrix3x2f(graphics.pose()),
            x, y, x + width, y + height, radius, 0.0F, color, graphics.scissorStack.peek()
        ));
    }

    public static void drawLiquidGlass(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, int color) {
        markBlur(graphics);
        graphics.guiRenderState.addGuiElement(new RectRenderState(
            liquid, TextureSetup.singleTexture(BlurEngine.resultView(), BlurEngine.sampler()),
            new Matrix3x2f(graphics.pose()),
            x, y, x + width, y + height, radius, 0.0F, color, graphics.scissorStack.peek()
        ));
    }

    private static void markBlur(GuiGraphicsExtractor graphics) {
        if (!blurMarked) {
            graphics.blurBeforeThisStratum();
            blurMarked = true;
        }
        if (blurType == BlurType.KAWASE) {
            kawaseNeeded = true;
        } else {
            gaussNeeded = true;
        }
    }

    public static boolean runBlurPasses() {
        blurMarked = false;
        if (!kawaseNeeded && !gaussNeeded) {
            return false;
        }
        boolean kawase = kawaseNeeded;
        boolean gauss = gaussNeeded;
        kawaseNeeded = false;
        gaussNeeded = false;
        BlurEngine.prepare(kawase, gauss);
        return true;
    }

    private static RenderPipeline pipeline(String location, String shader, boolean textured) {
        Identifier shaderId = Identifier.parse(shader);
        RenderPipeline.Builder builder = RenderPipeline.builder()
            .withLocation(location)
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withVertexShader(shaderId)
            .withFragmentShader(shaderId)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexBinding(0, params)
            .withPrimitiveTopology(PrimitiveTopology.QUADS);
        if (textured) {
            builder.withBindGroupLayout(BindGroupLayouts.SAMPLER0);
        }
        return builder.build();
    }

    private static RenderPipeline windowPipeline(String location, String fragment, String[] defineNames, float[] defineValues) {
        RenderPipeline.Builder builder = RenderPipeline.builder()
            .withLocation(location)
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withVertexShader(Identifier.parse("velvet:core/rectangle"))
            .withFragmentShader(Identifier.parse(fragment))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexBinding(0, params)
            .withPrimitiveTopology(PrimitiveTopology.QUADS);
        for (int i = 0; i < defineNames.length; i++) {
            builder.withShaderDefine(defineNames[i], defineValues[i]);
        }
        return builder.build();
    }

    public static int rgb(int r, int g, int b) {
        return rgba(r, g, b, 255);
    }

    public static int rgba(int r, int g, int b, int a) {
        return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    public static void drawRoundedRectangle(GuiGraphicsExtractor graphics, float x, float y, float width, float radius, float height, int color) {
        graphics.guiRenderState.addGuiElement(new RectRenderState(
            rect, TextureSetup.noTexture(), new Matrix3x2f(graphics.pose()),
            x, y, x + width, y + height, radius, 0.0F, color, graphics.scissorStack.peek()
        ));
    }

    public static void drawRoundedBorder(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, float thickness, int color) {
        graphics.guiRenderState.addGuiElement(new RectRenderState(
            border, TextureSetup.noTexture(), new Matrix3x2f(graphics.pose()),
            x, y, x + width, y + height, radius, thickness, color, graphics.scissorStack.peek()
        ));
    }

    public static void drawTexture(GuiGraphicsExtractor graphics, Identifier id, float x, float y, float width, float height, float radius, int color) {
        graphics.guiRenderState.addGuiElement(new TextureRectRenderState(
            texture, TextureSetup.singleTexture(getTexture(id), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)),
            new Matrix3x2f(graphics.pose()),
            x, y, x + width, y + height, 0.0F, 0.0F, 1.0F, 1.0F, width, height, radius, color, graphics.scissorStack.peek()
        ));
    }

    public static GpuTextureView getTexture(Identifier id) {
        GpuTextureView view = textures.get(id);
        if (view == null) {
            Identifier resource = Identifier.fromNamespaceAndPath(id.getNamespace(), id.getPath() + ".png");
            NativeImage image;
            try (InputStream stream = Minecraft.getInstance().getResourceManager().open(resource)) {
                image = NativeImage.read(stream);
            } catch (IOException e) {
                throw new RuntimeException("cannot load " + resource, e);
            }
            DynamicTexture texture = new DynamicTexture(() -> "" + id, image);
            Minecraft.getInstance().getTextureManager().register(id, texture);
            view = texture.getTextureView();
            textures.put(id, view);
        }
        return view;
    }

    public static void blurBeforeThis(GuiGraphicsExtractor graphics) {
        graphics.blurBeforeThisStratum();
    }

    private static final class BlurEngine {
        private static final int KAWASE_PASSES = 4;
        private static final RenderPipeline DOWNSAMPLE = passPipeline("pipeline/blur_downsample", "velvet:core/blur_sample", new String[0], new float[0]);
        private static final RenderPipeline[] KAWASE = {
            passPipeline("pipeline/kawase_1", "velvet:core/blur_kawase", new String[]{"OFFSET"}, new float[]{1.0F}),
            passPipeline("pipeline/kawase_2", "velvet:core/blur_kawase", new String[]{"OFFSET"}, new float[]{2.0F}),
            passPipeline("pipeline/kawase_3", "velvet:core/blur_kawase", new String[]{"OFFSET"}, new float[]{3.0F}),
            passPipeline("pipeline/kawase_4", "velvet:core/blur_kawase", new String[]{"OFFSET"}, new float[]{4.0F})
        };
        private static final RenderPipeline[] GAUSS = {
            passPipeline("pipeline/gauss_h1", "velvet:core/blur_gauss", new String[]{"DIR_X", "DIR_Y", "SCALE"}, new float[]{1.0F, 0.0F, 1.0F}),
            passPipeline("pipeline/gauss_v1", "velvet:core/blur_gauss", new String[]{"DIR_X", "DIR_Y", "SCALE"}, new float[]{0.0F, 1.0F, 1.0F}),
            passPipeline("pipeline/gauss_h2", "velvet:core/blur_gauss", new String[]{"DIR_X", "DIR_Y", "SCALE"}, new float[]{1.0F, 0.0F, 2.0F}),
            passPipeline("pipeline/gauss_v2", "velvet:core/blur_gauss", new String[]{"DIR_X", "DIR_Y", "SCALE"}, new float[]{0.0F, 1.0F, 2.0F})
        };
        private static int texW = -1;
        private static int texH = -1;
        private static GpuTexture kawaseA;
        private static GpuTexture kawaseB;
        private static GpuTexture gaussA;
        private static GpuTexture gaussB;
        private static GpuTextureView kawaseAView;
        private static GpuTextureView kawaseBView;
        private static GpuTextureView gaussAView;
        private static GpuTextureView gaussBView;
        private static GpuBuffer quad;
        private static GpuSampler sampler;

        private static RenderPipeline passPipeline(String location, String fragment, String[] defineNames, float[] defineValues) {
            RenderPipeline.Builder builder = RenderPipeline.builder()
                .withLocation(location)
                .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                .withVertexShader(Identifier.parse("velvet:core/blur_pass"))
                .withFragmentShader(Identifier.parse(fragment))
                .withColorTargetState(new ColorTargetState(Optional.empty(), GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_ALL))
                .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .withCull(false);
            for (int i = 0; i < defineNames.length; i++) {
                builder.withShaderDefine(defineNames[i], defineValues[i]);
            }
            return builder.build();
        }

        private static GpuTextureView resultView() {
            return blurType == BlurType.KAWASE ? kawaseAView : gaussAView;
        }

        private static GpuSampler sampler() {
            if (sampler == null) {
                sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
            }
            return sampler;
        }

        private static void prepare(boolean kawase, boolean gauss) {
            Minecraft minecraft = Minecraft.getInstance();
            int bw = Math.max(1, minecraft.getWindow().getWidth() / 2);
            int bh = Math.max(1, minecraft.getWindow().getHeight() / 2);
            if (bw != texW || bh != texH) {
                recreate(bw, bh);
            }
            if (quad == null) {
                createQuad();
            }
            GpuTextureView screen = minecraft.gameRenderer.mainRenderTarget().getColorTextureView();
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            if (kawase) {
                runChain(encoder, screen, kawaseAView, kawaseBView, KAWASE);
            }
            if (gauss) {
                runChain(encoder, screen, gaussAView, gaussBView, GAUSS);
            }
        }

        private static void runChain(CommandEncoder encoder, GpuTextureView screen, GpuTextureView a, GpuTextureView b, RenderPipeline[] passes) {
            blit(encoder, DOWNSAMPLE, a, screen);
            GpuTextureView src = a;
            GpuTextureView dst = b;
            for (RenderPipeline pass : passes) {
                blit(encoder, pass, dst, src);
                GpuTextureView tmp = src;
                src = dst;
                dst = tmp;
            }
        }

        private static void blit(CommandEncoder encoder, RenderPipeline pipeline, GpuTextureView target, GpuTextureView input) {
            try (RenderPass pass = encoder.createRenderPass(() -> "dssasad", target, Optional.empty())) {
                pass.setPipeline(RenderSystem.getCompiledPipeline(pipeline));
                pass.setVertexBuffer(0, quad.slice());
                pass.setUniform("Sampler0", input, sampler());
                pass.draw(6, 1, 0, 0);
            }
        }

        private static void recreate(int w, int h) {
            closeTextures();
            GpuDevice device = RenderSystem.getDevice();
            int usage = GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_DST;
            kawaseA = device.createTexture("velvet kawase a", usage, GpuFormat.RGBA8_UNORM, w, h, 1, 1);
            kawaseB = device.createTexture("velvet kawase b", usage, GpuFormat.RGBA8_UNORM, w, h, 1, 1);
            gaussA = device.createTexture("velvet gauss a", usage, GpuFormat.RGBA8_UNORM, w, h, 1, 1);
            gaussB = device.createTexture("velvet gauss b", usage, GpuFormat.RGBA8_UNORM, w, h, 1, 1);
            kawaseAView = device.createTextureView(kawaseA);
            kawaseBView = device.createTextureView(kawaseB);
            gaussAView = device.createTextureView(gaussA);
            gaussBView = device.createTextureView(gaussB);
            texW = w;
            texH = h;
        }

        private static void closeTextures() {
            if (kawaseA != null) {
                kawaseA.close();
                kawaseB.close();
                gaussA.close();
                gaussB.close();
                kawaseAView.close();
                kawaseBView.close();
                gaussAView.close();
                gaussBView.close();
            }
        }

        private static void createQuad() {
            ByteBuffer data = ByteBuffer.allocateDirect(120).order(ByteOrder.nativeOrder());
            float[] verts = {
                -1.0F, 1.0F, 0.0F, 0.0F, 0.0F,
                -1.0F, -1.0F, 0.0F, 0.0F, 1.0F,
                1.0F, -1.0F, 0.0F, 1.0F, 1.0F,
                -1.0F, 1.0F, 0.0F, 0.0F, 0.0F,
                1.0F, -1.0F, 0.0F, 1.0F, 1.0F,
                1.0F, 1.0F, 0.0F, 1.0F, 0.0F
            };
            for (float vert : verts) {
                data.putFloat(vert);
            }
            data.flip();
            quad = RenderSystem.getDevice().createBuffer(() -> "", GpuBuffer.USAGE_VERTEX, data);
        }
    }

    public static record RectRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2fc pose,
        float x0,
        float y0,
        float x1,
        float y1,
        float radius,
        float thickness,
        int color,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
    ) implements GuiElementRenderState {

        public RectRenderState(
            final RenderPipeline pipeline,
            final TextureSetup textureSetup,
            final Matrix3x2fc pose,
            final float x0,
            final float y0,
            final float x1,
            final float y1,
            final float radius,
            final float thickness,
            final int color,
            @Nullable final ScreenRectangle scissorArea
        ) {
            this(pipeline, textureSetup, pose, x0, y0, x1, y1, radius, thickness, color, scissorArea, null);
        }

        public RectRenderState {
            ScreenRectangle rect = new ScreenRectangle(
                Math.round(x0), Math.round(y0), Math.round(x1 - x0), Math.round(y1 - y0)
            ).transformMaxBounds(pose);
            bounds = scissorArea != null ? scissorArea.intersection(rect) : rect;
        }

        @Override
        public void buildVertices(VertexConsumer vertexConsumer) {
            float width = x1 - x0;
            float height = y1 - y0;
            int packedThickness = Math.max(0, Math.min(32000, (int) (thickness * 16.0F)));

            vertexConsumer.addVertexWith2DPose(pose, x0, y0).setColor(color).setUv(x0, y0).setUv1(0, 0).setUv2(packedThickness, 0).setUv3(width, height).setLineWidth(radius);
            vertexConsumer.addVertexWith2DPose(pose, x0, y1).setColor(color).setUv(x0, y0).setUv1(0, 0).setUv2(packedThickness, 0).setUv3(width, height).setLineWidth(radius);
            vertexConsumer.addVertexWith2DPose(pose, x1, y1).setColor(color).setUv(x0, y0).setUv1(0, 0).setUv2(packedThickness, 0).setUv3(width, height).setLineWidth(radius);
            vertexConsumer.addVertexWith2DPose(pose, x1, y0).setColor(color).setUv(x0, y0).setUv1(0, 0).setUv2(packedThickness, 0).setUv3(width, height).setLineWidth(radius);
        }
    }

    public static record TextureRectRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2fc pose,
        float x0,
        float y0,
        float x1,
        float y1,
        float u0,
        float v0,
        float u1,
        float v1,
        float paramA,
        float paramB,
        float lineWidth,
        int color,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
    ) implements GuiElementRenderState {

        public TextureRectRenderState(
            final RenderPipeline pipeline,
            final TextureSetup textureSetup,
            final Matrix3x2fc pose,
            final float x0,
            final float y0,
            final float x1,
            final float y1,
            final float u0,
            final float v0,
            final float u1,
            final float v1,
            final float paramA,
            final float paramB,
            final float lineWidth,
            final int color,
            @Nullable final ScreenRectangle scissorArea
        ) {
            this(pipeline, textureSetup, pose, x0, y0, x1, y1, u0, v0, u1, v1, paramA, paramB, lineWidth, color, scissorArea, null);
        }

        public TextureRectRenderState {
            ScreenRectangle rect = new ScreenRectangle(
                Math.round(x0), Math.round(y0), Math.round(x1 - x0), Math.round(y1 - y0)
            ).transformMaxBounds(pose);
            bounds = scissorArea != null ? scissorArea.intersection(rect) : rect;
        }

        @Override
        public void buildVertices(VertexConsumer vertexConsumer) {
            vertexConsumer.addVertexWith2DPose(pose, x0, y0).setColor(color).setUv(u0, v0).setUv1(0, 0).setUv2(0, 0).setUv3(paramA, paramB).setLineWidth(lineWidth);
            vertexConsumer.addVertexWith2DPose(pose, x0, y1).setColor(color).setUv(u0, v1).setUv1(0, 0).setUv2(0, 0).setUv3(paramA, paramB).setLineWidth(lineWidth);
            vertexConsumer.addVertexWith2DPose(pose, x1, y1).setColor(color).setUv(u1, v1).setUv1(0, 0).setUv2(0, 0).setUv3(paramA, paramB).setLineWidth(lineWidth);
            vertexConsumer.addVertexWith2DPose(pose, x1, y0).setColor(color).setUv(u1, v0).setUv1(0, 0).setUv2(0, 0).setUv3(paramA, paramB).setLineWidth(lineWidth);
        }
    }
}
