package reika.dragonapi.auxiliary.trackers;

import joptsimple.internal.Strings;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.fml.ModList;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.DragonOptions;
import reika.dragonapi.APIPacketHandler;
import reika.dragonapi.instantiable.io.PacketTarget;
import reika.dragonapi.libraries.io.ReikaPacketHelper;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import reika.dragonapi.base.DragonAPIMod;

import java.io.File;
import java.io.IOException;
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
        return world.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("modversions.list").toFile();
    }

    public void onWorldLoad(Level world) {
        if (world.dimension() == Level.OVERWORLD && !world.isClientSide() && DragonOptions.VERSIONCHANGEWARN.getValue() > 0) {
            this.loadCacheAndUpdate(world);
        }
    }

    private void loadCacheAndUpdate(Level world) {
        lastVersions.clear();
        newVersions.clear();

        File f = this.getFilename(world);
        if (f.exists()) {
            try {
                lastVersions.putAll(parseCache(Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)));
            } catch (IOException e) {
                DragonAPI.LOGGER.warn("Could not read version cache {}", f, e);
            }

            for (IModInfo mc : ModList.get().getMods()) {
                if (this.updated(mc)) {
                    newVersions.add(mc.getModId());
                }
            }
        }

        this.saveCache(world);
    }

    private void saveCache(Level world) {
        try {
            var target = getFilename(world).toPath();
            Files.createDirectories(target.getParent());
            List<String> lines = ModList.get().getMods().stream()
                    .map(mod -> mod.getModId() + "=" + parseModVersion(mod)).sorted().toList();
            var pending = target.resolveSibling("modversions.list.tmp");
            Files.write(pending, lines, StandardCharsets.UTF_8);
            try {
                Files.move(pending, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(pending, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException e) {
            DragonAPI.LOGGER.warn("Could not save mod version cache", e);
        }
    }

    private String parseModVersion(IModInfo mc) {
        DragonAPIMod m = DragonAPIMod.getByName(mc.getDisplayName().toUpperCase(Locale.ROOT));
        String ret = m != null ? m.getModVersion().toString() : String.valueOf(mc.getVersion());
        return Strings.isNullOrEmpty(ret) ? "[NONE]" : ret;
    }

    private String getDisplayName(IModInfo mc) {
        DragonAPIMod m = DragonAPIMod.getByName(mc.getDisplayName().toUpperCase(Locale.ROOT));
        return m != null ? m.getDisplayName() : mc.getDisplayName();
    }

    public String getPreviousModVersion(IModInfo mod) {
        return lastVersions.get(mod.getModId());
    }

    public boolean updated(IModInfo mod) {
        if (DragonOptions.VERSIONCHANGEWARN.getValue() == 1) {
            DragonAPIMod modo = DragonAPIMod.getByName(mod.getDisplayName().toUpperCase(Locale.ROOT));
            if (modo != null) {
                if (!modo.isReikasMod())
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
            ReikaPacketHelper.sendStringIntPacket(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.POPUP.ordinal(), new PacketTarget.PlayerTarget(emp), s0, 300);
            DragonAPI.LOGGER.info(newVersions.size()+" mod version changes detected: ");
            for (IModInfo mod : ModList.get().getMods()) {
                if (newVersions.contains(mod.getModId()))
                    DragonAPI.LOGGER.info("{}: {} --> {}", getDisplayName(mod), lastVersions.get(mod.getModId()), parseModVersion(mod));
            }
        }
    }

    static Map<String, String> parseCache(List<String> lines) {
        Map<String, String> versions = new HashMap<>();
        for (String line : lines) {
            int split = line.indexOf('=');
            if (split <= 0 || line.stripLeading().startsWith("#")) continue;
            String id = line.substring(0, split).trim();
            String version = line.substring(split + 1).trim();
            if (!id.isEmpty() && !version.isEmpty()) versions.put(id, version);
        }
        return versions;
    }

    public void clear() {
        lastVersions.clear();
        newVersions.clear();
    }

    public boolean haveModsUpdated() {
        return !newVersions.isEmpty();
    }
}
