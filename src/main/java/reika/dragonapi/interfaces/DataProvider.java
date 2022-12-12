package reika.dragonapi.interfaces;

import java.io.IOException;
import java.io.InputStream;

public interface DataProvider {

    InputStream getDataStream() throws IOException;

    boolean canBeReloaded();

}
