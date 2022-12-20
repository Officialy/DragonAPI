package reika.dragonapi.auxiliary.trackers;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.exception.MisuseException;
import reika.dragonapi.exception.WTFException;
import reika.dragonapi.instantiable.ItemMaterial;
import reika.dragonapi.instantiable.data.maps.ItemHashMap;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

import java.util.ArrayList;

@Deprecated /** this is impossible to keep reasoanbly updated */
public class ItemMaterialController {

    private final ItemHashMap<ItemMaterial> data = new ItemHashMap<>();
    private final ArrayList<ItemStack> locks = new ArrayList<>();

    public static final ItemMaterialController instance = new ItemMaterialController();

    private ItemMaterialController() {
        this.addVanillaItem(Items.GOLDEN_HOE, ItemMaterial.GOLD);
        this.addVanillaItem(Items.GOLDEN_PICKAXE, ItemMaterial.GOLD);
        this.addVanillaItem(Items.GOLDEN_SHOVEL, ItemMaterial.GOLD);
        this.addVanillaItem(Items.GOLDEN_SWORD, ItemMaterial.GOLD);
        this.addVanillaItem(Items.GOLDEN_AXE, ItemMaterial.GOLD);
        this.addVanillaItem(Blocks.POWERED_RAIL, ItemMaterial.GOLD);
        this.addVanillaItem(Items.GOLDEN_HELMET, ItemMaterial.GOLD);
        this.addVanillaItem(Items.GOLDEN_BOOTS, ItemMaterial.GOLD);
        this.addVanillaItem(Items.GOLDEN_LEGGINGS, ItemMaterial.GOLD);
        this.addVanillaItem(Items.GOLDEN_CHESTPLATE, ItemMaterial.GOLD);

        this.addVanillaItem(Items.IRON_HOE, ItemMaterial.IRON);
        this.addVanillaItem(Items.IRON_PICKAXE, ItemMaterial.IRON);
        this.addVanillaItem(Items.IRON_SHOVEL, ItemMaterial.IRON);
        this.addVanillaItem(Items.IRON_SWORD, ItemMaterial.IRON);
        this.addVanillaItem(Items.IRON_AXE, ItemMaterial.IRON);
        this.addVanillaItem(Blocks.DETECTOR_RAIL, ItemMaterial.IRON);
        this.addVanillaItem(Blocks.RAIL, ItemMaterial.IRON);
        this.addVanillaItem(Blocks.ACTIVATOR_RAIL, ItemMaterial.IRON);
        this.addVanillaItem(Items.IRON_HELMET, ItemMaterial.IRON);
        this.addVanillaItem(Items.IRON_BOOTS, ItemMaterial.IRON);
        this.addVanillaItem(Items.IRON_LEGGINGS, ItemMaterial.IRON);
        this.addVanillaItem(Items.IRON_CHESTPLATE, ItemMaterial.IRON);
        this.addVanillaItem(Items.FLINT_AND_STEEL, ItemMaterial.IRON);
        this.addVanillaItem(Blocks.IRON_BARS, ItemMaterial.IRON);
        this.addVanillaItem(Items.CAULDRON, ItemMaterial.IRON);
        this.addVanillaItem(Blocks.ANVIL, ItemMaterial.IRON);
        this.addVanillaItem(Blocks.HOPPER, ItemMaterial.IRON);
        this.addVanillaItem(Items.IRON_DOOR, ItemMaterial.IRON);
        this.addVanillaItem(Items.BUCKET, ItemMaterial.IRON);
        this.addVanillaItem(Items.MINECART, ItemMaterial.IRON);

        this.addVanillaItem(Blocks.OBSIDIAN, ItemMaterial.OBSIDIAN);
        this.addVanillaItem(Blocks.DIAMOND_BLOCK, ItemMaterial.DIAMOND);
        this.addVanillaItem(Blocks.IRON_BLOCK, ItemMaterial.IRON);
        this.addVanillaItem(Blocks.GOLD_BLOCK, ItemMaterial.GOLD);
        this.addVanillaItem(Blocks.STONE, ItemMaterial.STONE);
        this.addVanillaItem(Blocks.COBBLESTONE, ItemMaterial.STONE);
        this.addVanillaItem(Blocks.STONE_BRICKS, ItemMaterial.STONE);
        this.addVanillaItem(Blocks.BRICKS, ItemMaterial.STONE);
        this.addVanillaItem(Blocks.BOOKSHELF, ItemMaterial.WOOD);
//        this.addVanillaItem(Items.WOODEN_DOOR, ItemMaterial.WOOD); todo wood types
//        this.addVanillaItem(Items.SIGN, ItemMaterial.WOOD); todo wood types
        this.addVanillaItem(Blocks.CRAFTING_TABLE, ItemMaterial.WOOD);
        this.addVanillaItem(Blocks.CHEST, ItemMaterial.WOOD);
        this.addVanillaItem(Blocks.TRAPPED_CHEST, ItemMaterial.WOOD);
        this.addVanillaItem(Blocks.OAK_STAIRS, ItemMaterial.WOOD);
        this.addVanillaItem(Blocks.BIRCH_STAIRS, ItemMaterial.WOOD);
        this.addVanillaItem(Blocks.SPRUCE_STAIRS, ItemMaterial.WOOD);
        this.addVanillaItem(Blocks.JUNGLE_STAIRS, ItemMaterial.WOOD);
        this.addVanillaItem(Items.STICK, ItemMaterial.WOOD);
        this.addVanillaItem(Items.BOWL, ItemMaterial.WOOD);
        this.addVanillaItem(Items.WOODEN_SWORD, ItemMaterial.WOOD);
        this.addVanillaItem(Items.WOODEN_PICKAXE, ItemMaterial.WOOD);
        this.addVanillaItem(Items.WOODEN_AXE, ItemMaterial.WOOD);
        this.addVanillaItem(Items.WOODEN_SHOVEL, ItemMaterial.WOOD);

   /* todo    for (int i = 0; i < 4; i++) {
            this.addVanillaItem(new ItemStack(Blocks.PLANKS, 1, i), ItemMaterial.WOOD);
            this.addVanillaItem(new ItemStack(Blocks.log, 1, i), ItemMaterial.WOOD);;
            this.addVanillaItem(new ItemStack(Blocks.log2, 1, i), ItemMaterial.WOOD);
        }*/

        this.addVanillaItem(Items.DIAMOND, ItemMaterial.DIAMOND);
        this.addVanillaItem(Items.IRON_INGOT, ItemMaterial.IRON);
        this.addVanillaItem(Items.GOLD_INGOT, ItemMaterial.GOLD);

        this.addVanillaItem(Items.COAL, ItemMaterial.COAL);
        this.addVanillaItem(Items.CHARCOAL, ItemMaterial.COAL);
        this.addVanillaItem(Blocks.COAL_BLOCK, ItemMaterial.COAL);
    }

    public void addItem(ItemStack is, ItemMaterial mat) {
        if (this.hasImmutableMapping(is))
            throw new MisuseException("Do not try to overwrite mappings of vanilla items!");
        DragonAPI.LOGGER.info("DRAGONAPI: Adding "+mat+" material properties to "+is);
        data.put(is, mat);
    }

    public void addItem(Item i, ItemMaterial mat) {
        this.addItem(new ItemStack(i), mat);
    }

    public void addItem(Block b, ItemMaterial mat) {
        this.addItem(new ItemStack(b), mat);
    }

    public boolean hasImmutableMapping(ItemStack is) {
        return ReikaItemHelper.collectionContainsItemStack(locks, is);
    }

    private void addVanillaItem(ItemStack is, ItemMaterial mat) {
        if (this.hasImmutableMapping(is))
            throw new MisuseException("Do not try to overwrite mappings of vanilla items!");
        DragonAPI.LOGGER.info("Adding immutable material "+mat+" properties to vanilla item "+is);
        data.put(is, mat);
        locks.add(is);
    }

    private void addVanillaItem(Item i, ItemMaterial mat) {
        if (i == null)
            throw new WTFException("Some mod is deleting the vanilla item "+i+"!", true);
        this.addVanillaItem(new ItemStack(i), mat);
    }

    private void addVanillaItem(Block b, ItemMaterial mat) {
        if (b == null)
            throw new WTFException("Some mod is deleting the vanilla block "+b+"!", true);
        if (Item.BY_BLOCK.get(b) == null)
            DragonAPI.LOGGER.error("Block "+b+" has no corresponding item!");
        else
            this.addVanillaItem(new ItemStack(b), mat);
    }

    public int getMeltingPoint(ItemStack is) {
        if (!this.hasDataFor(is))
            return 0;
        return data.get(is).getMelting();
    }

    public boolean hasDataFor(ItemStack is) {
        return data.containsKey(is);
    }

    public ItemMaterial getMaterial(ItemStack is) {
        return data.get(is);
    }

}
