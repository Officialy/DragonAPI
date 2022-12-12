package reika.dragonapi.auxiliary.trackers;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import reika.dragonapi.APIPacketHandler;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.base.DragonAPIMod;
import reika.dragonapi.exception.InstallationException;
import reika.dragonapi.exception.MisuseException;
import reika.dragonapi.instantiable.data.maps.PlayerMap;
import reika.dragonapi.instantiable.event.RawKeyPressEvent;
import reika.dragonapi.instantiable.io.oldforge.Property;
import reika.dragonapi.interfaces.configuration.StringConfig;
import reika.dragonapi.libraries.io.ReikaPacketHelper;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
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

    public static enum Key {
        JUMP(),
        SNEAK(),
        FORWARD(),
        BACK(),
        LEFT(),
        RIGHT(),
        INVENTORY(),
        DROPITEM(),
        ATTACK(),
        USE(),
        CHAT(),
        LSHIFT(),
        LCTRL(),
        LALT(),
        PGUP(),
        PGDN(),
        TAB(),
        TILDE(),
        BACKSPACE(),
        HOME(),
        END(),
        INSERT(),
        DELETE(),
        ENTER(),
        MINUS(),
        PLUS(),
        PRTSCRN(),
        PAUSE();

        public static final Key[] keyList = values();

        public static Key readFromConfig(DragonAPIMod mod, StringConfig cfg) {
            if (!(cfg instanceof Property))
                throw new MisuseException(mod, "Cannot read a key from a non-string config!");
            String s = cfg.getString().toUpperCase(Locale.ENGLISH);
            try {
                return Key.valueOf(s);
            }
            catch (IllegalArgumentException e) {
                throw new InstallationException(mod, "Invalid specified keybind for config entry '"+cfg.getLabel()+"'; no such key '"+s+"' exists! Valid options: "+ Arrays.toString(values()));
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private enum Keys {
        JUMP(Minecraft.getInstance().options.keyJump),
        SNEAK(Minecraft.getInstance().options.keyShift),
        FOWARD(Minecraft.getInstance().options.keyUp),
        BACK(Minecraft.getInstance().options.keyDown),
        LEFT(Minecraft.getInstance().options.keyLeft),
        RIGHT(Minecraft.getInstance().options.keyRight),
        INVENTORY(Minecraft.getInstance().options.keyInventory),
        DROPITEM(Minecraft.getInstance().options.keyDrop),
        ATTACK(Minecraft.getInstance().options.keyAttack),
        USE(Minecraft.getInstance().options.keyUse),
        CHAT(Minecraft.getInstance().options.keyChat),
        LSHIFT(isCtrlSneak() ? getLCtrl() : InputConstants.KEY_LSHIFT),
        LCTRL(isCtrlSneak() ? InputConstants.KEY_LSHIFT : getLCtrl()), //swap them
        PGUP(InputConstants.KEY_PAGEUP),
        PGDN(InputConstants.KEY_PAGEDOWN),
        TAB(InputConstants.KEY_TAB),
        TILDE(InputConstants.KEY_GRAVE), //Not on Euro InputConstantss
        BACKSPACE(InputConstants.KEY_BACKSPACE),
        HOME(InputConstants.KEY_HOME),
        END(InputConstants.KEY_END),
        INSERT(InputConstants.KEY_INSERT),
        DELETE(InputConstants.KEY_DELETE),
        ENTER(InputConstants.KEY_RETURN),
        MINUS(InputConstants.KEY_MINUS),
        PLUS(InputConstants.KEY_EQUALS),
        PRTSCRN(InputConstants.KEY_PRINTSCREEN), //no idea how common this one is
        PAUSE(InputConstants.KEY_PAUSE);

        private KeyMapping key;
        private int keyInt;

        public static final Keys[] keyList = values();

        private Keys(KeyMapping key) {
            this.key = key;
        }

        private Keys(int key) {
            keyInt = key;
        }

        public boolean pollKey() {
            return key != null ? key.isDown() : InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), keyInt);
        }

        public int keyID() {
            return key != null ? key.getKey().getValue() : keyInt;
        }

        public Key getServerKey() {
            return Key.keyList[this.ordinal()];
        }

        private void sendPacket() {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(8);
            DataOutputStream data = new DataOutputStream(bytes);
            boolean flag = false;
            try {
                data.writeInt(APIPacketHandler.PacketIDs.KEYUPDATE.ordinal());
                data.writeInt(this.ordinal());
                data.writeInt(this.pollKey() ? 1 : 0);
                flag = true;
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            if (flag)
                ReikaPacketHelper.sendRawPacket(DragonAPI.packetChannel, bytes);
            else
                DragonAPI.LOGGER.info("Could not send key "+this+" packet, as it was malformed.");
        }

        private static int getLCtrl() {
            return /*Minecraft.getInstance().isisRunningOnMac ? InputConstants.KEY_LMETA :*/ InputConstants.KEY_LCONTROL;
        }

        private static boolean isCtrlSneak() {
            return Minecraft.getInstance().options.keyShift.getKey().getValue() == getLCtrl();
        }
    }

    private static class KeyState {

        private final PlayerMap<Boolean> data = new PlayerMap<>();

        public boolean getKeyState(Player ep) {
            return data.containsKey(ep) && data.get(ep);
        }

        public void updateKey(Player ep, boolean key) {
            data.put(ep, key);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class KeyTicker implements TickRegistry.TickHandler {

        public static final KeyTicker instance = new KeyTicker();
        private final EnumMap<Keys, Boolean> keyStates = new EnumMap<Keys, Boolean>(Keys.class);

        private KeyTicker() {

        }

        @Override
        public void tick(TickRegistry.TickType type, Object... tickData) {

            Player ep = Minecraft.getInstance().player;
            if (ep != null) {
                for (int i = 0; i < Keys.keyList.length; i++) {
                    Keys key = Keys.keyList[i];
                    boolean wasPressed = keyStates.containsKey(key) && keyStates.get(key);
                    boolean isPressed = key.pollKey();
                    if (wasPressed != isPressed) {
                        keyStates.put(key, isPressed);
                        key.sendPacket();
                        KeyWatcher.instance.setKey(ep, key.getServerKey(), isPressed);
                        MinecraftForge.EVENT_BUS.post(new RawKeyPressEvent(key.getServerKey(), ep));
                    }
                }
            }

        }

        @Override
        public EnumSet<TickRegistry.TickType> getType() {
            return EnumSet.of(TickRegistry.TickType.CLIENT);
        }

        @Override
        public String getLabel() {
            return "KeyWatcher";
        }

        @Override
        public boolean canFire(TickEvent.Phase p) {
            return p == TickEvent.Phase.START;
        }

    }
}
