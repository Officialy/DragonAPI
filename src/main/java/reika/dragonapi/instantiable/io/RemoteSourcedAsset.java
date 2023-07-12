package reika.dragonapi.instantiable.io;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.auxiliary.trackers.RemoteAssetLoader;
import reika.dragonapi.base.DragonAPIMod;
import reika.dragonapi.io.ReikaFileReader;
import reika.dragonapi.libraries.mathsci.ReikaDateHelper;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

public class RemoteSourcedAsset {

    private static final File mcDir = DragonAPI.getMinecraftDirectory();
    public final String reference;
    public final String path;
    public final String remotePath;
    public final String localRemote;

    private RemoteSourcedAsset(String ref, String s, String rem, String loc) {
        reference = ref;
        path = s;
        remotePath = rem;
        localRemote = loc;
    }

    /**
     * Make sure you close this!
     */
    public InputStream getData() throws IOException {
        InputStream main = this.getPrimary();
        if (main != null) {
            return main;
        }
        File f = new File(this.getLocalAssetPath());
        if (f.exists()) {
            return new FileInputStream(f);
        }
        String fall = this.getFallbackPath();
        f = new File(fall);
        if (f.exists()) {
            return new FileInputStream(f);
        } else {
            DragonAPI.LOGGER.error("Could not find main resource for asset " + reference + "/" + path + "!");
            InputStream in = Minecraft.getInstance().getResourceManager().getResource(new ResourceLocation(reference, fall)).get().open(); //todo attempt for getting resource inputstream from pack
            if (in != null)
                return in;
            DragonAPI.LOGGER.error("Could not find ANY resource for asset " + reference + "/" + path + "!");
            return null;
        }
    }

    private InputStream getPrimary() throws IOException {
        return Minecraft.getInstance().getResourceManager().getResource(new ResourceLocation(reference, path)).get().open(); //todo attempt for getting resource inputstream from pack
    }

    public void load() {
        try (InputStream main = this.getPrimary()) {
            if (main != null) {
                return;
            }
            File f = new File(this.getLocalAssetPath());
            if (!f.exists()) {
                DragonAPI.LOGGER.info("Downloading dynamic asset " + path + " from remote, as its local copy does not exist.");
                this.queueDownload();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void queueDownload() {
        DynamicAssetDownloader al = new DynamicAssetDownloader(this.getRemotePath(), this.getLocalAssetPath());
        new Thread(al, "Dynamic Asset Download " + path).start();
    }

    private String getFileExt() {
        return path.substring(path.lastIndexOf('.'));
    }

    private String getFilename() {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    public String getRemotePath() {
        return remotePath + "/" + path;//this.getFilename();
    }

    private String getLocalAssetPath() {
        return mcDir + "/mods/" + localRemote + "/" + path;
    }

    private String getFallbackPath() {
        String ext = this.getFileExt();
        String main = path.substring(0, path.length() - ext.length());
        return main + "_fallback" + ext;
    }

    private static class DynamicAssetDownloader implements Runnable {

        private final String remotePath;
        private final String localPath;
        private final File targetFile;

        private boolean isComplete = false;

        private DynamicAssetDownloader(String rem, String loc) {
            remotePath = rem;
            localPath = loc;
            targetFile = new File(localPath);
        }

        @Override
        public void run() {
            long time = System.currentTimeMillis();
            DragonAPI.LOGGER.info("Remote asset download thread starting...");
            this.tryDownload(5);
            //MinecraftForge.EVENT_BUS.post(new RemoteAssetsDownloadCompleteEvent(instance.downloadingAssets, totalSize));
        }

        private void tryDownload(int max) {
            for (int i = 0; i < max; i++) {
                try {
                    this.download();
                    break;
                } catch (ReikaFileReader.FileReadException e) {
                    boolean end = i == max - 1;
                    String text = end ? "Skipping file." : "Retrying...";
                    /*dat.asset.mod.getModLogger()*/
                    DragonAPI.LOGGER.error("Could not read remote asset '" + remotePath + "'. " + text);
                    e.printStackTrace();
                    targetFile.delete();
                    if (end)
                        break;
                } catch (ReikaFileReader.FileWriteException e) {
                    /*dat.asset.mod.getModLogger()*/
                    DragonAPI.LOGGER.error("Could not save asset '" + localPath + "'. Skipping file.");
                    e.printStackTrace();
                    targetFile.delete();
                    break;
                } catch (IOException e) {
                    /*dat.asset.mod.getModLogger()*/
                    DragonAPI.LOGGER.error("Could not download remote asset '" + remotePath + "'. Skipping file.");
                    e.printStackTrace();
                    targetFile.delete();
                    break;
                }
            }
        }

        private void download() throws IOException {
            if (!ReikaFileReader.isFileWithin(targetFile, DragonAPI.getMinecraftDirectory())) {
                String s = "Dynamic Remote Asset " + remotePath + " attempted to download to " + targetFile.getCanonicalPath() + "!" +
                        " This is not in the MC directory and very likely either malicious or poorly implemented, or the remote server has been compromised!";
                DragonAPI.LOGGER.error(s);
                return;
                //throw new RuntimeException(s);
            }
            targetFile.getParentFile().mkdirs();
            targetFile.delete();
            targetFile.createNewFile();
            URLConnection c = new URL(remotePath).openConnection();
            InputStream in = c.getInputStream();
            OutputStream out = new FileOutputStream(targetFile);

            long time = System.currentTimeMillis();
            ReikaFileReader.copyFile(in, out, 4096);
            long duration = System.currentTimeMillis() - time;

            String s = "Download of '" + remotePath + "' to '" + localPath + "' complete. Elapsed time: " + ReikaDateHelper.millisToHMSms(duration) + ". Filesize: " + targetFile.length();
            /*dat.asset.mod.getModLogger()*/
            DragonAPI.LOGGER.info(s);
            isComplete = true;

            in.close();
            out.close();
        }

    }

    public static final class RemoteSourcedAssetRepository {

        public final String rootClass;
        public final String rootPath;
        public final String rootRemote;
        public final String rootLocal;

        public final DragonAPIMod owner;
//        private final RemoteAssetLoader.RemoteAssetRepository repository;

        public RemoteSourcedAssetRepository(DragonAPIMod mod, String c, String r, String l) {
            this(mod, c, "", r, l);
        }

        public RemoteSourcedAssetRepository(DragonAPIMod mod, String c, String p, String r, String l) {
            rootClass = c;
            rootLocal = l;
            rootRemote = r;
            rootPath = p;

            owner = mod;
//            repository = new DynamicRemoteAssetRepository();
        }

        public RemoteSourcedAsset createAsset(String file) {
            RemoteSourcedAsset rem = new RemoteSourcedAsset(rootClass, rootPath.isEmpty() ? file : rootPath + "/" + file, rootRemote, rootLocal);
            rem.load();
            return rem;
        }

        public void addToAssetLoader() {
//            RemoteAssetLoader.instance.registerAssets(repository);
        }

    }

}
