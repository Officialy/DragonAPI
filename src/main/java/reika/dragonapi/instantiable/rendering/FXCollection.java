/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.rendering;

import java.util.ArrayList;
import java.util.Iterator;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * V33a {@code FXCollection}: particles owned by a tile and drawn by its renderer rather than the particle engine --
 * camera-facing sprites at positions relative to the tile, fading in size over their life (a sine swell, or the
 * rapid-expand envelope). Client only; {@link #update} once a client tick, {@link #render} from the renderer.
 */
public class FXCollection {

	private final ArrayList<BasicFX> data = new ArrayList<>();

	public FXCollection() {

	}

	public void addEffect(double x, double y, double z, TextureAtlasSprite ico, int life, float size, int color) {
		this.addEffect(x, y, z, ico, life, size, color, false);
	}

	public void addEffect(double x, double y, double z, TextureAtlasSprite ico, int life, float size, int color, boolean rapidExpand) {
		data.add(new BasicFX(x, y, z, ico, life, size, color, rapidExpand));
	}

	public void addEffectWithVelocity(double x, double y, double z, double vx, double vy, double vz, TextureAtlasSprite ico, int life, float size, int color, boolean rapidExpand) {
		data.add(new MovingBasicFX(x, y, z, ico, life, size, color, rapidExpand, vx, vy, vz));
	}

	public boolean isEmpty() {
		return data.isEmpty();
	}

	public void update() {
		Iterator<BasicFX> it = data.iterator();
		while (it.hasNext()) {
			BasicFX fx = it.next();
			if (fx.update()) {
				it.remove();
			}
		}
	}

	/**
	 * Emits every effect as a camera-facing quad of position/UV/colour vertices, in the tile's pose; the caller picks
	 * the render type (V33a: additive-dark or default blend, fullbright, no depth write).
	 */
	public void render(VertexConsumer v5, PoseStack.Pose pose, Quaternionf cameraOrientation) {
		Vector3f right = cameraOrientation.transform(new Vector3f(1, 0, 0));
		Vector3f up = cameraOrientation.transform(new Vector3f(0, 1, 0));
		for (BasicFX fx : data) {
			fx.render(v5, pose, right, up);
		}
	}

	private static class BasicFX {

		protected double posX;
		protected double posY;
		protected double posZ;

		private final int lifespan;
		private final int renderColor;
		private final float size;

		private final boolean rapidExpand;

		private final TextureAtlasSprite icon;

		private int ticks;

		private BasicFX(double x, double y, double z, TextureAtlasSprite ico, int life, float size, int color, boolean rapid) {
			posX = x;
			posY = y;
			posZ = z;
			lifespan = life;
			renderColor = color;
			this.size = size;
			icon = ico;
			rapidExpand = rapid;
		}

		public boolean update() {
			ticks++;
			return ticks >= lifespan;
		}

		private void render(VertexConsumer v5, PoseStack.Pose pose, Vector3f right, Vector3f up) {
			// V33a's integer division in the rapid-expand test is kept.
			double fs = rapidExpand ? 0.1*size*(lifespan/(ticks+1) >= 12 ? (ticks+1)*12D/lifespan : 1-(ticks+1)/(double)lifespan) : 0.1*size*Math.sin(Math.toRadians(180D*ticks/lifespan));
			float f = (float)fs;
			int color = 0xff000000 | renderColor;

			float u = icon.getU0();
			float v = icon.getV0();
			float du = icon.getU1();
			float dv = icon.getV1();

			this.vertex(v5, pose, -right.x*f-up.x*f, -right.y*f-up.y*f, -right.z*f-up.z*f, du, dv, color);
			this.vertex(v5, pose, -right.x*f+up.x*f, -right.y*f+up.y*f, -right.z*f+up.z*f, du, v, color);
			this.vertex(v5, pose, right.x*f+up.x*f, right.y*f+up.y*f, right.z*f+up.z*f, u, v, color);
			this.vertex(v5, pose, right.x*f-up.x*f, right.y*f-up.y*f, right.z*f-up.z*f, u, dv, color);
		}

		private void vertex(VertexConsumer v5, PoseStack.Pose pose, float dx, float dy, float dz, float u, float v, int color) {
			v5.addVertex(pose, (float)posX+dx, (float)posY+dy, (float)posZ+dz).setUv(u, v).setColor(color);
		}

	}

	private static class MovingBasicFX extends BasicFX {

		private final double motionX;
		private final double motionY;
		private final double motionZ;

		private MovingBasicFX(double x, double y, double z, TextureAtlasSprite ico, int life, float size, int color, boolean rapid, double vx, double vy, double vz) {
			super(x, y, z, ico, life, size, color, rapid);

			motionX = vx;
			motionY = vy;
			motionZ = vz;
		}

		@Override
		public boolean update() {
			posX += motionX;
			posY += motionY;
			posZ += motionZ;
			return super.update();
		}

	}

}
