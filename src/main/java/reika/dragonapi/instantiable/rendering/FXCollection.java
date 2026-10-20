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
    public void render(VertexConsumer vertices, PoseStack.Pose pose, Quaternionf cameraOrientation) {
        snapshot().render(vertices, pose, cameraOrientation);
    }

    /** Capture on the collection's client tick/extraction owner before queuing deferred work. */
    public Snapshot snapshot() {
        var effects = new ArrayList<Effect>(data.size());
        for (var fx : data) effects.add(fx.snapshot());
        return new Snapshot(effects);
    }

    /** Geometry owns scalar positions, envelope and UVs; it retains no live effects or sprites. */
    public record Effect(double x, double y, double z, float radius, int color,
            float u0, float v0, float u1, float v1) {
        private void render(VertexConsumer out, PoseStack.Pose pose, Vector3f right, Vector3f up) {
            vertex(out,pose,-right.x*radius-up.x*radius,-right.y*radius-up.y*radius,-right.z*radius-up.z*radius,u1,v1);
            vertex(out,pose,-right.x*radius+up.x*radius,-right.y*radius+up.y*radius,-right.z*radius+up.z*radius,u1,v0);
            vertex(out,pose,right.x*radius+up.x*radius,right.y*radius+up.y*radius,right.z*radius+up.z*radius,u0,v0);
            vertex(out,pose,right.x*radius-up.x*radius,right.y*radius-up.y*radius,right.z*radius-up.z*radius,u0,v1);
        }
        private void vertex(VertexConsumer out,PoseStack.Pose pose,float dx,float dy,float dz,float u,float v) {
            out.addVertex(pose,(float)x+dx,(float)y+dy,(float)z+dz).setUv(u,v).setColor(color);
        }
    }
    public record Snapshot(java.util.List<Effect> effects) {
        public static final Snapshot EMPTY = new Snapshot(java.util.List.of());
        public Snapshot { effects = java.util.List.copyOf(effects); }
        public boolean isEmpty() { return effects.isEmpty(); }
        public void render(VertexConsumer out,PoseStack.Pose pose,org.joml.Quaternionfc cameraOrientation) {
            var right=cameraOrientation.transform(new Vector3f(1,0,0));
            var up=cameraOrientation.transform(new Vector3f(0,1,0));
            for(var effect:effects) effect.render(out,pose,right,up);
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

        private Effect snapshot() {
            // Keep V33a's integer division and update-before-expiry semantics.
            double radius = rapidExpand ? 0.1*size*(lifespan/(ticks+1) >= 12 ? (ticks+1)*12D/lifespan : 1-(ticks+1)/(double)lifespan)
                    : 0.1*size*Math.sin(Math.toRadians(180D*ticks/lifespan));
            return new Effect(posX,posY,posZ,(float)radius,0xff000000|renderColor,
                    icon.getU0(),icon.getV0(),icon.getU1(),icon.getV1());
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
