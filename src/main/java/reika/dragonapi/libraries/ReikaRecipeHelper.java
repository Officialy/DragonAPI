//package reika.dragonapi.libraries;
//
//import java.lang.reflect.Field;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Random;
//
//import net.minecraft.core.NonNullList;
//import net.minecraft.world.item.Item;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.item.crafting.Ingredient;
//import net.minecraft.world.item.crafting.Recipe;
//import net.minecraft.world.item.crafting.ShapedRecipe;
//import net.minecraft.world.item.crafting.ShapelessRecipe;
//import net.minecraft.world.level.ItemLike;
//import net.minecraft.world.level.block.Blocks;
//import reika.dragonapi.DragonAPI;
//import reika.dragonapi.ModList;
//import reika.dragonapi.auxiliary.trackers.ReflectiveFailureTracker;
//import reika.dragonapi.exception.MisuseException;
//import reika.dragonapi.instantiable.RecipePattern;
//import reika.dragonapi.instantiable.data.KeyedItemStack;
//import reika.dragonapi.instantiable.data.maps.ItemHashMap;
//import reika.dragonapi.interfaces.CustomToStringRecipe;
//import reika.dragonapi.libraries.java.ReikaJavaLibrary;
//import reika.dragonapi.libraries.java.ReikaObfuscationHelper;
//import reika.dragonapi.libraries.registry.ReikaItemHelper;
//
//public class ReikaRecipeHelper {
//
////    private static final CraftingManager cr = CraftingManager.getInstance();
//
//    private static final Random rand = new Random();
//
//    private static final int[] permuOffsets = new int[9];
//
//    private static final HashMap<Recipe<?>, RecipeCache> recipeCache = new HashMap();
//    private static final HashMap<Recipe<?>, RecipeCache> recipeCacheClient = new HashMap();
//
//    private static Field shapedOreHeight;
//    private static Field shapedOreWidth;
//    private static Field shapedOreInput;
//
//    private static Class ic2ShapedClass;
//    private static Class ic2ShapelessClass;
//    private static Field shapedIc2Input;
//    private static Field shapedIc2InputMirror;
//    private static Field shapedIc2Height;
//    private static Field shapedIc2Width;
//    private static Field ic2MasksField;
//    private static Field shapelessIc2Input;
//
//    private static Class aeShapedClass;
//    private static Class aeShapelessClass;
//    private static Field shapedAEInput;
//    private static Field shapelessAEInput;
//    private static Field shapedAEHeight;
//    private static Field shapedAEWidth;
//
//    private static Class computerTurtleClass;
//    private static Field computerTurtleInput;
//
//    private static Class fairyComponentClass;
//    private static Field fairyComponentInput;
//    private static Field fairyComponentOutput;
//    private static Field fairyComponentHeight;
//    private static Field fairyComponentWidth;
//    private static Class fairyStringClass;
//    private static Field fairyStringInput;
//    public static Field fairyStringOutput;
//    private static Field fairyStringHeight;
//    private static Field fairyStringWidth;
//
//    private static Class teNEIClass;
//    private static Field teNEIWrappedRecipe;
//
//    private static class RecipeCache {
//
//        private final List<ItemStack>[] items;
//        private final int width;
//        private final int height;
//
//        private RecipeCache(List<ItemStack>[] items, int w, int h) {
//            this.items = items;
//            width = w;
//            height = h;
//        }
//    }
//
//    private static class UnparsableRecipeCache extends RecipeCache {
//
//        private UnparsableRecipeCache() {
//            super(new List[0], 0, 0);
//        }
//
//    }
//
//
//    public static interface ReplacementCallback {
//
//        void onReplaced(Recipe<?> ir, int slot, Object from, Object to);
//
//    }
//
//    public static Class fairyComponentClass() {
//        return fairyComponentClass;
//    }
//
//    public static Class fairyStringClass() {
//        return fairyStringClass;
//    }
//
//    public static ItemStack getFairyLightOutput(Recipe<?> ir) {
//        try {
//            return ir != null && ir.getClass() == fairyComponentClass ? (ItemStack) fairyComponentOutput.get(ir) : null;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//    static {
//        try {
//            shapedOreHeight = ShapedRecipe.class.getDeclaredField("height");
//            shapedOreWidth = ShapedRecipe.class.getDeclaredField("width");
//            shapedOreInput = ShapedRecipe.class.getDeclaredField("input");
//
//            shapedOreHeight.setAccessible(true);
//            shapedOreWidth.setAccessible(true);
//            shapedOreInput.setAccessible(true);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        if (ModList.IC2.isLoaded()) {
//            try {
//                ic2ShapedClass = Class.forName("ic2.core.AdvRecipe");
//                ic2ShapelessClass = Class.forName("ic2.core.AdvShapelessRecipe");
//
//                shapedIc2Input = ic2ShapedClass.getDeclaredField("input");
//                shapedIc2Input.setAccessible(true);
//
//                shapedIc2Width = ic2ShapedClass.getDeclaredField("inputWidth");
//                shapedIc2Width.setAccessible(true);
//
//                shapedIc2Height = ic2ShapedClass.getDeclaredField("inputHeight");
//                shapedIc2Height.setAccessible(true);
//
//                shapedIc2InputMirror = ic2ShapedClass.getDeclaredField("inputMirrored");
//                shapedIc2InputMirror.setAccessible(true);
//
//                ic2MasksField = ic2ShapedClass.getDeclaredField("masks");
//                ic2MasksField.setAccessible(true);
//
//                shapelessIc2Input = ic2ShapelessClass.getDeclaredField("input");
//                shapelessIc2Input.setAccessible(true);
//            } catch (Exception e) {
//                e.printStackTrace();
//                DragonAPI.LOGGER.error("Could not load IC2 recipe handling!");
//                ReflectiveFailureTracker.instance.logModReflectiveFailure(ModList.IC2, e);
//            }
//        }
//
//        if (ModList.THERMALEXPANSION.isLoaded()) {
//            try {
//                teNEIClass = Class.forName("cofh.thermalexpansion.plugins.nei.handlers.NEIRecipeWrapper");
//                teNEIWrappedRecipe = teNEIClass.getDeclaredField("recipe");
//                teNEIWrappedRecipe.setAccessible(true);
//            } catch (Exception e) {
//                e.printStackTrace();
//                DragonAPI.LOGGER.error("Could not load TE recipe handling!");
//                ReflectiveFailureTracker.instance.logModReflectiveFailure(ModList.THERMALEXPANSION, e);
//            }
//        }
//
//        if (ModList.COMPUTERCRAFT.isLoaded()) {
//            try {
//                computerTurtleClass = Class.forName("dan200.computercraft.shared.turtle.recipes.TurtleRecipe");
//                computerTurtleInput = computerTurtleClass.getDeclaredField("m_recipe");
//                computerTurtleInput.setAccessible(true);
//            } catch (Exception e) {
//                e.printStackTrace();
//                DragonAPI.LOGGER.error("Could not load ComputerCraft recipe handling!");
//                ReflectiveFailureTracker.instance.logModReflectiveFailure(ModList.COMPUTERCRAFT, e);
//            }
//        }
//
//        if (ModList.APPENG.isLoaded()) {
//            try {
//                aeShapedClass = Class.forName("appeng.recipes.game.ShapedRecipe");
//                aeShapelessClass = Class.forName("appeng.recipes.game.ShapelessRecipe");
//
//                shapedAEInput = aeShapedClass.getDeclaredField("input");
//                shapedAEInput.setAccessible(true);
//
//                shapedAEWidth = aeShapedClass.getDeclaredField("width");
//                shapedAEWidth.setAccessible(true);
//
//                shapedAEHeight = aeShapedClass.getDeclaredField("height");
//                shapedAEHeight.setAccessible(true);
//
//                shapelessAEInput = aeShapelessClass.getDeclaredField("input");
//                shapelessAEInput.setAccessible(true);
//            } catch (Exception e) {
//                e.printStackTrace();
//                DragonAPI.LOGGER.error("Could not load AE recipe handling!");
//                ReflectiveFailureTracker.instance.logModReflectiveFailure(ModList.APPENG, e);
//            }
//        }
//
///*        if (Loader.isModLoaded("fairylights")) {
//            try {
//                fairyComponentClass = Class.forName("com.pau101.fairylights.item.crafting.RecipeDyeColorNBT");
//                fairyComponentInput = fairyComponentClass.getDeclaredField("getIngredients()");
//                fairyComponentInput.setAccessible(true);
//                fairyComponentOutput = fairyComponentClass.getDeclaredField("recipeOutput");
//                fairyComponentOutput.setAccessible(true);
//                fairyComponentHeight = fairyComponentClass.getDeclaredField("getHeight()");
//                fairyComponentHeight.setAccessible(true);
//                fairyComponentWidth = fairyComponentClass.getDeclaredField("getWidth()");
//                fairyComponentWidth.setAccessible(true);
//
//                fairyStringClass = Class.forName("com.pau101.fairylights.item.crafting.RecipeFairyLights");
//                fairyStringInput = fairyStringClass.getDeclaredField("getIngredients()");
//                fairyStringInput.setAccessible(true);
//                fairyStringOutput = fairyStringClass.getDeclaredField("recipeOutput");
//                fairyStringOutput.setAccessible(true);
//                fairyStringHeight = fairyStringClass.getDeclaredField("getHeight()");
//                fairyStringHeight.setAccessible(true);
//                fairyStringWidth = fairyStringClass.getDeclaredField("getWidth()");
//                fairyStringWidth.setAccessible(true);
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//                DragonAPI.LOGGER.error("Could not load FairyLights recipe handling!");
//                ReflectiveFailureTracker.instance.logModReflectiveFailure(new BasicModEntry("fairylights"), e);
//            }
//        }*/
//    }
//
//    public static Class getIC2ShapedClass() {
//        return ic2ShapedClass;
//    }
//
//    public static Class getIC2ShapelessClass() {
//        return ic2ShapelessClass;
//    }
//
//    public static Class getAEShapedClass() {
//        return aeShapedClass;
//    }
//
//    public static Class getAEShapelessClass() {
//        return aeShapelessClass;
//    }
//
//    public static void overwriteShapedOreRecipeInput(ShapedRecipe s, Object[] in, int height, int width) {
//        try {
//            shapedOreInput.set(s, in);
//            shapedOreHeight.set(s, height);
//            shapedOreWidth.set(s, width);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static int getOreRecipeHeight(ShapedRecipe s) {
//        try {
//            return shapedOreHeight.getInt(s);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return 0;
//        }
//    }
//
//    public static int getOreRecipeWidth(ShapedRecipe s) {
//        try {
//            return shapedOreWidth.getInt(s);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return 0;
//        }
//    }
//
//    /*
//    /** Finds recipes by product. NOT PERFORMANT! *//*
//	public static List<Recipe<?>> getRecipesByOutput(ItemStack out) {
//		List<Recipe<?>> li = new ArrayList<Recipe<?>>();
//		for (int i = 0; i < recipes.size(); i++) {
//			Recipe<?> ir = recipes.get(i);
//			if (ItemStack.areItemStacksEqual(ir.getResultItem(), out))
//				li.add(ir);
//		}
//		return li;
//	}
//	/** Finds recipes by product. NOT PERFORMANT! *//*
//	public static List<ShapedRecipe> getShapedRecipesByOutput(ItemStack out) {
//		List<ShapedRecipe> li = new ArrayList<ShapedRecipe>();
//		for (int i = 0; i < recipes.size(); i++) {
//			Recipe<?> ir = recipes.get(i);
//			if (ir instanceof ShapedRecipe) {
//				if (ItemStack.areItemStacksEqual(ir.getResultItem(), out))
//					li.add((ShapedRecipe)ir);
//			}
//		}
//		return li;
//	}
//	/** Finds recipes by product. NOT PERFORMANT! *//*
//	public static List<ShapedRecipe> getShapedRecipesByOutput(List<Recipe<?>> in, ItemStack out) {
//		List<ShapedRecipe> li = new ArrayList<ShapedRecipe>();
//		for (int i = 0; i < in.size(); i++) {
//			Recipe<?> ir = in.get(i);
//			//DragonAPI.LOGGER.info(ir.getResultItem()+" == "+out);
//			if (ir instanceof ShapedRecipe) {
//				if (ReikaItemHelper.matchStacks(ir.getResultItem(), out))
//					li.add((ShapedRecipe)ir);
//			}
//		}
//		//DragonAPI.LOGGER.info(li);
//		return li;
//	}
//	 */
//    public static boolean isUniformInput(Recipe<?> ir) {
//        HashSet<KeyedItemStack> set = new HashSet<>();
//        for (ItemStack is : getAllItemsInRecipe(ir)) {
//            KeyedItemStack ks = new KeyedItemStack(is).setSimpleHash(true);
//            set.add(ks);
//        }
//        return set.size() == 1;
//    }
//
//    /**
//     * Returns the item in a shaped recipe at x, y in the grid.
//     */
//    public static Ingredient getItemInRecipeAtXY(ShapedRecipe r, int x, int y) {
//        int xy = x + r.getWidth() * y;
//        return r.getIngredients().get(xy);
//    }
//
//    /**
//     * Finds recipes by product.
//     */
//    public static ArrayList<Recipe<?>> getAllRecipesByOutput(List<Recipe<?>> in, ItemStack out) {
//        ArrayList<Recipe<?>> li = new ArrayList<>();
//        for (Recipe<?> ir : in) {
//            //DragonAPI.LOGGER.info(ir.getResultItem()+" == "+out);
//            if (ReikaItemHelper.matchStacks(ir.getResultItem(), out))
//                li.add(ir);
//        }
//        //DragonAPI.LOGGER.info(li);
//        return li;
//    }
//
//    public static boolean isCraftable(List<Recipe<?>> in, ItemStack is) {
//        return getAllRecipesByOutput(in, is).size() > 0;
//    }
//
//    /**
//     * Finds recipes by product.
//     */
//    public static List<ShapedRecipe> getShapedOreRecipesByOutput(List<Recipe<?>> in, ItemStack out) {
//        List<ShapedRecipe> li = new ArrayList<ShapedRecipe>();
//        for (Recipe<?> ir : in) {
//            //DragonAPI.LOGGER.info(ir.getResultItem()+" == "+out);
//            if (ir instanceof ShapedRecipe) {
//                if (ReikaItemHelper.matchStacks(ir.getResultItem(), out))
//                    li.add((ShapedRecipe) ir);
//            }
//        }
//        //DragonAPI.LOGGER.info(li);
//        return li;
//    }
//
//    /**
//     * Finds recipes by product.
//     */
//    public static List<ShapelessRecipe> getShapelessRecipesByOutput(List<Recipe<?>> in, ItemStack out) {
//        List<ShapelessRecipe> li = new ArrayList<>();
//        for (Recipe<?> ir : in) {
//            if (ir instanceof ShapelessRecipe) {
//                if (ReikaItemHelper.matchStacks(ir.getResultItem(), out))
//                    li.add((ShapelessRecipe) ir);
//            }
//        }
//        //DragonAPI.LOGGER.info(li);
//        return li;
//    }
//
//    /**
//     * Finds recipes by product.
//     */
//    public static List<ShapelessRecipe> getShapelessOreRecipesByOutput(List<Recipe<?>> in, ItemStack out) {
//        List<ShapelessRecipe> li = new ArrayList<>();
//        for (Recipe<?> ir : in) {
//            if (ir instanceof ShapelessRecipe) {
//                if (ReikaItemHelper.matchStacks(ir.getResultItem(), out))
//                    li.add((ShapelessRecipe) ir);
//            }
//        }
//        //DragonAPI.LOGGER.info(li);
//        return li;
//    }
//
//    private static List<ItemStack> getRecipeItemStack(ItemStack is, boolean client) {
//        if (is == null)
//            return null;
//        else {
//            return ReikaJavaLibrary.makeListFrom(is);
//        }
//    }
//
//    private static List<ItemStack> getRecipeItemStacks(Object[] c, boolean client) {
//        ArrayList<ItemStack> ret = new ArrayList<>();
//        for (Object o : c) {
//            if (o == null)
//                continue;
//            if (o instanceof ItemStack) {
//                ItemStack is = (ItemStack) o;
//                ret.add(is);
//            } else if (o instanceof Collection) {
//                ret.addAll((Collection<? extends ItemStack>) o);
//            }
//        }
//        return ret;
//    }
//
//    private static List<ItemStack> getRecipeItemStacks(Collection<?> c, boolean client) {
//        ArrayList<ItemStack> ret = new ArrayList<>();
//        for (Object o : c) {
//            if (o == null)
//                continue;
//            if (o instanceof ItemStack) {
//                ItemStack is = (ItemStack) o;
//                ret.add(is);
//            }
//           /* if (ModList.IC2.isLoaded()) {
//                handleIC2Inputs(o, ret);
//            }
//            if (ModList.APPENG.isLoaded()) {
//                handleAEInputs(o, ret);
//            }*/
//        }
//        return ret;
//    }
//
///*    @ModDependent(ModList.IC2)
//    private static void handleIC2Inputs(Object o, ArrayList<ItemStack> ret) {
//        if (o instanceof IRecipeInput) {
//            ret.addAll(((IRecipeInput) o).getInputs());
//        }
//    }
//
//    @ModDependent(ModList.APPENG)
//    private static void handleAEInputs(Object o, ArrayList<ItemStack> ret) {
//        if (o instanceof IAEItemStack) {
//            ret.add(((IAEItemStack) o).getItemStack());
//        }
//    }*/
//
//    private static RecipeCache getRecipeCacheObject(Recipe<?> ir, boolean client) {
//        HashMap<Recipe<?>, RecipeCache> map = client ? recipeCacheClient : recipeCache;
//        RecipeCache cache = map.get(ir);
//        if (cache == null) {
//            cache = calculateRecipeToItemStackArray(ir, client);
//            if (!ReikaObfuscationHelper.isDeObfEnvironment())
//                map.put(ir, cache);
//        }
//        return cache;
//    }
//
//    /**
//     * Turns a recipe into a 3x3 itemstack array. Args: Recipe<?>
//     */
//    public static List<ItemStack>[] getRecipeArray(Recipe<?> ir) {
//        List<ItemStack>[] lists = new List[9];
//        RecipeCache c = getRecipeCacheObject(ir, false);
//        if (c instanceof UnparsableRecipeCache)
//            return null;
//        for (int i = 0; i < 9; i++) {
//            List li = c.items[i];
//            if (li != null && !li.isEmpty())
//                lists[i] = Collections.unmodifiableList(li);
//        }
//        return lists;
//    }
//
//    /**
//     * Turns a recipe into a 3x3 itemstack array, permuting it as well, usually for rendering. Args: Recipe<?>
//     */
////    @SideOnly(Side.CLIENT)
//    public static ItemStack[] getPermutedRecipeArray(Recipe<?> ir) {
//        RecipeCache r = getRecipeCacheObject(ir, true);
//        if (r instanceof UnparsableRecipeCache)
//            return null;
//        List<ItemStack>[] isin = r.items;
//
//        long ttick = System.currentTimeMillis();
////    todo    if (GuiScreen.isShiftKeyDown())
////            ttick *= 4;
////        if (GuiScreen.isCtrlKeyDown())
////            ttick /= 8;
//        int time = 1000;
//        for (int i = 0; i < isin.length; i++) {
//            if (isin[i] != null && !isin[i].isEmpty()) {
//                if (ttick % time == 0) {
//                    permuOffsets[i] = rand.nextInt(isin[i].size());
//                }
//            }
//        }
//
//        int[] indices = new int[9];
//        ItemStack[] add = new ItemStack[9];
//        for (int i = 0; i < 9; i++) {
//            List<ItemStack> li = isin[i];
//            if (li != null && !li.isEmpty()) {
//                int tick = (int) (((ttick / time) + permuOffsets[i]) % li.size());
//                add[i] = li.get(tick);
//            }
//        }
//
//        ItemStack[] in = new ItemStack[9];
//        if (r.width == 3 && r.height == 3) {
//            for (int i = 0; i < 9; i++)
//                in[i] = add[i];
//        }
//        if (r.width == 1 && r.height == 1) {
//            in[4] = add[0];
//        }
//        if (r.width == 2 && r.height == 2) {
//            in[0] = add[0];
//            in[1] = add[1];
//            in[3] = add[2];
//            in[4] = add[3];
//        }
//        if (r.width == 1 && r.height == 2) {
//            in[4] = add[0];
//            in[7] = add[1];
//        }
//        if (r.width == 2 && r.height == 1) {
//            in[0] = add[0];
//            in[1] = add[1];
//        }
//        if (r.width == 3 && r.height == 1) {
//            in[0] = add[0];
//            in[1] = add[1];
//            in[2] = add[2];
//        }
//        if (r.width == 1 && r.height == 3) {
//            in[1] = add[0];
//            in[4] = add[1];
//            in[7] = add[2];
//        }
//        if (r.width == 2 && r.height == 3) {
//            in[0] = add[0];
//            in[1] = add[1];
//            in[3] = add[2];
//            in[4] = add[3];
//            in[6] = add[4];
//            in[7] = add[5];
//        }
//        if (r.width == 3 && r.height == 2) {
//            in[3] = add[0];
//            in[4] = add[1];
//            in[5] = add[2];
//            in[6] = add[3];
//            in[7] = add[4];
//            in[8] = add[5];
//        }
//
//        return in;
//    }
//
//    private static RecipeCache calculateRecipeToItemStackArray(Recipe<?> ire, boolean client) {
//        List<ItemStack>[] isin = new List[9];
//        int num;
//        int w = 0;
//        int h = 0;
//        if (ire == null)
//            DragonAPI.LOGGER.error("Recipe is null!");
//        if (ire == null) {
//            ReikaJavaLibrary.dumpStack();
//            return null;
//        }
//
////        ire = getTEWrappedRecipe(ire);
//
//        if (ire instanceof ShapedRecipe) {
//            ShapedRecipe r = (ShapedRecipe) ire;
//            num = r.getIngredients().size();
//            w = r.getWidth();
//            h = r.getHeight();
//            for (int i = 0; i < r.getIngredients().size(); i++) {
//                Ingredient is = r.getIngredients().get(i);
//                isin[i] = getRecipeItemStack(is.getItems()[i], client);
//            }
//
//        } else if (ire instanceof ShapedRecipe) {
//            ShapedRecipe so = (ShapedRecipe) ire;
//            Object[] objin = new NonNullList[]{so.getIngredients()};
//            //DragonAPI.LOGGER.info(Arrays.toString(objin));
//            w = 3;
//            h = 3;
//            for (int i = 0; i < objin.length; i++) {
//                if (objin[i] instanceof ItemStack) {
//                    ItemStack is = (ItemStack) objin[i];
//                    isin[i] = getRecipeItemStack(is, client);
//                } else if (objin[i] instanceof List) {
//                    List li = (List) objin[i];
//                    if (!li.isEmpty()) {
//                        isin[i] = getRecipeItemStacks(li, client);
//                    }
//                }
//            }
//        } else if (ire instanceof ShapelessRecipe) {
//            ShapelessRecipe sr = (ShapelessRecipe) ire;
//            //DragonAPI.LOGGER.info(ire);
//            for (int i = 0; i < sr.getIngredients().size(); i++) {
//                ItemStack is = sr.getIngredients().get(i).getItems()[i]; //todo check array i
//                isin[i] = getRecipeItemStack(is, client);
//            }
//            w = sr.getIngredients().size() >= 3 ? 3 : sr.getIngredients().size();
//            h = (sr.getIngredients().size() + 2) / 3;
//        } else if (ire instanceof ShapelessRecipe) {
//            ShapelessRecipe so = (ShapelessRecipe) ire;
//            for (int i = 0; i < so.getIngredients().size(); i++) {
//                Object obj = so.getIngredients().get(i);
//                if (obj instanceof ItemStack) {
//                    ItemStack is = (ItemStack) obj;
//                    isin[i] = getRecipeItemStack(is, client);
//                } else if (obj instanceof List) {
//                    List li = (List) obj;
//                    if (!li.isEmpty()) {
//                        isin[i] = getRecipeItemStacks(li, client);
//                    }
//                } else {
//                    DragonAPI.LOGGER.info("Could not parse ingredient type " + obj.getClass() + " with value " + obj.toString());
//                    isin[i] = Arrays.asList(new ItemStack(Blocks.FIRE));
//                }
//                //DragonAPI.LOGGER.info(ire);
//            }
//            w = so.getIngredients().size() >= 3 ? 3 : so.getIngredients().size();
//            h = (so.getIngredients().size() + 2) / 3;
//        } else if (ire.getClass() == ic2ShapedClass) {
//            try {
//                Object[] in = (Object[]) shapedIc2Input.get(ire);
////                in = padIC2CrushedArray(in, ire);
//                w = Math.min(3, shapedIc2Width.getInt(ire));
//                h = Math.min(3, shapedIc2Height.getInt(ire));
//                for (int i = 0; i < in.length; i++) {
//                    Object o = in[i];
//                    if (o == null)
//                        continue;
//                    if (o instanceof ItemStack)
//                        isin[i] = getRecipeItemStack((ItemStack) o, client);
//                    else if (o instanceof List)
//                        isin[i] = getRecipeItemStacks((List) o, client);
////                    else if (o instanceof IRecipeInput)
////                        isin[i] = getRecipeItemStacks(((IRecipeInput)o).getInputs(), client);
//                    else {
//                        DragonAPI.LOGGER.info("Could not parse ingredient type " + o.getClass() + " with value " + o);
//                        isin[i] = Arrays.asList(new ItemStack(Blocks.FIRE));
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        } else if (ire.getClass() == ic2ShapelessClass) {
//            try {
//                Object[] in = (Object[]) shapelessIc2Input.get(ire);
//                for (int i = 0; i < in.length; i++) {
//                    Object o = in[i];
//                    if (o == null)
//                        continue;
//                    if (o instanceof ItemStack)
//                        isin[i] = getRecipeItemStack((ItemStack) o, client);
//                    else if (o instanceof List)
//                        isin[i] = getRecipeItemStacks((List) o, client);
////                    else if (o instanceof IRecipeInput)
////                        isin[i] = getRecipeItemStacks(((IRecipeInput) o).getInputs(), client);
//                    else {
//                        DragonAPI.LOGGER.info("Could not parse ingredient type " + o.getClass() + " with value " + o.toString());
//                        isin[i] = Arrays.asList(new ItemStack(Blocks.FIRE));
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        } else if (ire.getClass() == aeShapedClass) {
//            try {
//                Object[] in = (Object[]) shapedAEInput.get(ire);
//                w = Math.min(3, shapedAEWidth.getInt(ire));
//                h = Math.min(3, shapedAEHeight.getInt(ire));
//                for (int i = 0; i < in.length; i++) {
//                    Object o = in[i];
//                    if (o == null)
//                        continue;
//                    if (o instanceof ItemStack)
//                        isin[i] = getRecipeItemStack((ItemStack) o, client);
//                    else if (o instanceof List)
//                        isin[i] = getRecipeItemStacks((List) o, client);
////                    else if (o instanceof IAEItemStack)
////                        isin[i] = getRecipeItemStack(((IAEItemStack) o).getItemStack(), client);
//                    else if (o instanceof Ingredient)
//                        isin[i] = getRecipeItemStacks(Arrays.asList(((Ingredient) o).getItems()), client);
//                    else {
//                        DragonAPI.LOGGER.info("Could not parse ingredient type " + o.getClass() + " with value " + o.toString());
//                        isin[i] = Arrays.asList(new ItemStack(Blocks.FIRE));
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        } else if (ire.getClass() == aeShapelessClass) {
//            try {
//                Object[] in = (Object[]) shapelessAEInput.get(ire);
//                for (int i = 0; i < in.length; i++) {
//                    Object o = in[i];
//                    if (o == null)
//                        continue;
//                    if (o instanceof ItemStack)
//                        isin[i] = getRecipeItemStack((ItemStack) o, client);
//                    else if (o instanceof List)
//                        isin[i] = getRecipeItemStacks((List) o, client);
////                    else if (o instanceof IAEItemStack)
////                        isin[i] = getRecipeItemStack(((IAEItemStack) o).getItemStack(), client);
//                    else if (o instanceof Ingredient)
//                        isin[i] = getRecipeItemStacks(Arrays.asList(((Ingredient) o).getItems()), client);
//                    else {
//                        DragonAPI.LOGGER.info("Could not parse ingredient type " + o.getClass() + " with value " + o.toString());
//                        isin[i] = Arrays.asList(new ItemStack(Blocks.FIRE));
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        } else if (ire.getClass() == computerTurtleClass) {
//            try {
//                Item[] in = (Item[]) computerTurtleInput.get(ire);
//                for (int i = 0; i < 3; i++) {
//                    for (int k = 0; k < 3; k++) {
//                        int idx = i * 3 + k;
//                        isin[idx] = getRecipeItemStack(new ItemStack(in[idx]), client);
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        } else if (ire.getClass() == fairyComponentClass) {
//            try {
//                Object[] in = (Object[]) fairyComponentInput.get(ire);
//                w = Math.min(3, fairyComponentWidth.getInt(ire));
//                h = Math.min(3, fairyComponentHeight.getInt(ire));
//                for (int i = 0; i < in.length; i++) {
//                    Object o = in[i];
//                    if (o == null)
//                        continue;
//                    if (o instanceof ItemStack)
//                        isin[i] = getRecipeItemStack((ItemStack) o, client);
//                    else if (o instanceof List)
//                        isin[i] = getRecipeItemStacks((List) o, client);
//                    else {
//                        DragonAPI.LOGGER.info("Could not parse ingredient type " + o.getClass() + " with value " + o);
//                        isin[i] = Arrays.asList(new ItemStack(Blocks.FIRE));
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        } else if (ire.getClass() == fairyStringClass) {
//            try {
//                Object[] in = (Object[]) fairyStringInput.get(ire);
//                w = Math.min(3, fairyStringWidth.getInt(ire));
//                h = Math.min(3, fairyStringHeight.getInt(ire));
//                for (int i = 0; i < in.length; i++) {
//                    Object o = in[i];
//                    if (o == null)
//                        continue;
//                    if (o instanceof ItemStack)
//                        isin[i] = getRecipeItemStack((ItemStack) o, client);
//                    else if (o instanceof Object[])
//                        isin[i] = getRecipeItemStacks((Object[]) o, client);
//                    else if (o instanceof List)
//                        isin[i] = getRecipeItemStacks((List) o, client);
//                    else {
//                        DragonAPI.LOGGER.info("Could not parse ingredient type " + o.getClass() + " with value " + o.toString());
//                        isin[i] = Arrays.asList(new ItemStack(Blocks.FIRE));
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        } else {
//            DragonAPI.LOGGER.error("Recipe " + toString(ire) + " could not be parsed!");
//            return new UnparsableRecipeCache();
//        }
//
//        return new RecipeCache(isin, w, h);
//    }
//
///*    @ModDependent(ModList.IC2)
//    private static Object[] padIC2CrushedArray(Object[] in, Recipe<?> ire) throws Exception {
//        int[] masks = (int[])ic2MasksField.get(ire);
//        int mask = masks[0];
//        int w = Math.min(3, shapedIc2Width.getInt(ire));
//        int h = Math.min(3, shapedIc2Height.getInt(ire));
//        boolean[] flags = ReikaArrayHelper.booleanFromBitflags(mask, w*h);
//        ArrayUtils.reverse(flags);
//        ArrayList<Object> li = ReikaJavaLibrary.makeListFromArray(in);
//        for (int i = 0; i < flags.length; i++) {
//            if (!flags[i]) {
//                li.add(i, null);
//            }
//        }
//        return li.toArray(new Object[li.size()]);
//    }*/
//
///*    @ModDependent(ModList.THERMALEXPANSION)
//    public static Recipe<?> getTEWrappedRecipe(Recipe<?> ir) {
//        if (ModList.THERMALEXPANSION.isLoaded() && ir.getClass() == teNEIClass) {
//            try {
//                ir = (Recipe<?>)teNEIWrappedRecipe.get(ir);
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        return ir;
//    }*/
//
//    /**
//     * Get the smelting recipe of an item by output. Args: output
//
//    public static ItemStack getFurnaceInput(ItemStack out) {
//        HashMap m = (HashMap) FurnaceRecipes.smelting().getSmeltingList();
//        for (Object o : m.keySet()) {
//            ItemStack in = (ItemStack) o;
//            if (ReikaItemHelper.matchStacks(FurnaceRecipes.smelting().getSmeltingResult(in), out)) {
//                return in;
//            }
//        }
//        return null;
//    }
//
//    /**
//     * Adds a smelting recipe. Args; Item in, item out, xp
//     */
//    public static void addSmelting(ItemStack in, ItemStack out, float xp) {
////    todo    FurnaceRecipes.smelting().func_151394_a(in, out, xp);
//    }
//
//    public static boolean replaceIngredientInRecipe(ItemStack ingredient, Object replacement, Recipe<?> ir) {
//        return replaceIngredientInRecipe(ingredient, replacement, ir, null);
//    }
//
//    public static boolean replaceIngredientInRecipe(ItemStack ingredient, Object replacement, Recipe<?> ir, ReplacementCallback rc) {
//        if (ir == null)
//            return false;
//        boolean flag = false;
//        if (ingredient == null)
//            throw new MisuseException("You cannot replace null in recipes!");
//
////        if (replacement instanceof String)
////            replacement = OreDictionary.getOres((String) replacement);
//
////        ir = getTEWrappedRecipe(ir);
//
//        if (ir instanceof ShapedRecipe) {
//            if (!(replacement instanceof ItemStack)) {
//                throw new MisuseException("You cannot put non-single-stack entries into a basic recipe type!");
//            }
//            if (ReikaItemHelper.matchStacks(ingredient, replacement)) //not replacing self with self
//                return false;
//            ShapedRecipe s = (ShapedRecipe) ir;
//            for (int i = 0; i < s.getIngredients().size(); i++) {
//                if (ReikaItemHelper.matchStacks(ingredient, s.getIngredients().get(i))) {
//                    flag = true;
//                    if (rc != null)
//                        rc.onReplaced(ir, i, s.getIngredients().get(i), replacement);
//                    s.getIngredients().set(i, (Ingredient) replacement);
//                }
//            }
//        } else if (ir instanceof ShapelessRecipe) {
//            if (!(replacement instanceof ItemStack)) {
//                throw new MisuseException("You cannot put non-single-stack entries into a basic recipe type!");
//            }
//            if (ReikaItemHelper.matchStacks(ingredient, replacement)) //not replacing self with self
//                return false;
//            ShapelessRecipe s = (ShapelessRecipe) ir;
//            List<Ingredient> in = s.getIngredients();
//            for (int i = 0; i < in.size(); i++) {
//                if (ReikaItemHelper.matchStacks(ingredient, in.get(i))) {
//                    flag = true;
//                    if (rc != null)
//                        rc.onReplaced(ir, i, in.get(i), replacement);
//                    in.set(i, (Ingredient) replacement);
//                }
//            }
//        } else if (ir instanceof ShapedRecipe) {
//            ShapedRecipe s = (ShapedRecipe) ir;
//            Object[] in = new NonNullList[]{s.getIngredients()};
//            for (int i = 0; i < in.length; i++) {
//                if (in[i] instanceof ItemStack && ReikaItemHelper.matchStacks(ingredient, (ItemStack) in[i])) {
//                    if (replacement instanceof ItemStack && ReikaItemHelper.matchStacks(ingredient, replacement))
//                        continue;
//                    flag = true;
//                    if (rc != null)
//                        rc.onReplaced(ir, i, in[i], replacement);
//                    in[i] = replacement;
//                } else if (in[i] instanceof List && ReikaItemHelper.collectionContainsItemStack((List<ItemStack>) in[i], ingredient)) {
//                    flag = ((List) in[i]).size() != 1;
//                    if (rc != null)
//                        rc.onReplaced(ir, i, in[i], replacement);
//                    in[i] = replacement;
//                }
//            }
//        } else if (ir instanceof ShapelessRecipe) {
//            ShapelessRecipe s = (ShapelessRecipe) ir;
//            NonNullList<Ingredient> in = s.getIngredients();
//            for (int i = 0; i < in.size(); i++) {
//                if (in.get(i) instanceof Ingredient && ReikaItemHelper.matchStacks(ingredient, (Ingredient) in.get(i))) {
//                    if (replacement instanceof ItemStack && ReikaItemHelper.matchStacks(ingredient, replacement))
//                        continue;
//                    flag = true;
//                    if (rc != null)
//                        rc.onReplaced(ir, i, in.get(i), replacement);
//                    in.set(i, (Ingredient) replacement);
//                } else if (in.get(i) instanceof List && ReikaItemHelper.collectionContainsItemStack((List<ItemStack>) in.get(i), ingredient)) {
//                    flag = ((List) in.get(i)).size() != 1;
//                    if (rc != null)
//                        rc.onReplaced(ir, i, in.get(i), replacement);
//                    in.set(i, (Ingredient) replacement);
//                }
//            }
//        } else if (ir.getClass() == ic2ShapedClass) {
//            try {
//                Object[] in = (Object[]) shapedIc2Input.get(ir);
//                for (int i = 0; i < in.length; i++) {
//                    if (in[i] instanceof ItemStack && ReikaItemHelper.matchStacks(ingredient, (ItemStack) in[i])) {
//                        flag = true;
//                        if (rc != null)
//                            rc.onReplaced(ir, i, in[i], replacement);
//                        in[i] = replacement;
//                    }
//                 /*   else if (in[i] instanceof IRecipeInput && ((IRecipeInput)in[i]).matches(ingredient)) {
//                        flag = true;
//                        if (rc != null)
//                            rc.onReplaced(ir, i, in[i], replacement);
//                        in[i] = replacement;
//                    }*/
//                    else if (in[i] instanceof Iterable) {
//                        boolean repl = false;
//                        for (Object o : (Iterable) in[i]) {
//                            if (o instanceof ItemStack && ReikaItemHelper.matchStacks(ingredient, (ItemStack) o)) {
//                                repl = true;
//                                break;
//                            }
//                            /*else if (o instanceof IRecipeInput && ((IRecipeInput)o).matches(ingredient)) {
//                                repl = true;
//                                break;
//                            }*/
//                        }
//                        if (repl) {
//                            flag = true;
//                            if (rc != null)
//                                rc.onReplaced(ir, i, in[i], replacement);
//                            in[i] = replacement;
//                        }
//                    }
//                }
//                Object[] in2 = (Object[]) shapedIc2InputMirror.get(ir);
//                if (in2 != null) {
//                    for (int i = 0; i < in2.length; i++) {
//                        if (in2[i] instanceof ItemStack && ReikaItemHelper.matchStacks(ingredient, (ItemStack) in2[i])) {
//                            flag = true;
//                            if (rc != null)
//                                rc.onReplaced(ir, i, in2[i], replacement);
//                            in2[i] = replacement;
//                        }
//                     /*   else if (in2[i] instanceof IRecipeInput && ((IRecipeInput)in2[i]).matches(ingredient)) {
//                            flag = true;
//                            if (rc != null)
//                                rc.onReplaced(ir, i, in2[i], replacement);
//                            in2[i] = replacement;
//                        }*/
//                        else if (in2[i] instanceof Iterable) {
//                            boolean repl = false;
//                            for (Object o : (Iterable) in2[i]) {
//                                if (o instanceof ItemStack && ReikaItemHelper.matchStacks(ingredient, (ItemStack) o)) {
//                                    repl = true;
//                                    break;
//                                }
//                                /*else if (o instanceof IRecipeInput && ((IRecipeInput)o).matches(ingredient)) {
//                                    repl = true;
//                                    break;
//                                }*/
//                            }
//                            if (repl) {
//                                flag = true;
//                                if (rc != null)
//                                    rc.onReplaced(ir, i, in2[i], replacement);
//                                in2[i] = replacement;
//                            }
//                        }
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        } else if (ir.getClass() == ic2ShapelessClass) {
//            try {
//                Object[] in = (Object[]) shapelessIc2Input.get(ir);
//                for (int i = 0; i < in.length; i++) {
//                    if (in[i] instanceof ItemStack && ReikaItemHelper.matchStacks(ingredient, (ItemStack) in[i])) {
//                        flag = true;
//                        if (rc != null)
//                            rc.onReplaced(ir, i, in[i], replacement);
//                        in[i] = replacement;
//                    }
//                  /*  else if (in[i] instanceof IRecipeInput && ((IRecipeInput)in[i]).matches(ingredient)) {
//                        flag = true;
//                        if (rc != null)
//                            rc.onReplaced(ir, i, in[i], replacement);
//                        in[i] = replacement;
//                    }*/
//                    else if (in[i] instanceof Iterable) {
//                        boolean repl = false;
//                        for (Object o : (Iterable) in[i]) {
//                            if (o instanceof ItemStack && ReikaItemHelper.matchStacks(ingredient, (ItemStack) o)) {
//                                repl = true;
//                                break;
//                            }
//                           /* else if (o instanceof IRecipeInput && ((IRecipeInput)o).matches(ingredient)) {
//                                repl = true;
//                                break;
//                            }*/
//                        }
//                        if (repl) {
//                            flag = true;
//                            if (rc != null)
//                                rc.onReplaced(ir, i, in[i], replacement);
//                            in[i] = replacement;
//                        }
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        } else if (ir.getClass() == aeShapedClass) {
//            try {
//                Object[] in = (Object[]) shapedAEInput.get(ir);
//                for (int i = 0; i < in.length; i++) {
//                    if (in[i] instanceof ItemStack && ReikaItemHelper.matchStacks(ingredient, (ItemStack) in[i])) {
//                        if (replacement instanceof ItemStack && ReikaItemHelper.matchStacks(ingredient, replacement))
//                            continue;
//                        flag = true;
//                        if (rc != null)
//                            rc.onReplaced(ir, i, in[i], replacement);
//                        in[i] = replacement;
//                    } else if (in[i] instanceof List && ReikaItemHelper.collectionContainsItemStack((List<ItemStack>) in[i], ingredient)) {
//                        flag = ((List) in[i]).size() != 1;
//                        if (rc != null)
//                            rc.onReplaced(ir, i, in[i], replacement);
//                        in[i] = replacement;
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        } else if (ir.getClass() == aeShapelessClass) {
//            try {
//                ArrayList in = (ArrayList) shapelessAEInput.get(ir);
//                for (int i = 0; i < in.size(); i++) {
//                    if (in.get(i) instanceof ItemStack && ReikaItemHelper.matchStacks(ingredient, (ItemStack) in.get(i))) {
//                        if (replacement instanceof ItemStack && ReikaItemHelper.matchStacks(ingredient, replacement))
//                            continue;
//                        flag = true;
//                        if (rc != null)
//                            rc.onReplaced(ir, i, in.get(i), replacement);
//                        in.set(i, replacement);
//                    } else if (in.get(i) instanceof List && ReikaItemHelper.collectionContainsItemStack((List<ItemStack>) in.get(i), ingredient)) {
//                        flag = ((List) in.get(i)).size() != 1;
//                        if (rc != null)
//                            rc.onReplaced(ir, i, in.get(i), replacement);
//                        in.set(i, replacement);
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        } else if (ir.getClass() == computerTurtleClass) {
//            try {
//                Item[] in = (Item[]) computerTurtleInput.get(ir);
//                for (int i = 0; i < 3; i++) {
//                    for (int k = 0; k < 3; k++) {
//                        int idx = i * 3 + k;
//                        if (in[idx] == ingredient.getItem()) {
//                            flag = true;
//                            if (rc != null)
//                                rc.onReplaced(ir, i, in[i], replacement);
//                            in[idx] = ((ItemStack) replacement).getItem();
//                        }
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        return flag;
//    }
//
//    public static ArrayList<ItemStack> getAllItemsInRecipe(Recipe<?> ire) {
//        ArrayList<ItemStack> li = new ArrayList<>();
//        for (int i = 0; i < ire.getIngredients().size(); i++) {
//            li.add(ire.getIngredients().get(i).getItems()[0]); //todo 0 could be i? idfk lmao
//        }
//        return li;
//    }
//
//    public static int getRecipeIngredientCount(Recipe<?> recipe) {
//        return recipe.getIngredients().size();
//    }
//
///*    *//**
//     * DISTINCT from getAllItems in that it returns a list of objects, including lists!
//     *//*
//    public static ArrayList<Object> getAllInputsInRecipe(Recipe<?> ire) {
//        ArrayList<Object> li = new ArrayList();
//        if (ire instanceof ShapedRecipe) {
//            ShapedRecipe r = (ShapedRecipe) ire;
//            for (int i = 0; i < r.getIngredients().size(); i++) {
//                li.add(r.getIngredients().get(i));
//            }
//        } else if (ire instanceof ShapedRecipe) {
//            ShapedRecipe so = (ShapedRecipe) ire;
//            Object[] objin = so.getIngredients();
//            for (int i = 0; i < objin.length; i++) {
//                li.add(objin[i]);
//            }
//        } else if (ire instanceof ShapelessRecipe) {
//            ShapelessRecipe sr = (ShapelessRecipe) ire;
//            li.addAll(sr.getIngredients());
//        } else if (ire instanceof ShapelessRecipe) {
//            ShapelessRecipe so = (ShapelessRecipe) ire;
//            for (int i = 0; i < so.getRecipeSize(); i++) {
//                Object obj = so.getIngredients().get(i);
//                li.add(obj);
//            }
//        }
//        return li;
//    }*/
//
//    public static Recipe<?> getShapelessRecipeFor(ItemStack out, ItemStack... in) {
//        NonNullList<Ingredient> ingredients = NonNullList.of(Ingredient.of(ReikaJavaLibrary.makeListFrom(in).stream()));
//        return new ShapelessRecipe(null, null, null, out.copy(), ingredients); //todo nulls
//    }
//
//    /*todo   public static boolean matchArrayToRecipe(ItemStack[] in, Recipe<?> ir) {
//        RecipePattern r = new RecipePattern(in);
//        return ir.matches(r, null);
//    }*/
//
//    public static boolean recipeContains(Recipe<?> ir, ItemStack is) {
//        return ReikaItemHelper.collectionContainsItemStack(getAllItemsInRecipe(ir), is);
//    }
//
//    public static Collection<Integer> getRecipeLocationIndices(Recipe<?> ir, ItemStack is) {
//        Collection<Integer> c = new ArrayList();
//        RecipeCache r = getRecipeCacheObject(ir, false);
//        if (r instanceof UnparsableRecipeCache)
//            return c;
//        for (int i = 0; i < 9; i++) {
//            List<ItemStack> li = r.items[i];
//            if (li != null && ReikaItemHelper.collectionContainsItemStack(li, is))
//                c.add(i);
//        }
//        return c;
//    }
//
//    public static ItemHashMap<Integer> getItemCountsForDisplay(Recipe<?> ir) {
//        ItemHashMap<Integer> map = new ItemHashMap();
//        ItemStack[] items = ReikaRecipeHelper.getPermutedRecipeArray(ir);
//        if (items == null)
//            return map;
//        for (int i = 0; i < 9; i++) {
//            ItemStack is = items[i];
//            if (is != null) {
//                Integer num = map.get(is);
//                int n = num != null ? num.intValue() : 0;
//                map.put(is, n + 1);
//            }
//        }
//        return map;
//    }
//
//    public static String toString(Recipe<?> r) {
//        if (r == null) {
//            return "<NULL>";
//        }
//
////        r = getTEWrappedRecipe(r);
//
//        if (r instanceof ShapedRecipe) {
//            return "Shaped " + Arrays.toString(new NonNullList[]{r.getIngredients()}) + " > " + r.getResultItem();
//        } else if (r instanceof ShapelessRecipe) {
//            return "Shapeless " + r.getIngredients() + " > " + r.getResultItem();
//        } else if (r instanceof ShapedRecipe) {
//            return "Shaped Ore " + Arrays.toString(new NonNullList[]{r.getIngredients()}) + " > " + r.getResultItem();
//        } else if (r instanceof ShapelessRecipe) {
//            return "Shapeless Ore " + r.getIngredients().toString() + " > " + r.getResultItem();
//        } else if (r.getClass() == ic2ShapedClass) {
//            try {
//                Object[] in = (Object[]) shapedIc2Input.get(r);
//                return "Shaped IC2 " + Arrays.deepToString(in) + " > " + r.getResultItem();
//            } catch (Exception e) {
//                e.printStackTrace();
//                return e.toString();
//            }
//        } else if (r.getClass() == ic2ShapelessClass) {
//            try {
//                Object[] in = (Object[]) shapelessIc2Input.get(r);
//                return "Shapeless IC2 " + Arrays.deepToString(in) + " > " + r.getResultItem();
//            } catch (Exception e) {
//                e.printStackTrace();
//                return e.toString();
//            }
//        } else if (r.getClass() == aeShapedClass) {
//            try {
//                Object[] in = (Object[]) shapedAEInput.get(r);
//                return "Shaped AE " + Arrays.deepToString(in) + " > " + r.getResultItem();
//            } catch (Exception e) {
//                e.printStackTrace();
//                return e.toString();
//            }
//        } else if (r.getClass() == aeShapelessClass) {
//            try {
//                List<Object> in = (List<Object>) shapelessAEInput.get(r);
//                return "Shapeless AE " + in.toString() + " > " + r.getResultItem();
//            } catch (Exception e) {
//                e.printStackTrace();
//                return e.toString();
//            }
//        } else if (r.getClass() == computerTurtleClass) {
//            try {
//                Item[] in = (Item[]) computerTurtleInput.get(r);
//                return "CC Turtle " + Arrays.deepToString(in) + " > " + r.getResultItem();
//            } catch (Exception e) {
//                e.printStackTrace();
//                return e.toString();
//            }
//        }
//        else if (r instanceof CustomToStringRecipe) {
//            return ((CustomToStringRecipe) r).toDisplayString();
//        }
//        else {
//            return "Unknown '" + r.getClass().getName() + "'" + " > " + r.getResultItem();
//        }
//    }
//
//    /**
//     * Rather slower than toString, so only use this where necessary.
//     */
//    public static String toDeterministicString(Recipe<?> r) {
//        if (r instanceof ShapedRecipe) {
//            Ingredient[] arr = Arrays.copyOf(r.getIngredients().toArray(new Ingredient[0]), r.getIngredients().size());
//            //Arrays.sort(arr, ReikaItemHelper.comparator); DO NOT CHANGE RECIPE ORDER
//            return "Shaped " + Arrays.toString(arr) + " > " + r.getResultItem();
//        } else if (r instanceof ShapelessRecipe) {
//            ArrayList<ItemStack> li = new ArrayList(r.getIngredients());
//            li.sort(ReikaItemHelper.comparator);
//            return "Shapeless " + li + " > " + r.getResultItem();
//        } else if (r instanceof ShapedRecipe) {
//            Object[] arr = Arrays.copyOf(r.getIngredients().toArray(), r.getIngredients().size());
//            //Arrays.sort(arr, ReikaItemHelper.itemListComparator);
//            for (int i = 0; i < arr.length; i++) {
//                Object o = arr[i];
//                if (o instanceof List) {
//                    o = new ArrayList((List) o);
//                    Collections.sort((List) o, ReikaItemHelper.comparator);
//                    arr[i] = o;
//                }
//            }
//            return "Shaped Ore " + Arrays.toString(arr) + " > " + r.getResultItem();
//        } else if (r instanceof ShapelessRecipe) {
//            ArrayList<Object> li = new ArrayList(r.getIngredients());
//            Collections.sort(li, ReikaItemHelper.itemListComparator);
//            for (int i = 0; i < li.size(); i++) {
//                Object o = li.get(i);
//                if (o instanceof List) {
//                    o = new ArrayList((List) o);
//                    Collections.sort((List) o, ReikaItemHelper.comparator);
//                    li.set(i, o);
//                }
//            }
//            return "Shapeless Ore " + li.toString() + " > " + r.getResultItem();
//        }
////        else if (r instanceof CustomToStringRecipe) {
////            return ((CustomToStringRecipe) r).toDeterministicString();
////        }
//        else {
//            return "Unknown '" + r.getClass().getName() + "'" + " > " + r.getResultItem();
//        }
//    }
//
///*    public static NonNullList<Ingredient> decode1DArray(NonNullList<Ingredient> array, int w, int h) {
//        if (array.size() != w * h)
//            throw new IllegalArgumentException("Recipe size does not match array length!");
//        ArrayList<Object> li = new ArrayList<>();
//        char[][] input = new char[h][w];
//        for (int i = 0; i < w; i++) {
//            for (int k = 0; k < h; k++) {
//                int idx = i + k * w;
//                Object at = parseIngredient(array.get(idx));
//                char c = at == null ? ' ' : (char) ('a' + idx);
//                input[k][i] = c;
//                if (at != null) {
//                    li.add(c);
//                    li.add(at);
//                }
//            }
//        }
//        ArrayList<String> shape = new ArrayList<>();
//        for (char[] line : input) {
//            StringBuilder sb = new StringBuilder();
//            for (char c : line) {
//                sb.append(c);
//            }
//            shape.add(sb.toString());
//        }
//        li.addAll(0, shape);
//        return li.toArray(new Object[li.size()]); //todo bad cast likely 98.22222% :P
//    }*/
//
//    public static Ingredient parseIngredient(Ingredient o) {
//        if (o instanceof Collection) {
//            Collection<Ingredient> c = (Collection) o;
//            if (c.isEmpty())
//                throw new IllegalArgumentException("Recipe had an empty collection ingredient?!");
//            return null;// getOreNameForCollection(c);
//        }
//        /*if (ModList.IC2.isLoaded()) {
//            o = parseIc2Ingredient(o);
//        }
//        if (ModList.APPENG.isLoaded()) {
//            o = parseAEIngredient(o);
//        }*/
//        return o;
//    }
//
//    private static String getOreNameForCollection(Collection<Ingredient> c) {
////        ItemStack is = c.iterator().next();
//        HashSet<String> set = new HashSet<>();//todo ReikaItemHelper.getOreNames(is);
//        for (Ingredient is2 : c) {
//            set.retainAll(Collections.singleton(is2.toString()));
//        }
//        if (set.isEmpty())
//            throw new IllegalArgumentException("Recipe had a collection ingredient, with no shared ore tags?!");
//        return set.iterator().next();
//    }
//
///*    @ModDependent(ModList.IC2)
//    private static Object parseIc2Ingredient(Object o) {
//        if (o instanceof IRecipeInput) {
//            if (o instanceof RecipeInputOreDict) {
//                return ((RecipeInputOreDict)o).input;
//            }
//            List<ItemStack> li = ((IRecipeInput)o).getInputs();
//            return li.size() == 1 ? li.get(0) : getOreNameForCollection(li);
//        }
//        return o;
//    }
//
//    @ModDependent(ModList.APPENG)
//    private static Object parseAEIngredient(Object o) {
//        if (o instanceof IAEItemStack) {
//            return ((IAEItemStack)o).getItemStack();
//        }
//        if (o instanceof Ingredient) {
//            Ingredient ii = (Ingredient)o;
//            ItemStack[] is;
//            try {
//                is = ii.getItems();
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//                return o;
//            }
//            if (is.length == 1) {
//                return is[0];
//            }
//            else {
//                List<ItemStack> li = Arrays.asList(is);
//                return getOreNameForCollection(li);
//            }
//        }
//        return o;
//    }*/
//
//    public static NonNullList<Ingredient> decode2DArray(Object[][] array) {
//        String[] input = new String[array.length];
//        NonNullList<Ingredient> objects = NonNullList.create();
//        NonNullList<Ingredient> entries = NonNullList.create();
//        for (int i = 0; i < array.length; i++) {
//            StringBuilder sb = new StringBuilder();
//            for (int k = 0; k < array[i].length; k++) {
//                Ingredient o = Ingredient.of((ItemLike) array[i][k]);
//                char c = o == null ? ' ' : (char) ('a' + (i * 3 + k));
//                sb.append(c);
//                if (o != null) {
////            todo        entries.add(c);
//                    entries.add(o);
//                }
//            }
//            input[i] = sb.toString();
//        }
////  todo      Collections.addAll(objects, input);
//        objects.addAll(entries);
//
//        return objects;
//    }
//
//    public static Recipe<?> convertRecipeToOre(Recipe<?> ire) {
////        ire = getTEWrappedRecipe(ire);
//        /*if (ire instanceof ShapedRecipe) {
//            ShapedRecipe r = (ShapedRecipe) ire;
//            return new ShapedRecipe(ire.getId(), ire.getGroup(), ((ShapelessRecipe) ire).category(), ((ShapedRecipe) ire).getWidth(), ((ShapedRecipe) ire).getHeight(), decode1DArray(r.getIngredients(), r.getWidth(), r.getHeight()), ire.getResultItem());
//        } else */if (ire instanceof ShapelessRecipe) {
//            ShapelessRecipe sr = (ShapelessRecipe) ire;
//            List<Ingredient> in = sr.getIngredients();
//
//            NonNullList<Ingredient> ingredients = NonNullList.create();
//            for (int i = 0; i < in.size(); i++) {
//                ingredients.set(i, parseIngredient(in.get(i)));
//            }
//            return new ShapelessRecipe(ire.getId(), ire.getGroup(), ((ShapelessRecipe) ire).category(), ire.getResultItem(), ingredients);
//        }
//        return ire;
//    }
//
//    @Deprecated
//    public static Recipe<?> copyRecipe(Recipe<?> ire) {
//        try {
////            ire = getTEWrappedRecipe(ire);
//           /* if (ire instanceof ShapedRecipe) {
//                ShapedRecipe r = (ShapedRecipe) ire;
//                return getShapedRecipeFor(ire.getResultItem(), decode1DArray(new NonNullList[]{r.getIngredients()}, r.getWidth(), r.getHeight()));
//            } else if (ire instanceof ShapedRecipe) {
//                ShapedRecipe so = (ShapedRecipe) ire;
//                return new ShapedRecipe(ire.getId(), ire.getGroup(), ((ShapedRecipe) ire).category(), 1, 1, decode1DArray(so.getIngredients(), getOreRecipeWidth(so), getOreRecipeHeight(so)), ire.getResultItem()); //todo nulls and 1's
//            } else */if (ire instanceof ShapelessRecipe) {
//                ShapelessRecipe sr = (ShapelessRecipe) ire;
//                return new ShapelessRecipe(ire.getId(), ire.getGroup(), ((ShapelessRecipe) ire).category(), ire.getResultItem(), sr.getIngredients()); //todo nulls
//            } else if (ire instanceof ShapelessRecipe) {
//                ShapelessRecipe sr = (ShapelessRecipe) ire;
//                NonNullList<Ingredient> in = sr.getIngredients();
//                NonNullList<Ingredient> ingredients = NonNullList.create();
//
//                ingredients.addAll(sr.getIngredients());
//
//                for (int i = 0; i < in.size(); i++) {
//                    ingredients.set(i, parseIngredient(in.get(i)));
//                }
//                return new ShapelessRecipe(ire.getId(), ire.getGroup(), ((ShapelessRecipe) ire).category(), ire.getResultItem(), ingredients); //todo nulls
//            }
//           /* else if (ire.getClass() == ic2ShapedClass) {
//                try {
//                    Object[] in = (Object[]) shapedIc2Input.get(ire);
//                    in = padIC2CrushedArray(in, ire);
//                    int w = shapedIc2Width.getInt(ire);
//                    int h = shapedIc2Height.getInt(ire);
//                    if (w * h != in.length) {
//                        DragonAPI.LOGGER.error("Error parsing IC2 recipe: input array does not match reported height and width values!");
//                    }
//                    return new ShapedRecipe(ire.getResultItem(), decode1DArray(in, w, h));
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//            else if (ire.getClass() == ic2ShapelessClass) {
//                try {
//                    Object[] in = (Object[]) shapelessIc2Input.get(ire);
//
//                    NonNullList<Ingredient> ingredients = NonNullList.create();
//                    for (int i = 0; i < in.length; i++) {
//                        ingredients.set(i, parseIngredient(in[i]));
//                    }
//                    return new ShapelessRecipe(null, null, ire.getResultItem(), ingredients); //todo nulls
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            } else if (ire.getClass() == aeShapedClass) {
//                try {
//                    Object[] in = (Object[]) shapedAEInput.get(ire);
//                    int w = shapedAEWidth.getInt(ire);
//                    int h = shapedAEHeight.getInt(ire);
//                    return new ShapedRecipe(null, null, 1, 1, ire.getResultItem(), decode1DArray(in, w, h));//todo nulls and 1's
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            } else if (ire.getClass() == aeShapelessClass) {
//                try {
//                    ArrayList<Object> in = (ArrayList<Object>) shapelessAEInput.get(ire);
//                    NonNullList<Ingredient> ingredients = NonNullList.create();
//
//                    for (int i = 0; i < in.size(); i++) {
//                        ingredients.set(i, parseIngredient(in.get(i)));
//                    }
//                    return new ShapelessRecipe(null, null, ire.getResultItem(), ingredients); //todo nulls
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            } else if (ire.getClass() == computerTurtleClass) {
//                try {
//                    Item[] in = (Item[]) computerTurtleInput.get(ire);
//                    return getShapedRecipeFor(ire.getResultItem(), decode1DArray(in, 3, 3));
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }*/
//        } catch (Exception e) {
//            DragonAPI.LOGGER.error("Could not copy recipe " + toString(ire));
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    public static boolean matchRecipes(Recipe<?> r1, Recipe<?> r2) {
//        if (r1 == null && r2 == null)
//            return true;
//        if (r1 == null || r2 == null)
//            return false;
//        if (r1.getClass() != r2.getClass())
//            return false;
////        r1 = getTEWrappedRecipe(r1);
////        r2 = getTEWrappedRecipe(r2);
//        if (!ItemStack.isSameItem(r1.getResultItem(), r2.getResultItem()))
//            return false;
//        if (r1 instanceof ShapedRecipe) {
//            ShapedRecipe sr1 = (ShapedRecipe) r1;
//            ShapedRecipe sr2 = (ShapedRecipe) r2;
//            return ReikaItemHelper.matchStackCollections(sr1.getIngredients(), sr2.getIngredients());
//        } else if (r1 instanceof ShapedRecipe) {
//            ShapedRecipe so1 = (ShapedRecipe) r1;
//            ShapedRecipe so2 = (ShapedRecipe) r2;
//            return matchIngredientCollections(so1.getIngredients(), so2.getIngredients());
//        } else if (r1 instanceof ShapelessRecipe) {
//            ShapelessRecipe sr1 = (ShapelessRecipe) r1;
//            ShapelessRecipe sr2 = (ShapelessRecipe) r2;
//            return ReikaItemHelper.matchStackCollections(sr1.getIngredients(), sr2.getIngredients());
//        } else if (r1 instanceof ShapelessRecipe) {
//            ShapelessRecipe sr1 = (ShapelessRecipe) r1;
//            ShapelessRecipe sr2 = (ShapelessRecipe) r2;
//            return matchIngredientCollections(sr1.getIngredients(), sr2.getIngredients());
//        }
//     /*   else if (r1.getClass() == ic2ShapedClass) {
//            try {
//                Object[] in1 = (Object[]) shapedIc2Input.get(r1);
//                Object[] in2 = (Object[]) shapedIc2Input.get(r2);
//                //in1 = padIC2CrushedArray(in1, r1);
//                //in2 = padIC2CrushedArray(in2, r2);
//                return matchIngredientCollections(Arrays.asList(in1), Arrays.asList(in2));
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        } else if (r1.getClass() == ic2ShapelessClass) {
//            try {
//                Object[] in1 = (Object[]) shapelessIc2Input.get(r1);
//                Object[] in2 = (Object[]) shapelessIc2Input.get(r2);
//                return matchIngredientCollections(Arrays.asList(in1), Arrays.asList(in2));
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        else if (r1.getClass() == aeShapedClass) {
//            try {
//                Object[] in1 = (Object[]) shapedAEInput.get(r1);
//                Object[] in2 = (Object[]) shapedAEInput.get(r2);
//                return matchIngredientCollections(Arrays.asList(in1), Arrays.asList(in2));
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        } else if (r1.getClass() == aeShapelessClass) {
//            try {
//                List<Object> in1 = (List<Object>) shapelessAEInput.get(r1);
//                List<Object> in2 = (List<Object>) shapelessAEInput.get(r2);
//                return matchIngredientCollections(in1, in2);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        } else if (r1.getClass() == computerTurtleClass) {
//            try {
//                Item[] in1 = (Item[]) computerTurtleInput.get(r1);
//                Item[] in2 = (Item[]) computerTurtleInput.get(r2);
//                if (in1.length != in2.length)
//                    return false;
//                for (int i = 0; i < in1.length; i++) {
//                    if (in1[i] != in2[i])
//                        return false;
//                }
//                return true;
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }*/
//        return false;
//    }
//
//    private static boolean matchIngredientCollections(NonNullList<Ingredient> input, NonNullList<Ingredient> input2) {
//        if (input.size() != input2.size())
//            return false;
//        for (int i = 0; i < input.size(); i++) {
//            Object o1 = input.get(i);
//            Object o2 = input2.get(i);
//            if (o1 == null && o2 == null)
//                continue;
//            if (o1 == null || o2 == null)
//                return false;
//            if (o1.getClass() != o2.getClass())
//                return false;
//            if (o1 instanceof Ingredient) {
//                if (!ReikaItemHelper.matchStacks(Arrays.stream(((Ingredient) o1).getItems()).toList().get(i), Arrays.stream(((Ingredient) o2).getItems()).toList().get(i)))
//                    return false;
//            } else { //if (o1 instanceof Collection || o1 instanceof String)
//                if (!o1.equals(o2))
//                    return false;
//            }
//        }
//        return true;
//    }
//}