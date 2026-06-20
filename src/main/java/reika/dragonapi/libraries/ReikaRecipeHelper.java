package reika.dragonapi.libraries;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.exception.MisuseException;
import reika.dragonapi.instantiable.data.KeyedItemStack;
import reika.dragonapi.instantiable.data.maps.ItemHashMap;
import reika.dragonapi.interfaces.CustomToStringRecipe;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;
import reika.dragonapi.libraries.java.ReikaObfuscationHelper;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

import java.lang.reflect.Field;
import java.util.*;

public class ReikaRecipeHelper {

//    private static final CraftingManager cr = CraftingManager.getInstance();

   private static final Random rand = new Random();

   private static final int[] permuOffsets = new int[9];

   private static final HashMap<Recipe<?>, RecipeCache> recipeCache = new HashMap();
   private static final HashMap<Recipe<?>, RecipeCache> recipeCacheClient = new HashMap();

   private static Class ic2ShapedClass;
   private static Class ic2ShapelessClass;
   private static Field shapedIc2Input;
   private static Field shapedIc2InputMirror;
   private static Field shapedIc2Height;
   private static Field shapedIc2Width;
   private static Field ic2MasksField;
   private static Field shapelessIc2Input;

   private static Class aeShapedClass;
   private static Class aeShapelessClass;
   private static Field shapedAEInput;
   private static Field shapelessAEInput;
   private static Field shapedAEHeight;
   private static Field shapedAEWidth;

   private static Class computerTurtleClass;
   private static Field computerTurtleInput;

   private static Class fairyComponentClass;
   private static Field fairyComponentInput;
   private static Field fairyComponentOutput;
   private static Field fairyComponentHeight;
   private static Field fairyComponentWidth;
   private static Class fairyStringClass;
   private static Field fairyStringInput;
   public static Field fairyStringOutput;
   private static Field fairyStringHeight;
   private static Field fairyStringWidth;

   private static Class teNEIClass;
   private static Field teNEIWrappedRecipe;

   private static class RecipeCache {

       private final List<ItemStack>[] items;
       private final int width;
       private final int height;

       private RecipeCache(List<ItemStack>[] items, int w, int h) {
           this.items = items;
           width = w;
           height = h;
       }
   }

   private static class UnparsableRecipeCache extends RecipeCache {

       private UnparsableRecipeCache() {
           super(new List[0], 0, 0);
       }

   }


   public static interface ReplacementCallback {

       void onReplaced(Recipe<?> ir, int slot, Object from, Object to);

   }

   // 26.2: ShapedRecipe's height/width/input fields moved into a private ShapedRecipePattern, so the
   // old reflection is gone — the public getWidth()/getHeight() accessors replace it. Recipes are also
   // immutable now, so the never-called overwriteShapedOreRecipeInput mutator is dropped.
   public static int getOreRecipeHeight(ShapedRecipe s) {
       return s.getHeight();
   }

   public static int getOreRecipeWidth(ShapedRecipe s) {
       return s.getWidth();
   }

	/** Finds recipes by product. NOT PERFORMANT! */
	public static List<ShapedRecipe> getShapedRecipesByOutput(List<Recipe<?>> in, ItemStack out) {
		List<ShapedRecipe> li = new ArrayList<ShapedRecipe>();
		for (int i = 0; i < in.size(); i++) {
			Recipe<?> ir = in.get(i);
			//DragonAPI.LOGGER.info(getRecipeOutput(ir)+" == "+out);
			if (ir instanceof ShapedRecipe) {
				if (ReikaItemHelper.matchStacks(getRecipeOutput(ir), out))
					li.add((ShapedRecipe)ir);
			}
		}
		//DragonAPI.LOGGER.info(li);
		return li;
	}
	 
   public static boolean isUniformInput(Recipe<?> ir) {
       HashSet<KeyedItemStack> set = new HashSet<>();
       for (ItemStack is : getAllItemsInRecipe(ir)) {
           KeyedItemStack ks = new KeyedItemStack(is).setSimpleHash(true);
           set.add(ks);
       }
       return set.size() == 1;
   }

   /**
    * Returns the item in a shaped recipe at x, y in the grid.
    */
   public static Ingredient getItemInRecipeAtXY(ShapedRecipe r, int x, int y) {
       int xy = x + r.getWidth() * y;
       return getRecipeIngredients(r).get(xy);
   }

   /**
    * Finds recipes by product.
    */
   public static <R extends Recipe<?>> List<R> getAllRecipesByOutput(List<R> in, ItemStack out) {
       List<R> result = new ArrayList<>();
       for (R recipe : in) {
           if (ReikaItemHelper.matchStacks(getRecipeOutput(recipe), out)) {
               result.add(recipe);
           }
       }
       return result;
   }

      public static <R extends Recipe<?>> Collection<R> getAllRecipesByOutput(Collection<R> in, ItemStack out) {
       Collection<R> result = new ArrayList<>();
       for (R recipe : in) {
           if (ReikaItemHelper.matchStacks(getRecipeOutput(recipe), out)) {
               result.add(recipe);
           }
       }
       return result;
   }

   public static boolean isCraftable(List<Recipe<?>> in, ItemStack is) {
       return getAllRecipesByOutput(in, is).size() > 0;
   }

   /**
    * Finds recipes by product.
    */
   public static List<ShapelessRecipe> getShapelessRecipesByOutput(List<Recipe<?>> in, ItemStack out) {
       List<ShapelessRecipe> li = new ArrayList<>();
       for (Recipe<?> ir : in) {
           if (ir instanceof ShapelessRecipe) {
               if (ReikaItemHelper.matchStacks(getRecipeOutput(ir), out))
                   li.add((ShapelessRecipe) ir);
           }
       }
       //DragonAPI.LOGGER.info(li);
       return li;
   }

   private static List<ItemStack> getRecipeItemStack(ItemStack is, boolean client) {
       if (is == null)
           return null;
       else {
           return ReikaJavaLibrary.makeListFrom(is);
       }
   }

   private static List<ItemStack> getRecipeItemStacks(Object[] c, boolean client) {
       ArrayList<ItemStack> ret = new ArrayList<>();
       for (Object o : c) {
           if (o == null)
               continue;
           if (o instanceof ItemStack) {
               ItemStack is = (ItemStack) o;
               ret.add(is);
           } else if (o instanceof Collection) {
               ret.addAll((Collection<? extends ItemStack>) o);
           }
       }
       return ret;
   }

   private static List<ItemStack> getRecipeItemStacks(Collection<?> c, boolean client) {
       ArrayList<ItemStack> ret = new ArrayList<>();
       for (Object o : c) {
           if (o == null)
               continue;
           if (o instanceof ItemStack) {
               ItemStack is = (ItemStack) o;
               ret.add(is);
           }
       }
       return ret;
   }

   private static RecipeCache getRecipeCacheObject(Recipe<?> ir, boolean client) {
       HashMap<Recipe<?>, RecipeCache> map = client ? recipeCacheClient : recipeCache;
       RecipeCache cache = map.get(ir);
       if (cache == null) {
           cache = calculateRecipeToItemStackArray(ir, client);
           if (!ReikaObfuscationHelper.isDeObfEnvironment())
               map.put(ir, cache);
       }
       return cache;
   }

   /**
    * Turns a recipe into a 3x3 itemstack array. Args: Recipe<?>
    */
   public static List<ItemStack>[] getRecipeArray(Recipe<?> ir) {
       List<ItemStack>[] lists = new List[9];
       RecipeCache c = getRecipeCacheObject(ir, false);
       if (c instanceof UnparsableRecipeCache)
           return null;
       for (int i = 0; i < 9; i++) {
           List li = c.items[i];
           if (li != null && !li.isEmpty())
               lists[i] = Collections.unmodifiableList(li);
       }
       return lists;
   }

   /**
    * Turns a recipe into a 3x3 itemstack array, permuting it as well, usually for rendering. Args: Recipe<?>
    */
//    @SideOnly(Side.CLIENT)
   public static ItemStack[] getPermutedRecipeArray(Recipe<?> ir) {
       RecipeCache r = getRecipeCacheObject(ir, true);
       if (r instanceof UnparsableRecipeCache)
           return null;
       List<ItemStack>[] isin = r.items;

       long ttick = System.currentTimeMillis();
//    todo    if (GuiScreen.isShiftKeyDown())
//            ttick *= 4;
//        if (GuiScreen.isCtrlKeyDown())
//            ttick /= 8;
       int time = 1000;
       for (int i = 0; i < isin.length; i++) {
           if (isin[i] != null && !isin[i].isEmpty()) {
               if (ttick % time == 0) {
                   permuOffsets[i] = rand.nextInt(isin[i].size());
               }
           }
       }

       int[] indices = new int[9];
       ItemStack[] add = new ItemStack[9];
       for (int i = 0; i < 9; i++) {
           List<ItemStack> li = isin[i];
           if (li != null && !li.isEmpty()) {
               int tick = (int) (((ttick / time) + permuOffsets[i]) % li.size());
               add[i] = li.get(tick);
           }
       }

       ItemStack[] in = new ItemStack[9];
       if (r.width == 3 && r.height == 3) {
           for (int i = 0; i < 9; i++)
               in[i] = add[i];
       }
       if (r.width == 1 && r.height == 1) {
           in[4] = add[0];
       }
       if (r.width == 2 && r.height == 2) {
           in[0] = add[0];
           in[1] = add[1];
           in[3] = add[2];
           in[4] = add[3];
       }
       if (r.width == 1 && r.height == 2) {
           in[4] = add[0];
           in[7] = add[1];
       }
       if (r.width == 2 && r.height == 1) {
           in[0] = add[0];
           in[1] = add[1];
       }
       if (r.width == 3 && r.height == 1) {
           in[0] = add[0];
           in[1] = add[1];
           in[2] = add[2];
       }
       if (r.width == 1 && r.height == 3) {
           in[1] = add[0];
           in[4] = add[1];
           in[7] = add[2];
       }
       if (r.width == 2 && r.height == 3) {
           in[0] = add[0];
           in[1] = add[1];
           in[3] = add[2];
           in[4] = add[3];
           in[6] = add[4];
           in[7] = add[5];
       }
       if (r.width == 3 && r.height == 2) {
           in[3] = add[0];
           in[4] = add[1];
           in[5] = add[2];
           in[6] = add[3];
           in[7] = add[4];
           in[8] = add[5];
       }

       return in;
   }

   private static RecipeCache calculateRecipeToItemStackArray(Recipe<?> ire, boolean client) {
       List<ItemStack>[] isin = new List[9];
       int num;
       int w = 0;
       int h = 0;
       if (ire == null)
           DragonAPI.LOGGER.error("Recipe is null!");
       if (ire == null) {
           ReikaJavaLibrary.dumpStack();
           return null;
       }
       if (ire instanceof ShapedRecipe) {
           ShapedRecipe r = (ShapedRecipe) ire;
           num = getRecipeIngredients(r).size();
           w = r.getWidth();
           h = r.getHeight();
           for (int i = 0; i < getRecipeIngredients(r).size(); i++) {
               Ingredient is = getRecipeIngredients(r).get(i);
               isin[i] = getRecipeItemStack(is.items().map(_h -> new net.minecraft.world.item.ItemStack(_h)).toArray(net.minecraft.world.item.ItemStack[]::new)[i], client);
           }

       } else if (ire instanceof ShapelessRecipe) {
           ShapelessRecipe sr = (ShapelessRecipe) ire;
           //DragonAPI.LOGGER.info(ire);
           for (int i = 0; i < getRecipeIngredients(sr).size(); i++) {
               ItemStack is = getRecipeIngredients(sr).get(i).items().map(_h -> new net.minecraft.world.item.ItemStack(_h)).toArray(net.minecraft.world.item.ItemStack[]::new)[i]; //todo check array i
               isin[i] = getRecipeItemStack(is, client);
           }
           w = getRecipeIngredients(sr).size() >= 3 ? 3 : getRecipeIngredients(sr).size();
           h = (getRecipeIngredients(sr).size() + 2) / 3;
       } else if (ire instanceof SmeltingRecipe) {
              SmeltingRecipe sr = (SmeltingRecipe) ire;
              ItemStack is = getRecipeOutput(sr);
              isin[0] = getRecipeItemStack(is, client);
              w = 1;
              h = 1;
       }
       else {
           DragonAPI.LOGGER.error("Recipe " + toString(ire) + " could not be parsed!");
           return new UnparsableRecipeCache();
       }

       return new RecipeCache(isin, w, h);
   }

   /**
    * Get the smelting recipe of an item by output. Args: output
    * <p>
    *     @return the input itemstack, or empty if no recipe exists
    */
   public static ItemStack getFurnaceInput(ItemStack out) {
//   todo    FurnaceRecipes.smelting().getSmeltingList();
//       for (Object o : m.keySet()) {
//           ItemStack in = (ItemStack) o;
//           if (ReikaItemHelper.matchStacks(FurnaceRecipes.smelting().getSmeltingResult(in), out)) {
//               return in;
//           }
//       }
       return ItemStack.EMPTY;
   }

   /**
    * Adds a smelting recipe. Args; Item in, item out, xp
    */
   public static void addSmelting(ItemStack in, ItemStack out, float xp) {
//    todo    FurnaceRecipes.smelting().func_151394_a(in, out, xp);
   }

   public static boolean replaceIngredientInRecipe(ItemStack ingredient, Object replacement, Recipe<?> ir) {
       return replaceIngredientInRecipe(ingredient, replacement, ir, null);
   }

   public static boolean replaceIngredientInRecipe(ItemStack ingredient, Object replacement, Recipe<?> ir, ReplacementCallback rc) {
       if (ir == null)
           return false;
       boolean flag = false;
       if (ingredient == null)
           throw new MisuseException("You cannot replace null in recipes!");

//        if (replacement instanceof String)
//            replacement = OreDictionary.getOres((String) replacement);

//        ir = getTEWrappedRecipe(ir);

       if (ir instanceof ShapedRecipe) {
           if (!(replacement instanceof ItemStack)) {
               throw new MisuseException("You cannot put non-single-stack entries into a basic recipe type!");
           }
           if (ReikaItemHelper.matchStacks(ingredient, replacement)) //not replacing self with self
               return false;
           ShapedRecipe s = (ShapedRecipe) ir;
           for (int i = 0; i < getRecipeIngredients(s).size(); i++) {
               if (ReikaItemHelper.matchStacks(ingredient, getRecipeIngredients(s).get(i))) {
                   flag = true;
                   if (rc != null)
                       rc.onReplaced(ir, i, getRecipeIngredients(s).get(i), replacement);
                   getRecipeIngredients(s).set(i, (Ingredient) replacement);
               }
           }
       } else if (ir instanceof ShapelessRecipe) {
           if (!(replacement instanceof ItemStack)) {
               throw new MisuseException("You cannot put non-single-stack entries into a basic recipe type!");
           }
           if (ReikaItemHelper.matchStacks(ingredient, replacement)) //not replacing self with self
               return false;
           ShapelessRecipe s = (ShapelessRecipe) ir;
           List<Ingredient> in = getRecipeIngredients(s);
           for (int i = 0; i < in.size(); i++) {
               if (ReikaItemHelper.matchStacks(ingredient, in.get(i))) {
                   flag = true;
                   if (rc != null)
                       rc.onReplaced(ir, i, in.get(i), replacement);
                   in.set(i, (Ingredient) replacement);
               }
           }
        } else if (ir.getClass() == ic2ShapedClass) {
           try {
               Object[] in = (Object[]) shapedIc2Input.get(ir);
               for (int i = 0; i < in.length; i++) {
                   if (in[i] instanceof ItemStack && ReikaItemHelper.matchStacks(ingredient, (ItemStack) in[i])) {
                       flag = true;
                       if (rc != null)
                           rc.onReplaced(ir, i, in[i], replacement);
                       in[i] = replacement;
                   }
                /*   else if (in[i] instanceof IRecipeInput && ((IRecipeInput)in[i]).matches(ingredient)) {
                       flag = true;
                       if (rc != null)
                           rc.onReplaced(ir, i, in[i], replacement);
                       in[i] = replacement;
                   }*/
                   else if (in[i] instanceof Iterable) {
                       boolean repl = false;
                       for (Object o : (Iterable) in[i]) {
                           if (o instanceof ItemStack && ReikaItemHelper.matchStacks(ingredient, (ItemStack) o)) {
                               repl = true;
                               break;
                           }
                           /*else if (o instanceof IRecipeInput && ((IRecipeInput)o).matches(ingredient)) {
                               repl = true;
                               break;
                           }*/
                       }
                       if (repl) {
                           flag = true;
                           if (rc != null)
                               rc.onReplaced(ir, i, in[i], replacement);
                           in[i] = replacement;
                       }
                   }
               }
               Object[] in2 = (Object[]) shapedIc2InputMirror.get(ir);
               if (in2 != null) {
                   for (int i = 0; i < in2.length; i++) {
                       if (in2[i] instanceof ItemStack && ReikaItemHelper.matchStacks(ingredient, (ItemStack) in2[i])) {
                           flag = true;
                           if (rc != null)
                               rc.onReplaced(ir, i, in2[i], replacement);
                           in2[i] = replacement;
                       }
                    /*   else if (in2[i] instanceof IRecipeInput && ((IRecipeInput)in2[i]).matches(ingredient)) {
                           flag = true;
                           if (rc != null)
                               rc.onReplaced(ir, i, in2[i], replacement);
                           in2[i] = replacement;
                       }*/
                       else if (in2[i] instanceof Iterable) {
                           boolean repl = false;
                           for (Object o : (Iterable) in2[i]) {
                               if (o instanceof ItemStack && ReikaItemHelper.matchStacks(ingredient, (ItemStack) o)) {
                                   repl = true;
                                   break;
                               }
                               /*else if (o instanceof IRecipeInput && ((IRecipeInput)o).matches(ingredient)) {
                                   repl = true;
                                   break;
                               }*/
                           }
                           if (repl) {
                               flag = true;
                               if (rc != null)
                                   rc.onReplaced(ir, i, in2[i], replacement);
                               in2[i] = replacement;
                           }
                       }
                   }
               }
           } catch (Exception e) {
               e.printStackTrace();
           }
       } else if (ir.getClass() == ic2ShapelessClass) {
           try {
               Object[] in = (Object[]) shapelessIc2Input.get(ir);
               for (int i = 0; i < in.length; i++) {
                   if (in[i] instanceof ItemStack && ReikaItemHelper.matchStacks(ingredient, (ItemStack) in[i])) {
                       flag = true;
                       if (rc != null)
                           rc.onReplaced(ir, i, in[i], replacement);
                       in[i] = replacement;
                   }
                 /*  else if (in[i] instanceof IRecipeInput && ((IRecipeInput)in[i]).matches(ingredient)) {
                       flag = true;
                       if (rc != null)
                           rc.onReplaced(ir, i, in[i], replacement);
                       in[i] = replacement;
                   }*/
                   else if (in[i] instanceof Iterable) {
                       boolean repl = false;
                       for (Object o : (Iterable) in[i]) {
                           if (o instanceof ItemStack && ReikaItemHelper.matchStacks(ingredient, (ItemStack) o)) {
                               repl = true;
                               break;
                           }
                          /* else if (o instanceof IRecipeInput && ((IRecipeInput)o).matches(ingredient)) {
                               repl = true;
                               break;
                           }*/
                       }
                       if (repl) {
                           flag = true;
                           if (rc != null)
                               rc.onReplaced(ir, i, in[i], replacement);
                           in[i] = replacement;
                       }
                   }
               }
           } catch (Exception e) {
               e.printStackTrace();
           }
       } else if (ir.getClass() == aeShapedClass) {
           try {
               Object[] in = (Object[]) shapedAEInput.get(ir);
               for (int i = 0; i < in.length; i++) {
                   if (in[i] instanceof ItemStack && ReikaItemHelper.matchStacks(ingredient, (ItemStack) in[i])) {
                       if (replacement instanceof ItemStack && ReikaItemHelper.matchStacks(ingredient, replacement))
                           continue;
                       flag = true;
                       if (rc != null)
                           rc.onReplaced(ir, i, in[i], replacement);
                       in[i] = replacement;
                   } else if (in[i] instanceof List && ReikaItemHelper.collectionContainsItemStack((List<ItemStack>) in[i], ingredient)) {
                       flag = ((List) in[i]).size() != 1;
                       if (rc != null)
                           rc.onReplaced(ir, i, in[i], replacement);
                       in[i] = replacement;
                   }
               }
           } catch (Exception e) {
               e.printStackTrace();
           }
       } else if (ir.getClass() == aeShapelessClass) {
           try {
               ArrayList in = (ArrayList) shapelessAEInput.get(ir);
               for (int i = 0; i < in.size(); i++) {
                   if (in.get(i) instanceof ItemStack && ReikaItemHelper.matchStacks(ingredient, (ItemStack) in.get(i))) {
                       if (replacement instanceof ItemStack && ReikaItemHelper.matchStacks(ingredient, replacement))
                           continue;
                       flag = true;
                       if (rc != null)
                           rc.onReplaced(ir, i, in.get(i), replacement);
                       in.set(i, replacement);
                   } else if (in.get(i) instanceof List && ReikaItemHelper.collectionContainsItemStack((List<ItemStack>) in.get(i), ingredient)) {
                       flag = ((List) in.get(i)).size() != 1;
                       if (rc != null)
                           rc.onReplaced(ir, i, in.get(i), replacement);
                       in.set(i, replacement);
                   }
               }
           } catch (Exception e) {
               e.printStackTrace();
           }
       } else if (ir.getClass() == computerTurtleClass) {
           try {
               Item[] in = (Item[]) computerTurtleInput.get(ir);
               for (int i = 0; i < 3; i++) {
                   for (int k = 0; k < 3; k++) {
                       int idx = i * 3 + k;
                       if (in[idx] == ingredient.getItem()) {
                           flag = true;
                           if (rc != null)
                               rc.onReplaced(ir, i, in[i], replacement);
                           in[idx] = ((ItemStack) replacement).getItem();
                       }
                   }
               }
           } catch (Exception e) {
               e.printStackTrace();
           }
       }
       return flag;
   }

   public static ArrayList<ItemStack> getAllItemsInRecipe(Recipe<?> ire) {
       ArrayList<ItemStack> li = new ArrayList<>();
       for (int i = 0; i < getRecipeIngredients(ire).size(); i++) {
           li.add(getRecipeIngredients(ire).get(i).items().map(_h -> new net.minecraft.world.item.ItemStack(_h)).findFirst().orElse(net.minecraft.world.item.ItemStack.EMPTY)); //todo 0 could be i? idfk lmao
       }
       return li;
   }

   public static int getRecipeIngredientCount(Recipe<?> recipe) {
       return getRecipeIngredients(recipe).size();
   }

/*    *//**
    * DISTINCT from getAllItems in that it returns a list of objects, including lists!
    *//*
   public static ArrayList<Object> getAllInputsInRecipe(Recipe<?> ire) {
       ArrayList<Object> li = new ArrayList();
       if (ire instanceof ShapedRecipe) {
           ShapedRecipe r = (ShapedRecipe) ire;
           for (int i = 0; i < getRecipeIngredients(r).size(); i++) {
               li.add(getRecipeIngredients(r).get(i)).orElse(net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.item.Items.AIR));
           }
       } else if (ire instanceof ShapedRecipe) {
           ShapedRecipe so = (ShapedRecipe) ire;
           Object[] objin = getRecipeIngredients(so);
           for (int i = 0; i < objin.length; i++) {
               li.add(objin[i]);
           }
       } else if (ire instanceof ShapelessRecipe) {
           ShapelessRecipe sr = (ShapelessRecipe) ire;
           li.addAll(getRecipeIngredients(sr));
       } else if (ire instanceof ShapelessRecipe) {
           ShapelessRecipe so = (ShapelessRecipe) ire;
           for (int i = 0; i < so.getRecipeSize(); i++) {
               Object obj = getRecipeIngredients(so).get(i);
               li.add(obj);
           }
       }
       return li;
   }*/

   public static Recipe<?> getShapelessRecipeFor(ItemStack out, ItemStack... in) {
       NonNullList<Ingredient> ingredients = NonNullList.of(Ingredient.of(ReikaJavaLibrary.makeListFrom(in).stream().map(net.minecraft.world.item.ItemStack::getItem)));
       return new ShapelessRecipe(new net.minecraft.world.item.crafting.Recipe.CommonInfo(true), new net.minecraft.world.item.crafting.CraftingRecipe.CraftingBookInfo(net.minecraft.world.item.crafting.CraftingBookCategory.MISC, ""), net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(out.copy()), ingredients); //todo nulls
   }

   /*todo   public static boolean matchArrayToRecipe(ItemStack[] in, Recipe<?> ir) {
       RecipePattern r = new RecipePattern(in);
       return ir.matches(r, null);
   }*/

   public static boolean recipeContains(Recipe<?> ir, ItemStack is) {
       return ReikaItemHelper.collectionContainsItemStack(getAllItemsInRecipe(ir), is);
   }

   public static Collection<Integer> getRecipeLocationIndices(Recipe<?> ir, ItemStack is) {
       Collection<Integer> c = new ArrayList();
       RecipeCache r = getRecipeCacheObject(ir, false);
       if (r instanceof UnparsableRecipeCache)
           return c;
       for (int i = 0; i < 9; i++) {
           List<ItemStack> li = r.items[i];
           if (li != null && ReikaItemHelper.collectionContainsItemStack(li, is))
               c.add(i);
       }
       return c;
   }

   public static ItemHashMap<Integer> getItemCountsForDisplay(Recipe<?> ir) {
       ItemHashMap<Integer> map = new ItemHashMap();
       ItemStack[] items = ReikaRecipeHelper.getPermutedRecipeArray(ir);
       if (items == null)
           return map;
       for (int i = 0; i < 9; i++) {
           ItemStack is = items[i];
           if (is != null) {
               Integer num = map.get(is);
               int n = num != null ? num.intValue() : 0;
               map.put(is, n + 1);
           }
       }
       return map;
   }

   public static String toString(Recipe<?> r) {
       if (r == null) {
           return "<NULL>";
       }

//        r = getTEWrappedRecipe(r);

       if (r instanceof ShapedRecipe) {
           return "Shaped " + getRecipeIngredients(r).toString() + " > " + getRecipeOutput(r);
       } else if (r instanceof ShapelessRecipe) {
           return "Shapeless " + getRecipeIngredients(r) + " > " + getRecipeOutput(r);
       } else if (r instanceof ShapedRecipe) {
           return "Shaped Ore " + getRecipeIngredients(r).toString() + " > " + getRecipeOutput(r);
       } else if (r instanceof ShapelessRecipe) {
           return "Shapeless Ore " + getRecipeIngredients(r).toString() + " > " + getRecipeOutput(r);
       }
       else if (r instanceof CustomToStringRecipe) {
           return ((CustomToStringRecipe) r).toDisplayString();
       }
       else {
           return "Unknown '" + r.getClass().getName() + "'" + " > " + getRecipeOutput(r);
       }
   }

   /**
    * Rather slower than toString, so only use this where necessary.
    */
   public static String toDeterministicString(Recipe<?> r) {
       if (r instanceof ShapedRecipe) {
           Ingredient[] arr = Arrays.copyOf(getRecipeIngredients(r).toArray(new Ingredient[0]), getRecipeIngredients(r).size());
           //Arrays.sort(arr, ReikaItemHelper.comparator); DO NOT CHANGE RECIPE ORDER
           return "Shaped " + Arrays.toString(arr) + " > " + getRecipeOutput(r);
       } else if (r instanceof ShapelessRecipe) {
           ArrayList<ItemStack> li = new ArrayList(getRecipeIngredients(r));
           li.sort(ReikaItemHelper.comparator);
           return "Shapeless " + li + " > " + getRecipeOutput(r);
       } else if (r instanceof ShapedRecipe) {
           Object[] arr = Arrays.copyOf(getRecipeIngredients(r).toArray(), getRecipeIngredients(r).size());
           //Arrays.sort(arr, ReikaItemHelper.itemListComparator);
           for (int i = 0; i < arr.length; i++) {
               Object o = arr[i];
               if (o instanceof List) {
                   o = new ArrayList((List) o);
                   Collections.sort((List) o, ReikaItemHelper.comparator);
                   arr[i] = o;
               }
           }
           return "Shaped Ore " + Arrays.toString(arr) + " > " + getRecipeOutput(r);
       } else if (r instanceof ShapelessRecipe) {
           ArrayList<Object> li = new ArrayList(getRecipeIngredients(r));
           Collections.sort(li, ReikaItemHelper.itemListComparator);
           for (int i = 0; i < li.size(); i++) {
               Object o = li.get(i);
               if (o instanceof List) {
                   o = new ArrayList((List) o);
                   Collections.sort((List) o, ReikaItemHelper.comparator);
                   li.set(i, o);
               }
           }
           return "Shapeless Ore " + li.toString() + " > " + getRecipeOutput(r);
       }
//        else if (r instanceof CustomToStringRecipe) {
//            return ((CustomToStringRecipe) r).toDeterministicString();
//        }
       else {
           return "Unknown '" + r.getClass().getName() + "'" + " > " + getRecipeOutput(r);
       }
   }

/*    public static NonNullList<Ingredient> decode1DArray(NonNullList<Ingredient> array, int w, int h) {
       if (array.size() != w * h)
           throw new IllegalArgumentException("Recipe size does not match array length!");
       ArrayList<Object> li = new ArrayList<>();
       char[][] input = new char[h][w];
       for (int i = 0; i < w; i++) {
           for (int k = 0; k < h; k++) {
               int idx = i + k * w;
               Object at = parseIngredient(array.get(idx));
               char c = at == null ? ' ' : (char) ('a' + idx);
               input[k][i] = c;
               if (at != null) {
                   li.add(c);
                   li.add(at);
               }
           }
       }
       ArrayList<String> shape = new ArrayList<>();
       for (char[] line : input) {
           StringBuilder sb = new StringBuilder();
           for (char c : line) {
               sb.append(c);
           }
           shape.add(sb.toString());
       }
       li.addAll(0, shape);
       return li.toArray(new Object[li.size()]); //todo bad cast likely 98.22222% :P
   }*/

   public static Ingredient parseIngredient(Ingredient o) {
       return o;
   }

   private static String getOreNameForCollection(Collection<Ingredient> c) {
//        ItemStack is = c.iterator().next();
       HashSet<String> set = new HashSet<>();//todo ReikaItemHelper.getOreNames(is);
       for (Ingredient is2 : c) {
           set.retainAll(Collections.singleton(is2.toString()));
       }
       if (set.isEmpty())
           throw new IllegalArgumentException("Recipe had a collection ingredient, with no shared ore tags?!");
       return set.iterator().next();
   }

   public static NonNullList<Ingredient> decode2DArray(Object[][] array) {
       String[] input = new String[array.length];
       NonNullList<Ingredient> objects = NonNullList.create();
       NonNullList<Ingredient> entries = NonNullList.create();
       for (int i = 0; i < array.length; i++) {
           StringBuilder sb = new StringBuilder();
           for (int k = 0; k < array[i].length; k++) {
               Ingredient o = Ingredient.of((ItemLike) array[i][k]);
               char c = o == null ? ' ' : (char) ('a' + (i * 3 + k));
               sb.append(c);
               if (o != null) {
//            todo        entries.add(c);
                   entries.add(o);
               }
           }
           input[i] = sb.toString();
       }
//  todo      Collections.addAll(objects, input);
       objects.addAll(entries);

       return objects;
   }

   public static Recipe<?> convertRecipeToOre(Recipe<?> ire) {
//        ire = getTEWrappedRecipe(ire);
       /*if (ire instanceof ShapedRecipe) {
           ShapedRecipe r = (ShapedRecipe) ire;
           return new ShapedRecipe(null, "", ((ShapelessRecipe) ire).category(), ((ShapedRecipe) ire).getWidth(), ((ShapedRecipe) ire).getHeight(), decode1DArray(getRecipeIngredients(r), r.getWidth(), r.getHeight()), getRecipeOutput(ire));
       } else */if (ire instanceof ShapelessRecipe) {
           ShapelessRecipe sr = (ShapelessRecipe) ire;
           List<Ingredient> in = getRecipeIngredients(sr);

           NonNullList<Ingredient> ingredients = NonNullList.create();
           for (int i = 0; i < in.size(); i++) {
               ingredients.set(i, parseIngredient(in.get(i)));
           }
           return new ShapelessRecipe(new net.minecraft.world.item.crafting.Recipe.CommonInfo(true), new net.minecraft.world.item.crafting.CraftingRecipe.CraftingBookInfo(((ShapelessRecipe) ire).category(), ""), net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(getRecipeOutput(ire)), ingredients);
       }
       return ire;
   }

   @Deprecated
   public static Recipe<?> copyRecipe(Recipe<?> ire) {
       try {
//            ire = getTEWrappedRecipe(ire);
          /* if (ire instanceof ShapedRecipe) {
               ShapedRecipe r = (ShapedRecipe) ire;
               return getShapedRecipeFor(getRecipeOutput(ire), decode1DArray(new NonNullList[]{getRecipeIngredients(r)}, r.getWidth(), r.getHeight()));
           } else if (ire instanceof ShapedRecipe) {
               ShapedRecipe so = (ShapedRecipe) ire;
               java.util.List<net.minecraft.world.item.crafting.Ingredient> decoded = decode1DArray(getRecipeIngredients(so), getOreRecipeWidth(so), getOreRecipeHeight(so));
               return new ShapedRecipe(new net.minecraft.world.item.crafting.Recipe.CommonInfo(true), new net.minecraft.world.item.crafting.CraftingRecipe.CraftingBookInfo(((ShapedRecipe) ire).category(), ""), new net.minecraft.world.item.crafting.ShapedRecipePattern(getOreRecipeWidth(so), getOreRecipeHeight(so), decoded.stream().map(i -> i.isEmpty() ? java.util.Optional.<net.minecraft.world.item.crafting.Ingredient>empty() : java.util.Optional.of(i)).toList(), java.util.Optional.empty()), net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(getRecipeOutput(ire))); //todo nulls and 1\'s
           } else */if (ire instanceof ShapelessRecipe) {
               ShapelessRecipe sr = (ShapelessRecipe) ire;
               return new ShapelessRecipe(new net.minecraft.world.item.crafting.Recipe.CommonInfo(true), new net.minecraft.world.item.crafting.CraftingRecipe.CraftingBookInfo(((ShapelessRecipe) ire).category(), ""), net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(getRecipeOutput(ire)), getRecipeIngredients(sr));
           } else if (ire instanceof ShapelessRecipe) {
               ShapelessRecipe sr = (ShapelessRecipe) ire;
               java.util.List<net.minecraft.world.item.crafting.Ingredient> in = getRecipeIngredients(sr);
               NonNullList<Ingredient> ingredients = NonNullList.create();

               ingredients.addAll(getRecipeIngredients(sr));

               for (int i = 0; i < in.size(); i++) {
                   ingredients.set(i, parseIngredient(in.get(i)));
               }
               return new ShapelessRecipe(new net.minecraft.world.item.crafting.Recipe.CommonInfo(true), new net.minecraft.world.item.crafting.CraftingRecipe.CraftingBookInfo(((ShapelessRecipe) ire).category(), ""), net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(getRecipeOutput(ire)), ingredients);
              }
       } catch (Exception e) {
           DragonAPI.LOGGER.error("Could not copy recipe " + toString(ire));
           e.printStackTrace();
       }
       return null;
   }

   public static boolean matchRecipes(Recipe<?> r1, Recipe<?> r2) {
       if (r1 == null && r2 == null)
           return true;
       if (r1 == null || r2 == null)
           return false;
       if (r1.getClass() != r2.getClass())
           return false;
    //    r1 = getTEWrappedRecipe(r1);
//        r2 = getTEWrappedRecipe(r2);
       if (!ItemStack.isSameItem(getRecipeOutput(r1), getRecipeOutput(r2)))
           return false;
       if (r1 instanceof ShapedRecipe) {
           ShapedRecipe sr1 = (ShapedRecipe) r1;
           ShapedRecipe sr2 = (ShapedRecipe) r2;
           return matchIngredientCollections(getRecipeIngredients(sr1), getRecipeIngredients(sr2));
       } else if (r1 instanceof ShapelessRecipe) {
            ShapelessRecipe sr1 = (ShapelessRecipe) r1;
            ShapelessRecipe sr2 = (ShapelessRecipe) r2;
            return matchIngredientCollections(getRecipeIngredients(sr1), getRecipeIngredients(sr2));
       }
       return false;
   }

   private static boolean matchIngredientCollections(java.util.List<net.minecraft.world.item.crafting.Ingredient> input, java.util.List<net.minecraft.world.item.crafting.Ingredient> input2) {
       if (input.size() != input2.size())
           return false;
       for (int i = 0; i < input.size(); i++) {
           Object o1 = input.get(i);
           Object o2 = input2.get(i);
           if (o1 == null && o2 == null)
               continue;
           if (o1 == null || o2 == null)
               return false;
           if (o1.getClass() != o2.getClass())
               return false;
           if (o1 instanceof Ingredient) {
               if (!ReikaItemHelper.matchStacks(Arrays.stream(((net.minecraft.world.item.crafting.Ingredient)o1).items().map(_h -> new net.minecraft.world.item.ItemStack(_h)).toArray(net.minecraft.world.item.ItemStack[]::new)).toList().get(i), Arrays.stream(((net.minecraft.world.item.crafting.Ingredient)o2).items().map(_h -> new net.minecraft.world.item.ItemStack(_h)).toArray(net.minecraft.world.item.ItemStack[]::new)).toList().get(i)))
                   return false;
           } else { //if (o1 instanceof Collection || o1 instanceof String)
               if (!o1.equals(o2))
                   return false;
           }
       }
       return true;
   }

    public static ItemStack getRecipeOutput(Recipe<?> recipe) {
        if (recipe == null) return ItemStack.EMPTY;
        java.util.List<net.minecraft.world.item.crafting.display.RecipeDisplay> displays = recipe.display();
        if (displays != null && !displays.isEmpty()) {
            net.minecraft.world.item.crafting.display.SlotDisplay resultSlot = displays.get(0).result();
            if (resultSlot != null) {
                return resultSlot.resolveForFirstStack(net.minecraft.util.context.ContextMap.EMPTY);
            }
        }
        return ItemStack.EMPTY;
    }


    public static List<Ingredient> getRecipeIngredients(Recipe<?> recipe) {
        if (recipe instanceof ShapedRecipe) {
            return ((ShapedRecipe)recipe).getIngredients().stream()
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .toList();
        }
        return recipe.placementInfo().ingredients();
    }

}
