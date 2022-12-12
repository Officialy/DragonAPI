/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.data;


import reika.dragonapi.exception.MisuseException;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;


public final class KeyedItemStack implements Comparable<KeyedItemStack> {

    private final ItemStack item;
    private final boolean[] enabledCriteria = new boolean[Criteria.list.length];
    private boolean lock = false;
    private boolean simpleHash = false;

    public KeyedItemStack(Block b) {
        this(Item.BY_BLOCK.get(b));
    }

    public KeyedItemStack(Item i) {
        this(new ItemStack(i, 1));
        this.setSized(false);
        this.setIgnoreNBT(true);
        this.setSimpleHash(true);
    }

    public KeyedItemStack(ItemStack is) {
        if (is == null || is.getItem() == null)
            throw new MisuseException("You cannot key a null itemstack!");
        item = is.copy();
        for (int i = 0; i < enabledCriteria.length; i++)
            enabledCriteria[i] = Criteria.list[i].defaultState;
    }

    public static KeyedItemStack load(CompoundTag nbt) {
        return new KeyedItemStack(ItemStack.of(nbt)).setIgnoreNBT(nbt.getBoolean("ignorenbt")).setSized(nbt.getBoolean("sized")).setSimpleHash(nbt.getBoolean("simplehash"));
    }

    public KeyedItemStack setSized(boolean size) {
        if (!lock)
            enabledCriteria[Criteria.SIZE.ordinal()] = size;
        return this;
    }


    public KeyedItemStack setIgnoreNBT(boolean ignore) {
        if (!lock)
            enabledCriteria[Criteria.NBT.ordinal()] = !ignore;
        return this;
    }

    public KeyedItemStack setSimpleHash(boolean flag) {
        if (!lock)
            simpleHash = flag;
        return this;
    }

    public KeyedItemStack lock() {
        lock = true;
        return this;
    }

    @Override
    public final int hashCode() {
        if (simpleHash)
            return item.getItem().hashCode();
        int hash = 0;
        for (int i = 0; i < Criteria.list.length; i++) {
            Criteria c = Criteria.list[i];
            if (enabledCriteria[i])
                hash += c.hash(this) << i;
        }
        return hash;
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof KeyedItemStack) {
            KeyedItemStack ks = (KeyedItemStack) o;
            return this.match(ks, false);
        }
        return false;
    }

    public boolean exactMatch(KeyedItemStack ks) {
        return this.match(ks, true);
    }

    private boolean match(KeyedItemStack ks, boolean force) {
        for (int i = 0; i < Criteria.list.length; i++) {
            Criteria c = Criteria.list[i];
            if ((force || (enabledCriteria[i] && ks.enabledCriteria[i])) && !c.match(this, ks))
                return false;
        }
        return true;
    }

    public boolean match(ItemStack is) {
        KeyedItemStack ks = new KeyedItemStack(is);
        ks.setSimpleHash(simpleHash);
        for (int i = 0; i < Criteria.list.length; i++) {
            ks.enabledCriteria[i] = enabledCriteria[i];
        }
        return this.equals(ks);
    }

    public ItemStack getItemStack() {
        return item.copy();
    }

    @Override
    public String toString() {
        return item.toString() + "|" + this.getCriteriaFlags();
    }

    private String getCriteriaFlags() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < enabledCriteria.length; i++) {
            sb.append(enabledCriteria[i] ? "1" : "0");
        }
        return sb.toString();
    }

    public KeyedItemStack copy() {
        KeyedItemStack ks = new KeyedItemStack(item.copy());
        ks.setSimpleHash(simpleHash);
        for (int i = 0; i < Criteria.list.length; i++) {
            ks.enabledCriteria[i] = enabledCriteria[i];
        }
        ks.lock = lock;
        return ks;
    }

    public String getCriteriaAsChatFormatting() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < enabledCriteria.length; i++) {
            if (enabledCriteria[i])
                sb.append(Criteria.list[i].chatChar.toString());
        }
        return sb.toString();
    }

    public void saveAdditional(CompoundTag nbt) {
        item.save(nbt);
        nbt.putBoolean("sized", enabledCriteria[Criteria.SIZE.ordinal()]);
        nbt.putBoolean("ignorenbt", !enabledCriteria[Criteria.NBT.ordinal()]);
        nbt.putBoolean("ignoremeta", !enabledCriteria[Criteria.METADATA.ordinal()]);
        nbt.putBoolean("useID", enabledCriteria[Criteria.ID.ordinal()]);
        nbt.putBoolean("simplehash", simpleHash);
    }

    public boolean contains(KeyedItemStack ks) {
        if (!this.exactMatch(ks) && this.equals(ks)) {
            boolean flag = true;
            for (int i = 0; i < Criteria.list.length; i++) {
                Criteria c = Criteria.list[i];
                if (!enabledCriteria[i] && ks.enabledCriteria[i]) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int compareTo(KeyedItemStack o) {
        //return ReikaItemHelper.comparator.compare(item, o.item);
        return 1; //- TODO compareto?
    }

    public String getDisplayName() {
        String base = item.getDisplayName().toString();
        //if (item.getItem() instanceof EnchantedBookItem)
        //	base = base + ": " + ReikaEnchantmentHelper.getEnchantmentsDisplay(item);
        return base;
    }

    private enum Criteria {
        ID(true, ChatFormatting.RESET), //none
        METADATA(true, ChatFormatting.LIGHT_PURPLE),
        SIZE(false, ChatFormatting.BOLD),
        NBT(true, ChatFormatting.UNDERLINE);

        private static final Criteria[] list = values();
        private final boolean defaultState;
        private final ChatFormatting chatChar;

        Criteria(boolean b, ChatFormatting f) {
            defaultState = b;
            chatChar = f;
        }

        public int hash(KeyedItemStack ks) {
            switch (this) {
                case ID:
                    return ks.item.getItem().hashCode();
                case SIZE:
                    return ks.item.getCount();
                case NBT:
                    return ks.item.getTag() != null ? ks.item.getTag().hashCode() : -1;
                default:
                    return 0;
            }
        }

        private boolean match(KeyedItemStack k1, KeyedItemStack k2) {
            switch (this) {
                case ID:
                    return k1.item.getItem() == k2.item.getItem();
                case SIZE:
                    return k1.item.getCount() == k2.item.getCount();
                case NBT:
                    return ItemStack.matches(k1.item, k2.item);
            }
            return false;
        }
    }

}
