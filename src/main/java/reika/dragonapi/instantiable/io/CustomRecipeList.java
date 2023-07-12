package reika.dragonapi.instantiable.io;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import reika.dragonapi.base.DragonAPIMod;
import reika.dragonapi.interfaces.IReikaRecipe;
import reika.dragonapi.io.ReikaFileReader;
import reika.dragonapi.libraries.ReikaNBTHelper;
import reika.dragonapi.libraries.ReikaRecipe;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

public class CustomRecipeList {

    private final DragonAPIMod mod;
    public final String recipeType;

    private LuaBlock.LuaBlockDatabase data = new LuaBlock.LuaBlockDatabase();
    private final HashSet<LuaBlock> entries = new HashSet<>();

    private final LuaBlock exampleBlock = new ExampleLuaBlock("exampleRoot", null, new LuaBlock.LuaBlockDatabase());

    private static final HashMap<String, Class> lookups = new HashMap<>();
    private static final HashMap<String, DelegateLookup> delegateCalls = new HashMap<>();

/*    static {
        if (ModList.MYSTCRAFT.isLoaded())
            delegateCalls.put("myst_page", new MystPageLookup());
        if (ModList.FORESTRY.isLoaded()) {
            delegateCalls.put("forestry_bee", new BeeLookup());
            delegateCalls.put("forestry_tree", new TreeLookup());
            delegateCalls.put("forestry_butterfly", new ButterflyLookup());
        }
    }*/

    private static final Pattern STACKSIZE_PATTERN = Pattern.compile("(.+?)(?:\\*(\\d+))?$");

    public CustomRecipeList(DragonAPIMod mod, String type) {
        this.mod = mod;
        recipeType = type;
    }

    public static void addFieldLookup(String key, Class c) {
        lookups.put(key, c);
    }

    public final boolean load() {
        File folder = this.getBaseFilepath();
        if (!folder.exists() || !folder.isDirectory())
            return false;
        ArrayList<File> files = ReikaFileReader.getAllFilesInFolder(folder, this.getExtension());
        this.load(files);
        return true;
    }

    public final void createFolders() {
        File folder = this.getBaseFilepath();
        folder.mkdirs();
    }

    public final LuaBlock createExample(String s) {
        return new ExampleLuaBlock(s, exampleBlock, exampleBlock.tree);
    }

    /*
    public final void addToExample(LuaBlock b) {
        exampleBlock.addChild(b.name);
    }
     */
    public final void createExampleFile() {
        try {
            File f = new File(this.getBaseFilepath(), "example" + this.getExtension());
            f.createNewFile();
            ReikaFileReader.writeLinesToFile(f, exampleBlock.writeToStrings(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public final boolean load(File f) {
        if (!f.exists())
            return false;
        this.load(ReikaJavaLibrary.makeListFrom(f));
        return true;
    }

    public final void load(Collection<File> files) {
        this.clear();
        for (File f : files) {
            if (ReikaFileReader.getFileNameNoExtension(f, false, false).equals("example"))
                continue;
            data.loadFromFile(f);
        }
        this.parseLuaBlocks();
    }

    public final void clear() {
        data = new LuaBlock.LuaBlockDatabase();
        entries.clear();
    }

    private void parseLuaBlocks() {
        LuaBlock root = data.getRootBlock();
        for (LuaBlock b : root.getChildren()) {
            try {
                data.addBlock(b.getString("type"), b);
                mod.getModLogger().debug("Loaded recipe prototype:\n" + b.getString("type"));
                entries.add(b);
            } catch (Exception e) {
                mod.getModLogger().error("Could not parse custom recipe section " + b.getString("type") + ": ");
                e.printStackTrace();
            }
        }
        if (!Strings.isNullOrEmpty(recipeType))
            mod.getModLogger().info("All custom " + recipeType + " recipe entries parsed.");
    }

    public final Collection<LuaBlock> getEntries() {
        return Collections.unmodifiableCollection(entries);
    }

    private File getBaseFilepath() {
        return new File(mod.getConfigFolder(), mod.getDisplayName() + "_" + this.getFolderName());
    }

    protected String getFolderName() {
        return "CustomRecipes";
    }

    protected String getExtension() {
        return ".recipes_" + recipeType;
    }

    public static Object parseObjectString(String item) {
        if (item.equals("null") || item.equals("empty") || item.equals("~") || item.equals("-"))
            return null;
        if (item.startsWith("ore:"))
            return item.substring("ore:".length());
        return parseItemString(item, null, true);
    }

    public static void writeItem(LuaBlock lb, ItemStack is) {
        String base = ForgeRegistries.ITEMS.getKey(is.getItem()).getPath();
        if (is.getCount() > 1) {
            base = base + "*" + is.getCount();
        }
        lb.putData("item", base);
        if (is.getTag() != null) {
            new LuaBlock.NBTLuaBlock("item_nbt", lb, lb.tree, is.getTag(), false);
        }
    }

    public static Collection<ItemStack> parseItemCollection(Collection<String> in, boolean tolerateNull) {
        Collection<ItemStack> c = new ArrayList<>();
        for (String s : in) {
            if (s.startsWith("ore:")) {
                s = s.substring("ore:".length());
                ArrayList<ItemStack> li = null;//OreDictionary.getOres(s);
                if (li.isEmpty() && !tolerateNull)
                    throw new IllegalArgumentException("Ore dictionary tag '" + s + "' has no items!");
                else
                    c.addAll(li);
            } else {
                ItemStack is = parseItemString(s, null, tolerateNull);
                if (is != null)
                    c.add(is);
                else if (!tolerateNull)
                    throw new IllegalArgumentException("Null stack not permitted!");
            }
        }
        return c;
    }

    public static ItemStack parseItemString(String s, LuaBlock nbt, boolean tolerateNull) {
        if (Strings.isNullOrEmpty(s)) {
            if (tolerateNull)
                return null;
            else
                throw new IllegalArgumentException("Null stack not permitted!");
        }
        String lookup = s;
        ItemStack ret = null;
        Matcher m = STACKSIZE_PATTERN.matcher(s);
        int amt = 1;
        if (m.find() && m.group(2) != null) {
            lookup = m.group(1);
            amt = Integer.parseInt(m.group(2));
            if (amt > 64) {
                throw new IllegalArgumentException("Stack size of " + amt + " is too large!");
            }
            if (amt <= 0) {
                throw new IllegalArgumentException("Stack size of " + amt + " is zero!");
            }
        }

        String key = s.substring(0, s.indexOf(':'));
        if (key.equals("delegate")) {
            lookup = lookup.substring(key.length() + 1);
            DelegateLookup delegate = delegateCalls.get(lookup);
            if (delegate == null)
                throw new IllegalArgumentException("No such Delegate Lookup '" + lookup + "'!");
            ItemStack is = delegate.getItem(nbt);
            if (is == null && !tolerateNull)
                throw new IllegalArgumentException("Delegate Lookup '" + lookup + "' yielded no item!");
            return is;
        } else if (lookups.containsKey(key)) {
            try {
                lookup = lookup.substring(key.length() + 1);
                ret = (ItemStack) lookups.get(key).getField(lookup).get(null);
            } catch (Exception e) {
                throw new IllegalArgumentException("No internal stack '" + lookup + "'");
            }
        } else {
            ret = ReikaItemHelper.lookupItem(lookup);
            if (ret == null && !tolerateNull) {
                throw new IllegalArgumentException("No such item '" + lookup + "'");
            }
        }

        ret = ReikaItemHelper.getSizedItemStack(ret, amt);

        if (ret != null && nbt != null) {
            ret.setTag(ReikaNBTHelper.constructNBT(nbt));
        }

        if (ret == null && !tolerateNull) {
            throw new IllegalArgumentException("Null stack not permitted!");
        }

        return ret;
    }


    public static IReikaRecipe parseCraftingRecipe(LuaBlock lb, ItemStack output) {
        boolean shaped = lb.getBoolean("shaped");
        if (shaped) {
            String input1 = lb.containsKey("input_top") ? lb.getString("input_top").replaceAll(" ", "") : null;
            String input2 = lb.containsKey("input_middle") ? lb.getString("input_middle").replaceAll(" ", "") : null;
            String input3 = lb.containsKey("input_bottom") ? lb.getString("input_bottom").replaceAll(" ", "") : null;
            String[] top = input1 != null ? input1.split(",") : null;
            String[] middle = input2 != null ? input2.split(",") : null;
            String[] bottom = input3 != null ? input3.split(",") : null;
            int w = 0;
            if (top != null) {
                w = top.length;
            }
            if (middle != null) {
                if (w != 0 && w != middle.length) {
                    throw new IllegalArgumentException("Rows must be of equal length!");
                }
                w = middle.length;
            }
            if (bottom != null) {
                if (w != 0 && w != bottom.length) {
                    throw new IllegalArgumentException("Rows must be of equal length!");
                }
                w = bottom.length;
            }
            if (w > 3) {
                throw new IllegalArgumentException("Rows must be at most three entries long!");
            }
            ArrayList<String[]> rows = new ArrayList<>();
            if (top != null)
                rows.add(top);
            if (middle != null)
                rows.add(middle);
            if (bottom != null)
                rows.add(bottom);
            Object[][] array = new Object[rows.size()][w];
            for (int i = 0; i < rows.size(); i++) {
                for (int k = 0; k < w; k++) {
                    String item = rows.get(i)[k];
                    array[i][k] = parseObjectString(item);
                }
            }
            return new ReikaRecipe(output, decode2DArray(array)); //todo fix nulls
        } else {
            String input = lb.getString("input").replaceAll(" ", "");
            String[] parts = input.split(",");
            if (parts.length > 9)
                throw new IllegalArgumentException("You cannot have more than nine items in recipes!");
            Object[] inputs = new Object[parts.length];

            for (int i = 0; i < parts.length; i++) {
                String s = parts[i];
                Object o = parseObjectString(s);
                if (o == null) {
                    throw new IllegalArgumentException("You cannot have blank spaces in shapeless recipes!");
                }
                inputs[i] = o;
            }
            return new ReikaRecipe(output, inputs);
        }
    }


    public static String fullID(Object o) {
        if (o instanceof ItemStack)
            return fullID((ItemStack) o);
        if (o instanceof Collection)
            return ((Collection) o).stream().map(e -> fullID(e)).collect(Collectors.toList()).toString();
        if (o instanceof Item)
            return ForgeRegistries.ITEMS.getKey((Item) o) + "[" + ForgeRegistries.ITEMS.getKey((Item) o).getNamespace() + "]";
        if (o instanceof Block)
            return ForgeRegistries.BLOCKS.getKey((Block) o) + "[" + ForgeRegistries.BLOCKS.getKey((Block) o).getNamespace() + "]";
        return String.valueOf(o);
    }

    public static String fullID(ItemStack is) {
        if (is == null)
            return "[null]";
        else if (is.getItem() == null)
            return "[null-item stack]";
        return is.getCount() + "x" + ForgeRegistries.ITEMS.getKey(is.getItem()) + "{" + is.getTag() + "}" + "[" + ForgeRegistries.ITEMS.getKey(is.getItem()).getNamespace() + "]";
    }

    private static class ExampleLuaBlock extends LuaBlock {

        protected ExampleLuaBlock(String n, LuaBlock lb, LuaBlockDatabase db) {
            super(n, lb, db);
        }

    }

    public interface DelegateLookup {
        ItemStack getItem(LuaBlock data);
    }

    /*
        private static class MystPageLookup implements DelegateLookup {

            @Override
            public ItemStack getItem(LuaBlock data) {
                return ReikaMystcraftHelper.getSymbolPage(data.getString("symbol"));
            }

        }



        private static class BeeLookup implements DelegateLookup {

            @Override
            public ItemStack getItem(LuaBlock data) {
                EnumBeeType type = EnumBeeType.valueOf(data.getString("class").toUpperCase(Locale.ENGLISH));
                ItemStack ret = ReikaBeeHelper.getBeeItem(data.getString("species"), type);
                for (int i = 0; i < EnumBeeChromosome.values().length; i++) {
                    EnumBeeChromosome ec = EnumBeeChromosome.values()[i];
                    if (ec != EnumBeeChromosome.SPECIES) {
                        String key = ec.name().toLowerCase(Locale.ENGLISH);

                        if (data.containsKey(key)) {
                            IAllele ia = null;

                            switch(ec) {
                                case CAVE_DWELLING:
                                case NOCTURNAL:
                                case TOLERANT_FLYER:
                                    ia = ReikaBeeHelper.getBooleanAllele(data.getBoolean(key));
                                    break;
                                case EFFECT:
                                case FERTILITY:
                                case FLOWER_PROVIDER:
                                case FLOWERING:
                                case LIFESPAN:
                                case SPEED:
                                case TERRITORY:
                                    String val = data.getString(key);
                                    BeeGene bg = BeeAlleleRegistry.getEnum(ec, val);
                                    ia = bg.getAllele();
                                    break;
                                case HUMIDITY_TOLERANCE:
                                case TEMPERATURE_TOLERANCE:
                                    EnumTolerance et = EnumTolerance.valueOf(data.getString(key).toUpperCase(Locale.ENGLISH));
                                    ia = ReikaBeeHelper.getToleranceGene(et);
                                    break;
                                case SPECIES:
                                case HUMIDITY:
                                default:
                                    break;
                            }

                            if (ia != null) {
                                IBeeGenome ibg = (IBeeGenome)ReikaBeeHelper.getGenome(ret);
                                ReikaBeeHelper.setGene(ret, ibg, ec, ia, false);
                                ReikaBeeHelper.setGene(ret, ibg, ec, ia, true);
                            }
                        }
                    }
                }
                return ret;
            }
        }


        /** Not yet implemented. */
/*
    private static class TreeLookup implements DelegateLookup {

        @Override
        public ItemStack getItem(LuaBlock data) {
            EnumGermlingType type = EnumGermlingType.valueOf(data.getString("class").toUpperCase(Locale.ENGLISH));
            ItemStack ret = ReikaBeeHelper.getTreeItem(data.getString("species"), type);
            for (int i = 0; i < EnumTreeChromosome.values().length; i++) {
                EnumTreeChromosome ec = EnumTreeChromosome.values()[i];
                if (ec != EnumTreeChromosome.SPECIES) {
                    String key = ec.name().toLowerCase(Locale.ENGLISH);

                    if (data.containsKey(key)) {
                        IAllele ia = null;

                        switch(ec) {
                            case FIREPROOF:
                                ia = ReikaBeeHelper.getBooleanAllele(data.getBoolean(key));
                                break;
                            case EFFECT:
                            case FERTILITY:
                            case FRUITS:
                            case GIRTH:
                            case HEIGHT:
                            case MATURATION:
                            case PLANT:
                            case SAPPINESS:
                            case TERRITORY:
                            case YIELD:
                            case GROWTH:
                                String val = data.getString(key);
                                TreeGene bg = TreeAlleleRegistry.getEnum(ec, val);
                                ia = bg.getAllele();
                                break;
                            case SPECIES:
                            default:
                                break;
                        }

                        if (ia != null) {
                            ITreeGenome ibg = (ITreeGenome)ReikaBeeHelper.getGenome(ret);
                            ReikaBeeHelper.setGene(ret, ibg, ec, ia, false);
                            ReikaBeeHelper.setGene(ret, ibg, ec, ia, true);
                        }
                    }
                }
            }
            return ret;
        }
    }


    /** Not yet implemented. */
/*
    private static class ButterflyLookup implements DelegateLookup {

        @Override
        public ItemStack getItem(LuaBlock data) {
            EnumFlutterType type = EnumFlutterType.valueOf(data.getString("class").toUpperCase(Locale.ENGLISH));
            ItemStack ret = ReikaBeeHelper.getButterflyItem(data.getString("species"), type);
            for (int i = 0; i < EnumButterflyChromosome.values().length; i++) {
                EnumButterflyChromosome ec = EnumButterflyChromosome.values()[i];
                if (ec != EnumButterflyChromosome.SPECIES) {
                    String key = ec.name().toLowerCase(Locale.ENGLISH);

                    if (data.containsKey(key)) {
                        IAllele ia = null;

                        switch(ec) {
                            case NOCTURNAL:
                            case TOLERANT_FLYER:
                            case FIRE_RESIST:
                                ia = ReikaBeeHelper.getBooleanAllele(data.getBoolean(key));
                                break;
                            case METABOLISM:
                                ia = ReikaBeeHelper.getIntegerAllele(data.getInt(key));
                                break;
                            case EFFECT:
                            case TERRITORY:
                            case FERTILITY:
                            case FLOWER_PROVIDER:
                            case LIFESPAN:
                            case SIZE:
                            case SPEED:
                                String val = data.getString(key);
                                ButterflyGene bg = ButterflyAlleleRegistry.getEnum(ec, val);
                                ia = bg.getAllele();
                                break;
                            case HUMIDITY_TOLERANCE:
                            case TEMPERATURE_TOLERANCE:
                                EnumTolerance et = EnumTolerance.valueOf(data.getString(key).toUpperCase(Locale.ENGLISH));
                                ia = ReikaBeeHelper.getToleranceGene(et);
                                break;
                            case SPECIES:
                            default:
                                break;
                        }

                        if (ia != null) {
                            IButterflyGenome ibg = (IButterflyGenome)ReikaBeeHelper.getGenome(ret);
                            ReikaBeeHelper.setGene(ret, ibg, ec, ia, false);
                            ReikaBeeHelper.setGene(ret, ibg, ec, ia, true);
                        }
                    }
                }
            }
            return ret;
        }
    }
*/
    public static Object[] decode2DArray(Object[][] array) {
        String[] input = new String[array.length];
        ArrayList objects = new ArrayList();
        ArrayList entries = new ArrayList();
        for (int i = 0; i < array.length; i++) {
            StringBuilder sb = new StringBuilder();
            for (int k = 0; k < array[i].length; k++) {
                Object o = array[i][k];
                char c = o == null ? ' ' : (char) ('a' + (i * 3 + k));
        sb.append(c);
                if (o != null) {
                    entries.add(c);
                    entries.add(o);
                }
            }
            input[i] = sb.toString();
        }
        Collections.addAll(objects, input);
        objects.addAll(entries);
        return objects.toArray(new Object[objects.size()]);
    }
}
