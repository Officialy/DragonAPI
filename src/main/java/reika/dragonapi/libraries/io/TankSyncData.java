package reika.dragonapi.libraries.io;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import reika.dragonapi.instantiable.HybridTank;

/** Detached, validated representation of the existing protocol 2.1 TANK body. */
public record TankSyncData(String field, BlockPos position, int amount, String fluid) {
    private static final ClassValue<Map<String, Field>> FIELDS = new ClassValue<>() {
        @Override protected Map<String, Field> computeValue(Class<?> type) {
            Map<String, Field> fields = new TreeMap<>();
            var names = new HashSet<String>();
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                for (Field field : current.getDeclaredFields()) {
                    // A shadowing non-tank field also hides the ancestor's name on the wire.
                    if (names.add(field.getName()) && !Modifier.isStatic(field.getModifiers())
                            && HybridTank.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true); fields.put(field.getName(), field);
                    }
                }
            }
            return java.util.Collections.unmodifiableMap(fields);
        }
    };
    public TankSyncData {
        if (field == null || field.isBlank() || field.length() > 32767) throw new IllegalArgumentException("Invalid tank field");
        position = Objects.requireNonNull(position).immutable();
        if (amount < 0 || fluid == null || fluid.length() > 32767 || (amount > 0 && fluid.isBlank()))
            throw new IllegalArgumentException("Invalid tank snapshot");
        if (amount == 0) fluid = "";
    }
    public static List<String> fields(Class<?> type) { return List.copyOf(FIELDS.get(type).keySet()); }
    public static HybridTank tank(Object owner, String name) {
        Field field = FIELDS.get(owner.getClass()).get(name);
        if (field == null) throw new IllegalArgumentException("Not an instance HybridTank field: " + name);
        try {
            Object value = field.get(owner);
            if (value == null) throw new IllegalArgumentException("Null tank field: " + name);
            return (HybridTank)value;
        } catch (IllegalAccessException error) { throw new IllegalStateException("Cannot access tank field: " + name, error); }
    }
    public static TankSyncData capture(BlockEntity tile, String field) {
        HybridTank tank = tank(tile, field);
        FluidStack stack = tank.getFluid();
        Identifier fluid = stack.isEmpty() ? null : BuiltInRegistries.FLUID.getKey(stack.getFluid());
        if (!stack.isEmpty() && fluid == null) throw new IllegalArgumentException("Unregistered tank fluid");
        return new TankSyncData(field, tile.getBlockPos(), stack.getAmount(), fluid == null ? "" : fluid.toString());
    }
    private static void writeString(DataOutputStream output, String value) throws IOException {
        output.writeShort(value.length()); output.writeChars(value);
    }
    public byte[] encode() {
        try {
            var bytes = new ByteArrayOutputStream(); var output = new DataOutputStream(bytes);
            writeString(output, field); output.writeInt(position.getX()); output.writeInt(position.getY()); output.writeInt(position.getZ());
            output.writeInt(amount); writeString(output, fluid); return bytes.toByteArray();
        } catch (IOException error) { throw new IllegalStateException("Cannot encode tank snapshot", error); }
    }
    public static TankSyncData read(DataInputStream input) throws IOException {
        String field = ReikaPacketHelper.readString(input);
        BlockPos pos = new BlockPos(input.readInt(), input.readInt(), input.readInt());
        int amount = input.readInt(); String fluid = ReikaPacketHelper.readString(input);
        if (input.available() != 0) throw new IllegalArgumentException("Trailing tank snapshot bytes");
        return new TankSyncData(field, pos, amount, fluid);
    }
    /** Level-only legacy frames cannot create a fluid; named unknown/empty fluids are rejected before mutation. */
    public static void apply(HybridTank tank, int amount, String fluidName) {
        if (amount < 0) throw new IllegalArgumentException("Negative tank level");
        if (amount == 0) { tank.empty(); return; }
        Fluid fluid;
        if (fluidName == null) throw new IllegalArgumentException("Missing tank fluid");
        if (fluidName.isEmpty()) {
            if (tank.isEmpty()) throw new IllegalArgumentException("Level-only update cannot fill an empty tank");
            fluid = tank.getFluid().getFluid();
        } else {
            Identifier id = Identifier.tryParse(fluidName);
            if (id == null || !BuiltInRegistries.FLUID.containsKey(id)) throw new IllegalArgumentException("Unknown tank fluid: " + fluidName);
            fluid = BuiltInRegistries.FLUID.getValue(id);
            if (fluid == Fluids.EMPTY) throw new IllegalArgumentException("Positive amount of empty fluid");
        }
        int clamped = Math.min(amount, Math.max(0, tank.getCapacity()));
        // A legacy level/id frame must not erase components already supplied by full BE synchronization.
        FluidStack replacement = tank.getFluid().getFluid() == fluid && !tank.isEmpty()
                ? tank.getFluid().copyWithAmount(clamped) : new FluidStack(fluid, clamped);
        tank.empty(); tank.fill(replacement, true);
    }
}
