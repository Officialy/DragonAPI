package reika.dragonapi.auxiliary.trackers;

import joptsimple.internal.Strings;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.DragonOptions;
import reika.dragonapi.auxiliary.PopupWriter;
import reika.dragonapi.base.DragonAPIMod;
import reika.dragonapi.io.ReikaFileReader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class VersionTransitionTracker {

    public static final VersionTransitionTracker instance = new VersionTransitionTracker();

    private final HashMap<String, String> lastVersions = new HashMap<>();
    private final HashSet<String> newVersions = new HashSet<>();

    private VersionTransitionTracker() {

    }

    private File getFilename(Level world) {
        return new File(world.getServer().getServerDirectory(), "modversions.list"); //world.getSaveHandler().getWorldDirectory()
    }

    public void onWorldLoad(Level world) {
        if (world.dimension() == Level.OVERWORLD && !world.isClientSide() && DragonOptions.VERSIONCHANGEWARN.getValue() > 0) {
            this.loadCacheAndUpdate(world);
            this.saveCache(world);
        }
    }

    private void loadCacheAndUpdate(Level world) {
        lastVersions.clear();
        newVersions.clear();

        File f = this.getFilename(world);
        if (f.exists()) {
            ArrayList<String> li = ReikaFileReader.getFileAsLines(f, true, Charset.defaultCharset());
            for (String s : li) {
                String[] parts = s.split("=");
                lastVersions.put(parts[0], parts[1]);
            }

            for (IModInfo mc : ModList.get().getMods()) {
                if (this.updated((ModContainer) mc)) {
                    newVersions.add(mc.getModId());
                }
            }
        }

        this.saveCache(world);
    }

    private void saveCache(Level world) {
        try {
            File f = this.getFilename(world);
            f.delete();
            f.getParentFile().mkdirs();
            f.createNewFile();

            ArrayList<String> li = new ArrayList<>();

            for (IModInfo mc : ModList.get().getMods()) {
                li.add(mc.getModId()+"="+this.parseModVersion((ModContainer) mc));
            }

            ReikaFileReader.writeLinesToFile(f, li, true);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String parseModVersion(ModContainer mc) {
        String ret = mc.getMod() instanceof DragonAPIMod ? ((DragonAPIMod)mc.getMod()).getModVersion().toString() : String.valueOf(mc.getModInfo().getVersion());
        return Strings.isNullOrEmpty(ret) ? "[NONE]" : ret;
    }

    private String getDisplayName(ModContainer mc) {
        return mc.getMod() instanceof DragonAPIMod ? ((DragonAPIMod)mc.getMod()).getDisplayName() : mc.getModInfo().getDisplayName();
    }

    public String getPreviousModVersion(ModContainer mod) {
        return lastVersions.get(mod.getModId());
    }

    public boolean updated(ModContainer mod) {
        if (DragonOptions.VERSIONCHANGEWARN.getValue() == 1) {
            Object modo = mod.getMod();
            if (modo instanceof DragonAPIMod) {
                if (!((DragonAPIMod)modo).isReikasMod())
                    return false;
            }
            else {
                return false;
            }
        }
        return !this.parseModVersion(mod).equals(this.getPreviousModVersion(mod));
    }

    public void notifyPlayerOfVersionChanges(ServerPlayer emp) {
        if (this.haveModsUpdated()) {
            String s0 = newVersions.size()+" of your mods have changed version (see the log for more details). It is strongly recommended you read their changelogs.";
            PopupWriter.instance.addMessage(s0);
            DragonAPI.LOGGER.info(newVersions.size()+" mod version changes detected: ");
        /* todo   Map<String, ModContainer> mods = Loader.instance.getIndexedModList();
            for (String s : newVersions) {
                String old = lastVersions.get(s);
                ModContainer mc = mods.get(s);
                DragonAPI.LOGGER.info(this.getDisplayName(mc)+": "+old+" --> "+this.parseModVersion(mc));
            }*/
        }
    }

    public boolean haveModsUpdated() {
        return !newVersions.isEmpty();
    }
}
