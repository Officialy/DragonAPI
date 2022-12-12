package reika.dragonapi;

import reika.dragonapi.auxiliary.trackers.RemoteAssetLoader;

import java.io.File;
import java.util.Collection;

public class MusicLoader {

    public static final MusicLoader instance = new MusicLoader();

    private static final File mcDir = DragonAPI.getMinecraftDirectory();

    private static final String hashURL = "https://cache.techjargaming.com/reika/list.php?dir=ccmusic/";
    //private static final String musicURL = "https://cache.techjargaming.com";

    public static final File musicPath = new File(mcDir, "mods/Reika/ChromatiCraft/Music");

    private final MusicFolder folder = new MusicFolder();

    private MusicLoader() {

    }

    public void registerAssets() {
        RemoteAssetLoader.instance.registerAssets(folder);
    }

    private static class MusicFolder extends RemoteAssetLoader.RemoteAssetRepository {

        private MusicFolder() {
            super(DragonAPI.instance);
        }

        @Override
        public String getRepositoryURL() {
            return hashURL;
        }

        @Override
        protected RemoteAssetLoader.RemoteAsset parseAsset(String line) {
            return new MusicAsset(this);
        }

        @Override
        public String getDisplayName() {
            return "ChromatiCraft Dimension Music";
        }

        private Collection<RemoteAssetLoader.RemoteAsset> getMusicAssets() {
            return this.getAssets();
        }

        @Override
        public File getLocalStorageFolder() {
            return musicPath;
        }
    }

    private static class MusicAsset extends RemoteAssetLoader.RemoteAsset {

        private String track;

        private MusicAsset(MusicFolder f) {
            super(DragonAPI.instance, f);
        }

        @Override
        protected RemoteAssetLoader.AssetData constructData(String line) {
            String[] parts = line.split("\\|");
            String path = parts[0];
            String hash = parts[1];
            String size = parts[2];
            String name = path.substring(path.lastIndexOf('/')+1, path.length()-4);
            return new RemoteAssetLoader.AssetData(this, path, name, hash, Long.parseLong(size));
        }

        @Override
        public String getDisplayName() {
            return "Music Track '"+track+"'";
        }

        @Override
        public String setFilename(String line) {
            String[] parts = line.split("\\|");
            String path = parts[0];
            String name = path.substring(path.lastIndexOf('/')+1, path.length()-4);
            track = name;
            return track;
        }

        @Override
        public String setExtension(String line) {
            String[] parts = line.split("\\|");
            String path = parts[0];
            String name = path.substring(path.lastIndexOf('/')+1, path.length()-4);
            return path.substring(path.length()-3, path.length());
        }
    }

    public Collection<String> getMusicFiles() {
        return folder.getAvailableResources();
    }
}
