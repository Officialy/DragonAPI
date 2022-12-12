package reika.dragonapi.interfaces.callbacks;

import java.util.Comparator;

public class EventWatchers {

    public static final Comparator<EventWatcher> comparator = new Comparator<EventWatcher>() {

        @Override
        public int compare(EventWatcher o1, EventWatcher o2) {
            return Integer.compare(o1.watcherSortIndex(), o2.watcherSortIndex());
        }

    };

    public interface EventWatcher {

        int watcherSortIndex();

    }

}
