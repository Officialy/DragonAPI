package reika.dragonapi.modinteract.deepinteract;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AECraftingPattern;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;

/**
 * 1.7.10 {@code AEPatternHandling}: read and write ME crafting patterns.
 *
 * <p>1.7.10 patterns were one item whose NBT said crafting-or-processing; modern AE2 has a crafting pattern
 * ({@link AECraftingPattern}, bound to a real crafting recipe) and a processing pattern. "Is crafting recipe" is now
 * "decodes to an {@code AECraftingPattern}", and encoding a crafting pattern needs the recipe the inputs make, so it
 * is looked up in the level's recipe manager.
 */
public class AEPatternHandling {

	/** 1.7.10 {@code InterfaceCache.AEPATTERN.instanceOf(item)}. */
	public static boolean isPattern(ItemStack is) {
		return is != null && !is.isEmpty() && PatternDetailsHelper.isEncodedPattern(is);
	}

	public static boolean isCraftingRecipe(ItemStack is, Level world) {
		IPatternDetails icpd = PatternDetailsHelper.decodePattern(is, world);
		return icpd instanceof AECraftingPattern;
	}

	public static ItemStack[] getPatternInput(ItemStack is, Level world) {
		IPatternDetails icpd = PatternDetailsHelper.decodePattern(is, world);
		ItemStack[] ret = new ItemStack[9];
		if (icpd instanceof AECraftingPattern acp) {
			List<GenericStack> in = acp.getSparseInputs();
			for (int i = 0; i < ret.length && i < in.size(); i++) {
				GenericStack gs = in.get(i);
				if (gs != null && gs.what() instanceof AEItemKey key)
					ret[i] = key.toStack((int)Math.max(1, gs.amount()));
			}
		}
		else if (icpd != null) {
			IPatternDetails.IInput[] in = icpd.getInputs();
			for (int i = 0; i < ret.length && i < in.length; i++) {
				GenericStack[] opts = in[i].getPossibleInputs();
				if (opts.length > 0 && opts[0].what() instanceof AEItemKey key)
					ret[i] = key.toStack((int)Math.max(1, opts[0].amount() * in[i].getMultiplier()));
			}
		}
		return ret;
	}

	public static ArrayList<ItemStack> getPatternOutputs(ItemStack is, Level world) {
		IPatternDetails icpd = PatternDetailsHelper.decodePattern(is, world);
		ArrayList<ItemStack> li = new ArrayList<>();
		if (icpd == null)
			return li;
		for (GenericStack iae : icpd.getOutputs()) {
			if (iae.what() instanceof AEItemKey key)
				li.add(key.toStack((int)Math.min(Integer.MAX_VALUE, iae.amount())));
		}
		return li;
	}

	@Nullable
	public static ItemStack getEncodedPattern(Level world, ItemStack out, boolean isCrafting, boolean allowOreSubs, ItemStack... in) {
		return getEncodedPattern(world, ReikaJavaLibrary.makeListFrom(out), isCrafting, allowOreSubs, in);
	}

	/**
	 * Returns null for a crafting pattern whose inputs match no crafting recipe (AE2 can no longer encode such a
	 * pattern; the 1.7.10 one would have been an unusable pattern).
	 */
	@Nullable
	public static ItemStack getEncodedPattern(Level world, Collection<ItemStack> out, boolean isCrafting, boolean allowOreSubs, ItemStack... in) {
		if (isCrafting) {
			ItemStack[] grid = new ItemStack[9];
			for (int i = 0; i < 9; i++)
				grid[i] = i < in.length && in[i] != null ? in[i].copyWithCount(1) : ItemStack.EMPTY;
			ItemStack result = out.isEmpty() ? ItemStack.EMPTY : out.iterator().next();
			if (!(world instanceof ServerLevel sl))
				return null;
			CraftingInput ci = CraftingInput.of(3, 3, List.of(grid));
			Optional<RecipeHolder<CraftingRecipe>> recipe = sl.recipeAccess().getRecipeFor(RecipeType.CRAFTING, ci, sl);
			if (recipe.isEmpty())
				return null;
			if (result.isEmpty())
				result = recipe.get().value().assemble(ci);
			return PatternDetailsHelper.encodeCraftingPattern(recipe.get(), grid, result, allowOreSubs, false);
		}
		List<GenericStack> gin = new ArrayList<>();
		for (ItemStack i : in)
			gin.add(i != null && !i.isEmpty() ? GenericStack.fromItemStack(i) : null);
		List<GenericStack> gout = new ArrayList<>();
		for (ItemStack i : out)
			gout.add(i != null && !i.isEmpty() ? GenericStack.fromItemStack(i) : null);
		return PatternDetailsHelper.encodeProcessingPattern(gin, gout);
	}

}
