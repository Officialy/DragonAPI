/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.io;

import java.io.IOException;
import java.io.InputStream;

public class DynamicDirectResource extends DirectResource {

    private final RemoteSourcedAsset asset;

    public DynamicDirectResource(RemoteSourcedAsset a) {
        super(a.path);

        asset = a;
    }

    @Override
    protected InputStream calcStream() throws IOException {
        InputStream stream = asset.getData();
        if (stream == null) throw new java.io.FileNotFoundException(asset.reference + ":" + asset.path);
        return stream;
    }

}
