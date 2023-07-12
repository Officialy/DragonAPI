package reika.dragonapi.auxiliary.trackers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.base.DragonAPIMod;
import reika.dragonapi.extras.ModVersion;

import reika.dragonapi.APIPacketHandler.PacketIDs;
import reika.dragonapi.DragonOptions;
import reika.dragonapi.auxiliary.PopupWriter;
import reika.dragonapi.io.ReikaFileReader;
import reika.dragonapi.io.ReikaFileReader.ConnectionErrorHandler;
import reika.dragonapi.io.ReikaFileReader.DataFetcher;
import reika.dragonapi.instantiable.data.collections.OneWayCollections.OneWayList;
import reika.dragonapi.instantiable.data.collections.OneWayCollections.OneWayMap;
import reika.dragonapi.instantiable.event.client.ClientLoginEvent;
import reika.dragonapi.instantiable.io.PacketTarget;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.io.ReikaChatHelper;
import reika.dragonapi.libraries.io.ReikaPacketHelper;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;
import reika.dragonapi.libraries.java.ReikaStringParser;
@Mod.EventBusSubscriber(modid = DragonAPI.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommandableUpdateChecker {

    public static final CommandableUpdateChecker instance = new CommandableUpdateChecker();

    public static final String reikaURL = "http://server.techjargaming.com/Reika/versions";

    private final HashMap<DragonAPIMod, ModVersion> latestVersions = new OneWayMap<>();
    private final Collection<UpdateChecker> checkers = new OneWayList<>();
    private final Collection<DragonAPIMod> oldMods = new OneWayList<>();
    private final Collection<DragonAPIMod> noURLMods = new OneWayList<>();

    private final HashMap<String, DragonAPIMod> modNames = new OneWayMap<>();
    private final HashMap<DragonAPIMod, String> modNamesReverse = new OneWayMap<>();

    private final HashMap<DragonAPIMod, Boolean> overrides = new OneWayMap<>();

    private final Collection<DragonAPIMod> dispatchedOldMods = new ArrayList<>();
    private final Collection<DragonAPIMod> dispatchedURLMods = new ArrayList<>();

    private final HashMap<DragonAPIMod, UpdateHash> hashes = new HashMap<>();

    private CommandableUpdateChecker() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void checkAll() {
        this.getOverrides();
        for (UpdateChecker c : checkers) {
            DragonAPIMod mod = c.mod;
            if (this.shouldCheck(mod)) {
                ModVersion version = c.version;
                ModVersion latest = latestVersions.get(mod);
                //if (version.isCompiled()) {
                if (latest == ModVersion.timeout) {
                    this.markUpdate(mod, version, latest);
                    ReikaJavaLibrary.pConsole("-----------------------" + mod.getTechnicalName() + "-----------------------");
                    ReikaJavaLibrary.pConsole("Could not connect to version server. Please check your internet settings,");
                    ReikaJavaLibrary.pConsole("and if the server is unavailable please contact " + mod.getModAuthorName() + ".");
                    ReikaJavaLibrary.pConsole("------------------------------------------------------------------------");
                } else if (version.compareTo(latest) < 0) {
                    this.markUpdate(mod, version, latest);
                    ReikaJavaLibrary.pConsole("-----------------------" + mod.getTechnicalName() + "-----------------------");
                    ReikaJavaLibrary.pConsole("This version of the mod (" + version + ") is out of date.");
                    ReikaJavaLibrary.pConsole("This version is likely to contain bugs, crashes, and/or exploits.");
                    ReikaJavaLibrary.pConsole("No technical support whatsoever will be provided for this version.");
                    ReikaJavaLibrary.pConsole("Update to " + latest + " as soon as possible; there is no good reason not to.");
                    ReikaJavaLibrary.pConsole("------------------------------------------------------------------------");
                    ReikaJavaLibrary.pConsole("");
                }
                //}
                //else {
                //
                //}
            }
        }
    }

    private void markUpdate(DragonAPIMod mod, ModVersion version, ModVersion latest) {
        if (latest == ModVersion.timeout) {
            noURLMods.add(mod);
        } else {
            oldMods.add(mod);

            CompoundTag nbt = new CompoundTag();
            nbt.putString("modDisplayName", mod.getDisplayName());
            nbt.putString("oldVersion", "v" + version.toString());
            nbt.putString("newVersion", "v" + latest.toString());
            nbt.putString("updateUrl", mod.getDocumentationSite().toString());
            nbt.putBoolean("isDirectLink", false);
            nbt.putString("changeLog", mod.getDocumentationSite().toString());
            InterModComms.sendTo(mod.getModContainer().getModId(), "VersionChecker", "addUpdate", (Supplier<?>) nbt);
        }
    }

    public void registerMod(DragonAPIMod mod) {
        ModVersion version = mod.getModVersion();
        if (version == ModVersion.source) {
            mod.getModLogger().info("Mod is in source code form. Not checking versions.");
            return;
        }
        if (mod.getUpdateCheckURL() == null)
            return;
        String url = mod.getUpdateCheckURL() + "_" + FMLLoader.versionInfo().mcVersion().replaceAll("\\.", "-") + ".txt";
        URL file = this.getURL(url);
        if (file == null) {
            mod.getModLogger().error("Could not create URL to update checker. Version will not be checked.");
            return;
        }
        UpdateChecker c = new UpdateChecker(mod, version, file);
        ModVersion latest = c.getLatestVersion();
        if (latest == null) {
            mod.getModLogger().error("Could not access online version reference. Please notify " + mod.getModAuthorName());
            return;
        }
        latestVersions.put(mod, latest);
        checkers.add(c);
        String label = ReikaStringParser.stripSpaces(mod.getDisplayName().toLowerCase(Locale.ENGLISH));
        modNames.put(label, mod);
        modNamesReverse.put(mod, label);
    }

    private URL getURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean shouldCheck(DragonAPIMod mod) {
        return overrides.containsKey(mod) ? overrides.get(mod) : true;
    }

    private void getOverrides() {
        File f = this.getFile();
        if (f.exists()) {
            boolean deleteFile = false;
            ArrayList<String> li = ReikaFileReader.getFileAsLines(f, true);
            for (int i = 0; i < li.size(); i++) {
                String line = li.get(i);
                String[] parts = line.split(":");
                DragonAPIMod mod = modNames.get(parts[0]);
                boolean b = Boolean.parseBoolean(parts[1]);
                ModVersion version = ModVersion.getFromString(parts[2]);
                if (version == ModVersion.timeout)
                    deleteFile = true;
                else if (version.equals(latestVersions.get(mod)))
                    overrides.put(mod, b);
            }
            if (deleteFile)
                f.delete();
        }
    }

    private void setChecker(DragonAPIMod mod, boolean enable) {
        File f = this.getFile();
        String name = ReikaStringParser.stripSpaces(mod.getDisplayName().toLowerCase(Locale.ENGLISH));
        ModVersion latest = latestVersions.get(mod);
        if (f.exists()) {
            ArrayList<String> li = ReikaFileReader.getFileAsLines(f, true);
            Iterator<String> it = li.iterator();
            while (it.hasNext()) {
                String line = it.next();
                if (line.startsWith(name)) {
                    it.remove();
                }
            }
            li.add(name + ":" + enable + ":" + latest);
            try {
                PrintWriter p = new PrintWriter(f);
                for (int i = 0; i < li.size(); i++)
                    p.append(li.get(i) + "\n");
                p.close();
            } catch (IOException e) {

            }
        } else {
            try {
                f.createNewFile();
                PrintWriter p = new PrintWriter(f);
                p.append(name + ":" + enable + ":" + latest);
                p.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private File getFile() {
        File parent0 = new File(DragonAPI.getMinecraftDirectory(), "saves");
        File parent = new File(parent0, "DragonAPI");
        if (!parent.exists())
            parent.mkdirs();
        return new File(parent, "ucheck.dat");
    }

    public void notifyPlayer(Player ep) {
        if (!oldMods.isEmpty() || !noURLMods.isEmpty()) {
            this.sendMessages(ep);
        }

        if (ep instanceof ServerPlayer) {
            PacketTarget pt = new PacketTarget.PlayerTarget((ServerPlayer) ep);
            for (DragonAPIMod mod : oldMods) {
                if (this.beAggressive(mod, (ServerPlayer) ep)) {
                    ReikaPacketHelper.sendStringPacket(DragonAPI.packetChannel, PacketIDs.OLDMODS.ordinal(), modNamesReverse.get(mod), pt);
                }
            }
            for (DragonAPIMod mod : noURLMods) {
                ReikaPacketHelper.sendStringPacket(DragonAPI.packetChannel, PacketIDs.OLDMODS.ordinal(), "URL_" + modNamesReverse.get(mod), pt);
            }
        }
    }

    private boolean beAggressive(DragonAPIMod mod, ServerPlayer ep) {
        boolean abandonedPack = latestVersions.get(mod).majorVersion - mod.getModVersion().majorVersion > 1;
        if (!abandonedPack && DragonOptions.PACKONLYUPDATE.getState()) {
            return this.isPackMaker(mod, ep);
        } else if (DragonOptions.OPONLYUPDATE.getState()) {
            return DragonAPI.isSinglePlayer() || ReikaPlayerAPI.isAdmin(ep);
        }
        return true;
    }

    private boolean isPackMaker(DragonAPIMod mod, ServerPlayer ep) {
        UpdateHash test = this.genHash(mod, ep);
        UpdateHash get = this.getOrCreateHash(mod, ep);
        return get.equals(test);
    }

    private UpdateHash getOrCreateHash(DragonAPIMod mod, Player ep) {
        UpdateHash uh = hashes.get(mod);
        if (uh == null) {
            uh = this.readHash(mod);
            if (uh == null) {
                uh = this.genHash(mod, ep);
                this.writeHash(mod, uh);
            }
            hashes.put(mod, uh);
        }
        return uh;
    }

    private UpdateHash readHash(DragonAPIMod mod) {
        File f = this.getHashFile();
        ArrayList<String> data = ReikaFileReader.getFileAsLines(f, true);
        for (String s : data) {
            String tag = mod.getDisplayName() + "=";
            if (s.startsWith(tag)) {
                return UpdateHash.decode(s.substring(tag.length()));
            }
        }
        return null;
    }

    private void writeHash(DragonAPIMod mod, UpdateHash uh) {
        File f = this.getHashFile();
        ArrayList<String> data = ReikaFileReader.getFileAsLines(f, true);
        String tag = mod.getDisplayName() + "=";
        data.add(tag + uh.toString());
        try {
            BufferedReader r = new BufferedReader(new FileReader(f));
            String sep = System.getProperty("line.separator");
            String line = r.readLine();
            StringBuilder out = new StringBuilder();
            for (String l : data) {
                out.append(l + sep);
            }
            r.close();
            FileOutputStream os = new FileOutputStream(f);
            os.write(out.toString().getBytes());
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File getHashFile() {
        File parent0 = new File(DragonAPI.getMinecraftDirectory(), "config");
        File parent = new File(parent0, "Reika");
        if (!parent.exists())
            parent.mkdirs();
        File f = new File(parent, "versions.dat");
        try {
            if (!f.exists())
                f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return f;
    }

    private UpdateHash genHash(DragonAPIMod mod, Player ep) {
        return new UpdateHash(ep.getUUID(), ReikaFileReader.getRealPath(mod.getModContainer().getModInfo().getOwningFile().getFile().getFilePath().toFile()), System.currentTimeMillis()); //todo get real path
    }


    public void onClientLogin(ClientLoginEvent evt) {
        this.genHashes(evt.player);
    }

    private void genHashes(Player ep) {
        for (DragonAPIMod mod : latestVersions.keySet()) {
            this.getOrCreateHash(mod, ep);
        }
    }


    public void onClientReceiveOldModID(String s) {
        Collection<DragonAPIMod> c = s.startsWith("URL_") ? dispatchedURLMods : dispatchedOldMods;
        if (s.startsWith("URL_"))
            s = s.substring(4);
        DragonAPIMod mod = modNames.get(s);
        if (!c.contains(mod))
            c.add(mod);
    }

    @SubscribeEvent

    public void onClientReceiveOldModsNote(ClientLoginEvent evt) {
        if (evt.newLogin) {
            ArrayList<String> li = new ArrayList<>();
            for (DragonAPIMod mod : dispatchedOldMods) {
                String sb = mod.getDisplayName() +
                        " " +
                        mod.getModVersion() +
                        " is out of date. Update to " +
                        latestVersions.get(mod).toString() +
                        " as soon as possible.";
                li.add(sb);
            }
            for (DragonAPIMod mod : dispatchedURLMods) {
                String sb = mod.getDisplayName() +
                        " could not verify its version; the version server may be inaccessible. Check your internet settings, and please notify " +
                        mod.getModAuthorName() +
                        " if the server is not accessible.";
                li.add(sb);
            }
            for (String s : li) {
                PopupWriter.instance.addMessage(s);
            }
        }
    }

    private void sendMessages(Player ep) {
        String sg = ChatFormatting.YELLOW + "DragonAPI Notification:";
        ReikaChatHelper.sendChatToPlayer(ep, sg);
        for (DragonAPIMod mod : oldMods) {
            String s = this.getChatMessage(mod);
            ReikaChatHelper.sendChatToPlayer(ep, s);
        }
        String g = ChatFormatting.YELLOW.toString();
        String sg2 = g + "To disable this notifcation for any mod, type \"/" + "checker" + " disable [modname]\".";
        ReikaChatHelper.sendChatToPlayer(ep, sg2);
        sg2 = g + "Changes take effect upon server or client restart.";
        ReikaChatHelper.sendChatToPlayer(ep, sg2);
    }

    private String getChatMessage(DragonAPIMod mod) {
        ModVersion latest = latestVersions.get(mod);
        String g = ChatFormatting.LIGHT_PURPLE.toString();
        String r = ChatFormatting.RESET.toString();
        return g + mod.getDisplayName() + r + " is out of date, likely has errors, and is no longer supported. Update to " + latest + ".";
    }

    public static class CheckerDisableCommand {

        public void processCommand(CommandDispatcher<CommandSourceStack> dispatcher, String[] args) {
            dispatcher.register(Commands.literal("checker").executes((context) -> {
                if (args.length == 2) {
                    String action = args[0];
                    String name = args[1].toLowerCase(Locale.ENGLISH);
                    DragonAPIMod mod = instance.modNames.get(name);
                    if (mod != null) {
                        if (action.equals("disable")) {
                            instance.setChecker(mod, false);
                            String sg = ChatFormatting.BLUE + "Update checker for " + mod.getDisplayName() + " disabled.";
                            ReikaChatHelper.sendChatToPlayer(context.getSource().getPlayerOrException(), sg);
                        } else if (action.equals("enable")) {
                            instance.setChecker(mod, true);
                            String sg = ChatFormatting.BLUE + "Update checker for " + mod.getDisplayName() + " enabled.";
                            ReikaChatHelper.sendChatToPlayer(context.getSource().getPlayerOrException(), sg);
                        } else {
                            String sg = ChatFormatting.RED + "Invalid argument '" + action + "'.";
                            ReikaChatHelper.sendChatToPlayer(context.getSource().getPlayerOrException(), sg);
                        }
                    } else {
                        String sg = ChatFormatting.RED + "Mod '" + name + "' not found.";
                        ReikaChatHelper.sendChatToPlayer(context.getSource().getPlayerOrException(), sg);
                    }
                } else {
                    String sg = ChatFormatting.RED + "Invalid arguments.";
                    ReikaChatHelper.sendChatToPlayer(context.getSource().getPlayerOrException(), sg);
                }
                return 1;
            }));
        }
    }
        private static class UpdateChecker implements ConnectionErrorHandler, DataFetcher {

            private final ModVersion version;
            private final URL checkURL;
            private final DragonAPIMod mod;
            private Date modified;

            private UpdateChecker(DragonAPIMod mod, ModVersion version, URL url) {
                this.mod = mod;
                this.version = version;
                checkURL = url;
            }

            private ModVersion getLatestVersion() {
                try {
                    ArrayList<String> lines = ReikaFileReader.getFileAsLines(checkURL, 10000, false, this, this);
                    if (lines == null || lines.isEmpty())
                        throw new VersionNotLoadableException("File was empty or null");
                    String name = ReikaStringParser.stripSpaces(mod.getDisplayName().toLowerCase(Locale.ENGLISH));
                    for (String line : lines) {
                        if (line.toLowerCase().startsWith(name)) {
                            String[] parts = line.split(":");
                            ModVersion version = ModVersion.getFromString(parts[1]);
                            return version;
                        }
                    }
                } catch (VersionNotLoadableException e) {
                    return ModVersion.timeout;
                } catch (Exception e) {
                    this.error(e);
                }
                return null;
            }

            private void error(Exception e) {
                if (e instanceof IOException) {
                    mod.getModLogger().error("IO Error accessing online file:");
                    mod.getModLogger().info(e.getClass().getCanonicalName() + ": " + e.getLocalizedMessage());
                    mod.getModLogger().info(e.getStackTrace()[0].toString());
                } else {
                    mod.getModLogger().error("Error accessing online file:");
                    e.printStackTrace();
                }
            }

            @Override
            public void onServerRedirected() {
                throw new VersionNotLoadableException("Version server not found!");
            }

            @Override
            public void onNoInternet() {
                mod.getModLogger().error("Error accessing online file: Is your internet disconnected?");
            }

            @Override
            public void onServerNotFound() {
                throw new VersionNotLoadableException("Version server not found!");
            }

            @Override
            public void onTimedOut() {
                mod.getModLogger().error("Error accessing online file: Timed Out");
            }

            @Override
            public void fetchData(URLConnection c) throws Exception {
                String lastModified = c.getHeaderField("Last-Modified");
                modified = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH).parse(lastModified);
            }
        }

        private static class UpdateHash {

            private final long timestamp;
            private final String filepath;
            private final UUID player;

            private UpdateHash(UUID id, String file, long time) {
                player = id;
                filepath = file;
                timestamp = time;
            }

            @Override
            public boolean equals(Object o) {
                if (o instanceof UpdateHash uh) {
                    return uh.player == player && uh.filepath.equals(player);
                }
                return false;
            }

            @Override
            public String toString() {
                String time = String.valueOf(timestamp);
                String id = player.toString();
                StringBuffer sb = new StringBuffer();
                int idx = 0;
                while (idx < time.length() || idx < id.length() || idx < filepath.length()) {
                    long c1 = idx >= time.length() ? '*' : time.charAt(idx);
                    long c2 = idx >= id.length() ? '*' : id.charAt(idx);
                    long c3 = idx >= filepath.length() ? '*' : filepath.charAt(idx);
                    long sum = c1 | (c2 << 16) | (c3 << 32);
                    idx++;
                    //ReikaJavaLibrary.pConsole(c1+" & "+c2+" & "+c3+" > "+sum+" $ "+this.getStringForInt(sum));
                    sb.append(getStringForInt(sum) + ":");
                }
                //ReikaJavaLibrary.pConsole("Final: "+time+" & "+id+" & "+filepath+" > "+sb.toString());
                return sb.toString();
            }

            public static UpdateHash decode(String s) {
                StringBuilder path = new StringBuilder();
                StringBuilder id = new StringBuilder();
                StringBuilder time = new StringBuilder();
                String[] parts = s.split(":");
                for (int i = 0; i < parts.length; i++) {
                    String p = parts[i];
                    long dat = getIntForString(p);
                    char c1 = (char) (dat & 65535);
                    char c2 = (char) ((dat >> 16) & 65535);
                    char c3 = (char) ((dat >> 32) & 65535);
                    //ReikaJavaLibrary.pConsole(c1+" & "+c2+" & "+c3+" < "+dat+" $ "+p);
                    if (c1 != '*')
                        time.append(c1);
                    if (c2 != '*')
                        id.append(c2);
                    if (c3 != '*')
                        path.append(c3);
                }
                //ReikaJavaLibrary.pConsole("Final: "+time+" & "+id+" & "+path+" < "+s);
                return new UpdateHash(UUID.fromString(id.toString()), path.toString(), Long.parseLong(time.toString()));
            }

            private static String getStringForInt(long l) {
                return Long.toString(l, 36);
            }

            private static long getIntForString(String s) {
                return Long.parseLong(s, 36);
            }

            private static final char[] chars = {
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                    'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
                    'k', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u',
                    'v', 'w', 'x', 'y', 'z', '~', '`', '+', '-', '=',
                    '!', '@', '#', '$', '%', '^', '&', '*', '(', ')',
                    '[', ']', '{', '}', ';', ':', '<', '>', ',', '.',
            };

        }

        private static class VersionNotLoadableException extends RuntimeException {

            public VersionNotLoadableException(String s) {
                super(s);
            }

        }

    }
