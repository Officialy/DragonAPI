package reika.dragonapi.exception;

import reika.dragonapi.DragonAPI;
import reika.dragonapi.ModList;
import reika.dragonapi.base.DragonAPIMod;

public class ModReflectionException extends DragonAPIException {

    public ModReflectionException(ModList target, String msg) {
        this(DragonAPI.instance, target, msg, false);
    }

    public ModReflectionException(DragonAPIMod mod, ModList target, String msg) {
        message.append(mod.getDisplayName()).append(" had an error reading ").append(target.getDisplayName()).append(":\n");
        message.append(msg).append("\n");
        message.append("Please notify ").append(mod.getModAuthorName()).append(" as soon as possible, and include your version of ").append(target.getDisplayName());
        this.crash();
    }

    public ModReflectionException(DragonAPIMod mod, ModList target, String msg, boolean fatal) {
        message.append(mod.getDisplayName()).append(" had an error reading ").append(target.getDisplayName()).append(":\n");
        message.append(msg).append("\n");
        message.append("Please notify ").append(mod.getModAuthorName()).append(" as soon as possible, and include your version of ").append(target.getDisplayName());
        if (fatal)
            this.crash();
    }

}
