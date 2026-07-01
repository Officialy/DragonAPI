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

import com.mojang.serialization.Codec;
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
 * DragonAPI's overlay-style connected-texture model (the legacy GeoStrata
 * {@code ConnectedStoneRenderer} scheme, rebuilt on NeoForge's {@link DynamicBlockStateModel} — no
 * external CT libraries). Renders a plain base cube, then per face draws one border-overlay sprite
 * for each UNCONNECTED numpad edge (1-9; the sprites live at {@code <overlays>/<k>}); with
 * {@code sections} enabled it also draws the {@code <k>_sec} sprite for each CONNECTED edge
 * (the legacy CONNECTED2 "central texture" variant).
 *
 * <p>Blockstate JSON usage (in a variant slot):
 * <pre>{"type": "dragonapi:connected_overlay", "base": "geostrata:block/basalt_smooth",
 *  "overlays": "geostrata:block/connected", "sections": false}</pre></p>
 *
 * <p>All quads are baked once at model-bake; {@code collectParts} only selects from the prebaked
 * per-face parts, so the per-chunk cost is the neighbour reads.</p>
 */
public class OverlayConnectedModel implements DynamicBlockStateModel {

	/** Outward inflation (model units, /16 block) so overlays never z-fight the base face. */
	private static final float OVERLAY_OFFSET = 0.03F;

	private final BlockStateModelPart basePart;
	private final BlockStateModelPart[][] edgeParts;    // [direction ordinal][numpad 1..9]
	private final BlockStateModelPart[][] sectionParts; // null when sections disabled
	private final Material.Baked particle;
	private final int flags;

	private OverlayConnectedModel(BlockStateModelPart basePart, BlockStateModelPart[][] edgeParts,
	                              BlockStateModelPart[][] sectionParts, Material.Baked particle) {
		this.basePart = basePart;
		this.edgeParts = edgeParts;
		this.sectionParts = sectionParts;
		this.particle = particle;
		int f = basePart.materialFlags();
		for (Direction d : Direction.values()) {
			for (int k = 1; k <= 9; k++) {
				f |= edgeParts[d.ordinal()][k].materialFlags();
				if (sectionParts != null)
					f |= sectionParts[d.ordinal()][k].materialFlags();
			}
		}
		this.flags = f;
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
		parts.add(basePart);
		for (Direction d : Direction.values()) {
			boolean[] un = ConnectedQuads.unconnectedEdges(level, pos, state, d);
			for (int k = 1; k <= 9; k++) {
				if (un[k])
					parts.add(edgeParts[d.ordinal()][k]);
				else if (sectionParts != null)
					parts.add(sectionParts[d.ordinal()][k]);
			}
		}
	}

	@Override
	public Material.Baked particleMaterial() {
		return particle;
	}

	@Override
	public int materialFlags() {
		return flags;
	}

	public record Unbaked(Identifier base, Identifier overlays, boolean sections) implements CustomUnbakedBlockStateModel {

		public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Identifier.CODEC.fieldOf("base").forGetter(Unbaked::base),
				Identifier.CODEC.fieldOf("overlays").forGetter(Unbaked::overlays),
				Codec.BOOL.optionalFieldOf("sections", false).forGetter(Unbaked::sections)
		).apply(i, Unbaked::new));

		@Override
		public net.minecraft.client.renderer.block.dispatch.BlockStateModel bake(ModelBaker baker) {
			Material.Baked baseMat = baker.materials().get(new Material(base), () -> "dragonapi:connected_overlay/" + base);
			BlockStateModelPart basePart = ConnectedQuads.cubePart(baker, baseMat);

			BlockStateModelPart[][] edges = new BlockStateModelPart[6][10];
			BlockStateModelPart[][] secs = sections ? new BlockStateModelPart[6][10] : null;
			for (int k = 1; k <= 9; k++) {
				Material.Baked em = baker.materials().get(
						new Material(Identifier.fromNamespaceAndPath(overlays.getNamespace(), overlays.getPath() + "/" + k)),
						() -> "dragonapi:connected_overlay/" + overlays);
				Material.Baked sm = sections ? baker.materials().get(
						new Material(Identifier.fromNamespaceAndPath(overlays.getNamespace(), overlays.getPath() + "/" + k + "_sec")),
						() -> "dragonapi:connected_overlay/" + overlays) : null;
				for (Direction d : Direction.values()) {
					edges[d.ordinal()][k] = ConnectedQuads.facePart(baker, d, em, OVERLAY_OFFSET);
					if (secs != null)
						secs[d.ordinal()][k] = ConnectedQuads.facePart(baker, d, sm, OVERLAY_OFFSET);
				}
			}
			return new OverlayConnectedModel(basePart, edges, secs, baseMat);
		}

		@Override
		public void resolveDependencies(Resolver resolver) {
			// Only sprite references — nothing to resolve (sprites are resolved via MaterialBaker at bake).
		}

		@Override
		public MapCodec<Unbaked> codec() {
			return CODEC;
		}
	}
}
