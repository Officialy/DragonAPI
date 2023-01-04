package reika.dragonapi.instantiable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.registries.ForgeRegistries;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;

import java.util.HashMap;

/**
 * A tank class that can handle direct operations as well as standard Forge Liquid operations.
 */
public class HybridTank extends FluidTank {

    private static final HashMap<String, String> nameSwaps = new HashMap<>();
    protected final String name;

    public HybridTank(String name, int capacity) {
        super(capacity);
        this.name = name;
    }

    public HybridTank(String name, FluidStack stack, int capacity) {
        this(name, capacity);
        this.setFluid(stack);
    }

    public HybridTank(String name, Fluid fluid, int amount, int capacity) {
        this(name, new FluidStack(fluid, amount), capacity);
    }

    public static String getFluidNameSwap(String oldName) {
        //if (FluidRegistry.isFluidRegistered(oldName)) //to avoid accidental unification
        //    return oldName;
        return nameSwaps.get(oldName);
    }

    @Override
    public final FluidTank readFromNBT(CompoundTag NBT) {
        try {
            if (NBT.contains(name)) {
                CompoundTag tankData = NBT.getCompound(name);
                String fluidName = tankData.getString("FluidName");
                String repl = getFluidNameSwap(fluidName);
                if (repl != null && ForgeRegistries.FLUIDS.getValue(new ResourceLocation(repl)) != null && !fluidName.equals(repl)) {
                    tankData.putString("FluidName", repl);
                    DragonAPI.LOGGER.info("Tank " + this + " has replaced its FluidName of '" + fluidName + "' with '" + repl + "', as the fluid has changed names.");
                }
                super.readFromNBT(tankData);
            }
        } catch (IllegalArgumentException e) { //"Empty String not allowed!" caused by fluid save failure
            DragonAPI.LOGGER.error("Loading HybridTank '" + name + "' has errored, its machine will not keep its fluid!");
            e.printStackTrace();
        }
        return this;
    }

    @Override
    public final CompoundTag writeToNBT(CompoundTag NBT) {
        CompoundTag tankData = new CompoundTag();
        super.writeToNBT(tankData);

        String fluidName = tankData.getString("FluidName");
        String repl = getFluidNameSwap(fluidName);
        if (repl != null && ForgeRegistries.FLUIDS.getValue(new ResourceLocation(repl)) != null && !fluidName.equals(repl)) {
            tankData.putString("FluidName", repl);
            DragonAPI.LOGGER.info("Tank " + this + " has replaced its FluidName of '" + fluidName + "' with '" + repl + "', as the fluid has changed names.");
        }

        NBT.put(name, tankData);

        return NBT;
    }

    public boolean isEmpty() {
        return this.getFluid() == null || this.getFluid().getAmount() <= 0;
    }

    public boolean isFull() {
        return this.getFluid() != null && this.getFluid().getAmount() >= this.getCapacity();
    }

    public int getLevel() {
        if (this.getFluid() == null)
            return 0;
        return this.getFluid().getAmount();
    }

    public void removeLiquid(float amt) {
        this.removeLiquid((int) amt);
    }

    public void removeLiquid(int amt) {
        if (this.getFluid() == null) {
            DragonAPI.LOGGER.error("Could not remove liquid from empty tank!");
            ReikaJavaLibrary.dumpStack();
        } else if (amt <= 0) {
            DragonAPI.LOGGER.error("Cannot remove <= 0!");
            ReikaJavaLibrary.dumpStack();
        } else {
            this.drain(amt, FluidAction.EXECUTE);
        }
    }

    public void addLiquid(int amt, Fluid type) {
        if (type == null){
            DragonAPI.LOGGER.info("Cannot add null fluid!");
            return;
        }
        if (amt > capacity) {
            amt = capacity;
        }
        if (this.getFluid().isEmpty()) {
//            DragonAPI.LOGGER.info("Adding liquid to tank "+this+" of type "+type+" and amount "+amt);
            this.fill(new FluidStack(type, amt), FluidAction.EXECUTE);
        } else if (type.equals(this.getFluid().getFluid())) {
//            DragonAPI.LOGGER.info("Adding liquid to tank "+this+" of type "+type+" and amount "+amt);
            this.fill(new FluidStack(this.getFluid().getFluid(), amt), FluidAction.EXECUTE);
        } else {
            DragonAPI.LOGGER.info("Cannot add liquid of type "+type+" to tank "+this+" of type "+this.getFluid().getFluid());
        }
    }

    public void empty() {
        this.drain(this.getLevel(), FluidAction.EXECUTE);
    }

    public void setFluidType(Fluid type) {
        int amt = this.getLevel();
        this.drain(amt, FluidAction.EXECUTE);
        this.fill(new FluidStack(type, amt), FluidAction.EXECUTE);
    }

    public void setContents(int amt, Fluid f) {
        this.empty();
        this.addLiquid(amt, f);
    }

    public FluidStack getActualFluid() {
        if (this.getFluid() == null)
            return null;
        return this.getFluid();
    }

    public float getFraction() {
        return this.getLevel() / (float) this.getCapacity();
    }

    @Override
    public String toString() {
        if (this.isEmpty())
            return "Empty Tank " + name;
        return "Tank " + name + ", containing " + this.getLevel() + " mB of " + this.getActualFluid().getFluid();
    }

    public int getRemainingSpace() {
        return capacity - this.getLevel();
    }

    public boolean canTakeIn(int amt) {
        return this.getRemainingSpace() >= amt;
    }

    public boolean canTakeIn(Fluid f, int amt) {
        if (this.isEmpty()) {
            return capacity >= amt;
        } else {
            return this.getRemainingSpace() >= amt && this.getActualFluid().getFluid().equals(f);
        }
    }

    public boolean canTakeIn(FluidStack fs) {
        int amt = fs.getAmount();
        return this.isEmpty() ? capacity >= amt : this.getRemainingSpace() >= amt && this.getActualFluid().getFluid().equals(fs.getFluid());
    }

    public void setNBT(CompoundTag nbt) {
        if (this.getFluid() != null)
            this.getFluid().writeToNBT(nbt);
    }

    public void setNBTInt(String key, int val) {
        if (this.getFluid() != null) {
            if (this.getFluid().getTag() == null)
                this.getFluid().writeToNBT(new CompoundTag());
            this.getFluid().getTag().putInt(key, val);
        }
    }

    public void setNBTString(String key, String s) {
        if (this.getFluid() != null) {
            if (this.getFluid().getTag() == null)
                this.getFluid().writeToNBT(new CompoundTag());
            this.getFluid().getTag().putString(key, s);
        }
    }

    public void setNBTBoolean(String key, boolean b) {
        if (this.getFluid() != null) {
            if (this.getFluid().getTag() == null)
                this.getFluid().writeToNBT(new CompoundTag());
            this.getFluid().getTag().putBoolean(key, b);
        }
    }

    public int getNBTInt(String key) {
        return this.getFluid() != null && this.getFluid().getTag() != null ? this.getFluid().getTag().getInt(key) : 0;
    }

    public String getNBTString(String key) {
        return this.getFluid() != null && this.getFluid().getTag() != null ? this.getFluid().getTag().getString(key) : "";
    }

    public boolean getNBTBoolean(String key) {
        return this.getFluid() != null && this.getFluid().getTag() != null && this.getFluid().getTag().getBoolean(key);
    }
}
