package reika.dragonapi.exception;

import reika.dragonapi.base.DragonAPIMod;
import reika.dragonapi.instantiable.io.oldforge.Property;

public class StupidIDException extends DragonAPIException {

    public StupidIDException(DragonAPIMod mod, Property id, Property.Type req) {
        message.append(mod.getDisplayName()).append(" was not installed correctly:\n");
        message.append("ID '").append(id.getString()).append("' is completely invalid, as it is the wrong type (").append(id.getType()).append(" when it should be ").append(req).append(").\n");
        message.append("Please learn how IDs work before attempting to modify configs.\n");
        if (req == Property.Type.INTEGER)
            message.append("IDs must be integers.\n");
        message.append("This is NOT a mod bug. Do not post it or ask for support or you will look extremely foolish.");
        this.crash();
    }

}
