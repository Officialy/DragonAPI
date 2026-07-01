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

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;

/**
 * DragonAPI's axis-run connected-texture model: whole-face sprite swap driven by which horizontal
 * neighbours connect (the legacy ReactorCraft {@code BlockSolenoidMulti.getTextureIndex} scheme for
 * the ferromagnetic base — no external CT libraries). Top/bottom faces show the {@code x_run} sprite
 * when connected along X, else {@code z_run} when connected along Z, else {@code isolated}; lateral
 * faces show {@code side_connected} when connected along either axis, else {@code side_isolated}.
 *
 * <p>Blockstate JSON usage (in a variant slot):
 * <pre>{"type": "dragonapi:connected_axis", "x_run": "...", "z_run": "...", "isolated": "...",
 *  "side_connected": "...", "side_isolated": "..."}</pre></p>
 */
public class AxisConnectedModel implements DynamicBlockStateModel {

	// [0]=UP, [1]=DOWN each; end cases: 0=x-run, 1=z-run, 2=isolated
	private final BlockStateModelPart[][] endParts;
	// [horizontal direction 2D index][0=connected, 1=isolated]
	private final BlockStateModelPart[][] sideParts;
	private final Material.Baked particle;
	private final int flags;

	private AxisConnectedModel(BlockStateModelPart[][] endParts, BlockStateModelPart[][] sideParts, Material.Baked particle) {
		this.endParts = endParts;
		this.sideParts = sideParts;
		this.particle = particle;
		int f = 0;
		for (BlockStateModelPart[] arr : endParts)
			for (BlockStateModelPart p : arr)
				f |= p.materialFlags();
		for (BlockStateModelPart[] arr : sideParts)
			for (BlockStateModelPart p : arr)
				f |= p.materialFlags();
		this.flags = f;
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
		boolean fx = ConnectedQuads.connects(level, pos, state, 1, 0, 0) || ConnectedQuads.connects(level, pos, state, -1, 0, 0);
		boolean fz = ConnectedQuads.connects(level, pos, state, 0, 0, 1) || ConnectedQuads.connects(level, pos, state, 0, 0, -1);
		int endCase = fx ? 0 : fz ? 1 : 2;
		parts.add(endParts[0][endCase]);
		parts.add(endParts[1][endCase]);
		int sideCase = (fx || fz) ? 0 : 1;
		for (int i = 0; i < 4; i++)
			parts.add(sideParts[i][sideCase]);
	}

	@Override
	public Material.Baked particleMaterial() {
		return particle;
	}

	@Override
	public int materialFlags() {
		return flags;
	}

	public record Unbaked(Identifier xRun, Identifier zRun, Identifier isolated,
	                      Identifier sideConnected, Identifier sideIsolated) implements CustomUnbakedBlockStateModel {

		public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Identifier.CODEC.fieldOf("x_run").forGetter(Unbaked::xRun),
				Identifier.CODEC.fieldOf("z_run").forGetter(Unbaked::zRun),
				Identifier.CODEC.fieldOf("isolated").forGetter(Unbaked::isolated),
				Identifier.CODEC.fieldOf("side_connected").forGetter(Unbaked::sideConnected),
				Identifier.CODEC.fieldOf("side_isolated").forGetter(Unbaked::sideIsolated)
		).apply(i, Unbaked::new));

		@Override
		public net.minecraft.client.renderer.block.dispatch.BlockStateModel bake(ModelBaker baker) {
			Material.Baked mx = mat(baker, xRun);
			Material.Baked mz = mat(baker, zRun);
			Material.Baked mi = mat(baker, isolated);
			Material.Baked msc = mat(baker, sideConnected);
			Material.Baked msi = mat(baker, sideIsolated);

			BlockStateModelPart[][] ends = new BlockStateModelPart[2][3];
			Direction[] vertical = {Direction.UP, Direction.DOWN};
			Material.Baked[] endMats = {mx, mz, mi};
			for (int v = 0; v < 2; v++)
				for (int c = 0; c < 3; c++)
					ends[v][c] = ConnectedQuads.facePart(baker, vertical[v], endMats[c], 0);

			BlockStateModelPart[][] sides = new BlockStateModelPart[4][2];
			Direction[] horizontal = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
			for (int i = 0; i < 4; i++) {
				sides[i][0] = ConnectedQuads.facePart(baker, horizontal[i], msc, 0);
				sides[i][1] = ConnectedQuads.facePart(baker, horizontal[i], msi, 0);
			}
			return new AxisConnectedModel(ends, sides, mi);
		}

		private static Material.Baked mat(ModelBaker baker, Identifier id) {
			return baker.materials().get(new Material(id), () -> "dragonapi:connected_axis/" + id);
		}

		@Override
		public void resolveDependencies(Resolver resolver) {
			// Only sprite references — resolved via MaterialBaker at bake.
		}

		@Override
		public MapCodec<Unbaked> codec() {
			return CODEC;
		}
	}
}
