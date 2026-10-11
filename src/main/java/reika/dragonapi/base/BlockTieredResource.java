/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/** Player-gated resources shared by ores, plants and DragonAPI mining helpers. */
public abstract class BlockTieredResource extends Block {

    /** Retained for source compatibility; accepted consumers use the owning level's RandomSource. */
    protected static final Random rand = new Random();

    /** Datagen tables opt into the tier-aware dynamic drop; conditions/functions remain data-driven. */
    public static final Identifier HARVEST_DROPS = Identifier.fromNamespaceAndPath("dragonapi", "tiered_resource");

    public BlockTieredResource(Properties properties) {
        super(properties);
    }

    @Override
    public final List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        // V33a forbids unowned drops (explosions, unsupported plants, generic destroyBlock).
        if (!(params.getOptionalParameter(LootContextParams.THIS_ENTITY) instanceof Player player)
                || player.isCreative())
            return List.of();
        Level level = params.getLevel();
        BlockPos pos = BlockPos.containing(params.getParameter(LootContextParams.ORIGIN));
        ItemInstance tool = params.getParameter(LootContextParams.TOOL);
        params.withDynamicDrop(HARVEST_DROPS, output -> {
            Collection<ItemStack> drops = this.isPlayerSufficientTier(level, pos, player)
                    ? this.getHarvestResources(level, pos, tool, player)
                    : this.getNoHarvestResources(level, pos, tool, player);
            drops.forEach(output);
        });
        return super.getDrops(state, params);
    }

    /** The removal hook owns harvesting, independently of tool suitability; never run it twice. */
    @Override
    public final boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public final boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player,
            ItemStack tool, boolean willHarvest, FluidState fluid) {
        // Choose while the original block is still present: implementations may read contextual state.
        var blockEntity = level.getBlockEntity(pos);
        boolean harvest = level instanceof ServerLevel && player != null && !player.isCreative();
        boolean sufficient = harvest && this.isPlayerSufficientTier(level, pos, player);
        List<ItemStack> stacks = harvest
                ? Block.getDrops(state, (ServerLevel)level, pos, blockEntity, player, tool) : List.of();
        int experience = sufficient ? 2 + level.getRandom().nextInt(5) : 0;
        boolean removed = super.onDestroyedByPlayer(state, level, pos, player, tool, willHarvest, fluid);
        if (removed && harvest) {
            ServerLevel server = (ServerLevel)level;
            List<ItemEntity> entities = new ArrayList<>();
            if (server.getGameRules().get(GameRules.BLOCK_DROPS) && !level.restoringBlockSnapshots) {
                for (ItemStack stack : stacks) {
                    if (stack.isEmpty()) continue;
                    var random = level.getRandom();
                    ItemEntity entity = new ItemEntity(level,
                            pos.getX() + 0.5 + Mth.nextDouble(random, -0.25, 0.25),
                            pos.getY() + 0.5 + Mth.nextDouble(random, -0.25, 0.25) - 0.125,
                            pos.getZ() + 0.5 + Mth.nextDouble(random, -0.25, 0.25), stack);
                    entity.setDefaultPickUpDelay();
                    entities.add(entity);
                }
            }
            BlockDropsEvent event = new BlockDropsEvent(server, pos, state, blockEntity, entities, player, tool);
            event.setDroppedExperience(EnchantmentHelper.processBlockExperience(server, tool, experience));
            NeoForge.EVENT_BUS.post(event);
            if (!event.isCanceled()) {
                event.getDrops().forEach(server::addFreshEntity);
                state.spawnAfterBreak(server, pos, tool, false);
                this.popExperience(server, pos, event.getDroppedExperience());
            }
        }
        return removed;
    }

    public abstract Collection<ItemStack> getHarvestResources(Level level, BlockPos pos, int fortune, Player player);

    protected Collection<ItemStack> getHarvestResources(Level level, BlockPos pos, ItemInstance tool, Player player) {
        return this.getHarvestResources(level, pos, enchantmentLevel(level, tool, Enchantments.FORTUNE), player);
    }

    /** Plants have no insufficient-tier resources; ores may override this with host-block drops. */
    public Collection<ItemStack> getNoHarvestResources(Level level, BlockPos pos, int fortune, Player player) {
        return List.of();
    }

    /** Preserve the pre-damage tool snapshot for native loot (including a tool that breaks). */
    protected Collection<ItemStack> getNoHarvestResources(Level level, BlockPos pos, ItemInstance tool, Player player) {
        return this.getNoHarvestResources(level, pos, enchantmentLevel(level, tool, Enchantments.FORTUNE), player);
    }

    public abstract boolean isPlayerSufficientTier(BlockGetter level, BlockPos pos, Player player);

    protected static int enchantmentLevel(Level level, ItemInstance tool, ResourceKey<Enchantment> key) {
        return tool.count() <= 0 ? 0 : EnchantmentHelper.getItemEnchantmentLevel(
                level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key), tool);
    }

    @Override
    public final void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof Player player
                && !this.isPlayerSufficientTier(level, pos, player)) {
            level.levelEvent(2001, pos, Block.getId(state));
            level.removeBlock(pos, false);
        }
    }

    /** Source-compatible entry point for existing ported callers; BlockItem calls setPlacedBy. */
    public final void onBlockPlacedBy(Level level, BlockPos pos, LivingEntity placer, ItemStack stack) {
        this.setPlacedBy(level, pos, level.getBlockState(pos), placer, stack);
    }
}
