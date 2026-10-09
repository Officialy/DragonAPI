package reika.dragonapi.instantiable.data;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import reika.dragonapi.instantiable.data.maps.PlayerMap;
import static org.junit.jupiter.api.Assertions.*;

class PlayerMapTest {
    @Test void uuidStringsResolveTheSameEntryAndRejectMalformedAliases() {
        var map = new PlayerMap<String>();
        var id = UUID.fromString("abcdef01-2345-6789-abcd-0123456789ab");
        map.directPut(id, "owner");
        assertEquals("owner", map.get(id.toString()));
        assertEquals("owner", map.get(id.toString().toUpperCase(java.util.Locale.ROOT)));
        for (String malformed : new String[]{"", "player-name", "1-1-1-1-1", " " + id, id + " "})
            assertNull(map.get(malformed));
        assertNull(map.get((String)null));
        assertNull(map.get(UUID.randomUUID().toString()));
        map.directRemove(id);
        assertNull(map.get(id.toString()));
    }
}
