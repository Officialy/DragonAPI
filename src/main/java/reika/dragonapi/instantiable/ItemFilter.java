package reika.dragonapi.instantiable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import reika.dragonapi.libraries.ReikaNBTHelper;
import reika.dragonapi.libraries.io.NBTCompat;
import reika.dragonapi.libraries.registry.ReikaItemHelper;
import reika.dragonapi.modinteract.deepinteract.MESystemReader.MatchMode;

import java.util.HashMap;

public abstract class ItemFilter {

    protected ItemFilter() {

    }

    public abstract void saveAdditional(CompoundTag tag);

    public abstract void load(CompoundTag tag);

    public abstract boolean matches(ItemStack is);

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object o);

    public abstract ItemStack getItem();

    public static final class ItemCategoryMatch extends ItemFilter {

        private static final HashMap<String, ItemCategory> categories = new HashMap();

        private ItemCategory category;

        public ItemCategoryMatch(ItemCategory cat) {
            category = cat;
        }

        public static void addCategory(ItemCategory cat) {
            categories.put(cat.getID(), cat);
        }

        public static ItemCategory getCategory(String cat) {
            return categories.get(cat);
        }

        public enum BasicCategories implements ItemCategory {
            ORE(),
            MOBDROP();

            BasicCategories() {
                addCategory(this);
            }

            @Override
            public boolean isItemInCategory(ItemStack is) {
                switch (this) {
                    case ORE:
                        break; //return Blocks.isOre(is);
                    case MOBDROP:
                        break;
                }
                return false;
            }

            @Override
            public String getID() {
                return this.name();
            }
        }

        public interface ItemCategory {

            boolean isItemInCategory(ItemStack is);

            String getID();

        }

        @Override
        public void saveAdditional(CompoundTag tag) {
            tag.putString("id", category.getID());
        }

        @Override
        public void load(CompoundTag tag) {
            category = getCategory(NBTCompat.getString(tag, "id", ""));
        }

        @Override
        public boolean matches(ItemStack is) {
            return category.isItemInCategory(is);
        }

        @Override
        public int hashCode() {
            return category.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof ItemCategoryMatch && category.equals(((ItemCategoryMatch) o).category);
        }

        @Override
        public ItemStack getItem() {
            return null;
        }

    }


    /**
     * 1.7.10 {@code ItemRule}: match one item under a {@link MatchMode}. {@code MatchMode} is nested in the AE reader
     * but {@link MatchMode#compare} touches no AE classes, so this filter works without AE2.
     */
    public static final class ItemRule extends ItemFilter {

        private ItemStack item;
        private MatchMode mode;

        public ItemRule(ItemStack is) {
            this(is, MatchMode.EXACTNONBT);
        }

        public ItemRule(ItemStack is, MatchMode m) {
            item = ReikaItemHelper.getSizedItemStack(is, is.getMaxStackSize());
            mode = m;
        }

        @Override
        public boolean matches(ItemStack is) {
            return mode.compare(is, item);
        }

        @Override
        public int hashCode() {
            return item.getItem().hashCode() ^ mode.ordinal();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof ItemRule ir) {
                return ItemStack.matches(item, ir.item) && mode == ir.mode;
            }
            return false;
        }

        @Override
        public String toString() {
            return item + " * " + mode;
        }

        @Override
        public ItemStack getItem() {
            return item.copy();
        }

        @Override
        public void load(CompoundTag tag) {
            ItemStack[] inv = ReikaNBTHelper.getInvFromNBT(tag.getCompoundOrEmpty("item"));
            item = inv.length > 0 && inv[0] != null ? inv[0] : ItemStack.EMPTY;
            mode = MatchMode.list[NBTCompat.getInt(tag, "mode", MatchMode.EXACTNONBT.ordinal())];
        }

        @Override
        public void saveAdditional(CompoundTag tag) {
            tag.putInt("mode", mode.ordinal());
            CompoundTag sub = new CompoundTag();
            ReikaNBTHelper.writeInvToNBT(new ItemStack[]{item}, sub);
            tag.put("item", sub);
        }

    }

}
