/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.event.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

/**
 * V33a {@code WaterColorEvent}: lets a mod recolour water at an exact position.
 *
 * <p>In 1.7.10 DragonAPI's ASM patch replaced the body of {@code BlockLiquid.colorMultiplier} with a
 * call to {@link #fire}, which averaged the 3x3 biome water multipliers and then offered the result to
 * listeners. A biome's own water colour in that era was a single per-biome number, so anything
 * positional — ChromatiCraft's Rainbow Stream mixing toward blue on noise, the Glowing Cliffs going
 * clear at shallow edges — had to arrive through this event.
 *
 * <p>26.2 still has only a per-biome water colour, resolved through {@code BiomeColors}' water
 * resolver, which is handed a biome and an X/Z and never a Y, a light level or a neighbour. The
 * modern hook therefore sits after {@code BiomeColors.getAverageWaterColor}: {@link #originalColor}
 * is vanilla's biome-blended value (the successor to V33a's 3x3 average) and listeners see the full
 * position through a {@link BlockAndTintGetter}.
 *
 * <p>Fired on chunk-meshing worker threads as well as the client thread, once per water block per
 * mesh build. Listeners must be thread-safe and must not load chunks.
 */
public class WaterColorEvent extends Event {

	public final BlockAndTintGetter access;
	public final BlockPos pos;
	public final int xCoord;
	public final int yCoord;
	public final int zCoord;

	public final int originalColor;
	public int color;

	public WaterColorEvent(BlockAndTintGetter access, BlockPos pos, int c) {
		this.access = access;
		this.pos = pos.immutable();
		xCoord = pos.getX();
		yCoord = pos.getY();
		zCoord = pos.getZ();
		color = originalColor = c;
	}

	/** Called from {@code MixinBiomeColors}; returns the possibly-modified colour. */
	public static int fire(BlockAndTintGetter access, BlockPos pos, int c) {
		WaterColorEvent evt = new WaterColorEvent(access, pos, c);
		NeoForge.EVENT_BUS.post(evt);
		return evt.color;
	}

	/**
	 * V33a {@code getBiome()}. {@link BlockAndTintGetter} exposes no biome lookup, so this goes through
	 * the client level, which is where every mesh-building getter's biomes come from.
	 *
	 * <p>On a meshing worker thread prefer a registered {@link net.minecraft.world.level.ColorResolver}
	 * probed through {@code access.getBlockTint}: it is per-column cached, thread-safe, and feathered
	 * across the biome blend radius, so a recolour fades in at a border instead of switching hard.
	 */
	public Holder<Biome> getBiome() {
		var level = net.minecraft.client.Minecraft.getInstance().level;
		return level == null ? null : level.getBiome(pos);
	}

	/** V33a {@code getLightLevel()}: the block's sky light, which the Glowing Cliffs reads. */
	public int getLightLevel() {
		return access.getBrightness(LightLayer.SKY, pos);
	}
}
