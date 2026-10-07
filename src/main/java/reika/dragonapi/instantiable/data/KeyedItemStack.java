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


import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import reika.dragonapi.exception.MisuseException;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import reika.dragonapi.libraries.io.NBTCompat;


public final class KeyedItemStack implements Comparable<KeyedItemStack> {

    private static final com.google.common.collect.Interner<ComponentOrder> COMPONENT_ORDERS =
            com.google.common.collect.Interners.newWeakInterner();
    private static final java.util.concurrent.atomic.AtomicLong NEXT_COMPONENT_ORDER = new java.util.concurrent.atomic.AtomicLong();
    private final ItemStack item;
    private final ComponentOrder componentOrder;
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
        componentOrder = COMPONENT_ORDERS.intern(new ComponentOrder(
                net.minecraft.core.component.DataComponentMap.builder().addAll(item.getComponents()).build(),
                NEXT_COMPONENT_ORDER.getAndIncrement()));
        for (int i = 0; i < enabledCriteria.length; i++)
            enabledCriteria[i] = Criteria.list[i].defaultState;
    }

    public static KeyedItemStack load(CompoundTag nbt) {
        return load(nbt, null);
    }

    public static KeyedItemStack load(CompoundTag nbt, HolderLookup.Provider provider) {
        boolean ignore = NBTCompat.getBoolean(nbt, "ignorenbt", false);
        boolean sized = NBTCompat.getBoolean(nbt, "sized", false);
        boolean simple = NBTCompat.getBoolean(nbt, "simplehash", false);
        return new KeyedItemStack(ItemStack.OPTIONAL_CODEC.parse(provider != null ? provider.createSerializationContext(NbtOps.INSTANCE) : NbtOps.INSTANCE, nbt).result().orElse(ItemStack.EMPTY)).setIgnoreNBT(ignore).setSized(sized).setSimpleHash(simple)
                .setIgnoreMetadata(NBTCompat.getBoolean(nbt, "ignoremeta", false));
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
    public int hashCode() {
        // Item-only hashing remains compatible with legacy simpleHash callers.
        return item.getItem().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof KeyedItemStack ks && java.util.Arrays.equals(enabledCriteria, ks.enabledCriteria)
                && this.match(ks, false);
    }

    /** Matching may deliberately ignore criteria; it is not a collection equality relation. */
    public boolean matches(KeyedItemStack ks) {
        return this.match(ks, false);
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
        System.arraycopy(enabledCriteria, 0, ks.enabledCriteria, 0, Criteria.list.length);
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
        System.arraycopy(enabledCriteria, 0, ks.enabledCriteria, 0, Criteria.list.length);
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
        saveAdditional(nbt, null);
    }

    public void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        nbt.merge((CompoundTag) ItemStack.OPTIONAL_CODEC.encodeStart(provider != null ? provider.createSerializationContext(NbtOps.INSTANCE) : NbtOps.INSTANCE, item).getOrThrow());
        nbt.putBoolean("sized", enabledCriteria[Criteria.SIZE.ordinal()]);
        nbt.putBoolean("ignorenbt", !enabledCriteria[Criteria.NBT.ordinal()]);
        nbt.putBoolean("ignoremeta", !enabledCriteria[Criteria.METADATA.ordinal()]);
        nbt.putBoolean("useID", enabledCriteria[Criteria.ID.ordinal()]);
        nbt.putBoolean("simplehash", simpleHash);
    }

    public boolean contains(KeyedItemStack ks) {
        if (!this.exactMatch(ks) && this.matches(ks)) {
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
        int result = this.getCriteriaFlags().compareTo(o.getCriteriaFlags());
        if (result != 0) return result;
        if (enabledCriteria[Criteria.ID.ordinal()]) {
            result = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem())
                    .compareTo(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(o.item.getItem()));
            if (result != 0) return result;
        }
        if (enabledCriteria[Criteria.METADATA.ordinal()]) {
            result = Integer.compare(item.getDamageValue(), o.item.getDamageValue());
            if (result != 0) return result;
        }
        if (enabledCriteria[Criteria.SIZE.ordinal()]) {
            result = Integer.compare(item.getCount(), o.item.getCount());
            if (result != 0) return result;
        }
        if (enabledCriteria[Criteria.NBT.ordinal()] && !componentOrder.components().equals(o.componentOrder.components()))
            return Long.compare(componentOrder.order(), o.componentOrder.order());
        return 0;
    }

    /** Native components include transient values without a persistence codec. Intern immutable
     * snapshots to give their equality classes a stable order for the lifetime of live keys.
     * This ordering is process-local; it is not a persisted id or a world-generation seed. */
    private record ComponentOrder(net.minecraft.core.component.DataComponentMap components, long order) {
        @Override public boolean equals(Object other) {
            return other instanceof ComponentOrder value && components.equals(value.components);
        }
        @Override public int hashCode() { return components.hashCode(); }
    }

    public KeyedItemStack setIgnoreMetadata(boolean ignore) {
        if (!lock) enabledCriteria[Criteria.METADATA.ordinal()] = !ignore;
        return this;
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
                    return ks.item.getComponents().hashCode();
                default:
                    return 0;
            }
        }

        private boolean match(KeyedItemStack k1, KeyedItemStack k2) {
            switch (this) {
                case ID:
                    return k1.item.getItem() == k2.item.getItem();
                case METADATA: //1.7.10 item damage
                    return k1.item.getDamageValue() == k2.item.getDamageValue();
                case SIZE:
                    return k1.item.getCount() == k2.item.getCount();
                case NBT: //components only; size is its own criterion (ItemStack.matches also compared counts)
                    return k1.componentOrder.components().equals(k2.componentOrder.components());
            }
            return false;
        }
    }

}
