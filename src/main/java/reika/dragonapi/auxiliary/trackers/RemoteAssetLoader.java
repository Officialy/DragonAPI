/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.auxiliary.trackers;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.auxiliary.PopupWriter;
import reika.dragonapi.base.DragonAPIMod;
import reika.dragonapi.instantiable.event.client.ClientLoginEvent;
import reika.dragonapi.io.ReikaFileReader;
import reika.dragonapi.libraries.java.ReikaStringParser;
import reika.dragonapi.libraries.mathsci.ReikaDateHelper;

import javax.swing.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

@Mod.EventBusSubscriber(modid = DragonAPI.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class RemoteAssetLoader {

    public static final RemoteAssetLoader instance = new RemoteAssetLoader();

    private final ArrayList<RemoteAsset> downloadingAssets = new ArrayList<>();
    private final ArrayList<BigWarning> bigWarnings = new ArrayList<>();
    private AssetDownloader downloader;
    private Thread downloadThread;

    private RemoteAssetLoader() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void registerAssets(RemoteAssetRepository rar) {
        rar.load();
        for (RemoteAsset a : rar.getAssets()) {
            if (a.requiresDownload)
                downloadingAssets.add(a);
        }
    }

    public void checkAndStartDownloads() {
        if (!downloadingAssets.isEmpty()) {
            DragonAPI.LOGGER.info("DRAGONAPI: Some remote assets need to be redownloaded:");
            downloader = new AssetDownloader();
            for (RemoteAsset a : downloadingAssets) {
                a.log("Remote Asset '" + a.getDisplayName() + "' for " + a.mod.getDisplayName() + " is either missing or out of date. Redownloading...");
                downloader.totalSize += a.data.size;
            }
            DragonAPI.LOGGER.info("Projected total download size: " + downloader.totalSize + " bytes in " + downloadingAssets.size() + " files.");
            downloadThread = new Thread(downloader, "Remote Asset Download");
            downloadThread.start();
        } else {
            MinecraftForge.EVENT_BUS.post(new RemoteAssetsDownloadCompleteEvent(downloadingAssets, 0));
        }
    }

    public float getDownloadProgress() {
        return downloader != null ? Math.min(1, downloader.getTotalCompletion()) : 1F;
    }

    public float getCurrentFileProgress() {
        return downloader != null ? Math.min(1, downloader.getCurrentFileCompletion()) : 1F;
    }

    public boolean isDownloadComplete() {
        return downloadThread == null || !downloadThread.isAlive() || downloader == null || downloader.isComplete;
    }

    @SubscribeEvent
    public void onClientReceiveWarning(ClientLoginEvent evt) {
        for (BigWarning w : bigWarnings) {
            String sg = w.message + " the file server for remote asset repository '" + w.repository.getDisplayName() + "' may be inaccessible. Check your internet settings, and please notify " + w.repository.mod.getModAuthorName() + " if the server is not accessible.";
            PopupWriter.instance.addMessage(sg);
        }
    }

    private static class AssetDownloader implements Runnable, ReikaFileReader.WriteCallback {

        private long totalSize = 0;
        private long downloaded = 0;

        private RemoteAsset activeAsset;
        private long currentDownload;

        private boolean isComplete = false;

        @Override
        public void run() {
            long time = System.currentTimeMillis();
            DragonAPI.LOGGER.info("DRAGONAPI: Remote asset download thread starting...");
            for (RemoteAsset a : instance.downloadingAssets) {
                activeAsset = a;
                currentDownload = 0;
                this.tryDownload(a.data, 5);
            }
            long duration = System.currentTimeMillis() - time;
            DragonAPI.LOGGER.info("DRAGONAPI: All asset downloads complete. Elapsed time: " + ReikaDateHelper.millisToHMSms(duration));
            isComplete = true;
            MinecraftForge.EVENT_BUS.post(new RemoteAssetsDownloadCompleteEvent(instance.downloadingAssets, totalSize));
        }

        public float getTotalCompletion() {
            return (float) downloaded / totalSize;
        }

        public float getCurrentFileCompletion() {
            return (float) currentDownload / activeAsset.data.size;
        }

        private void tryDownload(AssetData dat, int max) {
            for (int i = 0; i < max; i++) {
                try {
                    this.download(dat);
                    break;
                } catch (ReikaFileReader.FileReadException e) {
                    boolean end = i == max - 1;
                    String text = end ? "Skipping file." : "Retrying...";
                    /*dat.asset.mod.getModLogger()*/
                    DragonAPI.LOGGER.error("DRAGONAPI: Could not read remote asset '" + dat.getDisplayName() + "'. " + text);
                    e.printStackTrace();
                    if (end) {
                        dat.asset.parent.error(e.getLocalizedMessage(), true);
                        break;
                    }
                } catch (ReikaFileReader.FileWriteException e) {
                    /*dat.asset.mod.getModLogger()*/
                    DragonAPI.LOGGER.error("DRAGONAPI: Could not save asset '" + dat.getDisplayName() + "'. Skipping file.");
                    e.printStackTrace();
                    break;
                } catch (IOException e) {
                    /*dat.asset.mod.getModLogger()*/
                    DragonAPI.LOGGER.error("DRAGONAPI: Could not download remote asset '" + dat.getDisplayName() + "'. Skipping file.");
                    e.printStackTrace();
                    break;
                }
            }
        }

        private void download(AssetData dat) throws IOException {
            File f = dat.asset.getLocalPath();
            if (!ReikaFileReader.isFileWithin(f, DragonAPI.getMinecraftDirectory())) {
                String s = "Remote Asset " + dat.asset.getDisplayName() + " attempted to download to " + f.getCanonicalPath() + "!" +
                        " This is not in the MC directory and very likely either malicious or poorly implemented, or the remote server has been compromised!";
                dat.asset.parent.error(s, true);
                return;
                //throw new RuntimeException(s);
            }
            f.getParentFile().mkdirs();
            f.delete();
            f.createNewFile();
            URLConnection c = new URL(dat.path).openConnection();
            InputStream in = c.getInputStream();
            OutputStream out = new FileOutputStream(f);

            long time = System.currentTimeMillis();
            ReikaFileReader.copyFile(in, out, 4096, this);
            long duration = System.currentTimeMillis() - time;

            String s = "DRAGONAPI: Download of '" + dat.getDisplayName() + "' to '" + dat.asset.getLocalPath() + "' complete. Elapsed time: " + ReikaDateHelper.millisToHMSms(duration);
            //dat.asset.mod.getModLogger()
            DragonAPI.LOGGER.info(s);
            DragonAPI.LOGGER.info("DRAGONAPI: Remote asset downloads now " + String.format("%.2f", Math.min(100, this.getTotalCompletion() * 100)) + "% complete.");
            dat.asset.downloaded = true;

            in.close();
            out.close();
        }

        @Override
        public void onWrite(byte[] data) {
            downloaded += data.length;
            currentDownload += data.length;
        }

    }

    public static class AssetData {

        protected final RemoteAsset asset;
        protected final String name;
        protected final String path;
        protected final long size;
        protected final String hash;

        public AssetData(RemoteAsset a, String p, String n, String h, long s) {
            asset = a;
            name = n;
            path = p;
            size = s;
            hash = h;
        }

        private String getLocalHash() {
            File f = asset.getLocalPath();
            return f.exists() ? ReikaFileReader.getHash(f, ReikaFileReader.HashType.MD5) : "";
        }

        protected boolean match() {
            return this.getLocalHash().equalsIgnoreCase(hash);
        }

        public String getDisplayName() {
            return ReikaStringParser.capFirstChar(name);
        }

    }

    public static abstract class RemoteAssetRepository implements ReikaFileReader.ConnectionErrorHandler {

        private final Collection<RemoteAsset> assets = new ArrayList<>();
        private final DragonAPIMod mod;
        private boolean nonAccessible;

        protected RemoteAssetRepository(DragonAPIMod mod) {
            this.mod = mod;
        }

        private final void load() {
            URL url = null;
            try {
                url = URI.create(this.getRepositoryURL()).toURL();
            } catch (MalformedURLException e) {
                this.error("Asset Repository URL invalid", true);
                e.printStackTrace();
                return;
            }
            ArrayList<String> li = ReikaFileReader.getFileAsLines(url, 10000, true, this, null);
            if (li == null) {
                if (!nonAccessible)
                    this.error("Could not load asset repository", true);
                return;
            }
            for (String s : li) {
                RemoteAsset a = this.parseAsset(s);
                if (a != null) {
                    assets.add(a);
                    a.filename = a.setFilename(s);
                    a.extension = a.setExtension(s);
                    a.data = a.constructData(s);
                    a.requiresDownload = !a.data.match();
                }
            }
            this.writeList();
            DragonAPI.LOGGER.info(assets.size() + " remote assets for " + mod.getDisplayName() + " found at " + this.getDisplayName() + ": " + assets);
        }

        private void writeList() {
            try {
                File f = new File(this.getLocalStorageFolder(), "file_list.dat");
                f.mkdirs();
                f.delete();
                f.createNewFile();
                ArrayList<String> li = new ArrayList<>();
                li.add("File list for remote asset repository '" + this.getDisplayName() + "'");
                li.add("Downloaded from " + this.getRepositoryURL() + " to " + this.getLocalStorageFolder());
                int n = li.get(li.size() - 1).length();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < n; i++) {
                    sb.append("=");
                }
                li.add(sb.toString());
                for (RemoteAsset a : assets) {
                    li.add(a.getDisplayName() + " -> " + a.getLocalPath() + " {Size=" + a.data.size + " B,  Hash=" + a.data.hash + "}");
                }
                ReikaFileReader.writeLinesToFile(f, li, true);
                DragonAPI.LOGGER.info("Writing file list for remote asset repository '" + this.getDisplayName() + "' to disk.");
            } catch (IOException e) {
                DragonAPI.LOGGER.error("Remote asset repository '" + this.getDisplayName() + "' could not save its file list to disk.");
                e.printStackTrace();
            }
        }

        protected abstract RemoteAsset parseAsset(String line);

        protected final Collection<RemoteAsset> getAssets() {
            return Collections.unmodifiableCollection(assets);
        }

        public final Collection<String> getAvailableResources() {
            File file = new File(this.getLocalStorageFolder(), "file_list.dat");
            ArrayList<String> ret = new ArrayList<>();
            if (!file.exists()) {
                return ret;
            }
            ArrayList<String> li = ReikaFileReader.getFileAsLines(file, true, StandardCharsets.UTF_8);
            for (String s : li) {
                int idx = s.indexOf('>');
                int idx2 = s.indexOf('{');
                if (idx >= 0 && idx2 >= idx) {
                    String p = s.substring(idx + 2, idx2 - 1);
                    File f = new File(p);
                    if (f.exists())
                        ret.add(p);
                }
            }
            return ret;
        }

        public abstract String getRepositoryURL();

        public abstract File getLocalStorageFolder();

        @Override
        public final void onServerRedirected() {
            this.error("Asset Server access redirected!?", true);
        }

        @Override
        public final void onNoInternet() {
            this.error("Is your internet disconnected?", false);
        }

        @Override
        public final void onServerNotFound() {
            this.error("Asset Server not found!", true);
        }

        @Override
        public final void onTimedOut() {
            this.error("Timed Out", false);
        }

        private final void error(String msg, boolean bigWarn) {
            nonAccessible = true;
            /*mod.getModLogger()*/
            DragonAPI.LOGGER.error("DRAGONAPI: Error accessing online asset data file: " + msg);
            if (bigWarn) {
                instance.bigWarnings.add(new BigWarning("DRAGONAPI: Downloading the remote assets failed: " + msg, this));
            }
        }

        public abstract String getDisplayName();

        @Override
        public final String toString() {
            return this.getDisplayName() + ": " + assets.size() + "x" + assets;
        }

    }

    public static abstract class RemoteAsset {

        private final DragonAPIMod mod;
        private final RemoteAssetRepository parent;

        private String filename;
        private String extension;
        private boolean requiresDownload;
        private AssetData data;
        private boolean downloaded;

        protected RemoteAsset(DragonAPIMod mod, RemoteAssetRepository rar) {
            this.mod = mod;
            parent = rar;
        }

        public abstract String setFilename(String line);

        public abstract String setExtension(String line);

        public abstract String getDisplayName();

        public final File getLocalPath() {
            return new File(parent.getLocalStorageFolder(), filename + "." + extension);
        }

        @Override
        public final String toString() {
            return this.getDisplayName();
        }

        protected final void log(String s) {
            /*mod.getModLogger()*/
            DragonAPI.LOGGER.info("DRAGONAPI: " + s);
        }

        protected abstract AssetData constructData(String line);

        public final boolean downloadedSuccessfully() {
            return downloaded;
        }

        public final boolean available() {
            return !requiresDownload || this.downloadedSuccessfully();
        }

    }

    private static class BigWarning {

        private final String message;
        private final RemoteAssetRepository repository;

        private BigWarning(String msg, RemoteAssetRepository rar) {
            message = msg;
            repository = rar;
        }

    }

    public static class RemoteAssetsDownloadCompleteEvent extends Event {

        /**
         * If empty, this event is fired much earlier and all files were already present.
         */
        public final Collection<RemoteAsset> downloadQueue;
        public final long totalSize;

        private RemoteAssetsDownloadCompleteEvent(ArrayList<RemoteAsset> li, long size) {
            downloadQueue = Collections.unmodifiableCollection(li);
            totalSize = size;
        }

    }

    public static class DownloadDisplayWindow extends JOptionPane {
        private DownloadDisplayWindow() {
            //super(message, INFORMATION_MESSAGE, DEFAULT_OPTION, null, null, null);
        }
    }

}
