package reika.dragonapi.instantiable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

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

        public static enum BasicCategories implements ItemCategory {
            ORE(),
            MOBDROP();

            private BasicCategories() {
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

        public static interface ItemCategory {

            public boolean isItemInCategory(ItemStack is);

            public String getID();

        }

        @Override
        public void saveAdditional(CompoundTag tag) {
            tag.putString("id", category.getID());
        }

        @Override
        public void load(CompoundTag tag) {
            category = getCategory(tag.getString("id"));
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

}
