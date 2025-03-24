package reika.dragonapi.auxiliary.trackers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.DragonOptions;
import reika.dragonapi.auxiliary.PopupWriter;
import reika.dragonapi.instantiable.event.ProfileEvent;
import reika.dragonapi.io.ReikaFileReader;
import reika.dragonapi.libraries.rendering.ReikaRenderHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;

import static reika.dragonapi.DragonAPI.MODID;

/**
 * This class is for registering handlers for when certain user-specified settings might cause functional problems ingame, but where the developer does not
 * wish to simply override the setting value (be that due to concerns over practical effects, an ideological stance, or otherwise).
 * <p>
 * This will monitor the state of such settings and will notify the player appropriately, once with a message on login and with a small icon to both act as a
 * continuous reminder and a watermark of sorts (so that complaints resulting from said settings can be appropriately identified and treated accordingly).
 */
public class SettingInterferenceTracker implements ProfileEvent.ProfileEventWatcher {

    public static final SettingInterferenceTracker instance = new SettingInterferenceTracker();

    public static final SettingInterference muteInterference = new SettingInterference() {

        private final EnumSet<SoundSource> importantSounds = EnumSet.of(SoundSource.MASTER, SoundSource.AMBIENT, SoundSource.BLOCKS, SoundSource.PLAYERS);

        @Override
        public boolean isCurrentlyRelevant() {
            return true;
        }

        @Override
        public boolean isSetToInterfere() {
            Options gs = Minecraft.getInstance().options;
            for (SoundSource s : importantSounds) {
                if (gs.getSoundSourceVolume(s) < 0.1) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void drawIcon(PoseStack stack, int x, int y, int size) {
            Tesselator t = Tesselator.getInstance();
            BufferBuilder v5 = t.getBuilder();
            Minecraft.getInstance().textureManager.bindForSetup(ResourceLocation.fromNamespaceAndPath(MODID, "textures/mutewarn.png"));
            v5.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            v5.color(0xffffff);
            v5.vertex(x, y + size, 0).uv(0, 1).endVertex();
            v5.vertex(x + size, y + size, 0).uv(1, 1).endVertex();
            v5.vertex(x + size, y, 0).uv(1, 0).endVertex();
            v5.vertex(x, y, 0).uv(0, 0).endVertex();
            v5.end();
        }

        @Override
        public String getDescription() {
            return "Sounds are muted, despite being used as indicators or warnings in many situations.";
        }

        public String getID() {
            return "soundmute";
        }

    };

    private final HashMap<String, SettingInterference> settings = new HashMap<>();

    private WarningPersistence persistence = WarningPersistence.EVERYLOAD;

    private SettingInterferenceTracker() {
        //ProfileEvent.registerHandler("gui", this);
        persistence = WarningPersistence.valueOf(String.valueOf(DragonOptions.SETTINGWARN.getString()));
    }

    public void registerSettingHandler(SettingInterference s) {
        if (!settings.containsKey(s.getID()))
            settings.put(s.getID(), s);
    }

    public void onLogin(Player ep) {
        if (!persistence.isActive())
            return;
        this.cacheSettings();
        ArrayList<String> li = new ArrayList<>();
        for (SettingInterference si : settings.values()) {
            if (si.isSetToInterfere()) {
                li.add(si.getDescription());
            }
        }
        if (!li.isEmpty()) {
            String s0 = "You have one or more game settings configured in a manner that is likely to cause gameplay problems in some situations; consider changing them, and please do not report any issues that would not have arisen without that setting. See the next messages for more details.";
            PopupWriter.instance.addMessage(s0);
            DragonAPI.LOGGER.info(s0);
            for (String s : li) {
                PopupWriter.instance.addMessage(s);
                DragonAPI.LOGGER.info(s);
            }
        }
    }

    public void onCall(String tag) {
        if (settings.isEmpty())
            return;
        if ("gui".equals(tag)) {
            this.onRender();
        }
    }

    public void onRender() {
        if (!settings.isEmpty()) {
//      todo      GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
//            Minecraft.getInstance().entityRenderer.setupOverlayRendering();
            ReikaRenderHelper.disableEntityLighting();
            ReikaRenderHelper.disableLighting();
            RenderSystem.enableBlend();
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.defaultBlendFunc();
            PoseStack stack = new PoseStack();

            int x = 2;
            int y = 2;
            int size = 16;
            for (SettingInterference s : settings.values()) {
                if (s.isSetToInterfere() && s.isCurrentlyRelevant()) {
                    s.drawIcon(stack, x, y, size);
                    x += size + 4;
                }
            }
//    todo        GL11.glPopAttrib();
        }
    }

    private File getSettingCacheFile() {
        return new File(new File(DragonAPI.getMinecraftDirectory(), "DragonAPI"), "setting_interference_cache.dat");
    }

    private boolean settingsMatchCache() {
        File f = this.getSettingCacheFile();
        if (!f.exists())
            return false;
        ArrayList<String> li = ReikaFileReader.getFileAsLines(f, true);
        HashSet<SettingInterference> set = new HashSet<>();
        HashSet<SettingInterference> set2 = new HashSet<>();
        for (String s : li) {
            SettingInterference si = settings.get(s);
            if (si != null) {
                set.add(si);
            }
        }
        for (SettingInterference s : settings.values()) {
            if (s.isSetToInterfere()) {
                set2.add(s);
            }
        }
        return set.equals(set2);
    }

    private void cacheSettings() {
        try {
            File f = this.getSettingCacheFile();
            f.delete();
            f.getParentFile().mkdirs();
            f.createNewFile();

            ArrayList<String> li = new ArrayList<>();
            for (SettingInterference s : settings.values()) {
                if (s.isSetToInterfere()) {
                    li.add(s.getID());
                }
            }

            ReikaFileReader.writeLinesToFile(f, li, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface SettingInterference {

        /**
         * Whether the influence of the setting is relevant <i>at this exact second</i>.
         */
        boolean isCurrentlyRelevant();

        /**
         * Whether the setting is configured such that it might interfere under some circumstances, not necessarily at this instant.
         */
        boolean isSetToInterfere();

        /**
         * Draw the icon at the specified position and size. The tessellator is not running, in case you want to bind your own textures.
         */

        void drawIcon(PoseStack stack, int x, int y, int size);

        String getDescription();

        String getID();

    }

    public enum WarningPersistence {
        EVERYLOAD(),
        SETTINGCHANGE(),
        VERSION(),
        ONCE();

        public boolean isActive() {
            switch (this) {
                case EVERYLOAD:
                default:
                    return true;
                case VERSION:
                    return VersionTransitionTracker.instance.haveModsUpdated();
                case SETTINGCHANGE:
                    return !instance.settingsMatchCache();//EnvironmentPackager.instance.checkAndUpdateSettingsCache();
                case ONCE:
                    return !instance.getSettingCacheFile().exists();//EnvironmentPackager.instance.hasSettingsCache();
            }
        }
    }
}
