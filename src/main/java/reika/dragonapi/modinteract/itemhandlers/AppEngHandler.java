package reika.dragonapi.modinteract.itemhandlers;

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import reika.dragonapi.DragonAPI;
import reika.dragonapi.ModList;
import reika.dragonapi.auxiliary.trackers.ReflectiveFailureTracker;

/**
 * 1.7.10 {@code AppEngHandler}: the Applied Energistics items and blocks other Reika mods use in recipes and effects.
 *
 * <p>1.7.10 read these out of {@code appeng.core.Api}'s definition tables by reflection. AE2's content is now plain
 * registry entries, so they are looked up by id - no AE class is touched and this handler is safe without AE2.
 * Lookups are lazy because registries are not frozen when mods are constructed. Every getter returns null (or an
 * empty stack's null) when AE2 is absent, exactly as the 1.7.10 handler did when its reflection failed.
 *
 * <p>Renames: certus/fluix/silicon/processors/presses/cards/cells/components keep their roles under AE2's modern ids.
 * The 1.7.10 "basic/advanced chip pattern" getters returned the basic/advanced card too, and still do. There is no
 * encoded-pattern item any more, but one per pattern kind; {@link #getEncodedPattern()} is the crafting pattern
 * (the kind 1.7.10 recipes encoded) and {@link #getProcessingPattern()} is the other. AE2 has no ores any more
 * (certus grows from budding quartz), so {@link #chargedCertusOre} has no modern equivalent and is null.
 */
public class AppEngHandler {

	private static final AppEngHandler instance = new AppEngHandler();
	private static final RandomSource rand = RandomSource.create();

	private static final String NS = "ae2";

	private boolean loaded;

	private Item certus;
	private Item chargedCertus;
	private Item dust;

	private Item fluix;
	private Item fluixdust;

	private Item silicon;

	private Item basicChip;
	private Item advChip;
	private Item basicChipPattern;
	private Item advChipPattern;

	private Item siliconPress;
	private Item logicPress;
	private Item calcPress;
	private Item engPress;

	private Item goldProcessor;
	private Item quartzProcessor;
	private Item diamondProcessor;

	private Item cell1k;
	private Item cell4k;
	private Item cell16k;
	private Item cell64k;

	private Item storage1k;
	private Item storage4k;
	private Item storage16k;
	private Item storage64k;

	private Item blankPattern;
	private Item encodedPattern;
	private Item processingPattern;

	public Block skystone;
	public Block quartzGlass;
	/** 1.7.10 charged certus quartz ore. Modern AE2 has no ores; always null. */
	public Block chargedCertusOre;

	private AppEngHandler() {

	}

	public static AppEngHandler getInstance() {
		instance.load();
		return instance;
	}

	public boolean hasMod() {
		return ModList.APPENG.isLoaded();
	}

	public ModList getMod() {
		return ModList.APPENG;
	}

	private void load() {
		if (loaded || !this.hasMod())
			return;
		try {
			certus = this.getMaterial("certus_quartz_crystal");
			chargedCertus = this.getMaterial("charged_certus_quartz_crystal");
			dust = this.getMaterial("certus_quartz_dust");
			fluix = this.getMaterial("fluix_crystal");
			fluixdust = this.getMaterial("fluix_dust");

			silicon = this.getMaterial("silicon");

			basicChip = this.getMaterial("basic_card");
			advChip = this.getMaterial("advanced_card");
			basicChipPattern = this.getMaterial("basic_card");
			advChipPattern = this.getMaterial("advanced_card");

			calcPress = this.getMaterial("calculation_processor_press");
			engPress = this.getMaterial("engineering_processor_press");
			logicPress = this.getMaterial("logic_processor_press");
			siliconPress = this.getMaterial("silicon_press");

			goldProcessor = this.getMaterial("logic_processor");
			quartzProcessor = this.getMaterial("calculation_processor");
			diamondProcessor = this.getMaterial("engineering_processor");

			skystone = this.getBlock("sky_stone_block");
			quartzGlass = this.getBlock("quartz_glass");
			chargedCertusOre = null;

			cell1k = this.getItem("item_storage_cell_1k");
			cell4k = this.getItem("item_storage_cell_4k");
			cell16k = this.getItem("item_storage_cell_16k");
			cell64k = this.getItem("item_storage_cell_64k");

			storage1k = this.getMaterial("cell_component_1k");
			storage4k = this.getMaterial("cell_component_4k");
			storage16k = this.getMaterial("cell_component_16k");
			storage64k = this.getMaterial("cell_component_64k");

			blankPattern = this.getMaterial("blank_pattern");
			encodedPattern = this.getItem("crafting_pattern");
			processingPattern = this.getItem("processing_pattern");

			loaded = true;
		}
		catch (Exception e) {
			DragonAPI.LOGGER.error("Cannot read AE registry contents!", e);
			ReflectiveFailureTracker.instance.logModReflectiveFailure(ModList.APPENG, e);
		}
	}

	@Nullable
	private Item getItem(String id) {
		Item i = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(NS, id));
		if (i == null || i == Items.AIR) {
			DragonAPI.LOGGER.error("AE item " + NS + ":" + id + " not found!");
			return null;
		}
		return i;
	}

	/** Any AE2 item by its registry path (ae2:{@code id}), or null; safe where an ItemStack is not (datagen, setup). */
	@Nullable
	public Item getAEItem(String id) {
		return this.hasMod() ? this.getItem(id) : null;
	}

	/** Items only: an ItemStack made this early (mod setup, datagen) throws "Components not bound yet". */
	@Nullable
	private Item getMaterial(String id) {
		return this.getItem(id);
	}

	@Nullable
	private Block getBlock(String id) {
		Block b = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(NS, id));
		if (b == null || b == Blocks.AIR) {
			DragonAPI.LOGGER.error("AE block " + NS + ":" + id + " not found!");
			return null;
		}
		return b;
	}

	private static ItemStack copy(Item is) {
		return is != null ? new ItemStack(is) : null;
	}

	public boolean initializedProperly() {
		return certus != null && dust != null && skystone != null;
	}

	public ItemStack getCertusQuartz() {
		return copy(certus);
	}

	public ItemStack getChargedCertusQuartz() {
		return copy(chargedCertus);
	}

	public ItemStack getCertusQuartzDust() {
		return copy(dust);
	}

	public ItemStack getFluixCrystal() {
		return copy(fluix);
	}

	public ItemStack getFluixDust() {
		return copy(fluixdust);
	}

	public Collection<ItemStack> getPossibleMeteorChestLoot() {
		ArrayList<ItemStack> li = new ArrayList<>();
		li.add(copy(calcPress));
		li.add(copy(engPress));
		li.add(copy(logicPress));
		li.add(copy(siliconPress));
		return li;
	}

	public Collection<ItemStack> getMeteorChestLoot() {
		ArrayList<ItemStack> li = new ArrayList<>();
		int n = 1 + rand.nextInt(3);
		for (int i = 0; i < n; i++) {
			switch (rand.nextInt(4)) {
				case 0 -> li.add(copy(calcPress));
				case 1 -> li.add(copy(engPress));
				case 2 -> li.add(copy(logicPress));
				case 3 -> li.add(copy(siliconPress));
			}
		}
		return li;
	}

	public ItemStack getSiliconPress() {
		return copy(siliconPress);
	}

	public ItemStack getLogicPress() {
		return copy(logicPress);
	}

	public ItemStack getCalcPress() {
		return copy(calcPress);
	}

	public ItemStack getEngPress() {
		return copy(engPress);
	}

	public ItemStack getBasicChipPattern() {
		return copy(basicChipPattern);
	}

	public ItemStack getAdvancedChipPattern() {
		return copy(advChipPattern);
	}

	public ItemStack getBasicCard() {
		return copy(basicChip);
	}

	public ItemStack getAdvancedCard() {
		return copy(advChip);
	}

	public ItemStack getSilicon() {
		return copy(silicon);
	}

	public ItemStack getGoldProcessor() {
		return copy(goldProcessor);
	}

	public ItemStack getQuartzProcessor() {
		return copy(quartzProcessor);
	}

	public ItemStack getDiamondProcessor() {
		return copy(diamondProcessor);
	}

	public Item get1KCell() {
		return cell1k;
	}

	public Item get4KCell() {
		return cell4k;
	}

	public Item get16KCell() {
		return cell16k;
	}

	public Item get64KCell() {
		return cell64k;
	}

	public ItemStack get1KStorage() {
		return copy(storage1k);
	}

	public ItemStack get4KStorage() {
		return copy(storage4k);
	}

	public ItemStack get16KStorage() {
		return copy(storage16k);
	}

	public ItemStack get64KStorage() {
		return copy(storage64k);
	}

	public ItemStack getBlankPattern() {
		return copy(blankPattern);
	}

	/** The crafting pattern; see the class notes. */
	public Item getEncodedPattern() {
		return encodedPattern;
	}

	public Item getProcessingPattern() {
		return processingPattern;
	}

	/**
	 * 1.7.10 invoked AE's {@code PartPlacement.place(..., INTERACT_FIRST_PASS)}: let the held item interact with the
	 * AE block (cable bus parts) at the position. Modern AE routes that through the block's own item-use handling.
	 */
	public boolean tryRightClick(ItemStack is, BlockPos pos, Direction sideHit, Player player, Level world, int depth) {
		if (!this.hasMod())
			return false;
		try {
			BlockState state = world.getBlockState(pos);
			if (!NS.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace()))
				return false;
			BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), sideHit, pos, false);
			InteractionResult res = state.useItemOn(is, world, player, InteractionHand.MAIN_HAND, hit);
			return res.consumesAction();
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

}
