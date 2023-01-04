/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.libraries;

import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.exception.MisuseException;
import reika.dragonapi.instantiable.data.KeyedItemStack;
import reika.dragonapi.instantiable.io.LuaBlock;
import reika.dragonapi.libraries.java.ReikaStringParser;

import java.util.*;
import java.util.Map.Entry;

public final class ReikaNBTHelper extends DragonAPI {

    private static final HashMap<Class, EnumIO> enumIOMap = new HashMap<>();

    /**
     * Saves an inventory to NBT. Args: Inventory, NBT Tag
     */
    public static void writeInvToNBT(ItemStack[] inv, CompoundTag NBT) {
        ListTag ListTag = new ListTag();
        for (int i = 0; i < inv.length; i++) {
            if (inv[i] != null) {
                CompoundTag CompoundTag = new CompoundTag();
                CompoundTag.putByte("Slot", (byte) i);
                inv[i].save(CompoundTag);
                ListTag.add(CompoundTag);
            }
        }

        NBT.put("Items", ListTag);
    }

    public static CompoundTag constructNBT(LuaBlock lb) {
        if (lb.isList())
            throw new IllegalArgumentException("The top-level LuaBlock must be a map type (root NBTTagCompound)!");
        CompoundTag tag = new CompoundTag();
        addMapToTags(tag, lb.asHashMap());
        return tag;
    }

    /**
     * Reads an inventory from NBT. Args: NBT Tag
     */
    public static ItemStack[] getInvFromNBT(CompoundTag NBT) {
        ListTag ListTag = NBT.getList("Items", Tag.TAG_COMPOUND);
        ItemStack[] inv = new ItemStack[ListTag.size()];

        for (int i = 0; i < ListTag.size(); i++) {
            CompoundTag CompoundTag = ListTag.getCompound(i);
            byte byte0 = CompoundTag.getByte("Slot");

            if (byte0 >= 0 && byte0 < inv.length) {
                inv[byte0] = ItemStack.of(CompoundTag);
            }
        }
        return inv;
    }

    @Deprecated //Use FluidStack.loadFluidStackFromNBT(nbt); TODO: Remove
    public static FluidStack getFluidFromNBT(CompoundTag nbt) {
        return FluidStack.loadFluidStackFromNBT(nbt);
    }

    @Deprecated //Can likely use f.writeToNBT(nbt); //TODO: Remove
    public static void writeFluidToNBT(CompoundTag nbt, FluidStack f) {
        f.writeToNBT(nbt);
    }

    public static Object getValue(Tag NBT) {
        return getValue(NBT, null);
    }

    public static Object getValue(Tag NBT, NBTIO converter) {
        if (converter != null) {
            return converter.createFromNBT(NBT);
        } else if (NBT instanceof IntTag) {
            return ((IntTag) NBT).getAsInt();
        } else if (NBT instanceof ByteTag) {
            return ((ByteTag) NBT).getAsByte();
        } else if (NBT instanceof ShortTag) {
            return ((ShortTag) NBT).getAsShort();
        } else if (NBT instanceof LongTag) {
            return ((LongTag) NBT).getAsLong();
        } else if (NBT instanceof FloatTag) {
            return ((FloatTag) NBT).getAsFloat();
        } else if (NBT instanceof DoubleTag) {
            return ((DoubleTag) NBT).getAsDouble();
        } else if (NBT instanceof IntArrayTag) {
            return ((IntArrayTag) NBT).getAsIntArray();
        } else if (NBT instanceof StringTag) {
            return NBT.getAsString();
        } else if (NBT instanceof ByteArrayTag) {
            return ((ByteArrayTag) NBT).getAsByteArray();
        } else if (NBT instanceof CompoundTag) {
            if (((CompoundTag) NBT).getBoolean("flag_isItemStack")) {
                return ItemStack.of((CompoundTag) NBT);
            } else {
                HashMap<String, Object> map = new HashMap();
                CompoundTag tag = (CompoundTag) NBT;
                for (Object o : tag.getAllKeys()) {
                    String s = (String) o;
                    map.put(s, getValue(tag.get(s)));
                }
                return map;
            }
        } else if (NBT instanceof ListTag) {
            ArrayList li = new ArrayList<>();
            for (Tag o : ((ListTag) NBT)) {
                li.add(getValue(o));
            }
            return li;
        } else {
            return null;
        }
    }

    public static Tag getTagForObject(Object o) {
        return getTagForObject(o, null);
    }

    public static Tag getTagForObject(Object o, NBTIO converter) {
        if (converter != null) {
            return converter.convertToNBT(o);
        } else if (o instanceof Integer i) {
            return IntTag.valueOf(i);
        } else if (o instanceof Byte b) {
            return ByteTag.valueOf(b);
        } else if (o instanceof Short s) {
            return ShortTag.valueOf(s);
        } else if (o instanceof Long l) {
            return LongTag.valueOf(l);
        } else if (o instanceof Float f) {
            return FloatTag.valueOf(f);
        } else if (o instanceof Double d) {
            return DoubleTag.valueOf(d);
        } else if (o instanceof int[]) {
            return new IntArrayTag((int[]) o);
        } else if (o instanceof String s) {
            return StringTag.valueOf(s);
        } else if (o instanceof byte[]) {
            return new ByteArrayTag((byte[]) o);
        } else if (o instanceof Map) {
            CompoundTag tag = new CompoundTag();
            Map m = (Map) o;
            for (Object k : m.keySet()) {
                if (k instanceof String) {
                    tag.put((String) k, getTagForObject(m.get(k)));
                }
            }
            return tag;
        } else if (o instanceof List) {
            ListTag li = new ListTag();
            for (Object o2 : ((List) o)) {
                li.add(getTagForObject(o2));
            }
            return li;
        } else if (o instanceof Tag) {
            return (Tag) o;
        } else if (o instanceof ItemStack) {
            CompoundTag tag = ((ItemStack) o).save(new CompoundTag());
            tag.putBoolean("flag_isItemStack", true);
            return tag;
        } else {
            return null;
        }
    }

    public static boolean isIntNumberTag(Tag tag) {
        return tag instanceof IntTag || tag instanceof ByteTag || tag instanceof ShortTag || tag instanceof LongTag;
    }

    public static boolean isNumberTag(Tag tag) {
        return isIntNumberTag(tag) || tag instanceof FloatTag || tag instanceof DoubleTag;
    }

    public static Tag compressNumber(Tag tag) {
        if (!isIntNumberTag(tag))
            throw new MisuseException("Only integer-type numbers (byte, short, int, and long) can be compressed!");
        long value = (Long) getValue(tag);
        if (value > Integer.MAX_VALUE) {
            return LongTag.valueOf(value);
        } else if (value > Short.MAX_VALUE) {
            return IntTag.valueOf((int) value);
        } else if (value > Byte.MAX_VALUE) {
            return ShortTag.valueOf((short) value);
        } else {
            return ByteTag.valueOf((byte) value);
        }
    }

    public static ArrayList<String> parseNBTAsLines(CompoundTag nbt) {
        return parseNBTAsLines(nbt, 0);
    }

    private static ArrayList<String> parseNBTAsLines(CompoundTag nbt, int indent) {
        ArrayList<String> li = new ArrayList<>();
        String idt = ReikaStringParser.getNOf("  ", indent);
        for (String o : nbt.getAllKeys()) {
            Tag b = nbt.get(o);
            if (b instanceof CompoundTag) {
                li.add(idt + o + ": ");
                li.addAll(parseNBTAsLines((CompoundTag) b, indent + 1));
            } else {
                li.add(idt + o + ": " + b.toString());
            }
        }
        return li;
    }

    public static void combineNBT(CompoundTag tag1, CompoundTag tag2) {
        if (tag2 == null || tag2.isEmpty())
            return;
        for (Object o : tag2.getAllKeys()) {
            String s = (String) o;
            Tag key = tag2.get(s);
            tag1.put(s, combineTags(tag1.get(s), key.copy()));
        }
    }

    private static Tag combineTags(Tag a, Tag b) {
        if (a != null && b == null)
            return a;
        if (a == null || a.getClass() != b.getClass())
            return b;
        if (a instanceof CompoundTag) {
            combineNBT((CompoundTag) a, (CompoundTag) b);
            return a;
        }
        if (a instanceof ListTag) {
            for (Object o : ((ListTag) b)) {
                ((ListTag) a).add((Tag) o);
            }
        }
        return b;
    }

    public static void clearTagCompound(CompoundTag dat) {
        Collection<String> tags = new ArrayList(dat.getAllKeys());
        for (String tag : tags) {
            dat.remove(tag);
        }
    }

    public static void copyNBT(CompoundTag from, CompoundTag to) {
        Collection<String> tags = new ArrayList(from.getAllKeys());
        for (String tag : tags) {
            to.put(tag, from.get(tag).copy());
        }
    }

    public static int compareNBTTags(CompoundTag o1, CompoundTag o2) {
        if (o1 == o2 || (o1 != null && o1.equals(o2))) {
            return 0;
        } else if (o1 == null) {
            return -1;
        } else if (o2 == null) {
            return 1;
        } else {
            return o1.hashCode() - o2.hashCode();
        }
    }

    public static boolean areNBTTagsEqual(CompoundTag tag1, CompoundTag tag2) {
        if (tag1 == tag2)
            return true;
        if (tag1 == null || tag2 == null)
            return false;
        return tag1.equals(tag2);
    }

    public static boolean tagContains(CompoundTag tag, CompoundTag inner) {
        Set<String> set = inner.getAllKeys();
        for (String s : set) {
            Tag b1 = inner.get(s);
            Tag b2 = tag.get(s);
            if (b1 == b2)
                continue;
            if (b1 == null || b2 == null)
                return false;
            if (!b1.equals(b2))
                return false;
        }
        return true;
    }

    public static void overwriteNBT(CompoundTag tag, CompoundTag over) {
        for (Object o : over.getAllKeys()) {
            Tag b = over.get((String) o);
            tag.put((String) o, b);
        }
    }

    public static void addListToTags(ListTag tag, List<Object> li) {
        for (Object o : li) {
            Tag b = getTagForObject(o);
            if (b != null) {
                tag.add(b);
            }
        }
    }

    public static void addMapToTags(CompoundTag tag, HashMap<String, Object> map) {
        for (String s : map.keySet()) {
            Object o = map.get(s);
            Tag b = getTagForObject(o);
            if (b != null) {
                tag.put(s, b);
            }
        }
    }

    public static HashMap<String, ?> readMapFromNBT(CompoundTag tag) {
        return (HashMap<String, ?>) getValue(tag);
    }

    public static void writeMapToNBT(String s, CompoundTag tag, Map<String, ?> map) {
        Tag dat = getTagForObject(map);
        tag.put(s, dat);
    }

    public static <K, V> void writeMapToNBT(Map<K, V> map, ListTag li, NBTIO<K> converterK, NBTIO<V> converterV) {
        for (Entry<K, V> e : map.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.put("key", getTagForObject(e.getKey(), converterK));
            entry.put("value", getTagForObject(e.getValue(), converterV));
            li.add(entry);
        }
    }

    public static <K, V> void readMapFromNBT(Map<K, V> map, ListTag li, NBTIO<K> converterK, NBTIO<V> converterV) {
        map.clear();
        for (Object o : li) {
            CompoundTag entry = (CompoundTag) o;
            K key = (K) getValue(entry.getCompound("key"), converterK);
            V val = (V) getValue(entry.getCompound("value"), converterV);
            map.put(key, val);
        }
    }

    public static <E> void writeCollectionToNBT(Collection<E> c, CompoundTag NBT, String key) {
        writeCollectionToNBT(c, NBT, key, null);
    }

    public static <E> void writeCollectionToNBT(Collection<E> c, CompoundTag NBT, String key, NBTIO<E> converter) {
        ListTag li = new ListTag();
        for (Object o : c) {
            Tag b = getTagForObject(o, converter);
            CompoundTag tag = new CompoundTag();
            tag.put("value", b);
            li.add(tag);
        }
        NBT.put(key, li);
    }

    public static <E> void readCollectionFromNBT(Collection<E> c, CompoundTag NBT, String key) {
        readCollectionFromNBT(c, NBT, key, null);
    }

    public static <E> void readCollectionFromNBT(Collection<E> c, CompoundTag NBT, String key, NBTIO<E> converter) {
        c.clear();
        ListTag li = NBT.getList(key, Tag.TAG_COMPOUND);
        for (Object o : li) {
            CompoundTag tag = (CompoundTag) o;
            Tag b = tag.get("value");
            c.add((E) getValue(b, converter));
        }
    }

    public static Tag getNestedNBTTag(CompoundTag tag, ArrayList<String> li, String name) {
        for (String s : li) {
            tag = tag.getCompound(s);
            if (tag == null || tag.isEmpty())
                return null;
        }
        return tag.get(name);
    }

    public static void replaceTag(CompoundTag NBT, String s, Tag tag) {
        NBT.put(s, tag);
    }

    public static void replaceTag(ListTag NBT, int idx, Tag tag) {
        NBT.remove(idx);
        NBT.add(idx, tag);
    }

    public static NBTIO<? extends Enum> getEnumConverter(Class<? extends Enum> c) {
        EnumIO handler = enumIOMap.get(c);
        if (handler == null) {
            handler = new EnumIO(c);
            enumIOMap.put(c, handler);
        }
        return handler;
    }

    /*
    public static class CompoundNBTIO {

        private final HashMap<Class, NBTIO> data = new HashMap();

        public <V> void addHandler(Class<? extends V> c, NBTIO<V> h) {
            data.put(c, h);
        }

    }
     */
    public interface NBTIO<V> {

        V createFromNBT(Tag nbt);

        Tag convertToNBT(V obj);

    }

    public static class EnumNBTConverter implements NBTIO<Enum> {

        private final List<Enum> enumData;

        public EnumNBTConverter(Class<? extends Enum> c) {
            enumData = Arrays.asList(c.getEnumConstants());
        }

        @Override
        public Enum createFromNBT(Tag nbt) {
            int idx = ((IntTag) nbt).getAsInt();
            return idx >= 0 && idx < enumData.size() ? enumData.get(idx) : null;
        }

        @Override
        public Tag convertToNBT(Enum obj) {
            return IntTag.valueOf(enumData.indexOf(obj));
        }

    }

    public static class BlockConverter implements NBTIO<Block> {

        public static final BlockConverter instance = new BlockConverter();

        private BlockConverter() {

        }

        @Override
        public Block createFromNBT(Tag nbt) {
            return ForgeRegistries.BLOCKS.getValue(new ResourceLocation((nbt.getAsString())));
        }

        @Override
        public Tag convertToNBT(Block obj) {
            return StringTag.valueOf(ForgeRegistries.BLOCKS.getKey(obj).getNamespace());
        }

    }

    public static class ItemConverter implements NBTIO<Item> {

        public static final ItemConverter instance = new ItemConverter();

        private ItemConverter() {

        }

        @Override
        public Item createFromNBT(Tag nbt) {
            return ForgeRegistries.ITEMS.getValue(((new ResourceLocation(nbt.getAsString()))));
        }

        @Override
        public Tag convertToNBT(Item obj) {
            return StringTag.valueOf(ForgeRegistries.ITEMS.getKey(obj).getNamespace());
        }

    }

    public static class ItemStackConverter implements NBTIO<ItemStack> {

        public static final ItemStackConverter instance = new ItemStackConverter();

        private ItemStackConverter() {

        }

        @Override
        public ItemStack createFromNBT(Tag nbt) {
            return ItemStack.of((CompoundTag) nbt);
        }

        @Override
        public Tag convertToNBT(ItemStack obj) {
            CompoundTag ret = new CompoundTag();
            obj.save(ret);
            return ret;
        }

    }

    public static class KeyedItemStackConverter implements NBTIO<KeyedItemStack> {

        public static final KeyedItemStackConverter instance = new KeyedItemStackConverter();

        private KeyedItemStackConverter() {

        }

        @Override
        public KeyedItemStack createFromNBT(Tag nbt) {
            return KeyedItemStack.load((CompoundTag) nbt);
        }

        @Override
        public Tag convertToNBT(KeyedItemStack obj) {
            CompoundTag ret = new CompoundTag();
            obj.saveAdditional(ret);
            return ret;
        }

    }

    public static class UUIDConverter implements NBTIO<UUID> {

        public static final UUIDConverter instance = new UUIDConverter();

        private UUIDConverter() {

        }

        @Override
        public UUID createFromNBT(Tag nbt) {
            return UUID.fromString(nbt.getAsString());
        }

        @Override
        public Tag convertToNBT(UUID obj) {
            return StringTag.valueOf(obj.toString());
        }

    }

    private static class EnumIO implements NBTIO<Enum> {

        private final Enum[] objects;
        private final Class enumType;

        private EnumIO(Class<? extends Enum> c) {
            objects = c.getEnumConstants();
            enumType = c;
        }

        @Override
        public Enum createFromNBT(Tag nbt) {
            return objects[((IntTag) nbt).getAsInt()];
        }

        @Override
        public Tag convertToNBT(Enum obj) {
            return IntTag.valueOf(obj.ordinal());
        }

    }
}
