package reika.dragonapi.instantiable.gui;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ArmorSlot extends Slot {

    private final Inventory player;
    public final EquipmentSlot armorType;

    public ArmorSlot(Inventory ep, int index, int x, int y, int type) {
        super(ep, index, x, y);
        player = ep;
        armorType = switch (type) {
            case 0 -> EquipmentSlot.HEAD;
            case 1 -> EquipmentSlot.CHEST;
            case 2 -> EquipmentSlot.LEGS;
            case 3 -> EquipmentSlot.FEET;
            default -> throw new IllegalArgumentException("Invalid armor slot type: " + type);
        };
    }

    @Override
    public boolean mayPlace(ItemStack is) {
        return is.isEmpty() && is.getItem().canEquip(is, armorType, player.player);
    }
    @Override
    public int getMaxStackSize() {
        return 1;
    }
/*    @Override
    @SideOnly(Side.CLIENT)
    public final IIcon getBackgroundIconIndex()
    {
        return ItemArmor.func_94602_b(armorType);
    }*/
}
