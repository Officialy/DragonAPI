/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import org.joml.Vector3d;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import reika.dragonapi.instantiable.GridDistortion.OffsetGroup;
import reika.dragonapi.libraries.java.ReikaRandomHelper;

/**
 * V33a {@code CubePoints}: the eight corners of a deformable cube, each with a position and velocity, named "xyz" by
 * which end of each axis it sits at ("111" the low corner, "222" the high). In 26.2 {@link Vec3} is immutable, so the
 * corners hold mutable {@link Vector3d}s; drawing emits position/UV/colour vertices into a consumer.
 */
public class CubePoints {

	public final CubeVertex x1y1z1;
	public final CubeVertex x2y1z1;
	public final CubeVertex x1y1z2;
	public final CubeVertex x2y1z2;

	public final CubeVertex x1y2z1;
	public final CubeVertex x2y2z1;
	public final CubeVertex x1y2z2;
	public final CubeVertex x2y2z2;

	private final HashMap<String, CubeVertex> vertices = new HashMap<>();

	public CubePoints(Vec3 x1y1z1, Vec3 x2y1z1, Vec3 x1y1z2, Vec3 x2y1z2, Vec3 x1y2z1, Vec3 x2y2z1, Vec3 x1y2z2, Vec3 x2y2z2) {
		this.x1y1z1 = new CubeVertex("111", x1y1z1.x, x1y1z1.y, x1y1z1.z);
		this.x2y1z1 = new CubeVertex("211", x2y1z1.x, x2y1z1.y, x2y1z1.z);
		this.x1y1z2 = new CubeVertex("112", x1y1z2.x, x1y1z2.y, x1y1z2.z);
		this.x2y1z2 = new CubeVertex("212", x2y1z2.x, x2y1z2.y, x2y1z2.z);

		this.x1y2z1 = new CubeVertex("121", x1y2z1.x, x1y2z1.y, x1y2z1.z);
		this.x2y2z1 = new CubeVertex("221", x2y2z1.x, x2y2z1.y, x2y2z1.z);
		this.x1y2z2 = new CubeVertex("122", x1y2z2.x, x1y2z2.y, x1y2z2.z);
		this.x2y2z2 = new CubeVertex("222", x2y2z2.x, x2y2z2.y, x2y2z2.z);
	}

	private CubePoints(CubeVertex x1y1z1, CubeVertex x2y1z1, CubeVertex x1y1z2, CubeVertex x2y1z2, CubeVertex x1y2z1, CubeVertex x2y2z1, CubeVertex x1y2z2, CubeVertex x2y2z2) {
		this.x1y1z1 = new CubeVertex(x1y1z1);
		this.x2y1z1 = new CubeVertex(x2y1z1);
		this.x1y1z2 = new CubeVertex(x1y1z2);
		this.x2y1z2 = new CubeVertex(x2y1z2);

		this.x1y2z1 = new CubeVertex(x1y2z1);
		this.x2y2z1 = new CubeVertex(x2y2z1);
		this.x1y2z2 = new CubeVertex(x1y2z2);
		this.x2y2z2 = new CubeVertex(x2y2z2);
	}

	public Vec3 getCenter() {
		double x = 0;
		double y = 0;
		double z = 0;
		for (CubeVertex cv : vertices.values()) {
			x += cv.position.x;
			y += cv.position.y;
			z += cv.position.z;
		}
		int n = vertices.size();
		return new Vec3(x/n, y/n, z/n);
	}

	public void applyOffset(Direction side, OffsetGroup off) {
		switch(side) {
			case DOWN:
				x1y1z1.position.x += off.offsetAMM;
				x2y1z1.position.x += off.offsetAPM;
				x1y1z2.position.x += off.offsetAMP;
				x2y1z2.position.x += off.offsetAPP;
				x1y1z1.position.z += off.offsetBMM;
				x2y1z1.position.z += off.offsetBPM;
				x1y1z2.position.z += off.offsetBMP;
				x2y1z2.position.z += off.offsetBPP;
				break;
			case UP:
				x1y2z1.position.x += off.offsetAMM;
				x2y2z1.position.x += off.offsetAPM;
				x1y2z2.position.x += off.offsetAMP;
				x2y2z2.position.x += off.offsetAPP;
				x1y2z1.position.z += off.offsetBMM;
				x2y2z1.position.z += off.offsetBPM;
				x1y2z2.position.z += off.offsetBMP;
				x2y2z2.position.z += off.offsetBPP;
				break;
			case WEST:
				x1y1z1.position.y += off.offsetAMM;
				x1y2z1.position.y += off.offsetAMP;
				x1y1z2.position.y += off.offsetAPM;
				x1y2z2.position.y += off.offsetAPP;
				x1y1z1.position.z += off.offsetBMM;
				x1y2z1.position.z += off.offsetBMP;
				x1y1z2.position.z += off.offsetBPM;
				x1y2z2.position.z += off.offsetBPP;
				break;
			case EAST:
				x2y1z1.position.y += off.offsetAMM;
				x2y2z1.position.y += off.offsetAMP;
				x2y1z2.position.y += off.offsetAPM;
				x2y2z2.position.y += off.offsetAPP;
				x2y1z1.position.z += off.offsetBMM;
				x2y2z1.position.z += off.offsetBMP;
				x2y1z2.position.z += off.offsetBPM;
				x2y2z2.position.z += off.offsetBPP;
				break;
			case NORTH:
				x1y1z1.position.x += off.offsetAMM;
				x2y1z1.position.x += off.offsetAPM;
				x1y2z1.position.x += off.offsetAMP;
				x2y2z1.position.x += off.offsetAPP;
				x1y1z1.position.y += off.offsetBMM;
				x2y1z1.position.y += off.offsetBPM;
				x1y2z1.position.y += off.offsetBMP;
				x2y2z1.position.y += off.offsetBPP;
				break;
			case SOUTH:
				x1y1z2.position.x += off.offsetAMM;
				x2y1z2.position.x += off.offsetAPM;
				x1y2z2.position.x += off.offsetAMP;
				x2y2z2.position.x += off.offsetAPP;
				x1y1z2.position.y += off.offsetBMM;
				x2y1z2.position.y += off.offsetBPM;
				x1y2z2.position.y += off.offsetBMP;
				x2y2z2.position.y += off.offsetBPP;
				break;
		}
	}

	public void clamp() {
		for (CubeVertex cv : vertices.values()) {
			cv.position.x = Mth.clamp(cv.position.x, 0, 1);
			cv.position.y = Mth.clamp(cv.position.y, 0, 1);
			cv.position.z = Mth.clamp(cv.position.z, 0, 1);
		}
	}

	public void expand(double amt) {
		this.expand(amt, amt, amt);
	}

	public void expand(double x, double y, double z) {
		x1y1z1.offset(-x, -y, -z);
		x2y1z1.offset(x, -y, -z);
		x1y2z1.offset(-x, y, -z);
		x2y2z1.offset(x, y, -z);
		x1y1z2.offset(-x, -y, z);
		x2y1z2.offset(x, -y, z);
		x1y2z2.offset(-x, y, z);
		x2y2z2.offset(x, y, z);
	}

	public void setSidePosition(Direction side, double val) {
		switch(side) {
			case DOWN:
				x1y1z1.position.y = val;
				x2y1z1.position.y = val;
				x2y1z2.position.y = val;
				x1y1z2.position.y = val;
				break;
			case UP:
				x1y2z1.position.y = val;
				x2y2z1.position.y = val;
				x2y2z2.position.y = val;
				x1y2z2.position.y = val;
				break;
			case WEST:
				x1y1z1.position.x = val;
				x1y2z1.position.x = val;
				x1y2z2.position.x = val;
				x1y1z2.position.x = val;
				break;
			case EAST:
				x2y1z1.position.x = val;
				x2y2z1.position.x = val;
				x2y2z2.position.x = val;
				x2y1z2.position.x = val;
				break;
			case NORTH:
				x1y1z1.position.z = val;
				x1y2z1.position.z = val;
				x2y2z1.position.z = val;
				x2y1z1.position.z = val;
				break;
			case SOUTH:
				x1y1z2.position.z = val;
				x1y2z2.position.z = val;
				x2y2z2.position.z = val;
				x2y1z2.position.z = val;
				break;
		}
	}

	public CubePoints copy() {
		return new CubePoints(x1y1z1, x2y1z1, x1y1z2, x2y1z2, x1y2z1, x2y2z1, x1y2z2, x2y2z2);
	}

	public Collection<CubeVertex> getVertices() {
		return Collections.unmodifiableCollection(vertices.values());
	}

	public CubeVertex getVertex(String id) {
		return vertices.get(id);
	}

	public void applyVelocities() {
		this.applyVelocities(null);
	}

	public void applyVelocities(AABB bounds) {
		for (CubeVertex v : vertices.values()) {
			v.applyVelocity(bounds);
		}
	}

	public void multiplyVelocities(double d) {
		for (CubeVertex v : vertices.values()) {
			v.velocity.mul(d);
		}
	}

	public void setRandomVelocities(double bounds) {
		for (CubeVertex v : vertices.values()) {
			v.velocity.set(ReikaRandomHelper.getRandomPlusMinus(0, bounds), ReikaRandomHelper.getRandomPlusMinus(0, bounds),
					ReikaRandomHelper.getRandomPlusMinus(0, bounds));
		}
	}

	public void setVelocities(Vec3 vec) {
		for (CubeVertex v : vertices.values()) {
			v.velocity.set(vec.x, vec.y, vec.z);
		}
	}

	public final class CubeVertex {

		private final Vector3d position;
		public final Vector3d velocity = new Vector3d();
		public final String ID;

		private CubeVertex(String id, double x, double y, double z) {
			position = new Vector3d(x, y, z);
			ID = id;
			this.parent().vertices.put(id, this);
		}

		private CubeVertex(CubeVertex pos) {
			this(pos.ID, pos.position.x, pos.position.y, pos.position.z);
		}

		public void applyVelocity() {
			this.applyVelocity(null);
		}

		public void applyVelocity(AABB bounds) {
			position.add(velocity);
			if (bounds != null) {
				if (position.x >= bounds.maxX) {
					velocity.x = -velocity.x;
					position.x = bounds.maxX;
				}
				else if (position.x <= bounds.minX) {
					velocity.x = -velocity.x;
					position.x = bounds.minX;
				}
				if (position.y >= bounds.maxY) {
					velocity.y = -velocity.y;
					position.y = bounds.maxY;
				}
				else if (position.y <= bounds.minY) {
					velocity.y = -velocity.y;
					position.y = bounds.minY;
				}
				if (position.z >= bounds.maxZ) {
					velocity.z = -velocity.z;
					position.z = bounds.maxZ;
				}
				else if (position.z <= bounds.minZ) {
					velocity.z = -velocity.z;
					position.z = bounds.minZ;
				}
			}
		}

		/** V33a {@code icon.getInterpolatedU(16*coord)}: the sprite U at this corner's face coordinate. */
		public double textureU(TextureAtlasSprite icon, Direction side) {
			return switch(side) {
				case DOWN, UP, NORTH, SOUTH -> icon.getU((float)position.x);
				case WEST, EAST -> icon.getU((float)position.z);
			};
		}

		public double textureV(TextureAtlasSprite icon, Direction side) {
			return switch(side) {
				case DOWN, UP -> icon.getV((float)position.z);
				case WEST, EAST, NORTH, SOUTH -> icon.getV((float)position.y);
			};
		}

		public void draw(VertexConsumer v5, PoseStack.Pose pose, TextureAtlasSprite ico, Direction side, int color) {
			this.drawWithUV(v5, pose, 0, 0, 0, side, this.textureU(ico, side), this.textureV(ico, side), color);
		}

		public void drawWithUV(VertexConsumer v5, PoseStack.Pose pose, double dx, double dy, double dz, Direction side,
				double u, double v, int color) {
			v5.addVertex(pose, (float)(position.x+dx), (float)(position.y+dy), (float)(position.z+dz))
					.setUv((float)u, (float)v).setColor(color);
		}

		public Vec3 getOffsetFromCenter() {
			Vec3 v = this.parent().getCenter();
			return new Vec3(position.x-v.x, position.y-v.y, position.z-v.z);
		}

		public void offset(double x, double y, double z) {
			position.add(x, y, z);
		}

		public void setPosition(CubeVertex cv) {
			this.setPosition(cv.position.x, cv.position.y, cv.position.z);
		}

		public void setPosition(double x, double y, double z) {
			position.set(x, y, z);
		}

		private CubePoints parent() {
			return CubePoints.this;
		}

	}

	public static CubePoints fullBlock() {
		return new CubePoints(new Vec3(0, 0, 0), new Vec3(1, 0, 0), new Vec3(0, 0, 1), new Vec3(1, 0, 1),
				new Vec3(0, 1, 0), new Vec3(1, 1, 0), new Vec3(0, 1, 1), new Vec3(1, 1, 1));
	}

	/**
	 * V33a {@code renderIconOnSides}: the whole sprite on each of the six faces of the (deformed) cube at x, y, z, as
	 * quads of position/UV/colour vertices (the caller's pose already relative to the camera).
	 */
	public void renderIconOnSides(double x, double y, double z, TextureAtlasSprite ico, VertexConsumer v5, PoseStack.Pose pose,
			int color) {
		double u = ico.getU0();
		double du = ico.getU1();
		double v = ico.getV0();
		double dv = ico.getV1();

		Direction dir = Direction.DOWN;
		x1y1z1.drawWithUV(v5, pose, x, y, z, dir, u, v, color);
		x2y1z1.drawWithUV(v5, pose, x, y, z, dir, du, v, color);
		x2y1z2.drawWithUV(v5, pose, x, y, z, dir, du, dv, color);
		x1y1z2.drawWithUV(v5, pose, x, y, z, dir, u, dv, color);

		dir = Direction.UP;
		x1y2z2.drawWithUV(v5, pose, x, y, z, dir, u, dv, color);
		x2y2z2.drawWithUV(v5, pose, x, y, z, dir, du, dv, color);
		x2y2z1.drawWithUV(v5, pose, x, y, z, dir, du, v, color);
		x1y2z1.drawWithUV(v5, pose, x, y, z, dir, u, v, color);

		dir = Direction.WEST;
		x1y1z2.drawWithUV(v5, pose, x, y, z, dir, u, dv, color);
		x1y2z2.drawWithUV(v5, pose, x, y, z, dir, du, dv, color);
		x1y2z1.drawWithUV(v5, pose, x, y, z, dir, du, v, color);
		x1y1z1.drawWithUV(v5, pose, x, y, z, dir, u, v, color);

		dir = Direction.EAST;
		x2y1z1.drawWithUV(v5, pose, x, y, z, dir, u, v, color);
		x2y2z1.drawWithUV(v5, pose, x, y, z, dir, du, v, color);
		x2y2z2.drawWithUV(v5, pose, x, y, z, dir, du, dv, color);
		x2y1z2.drawWithUV(v5, pose, x, y, z, dir, u, dv, color);

		dir = Direction.NORTH;
		x1y1z1.drawWithUV(v5, pose, x, y, z, dir, u, v, color);
		x1y2z1.drawWithUV(v5, pose, x, y, z, dir, u, dv, color);
		x2y2z1.drawWithUV(v5, pose, x, y, z, dir, du, dv, color);
		x2y1z1.drawWithUV(v5, pose, x, y, z, dir, du, v, color);

		dir = Direction.SOUTH;
		x2y1z2.drawWithUV(v5, pose, x, y, z, dir, du, v, color);
		x2y2z2.drawWithUV(v5, pose, x, y, z, dir, du, dv, color);
		x1y2z2.drawWithUV(v5, pose, x, y, z, dir, u, dv, color);
		x1y1z2.drawWithUV(v5, pose, x, y, z, dir, u, v, color);
	}

}
