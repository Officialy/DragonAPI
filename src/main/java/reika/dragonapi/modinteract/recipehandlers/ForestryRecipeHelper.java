///*******************************************************************************
// * @author Reika Kalseki
// *
// * Copyright 2017
// *
// * All rights reserved.
// * Distribution of the software in any form is only allowed with
// * explicit, prior permission from the owner.
// ******************************************************************************/
//package reika.dragonapi.modinteract.RecipeHandlers;
//
//import java.lang.reflect.Field;
//import java.lang.reflect.Method;
//import java.util.Collection;
//import java.util.Map;
//
//import net.minecraft.world.item.ItemStack;
//import reika.dragonapi.DragonAPI;
//import reika.dragonapi.ModList;
//import reika.dragonapi.exception.MisuseException;
//import reika.dragonapi.instantiable.data.collections.ChancedOutputList;
//import reika.dragonapi.instantiable.data.maps.ItemHashMap;
//import reika.dragonapi.libraries.registry.ReikaItemHelper;
//
//public class ForestryRecipeHelper extends ModHandlerBase {
//
//	private static final ForestryRecipeHelper instance = new ForestryRecipeHelper();
//
//	public static final ForestryRecipeHelper getInstance() {
//		return instance;
//	}
//
//	private final ItemHashMap<ChancedOutputList> centrifuge = new ItemHashMap<>();
//	private final ItemHashMap<ISqueezerRecipe> squeezer = new ItemHashMap<>();
//	private final ItemHashMap<IFermenterRecipe> fermenter = new ItemHashMap<>();
//
//	private Field centrifugeOutputs;
//
//	private Method addFermenter;
//	private Enum[] fluids;
//
//	private ForestryRecipeHelper() {
//		super();
//
//		if (this.hasMod()) {
//			if (!Loader.instance.hasReachedState(LoaderState.POSTINITIALIZATION))
//				throw new MisuseException("You cannot load other mod's machine recipes before postload!");
//			try {
//				Collection<ICentrifugeRecipe> c = RecipeManagers.centrifugeManager.recipes();
//				for (ICentrifugeRecipe r : c) {
//					ItemStack in = r.getInput();
//					ChancedOutputList outputs = new ChancedOutputList(false);
//					Map<ItemStack, Float> out = r.getAllProducts();
//					for (ItemStack is : out.keySet()) {
//						float chance = out.get(is)*100;
//						for (int i = 0; i < is.getCount(); i++)
//							outputs.addItem(ReikaItemHelper.getSizedItemStack(is, 1), chance);
//					}
//					centrifuge.put(in, outputs);
//				}
//
//				Class cl = Class.forName("forestry.factory.recipes.CentrifugeRecipe");
//				centrifugeOutputs = cl.getDeclaredField("outputs");
//				centrifugeOutputs.setAccessible(true);
//
//				for (ISqueezerRecipe in : RecipeManagers.squeezerManager.recipes()) {
//					ItemStack[] items = in.getResources();
//					if (items.length == 1 && !FluidContainerRegistry.isFilledContainer(items[0])) {
//						squeezer.put(items[0], in);
//					}
//				}
//
//				for (IFermenterRecipe in : RecipeManagers.fermenterManager.recipes()) {
//					fermenter.put(in.getResource(), in);
//				}
//
//				Class fluids = Class.forName("forestry.core.fluids.Fluids");
//				this.fluids = (Enum[])fluids.getEnumConstants();
//
//				Class util = Class.forName("forestry.core.recipes.RecipeUtil");
//				addFermenter = util.getDeclaredMethod("addFermenterRecipes", ItemStack.class, int.class, fluids);
//			}
//			catch (Exception e) {
//				DragonAPI.LOGGER.error("Could not initialize Forestry recipe helper!");
//				e.printStackTrace();
//			}
//			/*
//			try {
//				String pre = "forestry.factory.gadgets.MachineCentrifuge$";
//				Class centri = Class.forName(pre+"RecipeManager");
//				boolean p6 = SemanticVersionParser.isVersionAtLeast(this.getMod().getVersion(), "3.6");
//				String rec = p6 ? pre+"CentrifugeRecipe" : pre+"Recipe";
//				Class recipe = Class.forName(rec);
//				Field list = centri.getDeclaredField("recipes"); //version safe
//				list.setAccessible(true);
//				Field input = recipe.getDeclaredField(p6 ? "input" : "resource");
//				input.setAccessible(true);
//				Field output = recipe.getDeclaredField(p6 ? "outputs" : "products");
//				output.setAccessible(true);
//				ArrayList li = (ArrayList)list.get(null);
//				for (Object r : li) {
//					ItemStack in = (ItemStack)input.get(r);
//					Map<ItemStack, Number> out = (Map)output.get(r);
//					ChancedOutputList outputs = new ChancedOutputList();
//					for (ItemStack item : out.keySet()) {
//						Number chance = out.get(item);
//						outputs.addItem(item, p6 ? chance.floatValue()*100 : chance.intValue()); //he changed the %/1 thing again T_T
//					}
//					outputs.lock();
//					centrifuge.put(in, outputs);
//				}
//			}
//			catch (ClassNotFoundException e) {
//				ReflectiveFailureTracker.instance.logModReflectiveFailure(ModList.FORESTRY, e);
//				DragonAPI.LOGGER.error(this.getMod()+" class not found! "+e.getMessage());
//				e.printStackTrace();
//			}
//			catch (ClassCastException e) {
//				ReflectiveFailureTracker.instance.logModReflectiveFailure(ModList.FORESTRY, e);
//				DragonAPI.LOGGER.error(this.getMod()+" classcast! "+e.getMessage());
//				e.printStackTrace();
//			}
//			catch (NoSuchFieldException e) {
//				ReflectiveFailureTracker.instance.logModReflectiveFailure(ModList.FORESTRY, e);
//				DragonAPI.LOGGER.error(this.getMod()+" field not found! "+e.getMessage());
//				e.printStackTrace();
//			}
//			catch (SecurityException e) {
//				ReflectiveFailureTracker.instance.logModReflectiveFailure(ModList.FORESTRY, e);
//				DragonAPI.LOGGER.error("Cannot read "+this.getMod()+" (Security Exception)! "+e.getMessage());
//				e.printStackTrace();
//			}
//			catch (IllegalArgumentException e) {
//				ReflectiveFailureTracker.instance.logModReflectiveFailure(ModList.FORESTRY, e);
//				DragonAPI.LOGGER.error("Illegal argument for reading "+this.getMod()+"!");
//				e.printStackTrace();
//			}
//			catch (IllegalAccessException e) {
//				ReflectiveFailureTracker.instance.logModReflectiveFailure(ModList.FORESTRY, e);
//				DragonAPI.LOGGER.error("Illegal access exception for reading "+this.getMod()+"!");
//				e.printStackTrace();
//			}
//			catch (NullPointerException e) {
//				ReflectiveFailureTracker.instance.logModReflectiveFailure(ModList.FORESTRY, e);
//				DragonAPI.LOGGER.error("Null pointer exception for reading "+this.getMod()+"! Was the class loaded?");
//				e.printStackTrace();
//			}
//			 */
//		}
//		else {
//			this.noMod();
//		}
//	}
//
//	public Collection<ItemStack> getCentrifugeRecipes() {
//		return centrifuge.keySet();
//	}
//
//	public Collection<ItemStack> getSqueezerRecipes() {
//		return squeezer.keySet();
//	}
//
//	public Collection<ItemStack> getFermenterRecipes() {
//		return fermenter.keySet();
//	}
//
//	public ChancedOutputList getCentrifugeOutput(ItemStack in) {
//		return centrifuge.get(in).copy();
//	}
//
//	public ISqueezerRecipe getSqueezerOutput(ItemStack in) {
//		return squeezer.get(in);
//	}
//
//	public IFermenterRecipe getFermenterOutput(ItemStack in) {
//		return fermenter.get(in);
//	}
//	/*
//	/** Chances are in percentages! *//*
//	public void addOutputToRecipe(ICentrifugeRecipe ir, ItemStack is, float chance) {
//		try {
//			Map<ItemStack, Float> map = (Map<ItemStack, Float>)centrifugeOutputs.get(ir);
//			if (map.containsKey(is))
//				throw new IllegalArgumentException("Item "+is+" already present in recipe!");
//			map.put(is, chance/100F);
//
//			ChancedOutputList c = centrifuge.get(ir.getInput());
//			c.addItem(is, chance);
//		}
//		catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//	 */
//	@Override
//	public boolean initializedProperly() {
//		return !centrifuge.isEmpty() && !squeezer.isEmpty();
//	}
//
//	@Override
//	public ModList getMod() {
//		return ModList.FORESTRY;
//	}
//
//	public void addStandardFermenterRecipes(ItemStack resource, int fermentationValue) {
//		try {
//			Object f = this.getFluidByName("biomass");
//			addFermenter.invoke(null, resource, fermentationValue, f);
//		}
//		catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//
//	public Object getFluidByName(String s) {
//		for (Enum e : fluids) {
//			if (e.name().equalsIgnoreCase(s))
//				return e;
//		}
//		return null;
//	}
//
//}
