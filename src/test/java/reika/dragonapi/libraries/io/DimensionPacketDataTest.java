package reika.dragonapi.libraries.io;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.util.Arrays;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import reika.dragonapi.APIPacketHandler.PacketIDs;

class DimensionPacketDataTest {
    private static DimensionPacketData decode(byte[] bytes) throws IOException {
        var input = new DataInputStream(new ByteArrayInputStream(bytes));
        return DimensionPacketData.read(PacketIDs.getEnum(input.readInt()), input);
    }
    @Test void independentGoldenCarriesNamespacedDimensionAndOriginalClassCheck() throws Exception {
        var packet = new DimensionPacketData(PacketIDs.ENTITYVERIFY, Level.NETHER, "", new int[]{-71, 12345});
        var expected = new ByteArrayOutputStream(); var output = new DataOutputStream(expected);
        output.writeInt(42); output.writeShort(20); output.writeChars("minecraft:the_nether");
        output.writeShort(0); output.writeInt(-71); output.writeInt(12345);
        assertArrayEquals(expected.toByteArray(), packet.encode());
        var restored = decode(packet.encode());
        assertEquals(Level.NETHER, restored.dimension()); assertArrayEquals(packet.values(), restored.values());
    }
    @Test void customDimensionAndUnicodeMapNamesRoundTripWithDetachedFields() throws Exception {
        var dimension = ResourceKey.create(Registries.DIMENSION, Identifier.parse("chromaticraft:chroma"));
        int[] fields = {9, -20, 40, 32, 4, 8, 1};
        var packet = new DimensionPacketData(PacketIDs.BIOMEPNGSTART, dimension, "world λ", fields);
        fields[0] = 8; packet.values()[0] = 7;
        var restored = decode(packet.encode());
        assertEquals(dimension, restored.dimension()); assertEquals("world λ", restored.name());
        assertEquals(9, restored.values()[0]);
    }
    @Test void legacyTruncatedTrailingAndMalformedDimensionBodiesAreRejected() throws Exception {
        var bytes = new DimensionPacketData(PacketIDs.ENTITYVERIFY, Level.OVERWORLD, "", new int[]{1, 2}).encode();
        assertThrows(EOFException.class, () -> decode(Arrays.copyOf(bytes, bytes.length - 1)));
        assertThrows(IOException.class, () -> decode(Arrays.copyOf(bytes, bytes.length + 1)));
        assertThrows(IllegalArgumentException.class, () -> decode(new byte[]{0,0,0,42,0,0,0,1,0,0,0,0,0,0,0,2}));
        var bad = new ByteArrayOutputStream(); var out = new DataOutputStream(bad);
        out.writeInt(42); out.writeShort(3); out.writeChars("bad");
        assertThrows(IllegalArgumentException.class, () -> decode(bad.toByteArray()));
        assertThrows(IllegalArgumentException.class, () -> new DimensionPacketData(PacketIDs.BIOMECHANGE, Level.OVERWORLD, "invalid", new int[]{1,2}));
        assertThrows(IllegalArgumentException.class, () -> new DimensionPacketData(PacketIDs.ENTITYVERIFY, Level.OVERWORLD, "", new int[]{1}));
    }
}
