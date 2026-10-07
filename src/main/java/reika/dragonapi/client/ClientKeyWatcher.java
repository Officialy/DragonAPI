package reika.dragonapi.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import reika.dragonapi.APIPacketHandler;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.auxiliary.trackers.KeyWatcher;
import reika.dragonapi.auxiliary.trackers.KeyWatcher.Key;
import reika.dragonapi.instantiable.event.RawKeyPressEvent;
import reika.dragonapi.libraries.io.ReikaPacketHelper;

/** Polls current bindings and SDL input codes without capturing options during class initialization. */
public final class ClientKeyWatcher {
    private static final EnumMap<Key, Boolean> states = new EnumMap<>(Key.class);
    private static Player previousPlayer;
    private ClientKeyWatcher() {}

    public static void reset() {
        if (previousPlayer != null) KeyWatcher.instance.clear(previousPlayer);
        previousPlayer = null;
        states.clear();
    }

    public static void tick() {
        Player player = Minecraft.getInstance().player;
        if (player != previousPlayer) {
            reset();
            previousPlayer = player;
        }
        if (player == null) return;
        for (Key key : Key.keyList) {
            boolean pressed = poll(key);
            Boolean old = states.put(key, pressed);
            if (old == null || old != pressed) {
                send(key, pressed);
                KeyWatcher.instance.setKey(player, key, pressed);
                if (pressed || Boolean.TRUE.equals(old)) NeoForge.EVENT_BUS.post(new RawKeyPressEvent(key, player));
            }
        }
    }

    public static boolean poll(Key key) {
        var options = Minecraft.getInstance().options;
        return switch (key) {
            case JUMP -> options.keyJump.isDown();
            case SNEAK -> options.keyShift.isDown();
            case FORWARD -> options.keyUp.isDown();
            case BACK -> options.keyDown.isDown();
            case LEFT -> options.keyLeft.isDown();
            case RIGHT -> options.keyRight.isDown();
            case INVENTORY -> options.keyInventory.isDown();
            case DROPITEM -> options.keyDrop.isDown();
            case ATTACK -> options.keyAttack.isDown();
            case USE -> options.keyUse.isDown();
            case CHAT -> options.keyChat.isDown();
            default -> InputConstants.isKeyDown(rawCode(key, options.keyShift.getKey().getValue() == InputConstants.KEY_LCONTROL));
        };
    }

    public static int rawCode(Key key, boolean controlSneak) {
        return switch (key) {
            case LSHIFT -> controlSneak ? InputConstants.KEY_LCONTROL : InputConstants.KEY_LSHIFT;
            case LCTRL -> controlSneak ? InputConstants.KEY_LSHIFT : InputConstants.KEY_LCONTROL;
            case LALT -> InputConstants.KEY_LALT;
            case PGUP -> InputConstants.KEY_PAGEUP;
            case PGDN -> InputConstants.KEY_PAGEDOWN;
            case TAB -> InputConstants.KEY_TAB;
            case TILDE -> InputConstants.KEY_GRAVE;
            case BACKSPACE -> InputConstants.KEY_BACKSPACE;
            case HOME -> InputConstants.KEY_HOME;
            case END -> InputConstants.KEY_END;
            case INSERT -> InputConstants.KEY_INSERT;
            case DELETE -> InputConstants.KEY_DELETE;
            case ENTER -> InputConstants.KEY_RETURN;
            case MINUS -> InputConstants.KEY_MINUS;
            case PLUS -> InputConstants.KEY_EQUALS;
            case PRTSCRN -> InputConstants.KEY_PRINTSCREEN;
            case PAUSE -> InputConstants.KEY_PAUSE;
            default -> throw new IllegalArgumentException("Key uses a configurable binding: " + key);
        };
    }

    private static void send(Key key, boolean pressed) {
        var bytes = new ByteArrayOutputStream(12);
        try (var data = new DataOutputStream(bytes)) {
            data.writeInt(APIPacketHandler.PacketIDs.KEYUPDATE.ordinal());
            data.writeInt(key.wireId);
            data.writeInt(pressed ? 1 : 0);
        } catch (IOException e) { throw new IllegalStateException("Unable to encode key state", e); }
        ReikaPacketHelper.sendRawPacket(DragonAPI.packetChannel, bytes);
    }
}
