package reika.dragonapi.instantiable.rendering;

import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Free-form 2D shapes for the 26.2 GUI layer, which itself draws only axis-aligned rectangles and blits: the
 * Tessellator triangles, line loops and arbitrary quads 1.7.10 HUD code drew with {@code GL_TRIANGLE_STRIP},
 * {@code GL_LINE_LOOP} and {@code startDrawingQuads}. Each shape is a quad on the GUI pipeline (a triangle repeats its
 * last corner; a line is a quad of the given width), submitted through NeoForge's
 * {@code submitGuiElementRenderState} with the graphics' current pose and scissor.
 */
public final class GuiShapes {

	private GuiShapes() {}

	/** A filled triangle, one colour per corner. */
	public static void triangle(GuiGraphicsExtractor g, float x1, float y1, float x2, float y2, float x3, float y3, int c1,
			int c2, int c3) {
		quad(g, x1, y1, x2, y2, x3, y3, x3, y3, c1, c2, c3, c3);
	}

	public static void triangle(GuiGraphicsExtractor g, float x1, float y1, float x2, float y2, float x3, float y3, int color) {
		triangle(g, x1, y1, x2, y2, x3, y3, color, color, color);
	}

	/** A filled quad with its corners in drawing order, one colour per corner. */
	public static void quad(GuiGraphicsExtractor g, float x1, float y1, float x2, float y2, float x3, float y3, float x4,
			float y4, int c1, int c2, int c3, int c4) {
		g.submitGuiElementRenderState(new ColoredQuad(RenderPipelines.GUI, TextureSetup.noTexture(), new Matrix3x2f(g.pose()),
				new float[] {x1, y1, x2, y2, x3, y3, x4, y4}, new int[] {c1, c2, c3, c4}, g.peekScissorStack()));
	}

	/** A straight line {@code width} GUI pixels thick, its colour shading from one end to the other. */
	public static void line(GuiGraphicsExtractor g, float x1, float y1, float x2, float y2, float width, int c1, int c2) {
		float dx = x2-x1;
		float dy = y2-y1;
		float len = (float)Math.sqrt(dx*dx+dy*dy);
		if (len <= 0)
			return;
		float nx = -dy/len*width/2;
		float ny = dx/len*width/2;
		quad(g, x1+nx, y1+ny, x1-nx, y1-ny, x2-nx, y2-ny, x2+nx, y2+ny, c1, c1, c2, c2);
	}

	/** A closed outline through the points ({@code x0, y0, x1, y1, ...}), each segment shading between its corners' colours. */
	public static void lineLoop(GuiGraphicsExtractor g, float[] points, int[] colors, float width) {
		int n = points.length/2;
		for (int i = 0; i < n; i++) {
			int k = (i+1)%n;
			line(g, points[i*2], points[i*2+1], points[k*2], points[k*2+1], width, colors[i%colors.length],
					colors[k%colors.length]);
		}
	}

	/** A quad textured from an atlas sprite, tinted, on the given GUI pipeline (plain or premultiplied/additive). */
	public static void sprite(GuiGraphicsExtractor g, RenderPipeline pipeline, TextureAtlasSprite sprite, float x0, float y0,
			float x1, float y1, int color) {
		AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(sprite.atlasLocation());
		g.submitGuiElementRenderState(new TexturedQuad(pipeline,
				TextureSetup.singleTexture(texture.getTextureView(), texture.getSampler()), new Matrix3x2f(g.pose()),
				new float[] {x0, y1, x1, y1, x1, y0, x0, y0},
				new float[] {sprite.getU0(), sprite.getV1(), sprite.getU1(), sprite.getV1(), sprite.getU1(), sprite.getV0(),
						sprite.getU0(), sprite.getV0()}, color, g.peekScissorStack()));
	}

	/** A quad from a standalone texture, with explicit UVs (0-1), tinted. */
	public static void texture(GuiGraphicsExtractor g, RenderPipeline pipeline, net.minecraft.resources.Identifier location,
			float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1, int color) {
		AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(location);
		g.submitGuiElementRenderState(new TexturedQuad(pipeline,
				TextureSetup.singleTexture(texture.getTextureView(), texture.getSampler()), new Matrix3x2f(g.pose()),
				new float[] {x0, y1, x1, y1, x1, y0, x0, y0}, new float[] {u0, v1, u1, v1, u1, v0, u0, v0}, color,
				g.peekScissorStack()));
	}

	private static @Nullable ScreenRectangle bounds(float[] xy, Matrix3x2fc pose, @Nullable ScreenRectangle scissor) {
		float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
		float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
		for (int i = 0; i < xy.length; i += 2) {
			minX = Math.min(minX, xy[i]);
			maxX = Math.max(maxX, xy[i]);
			minY = Math.min(minY, xy[i+1]);
			maxY = Math.max(maxY, xy[i+1]);
		}
		int x = (int)Math.floor(minX);
		int y = (int)Math.floor(minY);
		ScreenRectangle rect = new ScreenRectangle(x, y, (int)Math.ceil(maxX)-x+1, (int)Math.ceil(maxY)-y+1)
				.transformMaxBounds(pose);
		return scissor != null ? scissor.intersection(rect) : rect;
	}

	private record ColoredQuad(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2fc pose, float[] xy,
			int[] colors, @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds)
			implements GuiElementRenderState {

		private ColoredQuad(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2fc pose, float[] xy, int[] colors,
				@Nullable ScreenRectangle scissorArea) {
			this(pipeline, textureSetup, pose, xy, colors, scissorArea, GuiShapes.bounds(xy, pose, scissorArea));
		}

		@Override
		public void buildVertices(VertexConsumer vertexConsumer) {
			for (int i = 0; i < 4; i++)
				vertexConsumer.addVertexWith2DPose(pose, xy[i*2], xy[i*2+1]).setColor(colors[i]);
		}
	}

	private record TexturedQuad(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2fc pose, float[] xy,
			float[] uv, int color, @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds)
			implements GuiElementRenderState {

		private TexturedQuad(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2fc pose, float[] xy, float[] uv,
				int color, @Nullable ScreenRectangle scissorArea) {
			this(pipeline, textureSetup, pose, xy, uv, color, scissorArea, GuiShapes.bounds(xy, pose, scissorArea));
		}

		@Override
		public void buildVertices(VertexConsumer vertexConsumer) {
			for (int i = 0; i < 4; i++)
				vertexConsumer.addVertexWith2DPose(pose, xy[i*2], xy[i*2+1]).setUv(uv[i*2], uv[i*2+1]).setColor(color);
		}
	}

}
