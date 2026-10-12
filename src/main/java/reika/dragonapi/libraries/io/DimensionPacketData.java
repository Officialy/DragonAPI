package reika.dragonapi.libraries.io;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import reika.dragonapi.APIPacketHandler.PacketIDs;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.auxiliary.PacketTypes;
import reika.dragonapi.instantiable.io.PacketTarget;

/** Protocol 2.2 RAW body: control, dimension key, name, then the fixed integer fields. */
public record DimensionPacketData(PacketIDs control, ResourceKey<Level> dimension, String name, int[] values) {
    public DimensionPacketData {
        Objects.requireNonNull(dimension);
        Objects.requireNonNull(name);
        values = values.clone();
        if (values.length != fieldCount(control)) throw new IllegalArgumentException("Wrong dimension packet field count");
        if (control == PacketIDs.BIOMECHANGE) parseIdentifier(name);
    }

    @Override public int[] values() { return values.clone(); }

    public static boolean supports(PacketIDs control) {
        return switch (control) {
            case ENTITYVERIFY, ENTITYVERIFYFAIL, BIOMECHANGE, BIOMEPNGSTART, BIOMEPNGDAT, BIOMEPNGEND -> true;
            default -> false;
        };
    }

    private static int fieldCount(PacketIDs control) {
        return switch (control) {
            case ENTITYVERIFY, ENTITYVERIFYFAIL, BIOMECHANGE -> 2;
            case BIOMEPNGSTART -> 7;
            case BIOMEPNGDAT -> 1 + 3 * reika.dragonapi.command.BiomeMapCommand.PACKET_COMPILE;
            case BIOMEPNGEND -> 1;
            default -> throw new IllegalArgumentException("Not a dimension packet: " + control);
        };
    }

    private static Identifier parseIdentifier(String text) {
        if (!text.contains(":")) throw new IllegalArgumentException("Registry keys must include a namespace");
        return Identifier.parse(text);
    }

    public static DimensionPacketData read(PacketIDs control, DataInputStream input) throws IOException {
        var dimension = ResourceKey.create(Registries.DIMENSION, parseIdentifier(ReikaPacketHelper.readString(input)));
        String name = ReikaPacketHelper.readString(input);
        int[] values = new int[fieldCount(control)];
        for (int i = 0; i < values.length; i++) values[i] = input.readInt();
        if (input.available() != 0) throw new IOException("Trailing dimension packet data");
        return new DimensionPacketData(control, dimension, name, values);
    }

    public byte[] encode() {
        var bytes = new ByteArrayOutputStream();
        try (var output = new DataOutputStream(bytes)) {
            output.writeInt(control.wireId());
            ReikaPacketHelper.writeString(dimension.identifier().toString(), output);
            ReikaPacketHelper.writeString(name, output);
            for (int value : values) output.writeInt(value);
        } catch (IOException error) { throw new IllegalStateException("Cannot encode dimension packet", error); }
        return bytes.toByteArray();
    }

    public void send(PacketTarget target) {
        var pipe = Objects.requireNonNull(ReikaPacketHelper.getPipeline(DragonAPI.packetChannel), "DragonAPI channel");
        var packet = new ReikaPacketHelper.DataPacket(encode());
        packet.init(PacketTypes.RAW, pipe);
        target.dispatch(pipe, packet);
    }

    /** Apply only in the level named on the wire; direction is enforced by APIPacketHandler. */
    public boolean matches(Level level) { return dimension.equals(level.dimension()); }
}
