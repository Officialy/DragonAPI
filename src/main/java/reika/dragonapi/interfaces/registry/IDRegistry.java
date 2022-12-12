package reika.dragonapi.interfaces.registry;

import reika.dragonapi.interfaces.configuration.MatchingConfig;

public interface IDRegistry extends MatchingConfig {

    int getDefaultID();

    String getCategory();

}
