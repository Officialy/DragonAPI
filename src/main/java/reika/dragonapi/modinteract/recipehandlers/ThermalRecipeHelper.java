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
//import net.minecraft.nbt.CompoundTag;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.level.material.Fluid;
//import net.minecraftforge.common.MinecraftForge;
//import net.minecraftforge.fluids.FluidStack;
//import net.minecraftforge.fml.InterModComms;
//
//public class ThermalRecipeHelper {
//
//	public static void addMagmaticFuel(Fluid f, int energy) {
//		CompoundTag toSend = new CompoundTag();
//		toSend.putString("fluidName", f.toString());
//		toSend.putInt("energy", energy);
//		InterModComms.sendTo("ThermalExpansion", "MagmaticFuel", () -> toSend);
//	}
//
//	public static void addCompressionFuel(Fluid f, int energy) {
//		CompoundTag toSend = new CompoundTag();
//		toSend.putString("fluidName", f.toString());
//		toSend.putInt("energy", energy);
//		InterModComms.sendTo("ThermalExpansion", "CompressionFuel", () -> toSend);
//	}
//
//	public static void addCoolant(Fluid f, int energy) {
//		CompoundTag toSend = new CompoundTag();
//		toSend.putString("fluidName", f.toString());
//		toSend.putInt("energy", energy);
//		InterModComms.sendTo("ThermalExpansion", "Coolant", () -> toSend);
//	}
//
//	public static void addFluidTransposerFill(ItemStack in, ItemStack out, int energy, FluidStack f) {
//		addFluidTransposerFill(in, out, energy, f, false);
//	}
//
//	public static void addFluidTransposerFill(ItemStack in, ItemStack out, int energy, FluidStack f, boolean reversible) {
//		CompoundTag toSend = new CompoundTag();
//		toSend.putInt("energy", energy);
//		toSend.put("input", new CompoundTag());
//		toSend.put("output", new CompoundTag());
//		toSend.put("fluid", new CompoundTag());
//
//		in.save(toSend.getCompound("input"));
//		out.save(toSend.getCompound("output"));
//		toSend.putBoolean("reversible", reversible);
//		f.save(toSend.getCompound("fluid"));
//		InterModComms.sendTo("ThermalExpansion", "TransposerFillRecipe", () -> toSend);
//		fireEvent(ThermalRecipeEvent.ThermalMachine.TRANSPOSER, in, null, out, null, 0, energy);
//	}
//
//	private static void fireEvent(ThermalMachine type, ItemStack in, FluidStack out, int rf) {
//		MinecraftForge.EVENT_BUS.post(new ThermalRecipeEvent(type, in, out, rf));
//	}
//
//	private static void fireEvent(ThermalMachine type, ItemStack in1, ItemStack in2, ItemStack out1, ItemStack out2, int out2chance, int rf) {
//		MinecraftForge.EVENT_BUS.post(new ThermalRecipeEvent(type, in1, in2, out1, out2, out2chance, rf));
//	}
//
//	public static void addFluidTransposerDrain(ItemStack in, ItemStack out, int energy, FluidStack f) {
//		addFluidTransposerDrain(in, out, energy, f, 100);
//	}
//
//	public static void addFluidTransposerDrain(ItemStack in, ItemStack out, int energy, FluidStack f, boolean reversible) {
//		addFluidTransposerDrain(in, out, energy, f, 100, reversible);
//	}
//
//	public static void addFluidTransposerDrain(ItemStack in, ItemStack out, int energy, FluidStack f, int chance) {
//		addFluidTransposerDrain(in, out, energy, f, chance, false);
//	}
//
//	public static void addFluidTransposerDrain(ItemStack in, ItemStack out, int energy, FluidStack f, int chance, boolean reversible) {
//		CompoundTag toSend = new CompoundTag();
//		toSend.putInt("energy", energy);
//		toSend.put("input", new CompoundTag());
//		toSend.put("output", new CompoundTag());
//		toSend.put("fluid", new CompoundTag());
//
//		in.save(toSend.getCompound("input"));
//		out.save(toSend.getCompound("output"));
//		toSend.putBoolean("reversible", reversible);
//		toSend.putInt("chance", chance);
//		f.save(toSend.getCompound("fluid"));
//		InterModComms.sendTo("ThermalExpansion", "TransposerExtractRecipe", toSend);
//		fireEvent(ThermalRecipeEvent.ThermalMachine.TRANSPOSER, in, null, out, null, 0, energy);
//	}
//
//	public static void addInductionSmelter(ItemStack in1, ItemStack in2, ItemStack out1, int energy) {
//		addInductionSmelter(in1, in2, out1, null, energy);
//	}
//
//	public static void addInductionSmelter(ItemStack in1, ItemStack in2, ItemStack out1, ItemStack out2, int energy) {
//		addInductionSmelter(in1, in2, out1, out2, 100, energy);
//	}
//
//	public static void addInductionSmelter(ItemStack in1, ItemStack in2, ItemStack out1, ItemStack out2, int out2chance, int energy) {
//		addTwoInTwoOutWithChance("SmelterRecipe", in1, in2, out1, out2, out2chance, energy);
//	}
//
//	public static void addPulverizerRecipe(ItemStack in, ItemStack out1, ItemStack out2, int energy) {
//		addPulverizerRecipe(in, out1, out2, 100, energy);
//	}
//
//	public static void addPulverizerRecipe(ItemStack in, ItemStack out, int energy) {
//		addPulverizerRecipe(in, out, null, energy);
//	}
//
//	public static void addPulverizerRecipe(ItemStack in, ItemStack out1, ItemStack out2, int out2chance, int energy) {
//		addOneInTwoOutWithChance("PulverizerRecipe", in, out1, out2, out2chance, energy);
//	}
//
//	public static void addSawmillRecipe(ItemStack in, ItemStack out1, ItemStack out2, int energy) {
//		addSawmillRecipe(in, out1, out2, 100, energy);
//	}
//
//	public static void addSawmillRecipe(ItemStack in, ItemStack out, int energy) {
//		addSawmillRecipe(in, out, null, energy);
//	}
//
//	public static void addSawmillRecipe(ItemStack in, ItemStack out1, ItemStack out2, int out2chance, int energy) {
//		addOneInTwoOutWithChance("SawmillRecipe", in, out1, out2, out2chance, energy);
//	}
//
//	public static void addCrucibleRecipe(ItemStack in, FluidStack f, int energy) {
//		CompoundTag toSend = new CompoundTag();
//		toSend.putInt("energy", energy);
//		toSend.put("input", new CompoundTag());
//		toSend.put("output", new CompoundTag());
//
//		in.save(toSend.getCompound("input"));
//		f.save(toSend.getCompound("output"));
//		InterModComms.sendTo("ThermalExpansion", "CrucibleRecipe", toSend);
//		fireEvent(ThermalRecipeEvent.ThermalMachine.CRUCIBLE, in, f, energy);
//	}
//
//	private static void addTwoInTwoOutWithChance(String type, ItemStack in1, ItemStack in2, ItemStack out1, ItemStack out2, int out2chance, int energy) {
//		CompoundTag toSend = new CompoundTag();
//		toSend.putInt("energy", energy);
//		toSend.put("primaryInput", new CompoundTag());
//		toSend.put("secondaryInput", new CompoundTag());
//		toSend.put("primaryOutput", new CompoundTag());
//		if (out2 != null)
//			toSend.put("secondaryOutput", new CompoundTag());
//
//		in1.save(toSend.getCompound("primaryInput"));
//		in2.save(toSend.getCompound("secondaryInput"));
//		out1.save(toSend.getCompound("primaryOutput"));
//		if (out2 != null)
//			out2.save(toSend.getCompound("secondaryOutput"));
//		if (out2chance < 100)
//			toSend.putInt("secondaryChance", out2chance);
//		InterModComms.sendTo("ThermalExpansion", type, () -> toSend);
//		fireEvent(ThermalRecipeEvent.ThermalMachine.getType(type), in1, in2, out1, out2, out2chance, energy);
//	}
//
//	private static void addOneInTwoOutWithChance(String type, ItemStack in, ItemStack out1, ItemStack out2, int out2chance, int energy) {
//		CompoundTag toSend = new CompoundTag();
//		toSend.putInt("energy", energy);
//		toSend.put("input", new CompoundTag());
//		toSend.put("primaryOutput", new CompoundTag());
//		if (out2 != null)
//			toSend.put("secondaryOutput", new CompoundTag());
//
//		in.save(toSend.getCompound("input"));
//		out1.save(toSend.getCompound("primaryOutput"));
//		if (out2 != null)
//			out2.save(toSend.getCompound("secondaryOutput"));
//		if (out2chance < 100)
//			toSend.putInt("secondaryChance", out2chance);
//		InterModComms.sendTo("ThermalExpansion", type, () -> toSend);
//		fireEvent(ThermalRecipeEvent.ThermalMachine.getType(type), in, null, out1, out2, out2chance, energy);
//	}
//
//}
