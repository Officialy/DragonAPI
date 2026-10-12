package reika.dragonapi.interfaces;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import reika.dragonapi.APIPacketHandler.PacketIDs;
import reika.dragonapi.auxiliary.PacketTypes;

class PacketIdTest {
    private enum Reordered implements PacketId {
        LATER(90), EARLY(3), MIDDLE(42);
        private final int id;
        Reordered(int id) { this.id=id; }
        public int wireId() { return id; }
    }
    private enum Duplicate implements PacketId {
        FIRST, SECOND;
        public int wireId() { return 7; }
    }
    private enum Negative implements PacketId {
        BAD;
        public int wireId() { return -1; }
    }
    @Test void declarationOrderAndIndexInputOrderCannotChooseTheWireIdentity() {
        var normal=PacketId.index(Reordered.values());
        var scrambled=PacketId.index(new Reordered[]{Reordered.MIDDLE,Reordered.LATER,Reordered.EARLY});
        assertEquals(normal,scrambled);
        assertEquals(Reordered.EARLY,PacketId.lookup(normal,3));assertEquals(Reordered.MIDDLE,PacketId.lookup(normal,42));
        assertEquals(Reordered.LATER,PacketId.lookup(normal,90));
        assertThrows(UnsupportedOperationException.class,()->normal.put(1,Reordered.EARLY));
    }
    @Test void ambiguousAndNegativeTablesCannotInitialize() {
        assertThrows(IllegalStateException.class,()->PacketId.index(Duplicate.values()));
        assertThrows(IllegalStateException.class,()->PacketId.index(Negative.values()));
    }
    @Test void unknownIdsCannotClampToOrIndexAnotherOperation() {
        var ids=PacketId.index(Reordered.values());
        for(int id:new int[]{-1,0,4,Integer.MAX_VALUE})assertThrows(IllegalArgumentException.class,()->PacketId.lookup(ids,id));
        for(int id:new int[]{-1,16,255})assertThrows(IllegalArgumentException.class,()->PacketTypes.getPacketType(id));
        for(int id:new int[]{-1,47,Integer.MAX_VALUE})assertThrows(IllegalArgumentException.class,()->PacketIDs.getEnum(id));
    }
}
