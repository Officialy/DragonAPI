package reika.dragonapi.libraries;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;
import reika.dragonapi.instantiable.HybridTank;

import java.util.HashMap;
import java.util.Locale;

public class ReikaFluidHelper {
    private static final HashMap<String, Integer> fluidColorMap = new HashMap<>();

    static {
        fluidColorMap.put("water", 0x0065FF);
        fluidColorMap.put("lava", 0xFF3300);
        fluidColorMap.put("ender", 0x006470);
        fluidColorMap.put("glowstone", 0xFFE45E);
        fluidColorMap.put("redstone", 0xBC0000);
        fluidColorMap.put("cryotheum", 0x42DFFF);
        fluidColorMap.put("steam", 0xd0d0d0);
        fluidColorMap.put("xp", 0x84FF00);
        fluidColorMap.put("for.honey", 0xFFA300);
        fluidColorMap.put("ic2distilledwater", 0x4F65E2);
        fluidColorMap.put("oil", 0x101010);
        fluidColorMap.put("fuel", 0xC4A300);
        fluidColorMap.put("creosote", 0x963700);
        fluidColorMap.put("biomass", 0x35A536);
    }

    public static int getFluidColor(Fluid f) {
        int c = 555;//todo f.getColor();
        if (c == 0xffffff && fluidColorMap.containsKey(f.toString())) {
            c = fluidColorMap.get(f.toString());
        }
        return c;
    }

    public static boolean isFluidNullOrMatch(FluidStack f, HybridTank tank) {
        return f == null || f == tank.getActualFluid();
    }

    public static boolean isFluidDrainableFromTank(FluidStack f, HybridTank tank) {
        return !tank.isEmpty() && isFluidNullOrMatch(f, tank);
    }

    public static boolean isFlammable(FluidState f) {
        String s = f.toString().toLowerCase(Locale.ENGLISH);
        if (s.contains("fuel"))
            return true;
        if (s.contains("ethanol"))
            return true;
        if (s.contains("oil"))
            return true;
        if (s.equals("creosote"))
            return true;
        if (s.contains("gas"))
            return true;
        if (s.endsWith("ane") || s.endsWith("ene") || s.endsWith("yne")) //Hydrocarbons
            return true;
        //Other organics
        return s.endsWith("ol") || s.endsWith("al") || s.endsWith("one");
    }

    public static Fluid lookupFluidForBlock(BlockState b) {
        if (b == Blocks.LAVA.defaultBlockState() || b == Fluids.FLOWING_LAVA.getFlowing().defaultFluidState().createLegacyBlock())
            return Fluids.LAVA;
        if (b == Blocks.WATER.defaultBlockState() || b == Fluids.FLOWING_WATER.getFlowing().defaultFluidState().createLegacyBlock())
            return Fluids.WATER;
        Fluid f = b.getBlock().getFluidState(b).getType();//todo FluidRegistry.lookupFluidForBlock(b);
        if (f == null && b instanceof IFluidBlock)
            f = ((IFluidBlock) b).getFluid();
        return f;
    }

}
