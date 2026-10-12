package reika.dragonapi.libraries.io;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.List;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import reika.dragonapi.instantiable.HybridTank;

class TankSyncDataTest {
    @Test void encodingMatchesTheCommittedProtocolIncludingUnicodeAndFullSignedCoordinates() throws Exception {
        var snapshot = new TankSyncData("tankλ", new BlockPos(-100, -50000, 300), 1250, "minecraft:water");
        var bytes = new ByteArrayOutputStream(); var output = new DataOutputStream(bytes);
        output.writeShort(5); output.writeChars("tankλ");output.writeInt(-100);output.writeInt(-50000);output.writeInt(300);
        output.writeInt(1250);output.writeShort(15);output.writeChars("minecraft:water");
        assertArrayEquals(bytes.toByteArray(),snapshot.encode());
        assertEquals(snapshot,TankSyncData.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))));
    }
    @Test void malformedSnapshotsCannotReachTheMutationPhase() throws Exception {
        assertThrows(IllegalArgumentException.class,()->new TankSyncData("",BlockPos.ZERO,1,"minecraft:water"));
        assertThrows(IllegalArgumentException.class,()->new TankSyncData("tank",BlockPos.ZERO,-1,"minecraft:water"));
        assertThrows(IllegalArgumentException.class,()->new TankSyncData("tank",BlockPos.ZERO,1,""));
        byte[] good = new TankSyncData("tank",BlockPos.ZERO,0,"").encode();
        assertThrows(java.io.EOFException.class,()->TankSyncData.read(new DataInputStream(new ByteArrayInputStream(java.util.Arrays.copyOf(good,good.length-1)))));
        assertThrows(IllegalArgumentException.class,()->TankSyncData.read(new DataInputStream(new ByteArrayInputStream(java.util.Arrays.copyOf(good,good.length+1)))));
    }
    private static class Parent { private final HybridTank inherited=new HybridTank("parent",1000); private final HybridTank hidden=new HybridTank("hidden",1000); }
    private static class Derived extends Parent {
        static final HybridTank shared=new HybridTank("static",1000);
        private final String hidden="not a tank";
        private final HybridTank absent=null;
        private final SpecialTank special=new SpecialTank();
    }
    private static class SpecialTank extends HybridTank { SpecialTank(){super("special",1000);} }
    @Test void fieldDiscoveryHonorsInheritanceSubtypesShadowingAndInstanceOwnership() {
        var tile=new Derived();assertEquals(List.of("absent","inherited","special"),TankSyncData.fields(Derived.class));
        assertEquals(1000,TankSyncData.tank(tile,"inherited").getCapacity());assertInstanceOf(SpecialTank.class,TankSyncData.tank(tile,"special"));
        for(String name:List.of("shared","hidden","absent","missing"))assertThrows(IllegalArgumentException.class,()->TankSyncData.tank(tile,name));
    }
}
