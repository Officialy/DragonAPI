package reika.dragonapi.modinteract;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import reika.dragonapi.DragonAPI;

import java.lang.reflect.Field;

public class ReikaXPFluidHelper {


    /** Size is mB per units XP */
    private static FluidStack loaded;
    private static Fluid type;
    private static int ratio;

    static {
        addFluid("openblocks.OpenBlocks$Fluids", "xpJuice", "openblocks.Config", "xpToLiquidRatio");
        addFluid("openblocks.OpenBlocks$Fluids", "xpJuice", "openmods.utils.EnchantmentUtils", "RATIO");
        addFluid("crazypants.enderio.EnderIO", "fluidXpJuice", "crazypants.enderio.xp.XpUtil", "RATIO");
        addFluid("mods.immibis.lxp.LiquidXPMod", "defaultFluid", "mods.immibis.lxp.LiquidXPMod", "mbPerXp");
    }

    /** Reflective fluid-based version */
    private static void addFluid(String cf, String sf, String c, String f) {
        try {
            Class<?> cl = Class.forName(c);
            Field fd = cl.getDeclaredField(f);
            fd.setAccessible(true);

            Class<?> clf = Class.forName(cf);
            Field fdf = clf.getDeclaredField(sf);
            fdf.setAccessible(true);

            addFluid((Fluid)fdf.get(null), fd.getInt(null));
        }
        catch (Exception e) {
            DragonAPI.LOGGER.info("Could not load xp fluid type as loaded from "+cf+"#"+sf+": "+e);
            //e.printStackTrace();
        }
    }

    /** Reflective version */
    private static void addFluid(ResourceLocation s, String c, String f) {
        if (loaded != null)
            return;
        try {
            Class cl = Class.forName(c);
            Field fd = cl.getDeclaredField(f);
            fd.setAccessible(true);
            addFluid(s, fd.getInt(null));
        }
        catch (Exception e) {
            DragonAPI.LOGGER.error("Error loading xp fluid type "+s+": "+e);
            //e.printStackTrace();
        }
    }

    private static void addFluid(Fluid f, int amt) {
        if (loaded != null)
            return;
        if (f != null) {
            register(f, amt);
        }
    }

    private static void addFluid(ResourceLocation s, int amt) {
        Fluid f = ForgeRegistries.FLUIDS.getValue(s);
        if (f != null) {
            register(f, amt);
        }
    }

    private static void register(Fluid f, int amt) {
        if (loaded == null) {
            loaded = new FluidStack(f, amt);
            type = f;
            ratio = amt;
            DragonAPI.LOGGER.info("Loaded XP fluid "+f+" with ratio of "+amt+" mB/xp.");
        }
        else {
            DragonAPI.LOGGER.info("Rejected XP fluid "+f+" with ratio of "+amt+" mB/xp; a fluid is already loaded.");
        }
    }

    public static Fluid getFluidType() {
        return type;
    }

    public static FluidStack getFluid() {
        return loaded != null ? loaded.copy() : null;
    }

    public static FluidStack getFluid(int xp) {
        FluidStack fs = getFluid();
        if (fs != null) {
            var amount = fs.getAmount();
            fs.setAmount(amount *= xp); //todo figure out basic java lol, see how to do this without the above var
        }
        return fs;
    }

    public static int getXPForAmount(int fluid) {
        return ratio > 0 ? fluid/ratio : 0;
    }

    public static boolean fluidsExist() {
        return loaded != null;
    }
}
