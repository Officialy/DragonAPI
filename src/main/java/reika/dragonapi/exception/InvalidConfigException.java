package reika.dragonapi.exception;

import net.minecraftforge.common.ForgeConfigSpec;
import reika.dragonapi.base.DragonAPIMod;
import reika.dragonapi.instantiable.io.oldforge.Property;
import reika.dragonapi.interfaces.configuration.BoundedConfig;
import reika.dragonapi.interfaces.configuration.ConfigList;

public class InvalidConfigException extends UserErrorException {

    public InvalidConfigException(DragonAPIMod mod, ConfigList cfg, String bounds, Property p) {
        this(mod, cfg.getLabel(), bounds, p.getString());
    }

    public InvalidConfigException(DragonAPIMod mod, BoundedConfig cfg, Property p) {
        this(mod, cfg.getLabel(), cfg.getBoundsAsString(), p.getString());
    }

    private InvalidConfigException(DragonAPIMod mod, String name, String bounds, String val) {
        message.append(mod.getDisplayName()).append(" was not configured correctly:\n");
        message.append("Setting '").append(name).append("' was set to value '").append(val).append("', which is invalid. Value must be in the bounds ").append(bounds).append(".\n");
        message.append("Try consulting ").append(mod.getDocumentationSite().toString()).append("for information.\n");
        this.applyDNP(mod);
        this.crash();
    }
}
