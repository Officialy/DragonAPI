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

import reika.dragonapi.DragonAPI;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;
import net.minecraft.server.packs.resources.Resource;

import java.io.*;


public class DirectResource extends Resource {

    public final String path;

    public boolean cacheData = true;

    private byte[] data;

    public DirectResource(String path) {
        super(reika.dragonapi.io.DirectResourceManager.getInstance().sourcePack(), () -> openPath(path));
        this.path = path;
    }

    @Override
    public synchronized InputStream open() throws IOException {
        if (!cacheData) return this.calcStream();
        if (data == null) {
            try (InputStream stream = this.calcStream()) { data = stream.readAllBytes(); }
        }
        return new ByteArrayInputStream(data);
    }

    public synchronized void clearCache() { data = null; }

    private static InputStream openPath(String path) throws IOException {
        File file = new File(path);
        if (file.isFile()) return new FileInputStream(file);
        InputStream stream = DragonAPI.class.getClassLoader().getResourceAsStream(path);
        if (stream == null) throw new FileNotFoundException(path);
        return stream;
    }

    protected InputStream calcStream() throws IOException {
        return openPath(path);
    }

}
