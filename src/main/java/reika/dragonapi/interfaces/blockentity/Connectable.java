package reika.dragonapi.interfaces.blockentity;

import reika.dragonapi.interfaces.Location;

public interface Connectable<L extends Location> extends BreakAction, SimpleConnection {

    boolean isEmitting();

    void reset();

    void resetOther();

    L getConnection();

    boolean hasValidConnection();

}
