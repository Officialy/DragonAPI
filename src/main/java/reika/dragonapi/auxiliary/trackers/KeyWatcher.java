package reika.dragonapi.auxiliary.trackers;

import net.minecraft.world.entity.player.Player;
import reika.dragonapi.base.DragonAPIMod;
import reika.dragonapi.exception.InstallationException;
import reika.dragonapi.instantiable.data.maps.PlayerMap;
import reika.dragonapi.interfaces.configuration.StringConfig;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;

public class KeyWatcher {

    public static final KeyWatcher instance = new KeyWatcher();

    private final EnumMap<Key, KeyState> keyStates = new EnumMap<>(Key.class);

    private KeyWatcher() {
        for (int i = 0; i < Key.keyList.length; i++) {
            keyStates.put(Key.keyList[i], new KeyState());
        }
    }

    public boolean isKeyDown(Player ep, Key key) {
        return keyStates.get(key).getKeyState(ep);
    }

    public void setKey(Player ep, Key key, boolean press) {
        keyStates.get(key).updateKey(ep, press);
    }

    public void clear(Player player) { for (KeyState state : keyStates.values()) state.data.remove(player.getUUID()); }
    public void clear() { for (KeyState state : keyStates.values()) state.data.clear(); }

    public enum Key {
        JUMP(0),
        SNEAK(1),
        FORWARD(2),
        BACK(3),
        LEFT(4),
        RIGHT(5),
        INVENTORY(6),
        DROPITEM(7),
        ATTACK(8),
        USE(9),
        CHAT(10),
        LSHIFT(11),
        LCTRL(12),
        LALT(13),
        PGUP(14),
        PGDN(15),
        TAB(16),
        TILDE(17),
        BACKSPACE(18),
        HOME(19),
        END(20),
        INSERT(21),
        DELETE(22),
        ENTER(23),
        MINUS(24),
        PLUS(25),
        PRTSCRN(26),
        PAUSE(27);

        public static final Key[] keyList = values();
        public final int wireId;
        Key(int wireId) { this.wireId = wireId; }
        public static Key fromWireId(int id) {
            for (Key key : keyList) if (key.wireId == id) return key;
            throw new IllegalArgumentException("Unknown key id: " + id);
        }

        public static Key readFromConfig(DragonAPIMod mod, StringConfig cfg) {
            String s = cfg.getString().trim().toUpperCase(Locale.ROOT);
            try {
                return Key.valueOf(s);
            }
            catch (IllegalArgumentException e) {
                throw new InstallationException(mod, "Invalid specified keybind for config entry '"+cfg.getLabel()+"'; no such key '"+s+"' exists! Valid options: "+ Arrays.toString(values()));
            }
        }
    }

    
    private static class KeyState {

        private final java.util.concurrent.ConcurrentMap<java.util.UUID, Boolean> data = new java.util.concurrent.ConcurrentHashMap<>();

        public boolean getKeyState(Player ep) {
            return Boolean.TRUE.equals(data.get(ep.getUUID()));
        }

        public void updateKey(Player ep, boolean key) {
            data.put(ep.getUUID(), key);
        }
    }

    
    /** Compatibility ticker; client implementation is isolated from shared key state. */
    public static class KeyTicker implements TickRegistry.TickHandler {
        public static final KeyTicker instance = new KeyTicker();
        private KeyTicker() {}
        @Override public void tick(TickRegistry.TickType type, Object... data) {
            if (net.neoforged.fml.loading.FMLEnvironment.getDist().isClient())
                reika.dragonapi.client.ClientKeyWatcher.tick();
        }
        @Override public EnumSet<TickRegistry.TickType> getType() { return EnumSet.of(TickRegistry.TickType.CLIENT); }
        @Override public String getLabel() { return "KeyWatcher"; }
        @Override public boolean canFire(TickRegistry.Phase phase) { return phase == TickRegistry.Phase.START; }
    }
}
