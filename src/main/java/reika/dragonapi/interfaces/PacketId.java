package reika.dragonapi.interfaces;

import java.util.HashMap;
import java.util.Map;

/** Explicit compatibility-protocol identity, independent of enum declaration order. Never recycle an ID. */
public interface PacketId {
    int wireId();

    static <E extends Enum<E> & PacketId> Map<Integer, E> index(E[] entries) {
        Map<Integer, E> ids = new HashMap<>();
        for (E entry : entries) {
            int id = entry.wireId();
            if (id < 0 || ids.putIfAbsent(id, entry) != null)
                throw new IllegalStateException("Negative or duplicate packet ID " + id + " in " + entry.getDeclaringClass().getName());
        }
        return Map.copyOf(ids);
    }

    static <E extends Enum<E> & PacketId> E lookup(Map<Integer, E> ids, int id) {
        E value = ids.get(id);
        if (value == null) throw new IllegalArgumentException("Unknown packet ID: " + id);
        return value;
    }
}
