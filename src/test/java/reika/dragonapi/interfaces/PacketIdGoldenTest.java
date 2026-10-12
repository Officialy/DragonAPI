package reika.dragonapi.interfaces;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/** Golden name/number fingerprints taken from the pushed protocol-2.1 checkpoint before this migration. */
class PacketIdGoldenTest {
    @Test void everyCoreWireIdentityRetainsThePublishedValue() throws Exception {
        assertEquals("c98875ca4d11a214b2cd4a7c596441adc7e1557ff5e4ed0f27e3e6e18918c138",fingerprint("reika.dragonapi.auxiliary.PacketTypes"));
        assertEquals("f60db17f2344fa9cebd16cb6d27af212ea8a1f680abea0892ad76fece96566e6",fingerprint("reika.dragonapi.APIPacketHandler$PacketIDs"));
        assertEquals("76428ab0ea5d5f25ab260d3d3537c6cba5ee9cc61f84bfa88cbe8a2a1b90d730",fingerprint("reika.dragonapi.libraries.io.ReikaPacketHelper$PacketableData"));
    }
    private static String fingerprint(String name) throws Exception {
        Object[] values=Class.forName(name).getEnumConstants();
        String canonical=Arrays.stream(values).map(value -> ((Enum<?>)value).name()+"="+((PacketId)value).wireId()+"\n")
                .sorted().collect(java.util.stream.Collectors.joining());
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
    }
}
