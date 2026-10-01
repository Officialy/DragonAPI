/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.modinteract;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

/** Atmosphere queries for combustion, sound density and vacuum/toxic atmosphere damage.
 * Integrations supply the atmosphere at the queried position on the NeoForge game event bus.
 * Unclaimed queries describe an ordinary Minecraft atmosphere.
 */
public final class AtmosphereHandler {
    private AtmosphereHandler() {}
    public static boolean isNoAtmo(Level level, BlockPos pos, Block block, boolean needsOxygen) {
        var query = NeoForge.EVENT_BUS.post(new AtmosphereQuery(level, pos, block));
        return needsOxygen ? !query.combustionAllowed : query.soundReduction > 1;
    }
    public static float getAtmoDensity(Level level, BlockPos pos) {
        return 1 / NeoForge.EVENT_BUS.post(new AtmosphereQuery(level, pos, level.getBlockState(pos).getBlock())).soundReduction;
    }
    public static boolean isAtmoBreathabilityDamage(DamageSource source) {
        return NeoForge.EVENT_BUS.post(new BreathabilityDamageQuery(source)).atmosphereDamage;
    }
    public static final class AtmosphereQuery extends Event {
        public final Level level;
        public final BlockPos pos;
        public final Block block;
        private boolean combustionAllowed = true;
        private float soundReduction = 1;
        private AtmosphereQuery(Level level, BlockPos pos, Block block) {
            this.level = level; this.pos = pos.immutable(); this.block = block;
        }
        public void setCombustionAllowed(boolean allowed) { combustionAllowed = allowed; }
        public void setSoundReduction(float reduction) {
            if (!Float.isFinite(reduction) || reduction <= 0) throw new IllegalArgumentException("Atmosphere sound reduction must be positive and finite");
            soundReduction = reduction;
        }
        // ADVROCKET-PORT: the modern adapter must query the dimension's oxygen handler and
        // atmosphere type at pos, then pass atmosphere.allowsCombustion() here.
        // GALACTICRAFT-PORT: the modern adapter must preserve atmospheric combustion OR
        // (oxygen bubble AND torch oxygen check at pos), plus the provider's sound reduction.
    }
    public static final class BreathabilityDamageQuery extends Event {
        public final DamageSource source;
        private boolean atmosphereDamage;
        private BreathabilityDamageQuery(DamageSource source) { this.source = source; }
        public void setAtmosphereDamage(boolean matches) { atmosphereDamage = matches; }
        // ADVROCKET-PORT: identify both vacuum and oxygen-toxicity damage from the modern API.
    }
}
