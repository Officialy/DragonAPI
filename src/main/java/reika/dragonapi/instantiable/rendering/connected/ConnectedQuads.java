/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.rendering.connected;

import com.mojang.math.Quadrant;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import org.joml.Vector3f;

import reika.dragonapi.interfaces.block.ConnectedModelBlock;

/**
 * Shared quad-baking + connectivity math for DragonAPI's connected-texture block models. Everything
 * here is vanilla/NeoForge-native (FaceBakery + QuadCollection + SimpleModelWrapper) — no external
 * connected-texture libraries, per Reika's requirement.
 *
 * <p>Edge numbering follows the legacy 1.7.10 convention: each face's 8 surrounding positions map to
 * numpad indices 1-9 (5 = centre, always drawn), with the exact per-axis offset→index table copied
 * from the original {@code BlockConnectedRock.getEdgesForFace} so the original overlay art lines up.</p>
 */
public final class ConnectedQuads {

	private ConnectedQuads() {}

	/** Bake a single full face of the unit cube (optionally inflated outward) with the given sprite. */
	public static BakedQuad bakeFaceQuad(ModelBaker baker, Direction face, Material.Baked mat, float inflate) {
		Vector3f from = new Vector3f(0, 0, 0);
		Vector3f to = new Vector3f(16, 16, 16);
		switch (face) {
			case UP -> to.y += inflate;
			case DOWN -> from.y -= inflate;
			case SOUTH -> to.z += inflate;
			case NORTH -> from.z -= inflate;
			case EAST -> to.x += inflate;
			case WEST -> from.x -= inflate;
		}
		CuboidFace cf = new CuboidFace(face, CuboidFace.NO_TINT, "", null, Quadrant.R0);
		return FaceBakery.bakeQuad(baker, from, to, cf, mat, face, BlockModelRotation.IDENTITY, null, true, 0);
	}

	/** A one-face model part (quad culled against its face) with the given sprite. */
	public static BlockStateModelPart facePart(ModelBaker baker, Direction face, Material.Baked mat, float inflate) {
		QuadCollection.Builder b = new QuadCollection.Builder();
		b.addCulledFace(face, bakeFaceQuad(baker, face, mat, inflate));
		return new SimpleModelWrapper(b.build(), true, mat);
	}

	/** A full-cube model part (all six faces culled) with the given sprite. */
	public static BlockStateModelPart cubePart(ModelBaker baker, Material.Baked mat) {
		QuadCollection.Builder b = new QuadCollection.Builder();
		for (Direction d : Direction.values())
			b.addCulledFace(d, bakeFaceQuad(baker, d, mat, 0));
		return new SimpleModelWrapper(b.build(), true, mat);
	}

	/** Whether the connected-texture model should treat the block at {@code pos + (dx,dy,dz)} as connected. */
	public static boolean connects(BlockAndTintGetter level, BlockPos pos, BlockState self, int dx, int dy, int dz) {
		BlockState n = level.getBlockState(pos.offset(dx, dy, dz));
		if (self.getBlock() instanceof ConnectedModelBlock c)
			return c.connectsToCT(self, n);
		return n.is(self.getBlock());
	}

	/**
	 * The unconnected numpad edges (index 1-9; {@code un[k]} == true → edge overlay {@code k} drawn) of
	 * a face, from the 8 in-plane neighbours. Corner indices only clear when BOTH adjacent side edges
	 * are connected — the legacy {@code getEdgesForFace} rule, offsets copied verbatim per face axis.
	 */
	public static boolean[] unconnectedEdges(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction face) {
		boolean[] un = new boolean[10];
		for (int i = 1; i <= 9; i++)
			un[i] = true;

		switch (face.getAxis()) {
			case X -> { // test the YZ plane
				if (connects(level, pos, state, 0, 0, 1)) un[2] = false;
				if (connects(level, pos, state, 0, 0, -1)) un[8] = false;
				if (connects(level, pos, state, 0, 1, 0)) un[4] = false;
				if (connects(level, pos, state, 0, -1, 0)) un[6] = false;
				if (connects(level, pos, state, 0, 1, 1) && !un[4] && !un[2]) un[1] = false;
				if (connects(level, pos, state, 0, -1, -1) && !un[6] && !un[8]) un[9] = false;
				if (connects(level, pos, state, 0, 1, -1) && !un[4] && !un[8]) un[7] = false;
				if (connects(level, pos, state, 0, -1, 1) && !un[2] && !un[6]) un[3] = false;
			}
			case Y -> { // test the XZ plane
				if (connects(level, pos, state, 0, 0, 1)) un[2] = false;
				if (connects(level, pos, state, 0, 0, -1)) un[8] = false;
				if (connects(level, pos, state, 1, 0, 0)) un[4] = false;
				if (connects(level, pos, state, -1, 0, 0)) un[6] = false;
				if (connects(level, pos, state, 1, 0, 1) && !un[4] && !un[2]) un[1] = false;
				if (connects(level, pos, state, -1, 0, -1) && !un[6] && !un[8]) un[9] = false;
				if (connects(level, pos, state, 1, 0, -1) && !un[4] && !un[8]) un[7] = false;
				if (connects(level, pos, state, -1, 0, 1) && !un[2] && !un[6]) un[3] = false;
			}
			case Z -> { // test the XY plane
				if (connects(level, pos, state, 0, 1, 0)) un[4] = false;
				if (connects(level, pos, state, 0, -1, 0)) un[6] = false;
				if (connects(level, pos, state, 1, 0, 0)) un[2] = false;
				if (connects(level, pos, state, -1, 0, 0)) un[8] = false;
				if (connects(level, pos, state, 1, 1, 0) && !un[2] && !un[4]) un[1] = false;
				if (connects(level, pos, state, -1, -1, 0) && !un[8] && !un[6]) un[9] = false;
				if (connects(level, pos, state, 1, -1, 0) && !un[2] && !un[6]) un[3] = false;
				if (connects(level, pos, state, -1, 1, 0) && !un[4] && !un[8]) un[7] = false;
			}
		}
		return un;
	}
}
