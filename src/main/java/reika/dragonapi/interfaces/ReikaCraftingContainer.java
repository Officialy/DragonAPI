//package reika.dragonapi.interfaces;
//
//import net.minecraft.world.Container;
//import net.minecraft.world.entity.player.Player;
//import net.minecraft.world.inventory.ClickType;
//import net.minecraft.world.inventory.CraftingContainer;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.block.entity.BlockEntity;
//import reika.dragonapi.auxiliary.trackers.KeyWatcher;
//import reika.dragonapi.base.BlockEntityBase;
//import reika.dragonapi.base.CoreMenu;
//import reika.dragonapi.interfaces.blockentity.CraftingTile;
//import reika.dragonapi.libraries.registry.ReikaItemHelper;
//
//public abstract class ReikaCraftingContainer<V> extends CoreMenu {
//
//    private Level world;
//    private CraftingContainer craftMatrix = new CraftingContainer(this, 3, 3);
//    private Container craftResult = new InventoryCraftResult();
//    private boolean noUpdate;
//    private CraftingTile<V> crafter;
//    public final boolean isGUI;
//
//    public ReikaCraftingContainer(Player player, CraftingTile<V> te, Level worldObj, boolean gui) {
//        super(player, (BlockEntity)te);
//        world = worldObj;
//        crafter = te;
//        this.isGUI = gui;
//    }
//
//    protected final void updateCraftMatrix() {
//        for (int i = 0; i < 9; i++) {
//            ItemStack stack = crafter.getItem(i);
//            noUpdate = true;
//            craftMatrix.setItem(i, stack);
//        }
//    }
//
//    protected final void onCraftMatrixChanged()  {
//        this.onCraftMatrixChanged(craftMatrix);
//    }
//
//    @Override
//    public void slotsChanged(Container container) {
//        if (!this.isGUI) {
//            super.slotsChanged(container);
//            return;
//        }
//        if (noUpdate) {
//            noUpdate = false;
//            return;
//        }
//        V wr = this.getRecipe(craftMatrix, world);
//        if (wr == null) {
//            crafter.setToCraft(null);
//            return;
//        }
//        ItemStack is = this.getOutput(wr);
//        ItemStack slot13 = crafter.getItem(crafter.getOutputSlot());
//        if (slot13 != null) {
//            if (is.getItem() != slot13.getItem())
//                return;
////            if (is.getItemDamage() != slot13.getItemDamage())
////                return;
//            if (slot13.getCount() >= slot13.getMaxStackSize())
//                return;
//        }
//        crafter.setToCraft(wr);
//    }
//
//    private void craft(V wr, Player ep) {
//        if (crafter.handleCrafting(wr, ep, KeyWatcher.instance.isKeyDown(ep, KeyWatcher.Key.LSHIFT)))
//            this.updateCraftMatrix();
//        //tile.craftable = false;
//    }
//
//    @Override
//    public void clicked(int slot, int button, ClickType action, Player ep) {
//		/*
//		if (slot >= 18 && slot < tile.getSizeInventory()) {
//			ItemStack held = ep.inventory.getItemStack();
//			tile.setMapping(slot, ReikaItemHelper.getSizedItemStack(held, 1));
//			return held;
//		}
//		 */
//
//        //if (action == 4 && slot >= 18 && slot < tile.getSizeInventory())
//        //	action = 0;
//
//        ItemStack is = super.clicked(slot, button, action, ep);
//        this.updateCraftMatrix();
//        this.onCraftMatrixChanged(craftMatrix);
//        Player ip = ep;
//        //ReikaJavaLibrary.pConsole(ip.getItemStack());
//        V wr = this.getRecipe(craftMatrix, world);
//        if (wr != null && crafter.isReadyToCraft() && slot == 13) {
//            ItemStack drop = ip.getUseItem();
//            ItemStack craft = this.getOutput(wr);
//            if (drop != null && (!ReikaItemHelper.matchStacks(drop, craft) || drop.getCount()+craft.getCount() > drop.getMaxStackSize()))
//                return is;
//            this.craft(wr, ep);
//            craft.onCraftedBy(world, ep, craft.getCount());
//            int outslot = crafter.getOutputSlot();
//            if (drop == null)
//                ip.addItem(crafter.getItem(outslot));
//            else
//                drop.setCount(drop.getCount() + crafter.getItem(outslot).getCount());
//            crafter.setItem(outslot, ItemStack.EMPTY);
//        }
//        return is;
//    }
//
//    protected abstract ItemStack getOutput(V wr);
//
//    protected abstract V getRecipe(CraftingContainer craftMatrix, Level world);
//}
