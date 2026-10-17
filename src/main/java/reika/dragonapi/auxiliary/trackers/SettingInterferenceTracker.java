package reika.dragonapi.auxiliary.trackers;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.jspecify.annotations.Nullable;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.DragonOptions;
import reika.dragonapi.auxiliary.PopupWriter;
import reika.dragonapi.instantiable.event.client.ClientLoginEvent;
import reika.dragonapi.instantiable.event.client.ClientLogoutEvent;

/**
 * Warns about user settings that can interfere with gameplay without overriding those settings.
 * Login messages follow the configured persistence policy; relevant active settings also retain
 * the original small reminder icons on the HUD, independently of message persistence.
 * Registration, login and extraction are owned by the physical client's main thread.
 */
@EventBusSubscriber(modid = DragonAPI.MODID, value = Dist.CLIENT)
public final class SettingInterferenceTracker {
    public static final Identifier LAYER = Identifier.fromNamespaceAndPath(DragonAPI.MODID, "setting_interference");
    private static final Identifier MUTE_ICON = Identifier.fromNamespaceAndPath(DragonAPI.MODID, "textures/mutewarn.png");
    public static final SettingInterferenceTracker instance = new SettingInterferenceTracker();

    public static final SettingInterference muteInterference = new SettingInterference() {
        private final EnumSet<SoundSource> importantSounds = EnumSet.of(SoundSource.MASTER, SoundSource.AMBIENT, SoundSource.BLOCKS, SoundSource.PLAYERS);

        @Override public boolean isCurrentlyRelevant() { return true; }

        @Override public boolean isSetToInterfere() {
            var options = Minecraft.getInstance().options;
            return importantSounds.stream().anyMatch(source -> options.getSoundSourceVolume(source) < 0.1F);
        }

        @Override public void drawIcon(GuiGraphicsExtractor graphics, int x, int y, int size) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, MUTE_ICON, x, y, 0, 0, size, size, 32, 32, 32, 32);
        }

        @Override public String getDescription() {
            return Component.translatable("warning.dragonapi.settings.muted").getString();
        }

        @Override public String getID() { return "soundmute"; }
    };

    private final Map<String, SettingInterference> settings = new LinkedHashMap<>();
    private final Path cacheFile;
    private @Nullable UUID notifiedPlayer;

    private SettingInterferenceTracker() {
        this(DragonAPI.getMinecraftDirectory().toPath().resolve("DragonAPI/setting_interference_cache.dat"));
    }

    private SettingInterferenceTracker(Path cacheFile) {
        this.cacheFile = cacheFile;
    }

    @SubscribeEvent public static void registerLayers(RegisterGuiLayersEvent event) {
        // Login popups draw after TITLE; keep reminders beneath them instead of covering their text.
        event.registerBelow(VanillaGuiLayers.TITLE, LAYER, (graphics, delta) -> instance.onRender(graphics));
    }

    @SubscribeEvent public static void login(ClientLoginEvent event) {
        instance.onLogin(event.player);
    }

    @SubscribeEvent public static void logout(ClientLogoutEvent event) {
        instance.notifiedPlayer = null;
    }

    public void registerSettingHandler(SettingInterference setting) {
        Objects.requireNonNull(setting, "setting");
        String id = Objects.requireNonNull(setting.getID(), "setting ID");
        // Validate the persisted identifier before accepting a handler. First registration wins.
        new SettingWarningCache.Snapshot(Set.of(id), Map.of());
        settings.putIfAbsent(id, setting);
    }

    public void onLogin(Player player) {
        if (player == null || player != Minecraft.getInstance().player || settings.isEmpty() || player.getUUID().equals(notifiedPlayer)) return;
        notifiedPlayer = player.getUUID();
        var current = currentState();
        SettingWarningCache.Snapshot previous = null;
        boolean readable = true;
        try {
            previous = SettingWarningCache.read(cacheFile, settings.keySet());
        } catch (IOException error) {
            readable = false;
            DragonAPI.LOGGER.warn("Could not read setting warning cache {}; preserving it and showing warnings", cacheFile, error);
        }
        if (!SettingWarningCache.shouldWarn(persistence().policy(), previous, current)) return;
        if (!current.enabled().isEmpty()) {
            String header = Component.translatable("warning.dragonapi.settings.header").getString();
            PopupWriter.instance().addMessage(header);
            DragonAPI.LOGGER.info("{}", header);
            for (SettingInterference setting : settings.values()) {
                if (current.enabled().contains(setting.getID())) {
                    String description = setting.getDescription();
                    PopupWriter.instance().addMessage(description);
                    DragonAPI.LOGGER.info("{}", description);
                }
            }
        }
        if (readable) {
            try {
                SettingWarningCache.write(cacheFile, current);
            } catch (IOException error) {
                DragonAPI.LOGGER.warn("Could not save setting warning cache {}", cacheFile, error);
            }
        }
    }

    public void onRender(GuiGraphicsExtractor graphics) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.gui.hud.isHidden()) return;
        int x = 2;
        for (SettingInterference setting : settings.values()) {
            if (setting.isSetToInterfere() && setting.isCurrentlyRelevant()) {
                graphics.pose().pushMatrix();
                try {
                    setting.drawIcon(graphics, x, 2, 16);
                } finally {
                    graphics.pose().popMatrix();
                }
                x += 20;
            }
        }
    }

    private SettingWarningCache.Snapshot currentState() {
        Set<String> enabled = settings.values().stream().filter(SettingInterference::isSetToInterfere)
                .map(SettingInterference::getID).collect(Collectors.toSet());
        Map<String, String> versions = new TreeMap<>();
        ModList.get().getMods().forEach(mod -> versions.put(mod.getModId(), mod.getVersion().toString()));
        return new SettingWarningCache.Snapshot(enabled, versions);
    }

    private WarningPersistence persistence() {
        String value = DragonOptions.SETTINGWARN.getString();
        try {
            return WarningPersistence.valueOf(value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException invalid) {
            DragonAPI.LOGGER.warn("Invalid setting warning persistence '{}'; using EVERYLOAD", value, invalid);
            return WarningPersistence.EVERYLOAD;
        }
    }

    private boolean isWarningActive(WarningPersistence persistence) {
        try {
            return SettingWarningCache.shouldWarn(persistence.policy(), SettingWarningCache.read(cacheFile, settings.keySet()), currentState());
        } catch (IOException error) {
            DragonAPI.LOGGER.warn("Could not inspect setting warning cache {}", cacheFile, error);
            return true;
        }
    }

    public interface SettingInterference {
        /** Whether the influence of the setting is relevant at this exact second. */
        boolean isCurrentlyRelevant();
        /** Whether the setting can interfere, even when it is not currently relevant. */
        boolean isSetToInterfere();
        /** Extract the icon into the supplied native GUI context; no global render state changes. */
        void drawIcon(GuiGraphicsExtractor graphics, int x, int y, int size);
        String getDescription();
        String getID();
    }

    public enum WarningPersistence {
        EVERYLOAD, SETTINGCHANGE, VERSION, ONCE;

        private SettingWarningCache.Policy policy() {
            return SettingWarningCache.Policy.valueOf(name());
        }

        public boolean isActive() {
            return instance.isWarningActive(this);
        }
    }
}
