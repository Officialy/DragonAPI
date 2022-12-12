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
        super(null, null); //todo null for now to compile
        this.path = path;
    }

    @Override
    public InputStream open() {
        if (cacheData) {
            //ReikaJavaLibrary.pConsole("Loading "+path+", data="+data);
            if (data == null) {
                try (InputStream st = this.calcStream()) {
                    if (st == null)
                        throw new RuntimeException("Resource not found at " + path);
                    data = ReikaJavaLibrary.streamToBytes(st);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //ReikaJavaLibrary.pConsole("Loaded cache for "+path+", data="+data);
            return new ByteArrayInputStream(data);
        } else {
            //ReikaJavaLibrary.pConsole("Skipped cache for "+path);
            return this.calcStream();
        }
    }

    protected InputStream calcStream() {
        File f = new File(path);
        if (f.exists()) {
            try {
                return new FileInputStream(f);
            } catch (FileNotFoundException e) {
                return null;
            }
        } else
            return DragonAPI.class.getClassLoader().getResourceAsStream(path);
    }

}
