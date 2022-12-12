package reika.dragonapi.instantiable.event;


import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import reika.dragonapi.instantiable.data.maps.MultiMap;

import java.util.Collection;

/**
 * Generally not used for actual profiling handling, but for the massive number of hooks it provides into vanilla code. Check the profiler's calls
 * to see potential uses.
 */
public class ProfileEvent {

    private static final MultiMap<String, ProfileEventWatcher> watchedTags = new MultiMap<String, ProfileEventWatcher>().setNullEmpty();

    public static void fire(String tag) {
        Collection<ProfileEventWatcher> c = watchedTags.get(tag);
        if (c != null) {
            for (ProfileEventWatcher p : c) {
                p.onCall(tag);
            }
        }
        MinecraftForge.EVENT_BUS.post(new ProfileEventObject(tag));
    }

    public static void registerHandler(String tag, ProfileEventWatcher w) {
        watchedTags.addValue(tag, w);
    }

    public interface ProfileEventWatcher {

        void onCall(String tag);

    }

    private static class ProfileEventObject extends Event {
        public final String sectionName;

        public ProfileEventObject(String s) {
            sectionName = s;
        }
    }
}