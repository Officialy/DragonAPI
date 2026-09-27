package reika.dragonapi.instantiable;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.libraries.io.NBTCompat;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;

import java.util.HashMap;
import java.util.function.Predicate;

/**
 * A tank class that handles direct machine operations. Automation reaches it through
 * {@link reika.dragonapi.instantiable.storage.HybridTankResourceHandler}, which journals these
 * tanks for NeoForge transactions; this class itself is plain storage.
 *
 * <p>The direct {@code fill}/{@code drain} methods keep the 1.7.10 Forge {@code FluidTank}
 * contract ({@code doFill}/{@code doDrain} booleans) that Reika's machine code was written
 * against. Their bodies are NeoForge's former {@code FluidTank} logic, so component-aware
 * matching and {@link #onContentsChanged()} timing are unchanged.
 */
public class HybridTank {

    private static final HashMap<String, String> nameSwaps = new HashMap<>();
    protected final String name;
    protected Predicate<FluidStack> validator = fs -> true;
    protected FluidStack fluid = FluidStack.EMPTY;
    protected int capacity;

    public HybridTank(String name, int capacity) {
        this.capacity = capacity;
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

    public HybridTank setCapacity(int capacity) {
        this.capacity = capacity;
        return this;
    }

    public HybridTank setValidator(Predicate<FluidStack> validator) {
        if (validator != null) {
            this.validator = validator;
        }
        return this;
    }

    public boolean isFluidValid(FluidStack stack) {
        return validator.test(stack);
    }

    public int getCapacity() {
        return capacity;
    }

    /** The live stored stack. Mutating it bypasses {@link #onContentsChanged()} and any open
     *  transaction journal; prefer the tank's own operations. */
    public FluidStack getFluid() {
        return fluid;
    }

    public int getFluidAmount() {
        return fluid.getAmount();
    }

    public void setFluid(FluidStack stack) {
        this.fluid = stack;
    }

    public int getSpace() {
        return Math.max(0, capacity - fluid.getAmount());
    }

    /** Called after a {@code doFill}/{@code doDrain} operation actually changed the contents. */
    protected void onContentsChanged() {}

    /**
     * Fills this tank directly.
     *
     * @return the amount accepted (or that would be accepted, if {@code doFill} is false)
     */
    public int fill(FluidStack resource, boolean doFill) {
        if (resource.isEmpty() || !this.isFluidValid(resource)) {
            return 0;
        }
        if (!doFill) {
            if (fluid.isEmpty()) {
                return Math.min(capacity, resource.getAmount());
            }
            if (!FluidStack.isSameFluidSameComponents(fluid, resource)) {
                return 0;
            }
            return Math.min(capacity - fluid.getAmount(), resource.getAmount());
        }
        if (fluid.isEmpty()) {
            fluid = resource.copyWithAmount(Math.min(capacity, resource.getAmount()));
            this.onContentsChanged();
            return fluid.getAmount();
        }
        if (!FluidStack.isSameFluidSameComponents(fluid, resource)) {
            return 0;
        }
        int filled = capacity - fluid.getAmount();

        if (resource.getAmount() < filled) {
            fluid.grow(resource.getAmount());
            filled = resource.getAmount();
        } else {
            fluid.setAmount(capacity);
        }
        if (filled > 0)
            this.onContentsChanged();
        return filled;
    }

    /** Drains the given fluid (type and components must match) directly. */
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (resource.isEmpty() || !FluidStack.isSameFluidSameComponents(resource, fluid)) {
            return FluidStack.EMPTY;
        }
        return this.drain(resource.getAmount(), doDrain);
    }

    /** Drains up to {@code maxDrain} of whatever this tank holds, directly. */
    public FluidStack drain(int maxDrain, boolean doDrain) {
        int drained = maxDrain;
        if (fluid.getAmount() < drained) {
            drained = fluid.getAmount();
        }
        FluidStack stack = fluid.copyWithAmount(drained);
        if (doDrain && drained > 0) {
            fluid.shrink(drained);
            this.onContentsChanged();
        }
        return stack;
    }

    public final HybridTank readFromNBT(HolderLookup.Provider provider, CompoundTag NBT) {
        try {
            if (NBT.contains(name)) {
                CompoundTag tankData = NBTCompat.getCompound(NBT, name);
                String fluidName = NBTCompat.getString(tankData, "FluidName", "");
                String repl = getFluidNameSwap(fluidName);
                if (repl != null && BuiltInRegistries.FLUID.getValue(Identifier.parse(repl)) != null && !fluidName.equals(repl)) {
                    tankData.putString("FluidName", repl);
                    DragonAPI.LOGGER.info("Tank " + this + " has replaced its FluidName of '" + fluidName + "' with '" + repl + "', as the fluid has changed names.");
                }
                FluidStack fluid = FluidStack.OPTIONAL_CODEC.parse(provider != null ? provider.createSerializationContext(NbtOps.INSTANCE) : NbtOps.INSTANCE, tankData).result().orElse(FluidStack.EMPTY);
                this.setFluid(fluid);
            }
        } catch (IllegalArgumentException e) { //"Empty String not allowed!" caused by fluid save failure
            DragonAPI.LOGGER.error("Loading HybridTank '" + name + "' has errored, its machine will not keep its fluid!");
            e.printStackTrace();
        }
        return this;
    }

    public final CompoundTag writeToNBT(HolderLookup.Provider provider, CompoundTag NBT) {
        CompoundTag tankData = new CompoundTag();
                Tag serialized = FluidStack.OPTIONAL_CODEC.encodeStart(provider != null ? provider.createSerializationContext(NbtOps.INSTANCE) : NbtOps.INSTANCE, this.getFluid()).getOrThrow();
        if (serialized instanceof CompoundTag c) {
            tankData.merge(c);
        }

        String fluidName = NBTCompat.getString(tankData, "FluidName", "");
        String repl = getFluidNameSwap(fluidName);
        if (repl != null && BuiltInRegistries.FLUID.getValue(Identifier.parse(repl)) != null && !fluidName.equals(repl)) {
            tankData.putString("FluidName", repl);
            DragonAPI.LOGGER.info("Tank " + this + " has replaced its FluidName of '" + fluidName + "' with '" + repl + "', as the fluid has changed names.");
        }

        NBT.put(name, tankData);

        return NBT;
    }

    /** Convenience overloads for the many BlockEntity call sites that don't have a HolderLookup.Provider
     *  handy; the provider-aware versions already tolerate a null provider (plain NbtOps). */
    public final HybridTank readFromNBT(CompoundTag NBT) {
        return this.readFromNBT(null, NBT);
    }

    public final CompoundTag writeToNBT(CompoundTag NBT) {
        return this.writeToNBT(null, NBT);
    }

    public boolean isEmpty() {
        return this.getFluid() == null || this.getFluid().getAmount() <= 0;
    }

    public boolean isFull() {
        return this.getFluid() != null && this.getFluid().getAmount() >= this.getCapacity();
    }

    public int getFluidLevel() {
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
            this.drain(amt, true);
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
            this.fill(new FluidStack(type, amt), true);
        } else if (type.equals(this.getFluid().getFluid())) {
//            DragonAPI.LOGGER.info("Adding liquid to tank "+this+" of type "+type+" and amount "+amt);
            this.fill(new FluidStack(this.getFluid().getFluid(), amt), true);
        } else {
            DragonAPI.LOGGER.info("Cannot add liquid of type "+type+" to tank "+this+" of type "+this.getFluid().getFluid());
        }
    }

    public void empty() {
        this.drain(this.getFluidLevel(), true);
    }

    public void setFluidType(Fluid type) {
        int amt = this.getFluidLevel();
        this.drain(amt, true);
        this.fill(new FluidStack(type, amt), true);
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
        return this.getFluidLevel() / (float) this.getCapacity();
    }

    @Override
    public String toString() {
        if (this.isEmpty())
            return "Empty Tank " + name;
        return "Tank " + name + ", containing " + this.getFluidLevel() + " mB of " + this.getActualFluid().getFluid();
    }

    public int getRemainingSpace() {
        return capacity - this.getFluidLevel();
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
        if (!this.isEmpty())
            this.getFluid().set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
    }

    public void setNBTInt(String key, int val) {
        if (!this.isEmpty()) {
            CustomData data = this.getFluid().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            this.getFluid().set(DataComponents.CUSTOM_DATA, data.update(nbt -> nbt.putInt(key, val)));
        }
    }

    public void setNBTString(String key, String s) {
        if (!this.isEmpty()) {
            CustomData data = this.getFluid().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            this.getFluid().set(DataComponents.CUSTOM_DATA, data.update(nbt -> nbt.putString(key, s)));
        }
    }

    public void setNBTBoolean(String key, boolean b) {
        if (!this.isEmpty()) {
            CustomData data = this.getFluid().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            this.getFluid().set(DataComponents.CUSTOM_DATA, data.update(nbt -> nbt.putBoolean(key, b)));
        }
    }

    public int getNBTInt(String key) {
        if (this.isEmpty()) return 0;
        CustomData data = this.getFluid().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag().getIntOr(key, 0);
    }

    public String getNBTString(String key) {
        if (this.isEmpty()) return "";
        CustomData data = this.getFluid().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag().getStringOr(key, "");
    }

    public boolean getNBTBoolean(String key) {
        if (this.isEmpty()) return false;
        CustomData data = this.getFluid().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag().getBooleanOr(key, false);
    }
}

