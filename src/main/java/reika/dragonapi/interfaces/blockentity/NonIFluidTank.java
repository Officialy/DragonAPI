package reika.dragonapi.interfaces.blockentity;

import net.minecraft.world.level.material.Fluid;

public interface NonIFluidTank {

    boolean allowAutomation();

    int addFluid(Fluid fluid, int amount, boolean doFill);

}
