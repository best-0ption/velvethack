package im.best0ption.velvet.render.font;

import com.google.gson.Gson;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import im.best0ption.velvet.render.RenderUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;

public class MsdfFont {

    private static final Gson GSON = new Gson();
    private static final float SMOOTHNESS = 1.0F;
    private static final float OUTLINE_THICKNESS = 0.15F;

    private final Map<Character, Glyph> glyphs = new HashMap<>();
    private final float distanceRange;
    private final float ascender;
    private final float descender;
    private final float atlasWidth;
    private final float atlasHeight;
    private final GpuTextureView atlas;
    private final GpuSampler sampler;
    private final RenderPipeline pipeline;
    private final RenderPipeline outlinePipeline;

    public MsdfFont(String name) {
        AtlasData data;
        Identifier jsonId = Identifier.fromNamespaceAndPath("velvet", "fonts/" + name + ".json");
        try (InputStream stream = Minecraft.getInstance().getResourceManager().open(jsonId)) {
            data = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), AtlasData.class);
        } catch (IOException e) {
            throw new RuntimeException("cannot read " + jsonId, e);
        }

        this.distanceRange = data.atlas.distanceRange;
        this.ascender = data.metrics.ascender;
        this.descender = data.metrics.descender;
        this.atlasWidth = data.atlas.width;
        this.atlasHeight = data.atlas.height;
        for (GlyphData glyphData : data.glyphs) {
            glyphs.put((char) glyphData.unicode, new Glyph(glyphData.advance, glyphData.planeBounds, glyphData.atlasBounds));
        }

        Identifier pngId = Identifier.fromNamespaceAndPath("velvet", "fonts/" + name + ".png");
        NativeImage image;
        try (InputStream stream = Minecraft.getInstance().getResourceManager().open(pngId)) {
            image = NativeImage.read(stream);
        } catch (IOException e) {
            throw new RuntimeException("cannot read  " + pngId, e);
        }
        DynamicTexture texture = new DynamicTexture(() -> "" + name, image);
        Minecraft.getInstance().getTextureManager().register(Identifier.fromNamespaceAndPath("velvet", "fonts/" + name), texture);
        this.atlas = texture.getTextureView();
        this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        this.pipeline = buildPipeline("pipeline/msdf_" + name, false);
        this.outlinePipeline = buildPipeline("pipeline/msdf_" + name + "_outline", true);
    }

    private RenderPipeline buildPipeline(String location, boolean outline) {
        RenderPipeline.Builder builder = RenderPipeline.builder()
            .withLocation(location)
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withVertexShader(Identifier.parse("velvet:core/msdf_font"))
            .withFragmentShader(Identifier.parse("velvet:core/msdf_font"))
            .withShaderDefine("RANGE", distanceRange)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexBinding(0, RenderUtils.params)
            .withPrimitiveTopology(PrimitiveTopology.QUADS);
        if (outline) {
            builder.withShaderDefine("OUTLINE");
            builder.withShaderDefine("OUTLINE_R", 0.0F);
            builder.withShaderDefine("OUTLINE_G", 0.0F);
            builder.withShaderDefine("OUTLINE_B", 0.0F);
        }
        return builder.build();
    }

    public void drawDefaultString(GuiGraphicsExtractor graphics, String text, float x, float y, float size, int color) {
        drawStringThickness(graphics, text, x, y, 0.0F, size, color);
    }

    public void drawCenteredDefaultString(GuiGraphicsExtractor graphics, String text, float x, float y, float size, int color) {
        drawDefaultString(graphics, text, x - getWidth(text, size) / 2.0F, y, size, color);
    }

    public void drawStringThickness(GuiGraphicsExtractor graphics, String text, float x, float y, float thickness, float size, int color) {
        drawString(graphics, text, x, y, thickness, size, color, false);
    }

    public void drawStringOutlined(GuiGraphicsExtractor graphics, String text, float x, float y, float size, int color) {
        drawString(graphics, text, x, y, 0.0F, size, color, true);
    }

    public float getWidth(String text, float size) {
        float width = 0.0F;
        for (int i = 0; i < text.length(); i++) {
            Glyph glyph = glyphs.get(text.charAt(i));
            if (glyph != null) {
                width += glyph.advance() * size;
            }
        }
        return width;
    }

    public float getHeight(float size) {
        return (ascender - descender) * size;
    }

    private void drawString(GuiGraphicsExtractor graphics, String text, float x, float y, float thickness, float size, int color, boolean outline) {
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor = graphics.scissorStack.peek();
        RenderPipeline renderPipeline = outline ? outlinePipeline : pipeline;
        float lineWidth = outline ? OUTLINE_THICKNESS : 0.0F;
        float penX = x;
        float baseline = y + ascender * size;

        for (int i = 0; i < text.length(); i++) {
            Glyph glyph = glyphs.get(text.charAt(i));
            if (glyph != null && glyph.planeBounds() != null) {
                float gx0 = penX + glyph.planeBounds().left() * size;
                float gy0 = baseline - glyph.planeBounds().top() * size;
                float gx1 = penX + glyph.planeBounds().right() * size;
                float gy1 = baseline - glyph.planeBounds().bottom() * size;
                float u0 = glyph.atlasBounds().left() / atlasWidth;
                float u1 = glyph.atlasBounds().right() / atlasWidth;
                float v0 = 1.0F - glyph.atlasBounds().top() / atlasHeight;
                float v1 = 1.0F - glyph.atlasBounds().bottom() / atlasHeight;

                graphics.guiRenderState.addGuiElement(new RenderUtils.TextureRectRenderState(
                    renderPipeline, TextureSetup.singleTexture(atlas, sampler), pose,
                    gx0, gy0, gx1, gy1, u0, v0, u1, v1, thickness, SMOOTHNESS, lineWidth, color, scissor
                ));
            }
            if (glyph != null) {
                penX += glyph.advance() * size;
            }
        }
    }

    private record Glyph(float advance, PlaneBounds planeBounds, AtlasBounds atlasBounds) {
    }

    private record PlaneBounds(float left, float bottom, float right, float top) {
    }

    private record AtlasBounds(float left, float bottom, float right, float top) {
    }

    private static class AtlasData {
        Atlas atlas;
        Metrics metrics;
        List<GlyphData> glyphs;
    }

    private static class Atlas {
        float distanceRange;
        float width;
        float height;
    }

    private static class Metrics {
        float ascender;
        float descender;
    }

    private static class GlyphData {
        int unicode;
        float advance;
        PlaneBounds planeBounds;
        AtlasBounds atlasBounds;
    }
}
