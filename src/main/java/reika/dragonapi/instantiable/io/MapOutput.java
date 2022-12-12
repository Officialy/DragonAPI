package reika.dragonapi.instantiable.io;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public abstract class MapOutput<V> {

    public final String worldName;
    public final long startTime;
    protected final ResourceKey<Level> dimensionID;
    protected final int originX;
    protected final int originZ;
    protected final int range;
    protected final int resolution;
    protected final int gridSize;
    private final boolean fullGrid;
    private final int[][] data;

    protected MapOutput(String name, ResourceKey<Level> dim, int x, int z, int r, int res, int grid, boolean fgrid) {
        startTime = System.currentTimeMillis();

        worldName = name;
        dimensionID = dim;

        originX = x;
        originZ = z;
        range = r;
        resolution = res;
        gridSize = grid;
        fullGrid = fgrid;

        data = new int[range * 2 / resolution + 1][range * 2 / resolution + 1];
    }

    public void addGrid() {
        if (gridSize > 0) {
            for (int dx = originX - range; dx <= originX + range; dx += resolution) {
                for (int dz = originZ - range; dz <= originZ + range; dz += resolution) {
                    int i = (range + (dx - originX)) / resolution;
                    int k = (range + (dz - originZ)) / resolution;
                    int i2 = dx - originX;
                    int k2 = dz - originZ;
                    boolean flag1 = i2 % gridSize == 0;
                    boolean flag2 = k2 % gridSize == 0;
                    if ((flag1 || flag2) && ((flag1 && flag2) || fullGrid)) {
                        data[i][k] = ReikaColorAPI.mixColors(data[i][k], i2 == 0 && k2 == 0 ? 0xffff0000 : 0xffffffff, 0.25F);
                        if (i - 1 >= 0)
                            data[i - 1][k] = ReikaColorAPI.mixColors(data[i - 1][k], 0xff000000, 0.5F);
                        if (i + 1 < data.length)
                            data[i + 1][k] = ReikaColorAPI.mixColors(data[i + 1][k], 0xff000000, 0.5F);
                        if (k - 1 >= 0)
                            data[i][k - 1] = ReikaColorAPI.mixColors(data[i][k - 1], 0xff000000, 0.5F);
                        if (k + 1 < data[i].length)
                            data[i][k + 1] = ReikaColorAPI.mixColors(data[i][k + 1], 0xff000000, 0.5F);
                    }
                }
            }
        }
    }


    public final void addPoint(int x, int z, V data) {
        int c = 0xff000000 | this.getColor(x, z, data);
        int i = (range + (x - originX)) / resolution;
        int k = (range + (z - originZ)) / resolution;
        this.data[i][k] = c;
    }

    protected abstract int getColor(int x, int z, V data);


    public String createImage() throws IOException {
        String name = this.getFilename();
        File f = new File(DragonAPI.getMinecraftDirectory(), name);
        if (f.exists())
            f.delete();
        f.getParentFile().mkdirs();
        f.createNewFile();
        BufferedImage img = new BufferedImage(data.length, data.length, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < data.length; i++) {
            for (int k = 0; k < data[i].length; k++) {
                img.setRGB(i, k, data[i][k]);
            }
        }
        ImageIO.write(img, "png", f);

        this.onImageCreate(f);

        return f.getCanonicalPath();
    }

    protected void onImageCreate(File f) throws IOException {

    }

    private String getFilename() {
        String ret = this.getFilepath() + originX + ", " + originZ + " " + this.getFileNameDetails();
        return ret;
    }

    protected String getFileNameDetails() {
        String sr = String.valueOf(range * 2 + 1);
        return " (" + sr + "x" + sr + "; [R=" + resolution + " b-px, G=" + gridSize + "-" + fullGrid + "]).png";
    }

    private String getFilepath() {
        String ret = this.getClass().getSimpleName() + "/" + worldName + "/DIM" + dimensionID + "/";
        if (worldName.contains("SEED=")) {
            ret = this.getClass().getSimpleName() + "/Forced/" + worldName + "; ";
        }
        return ret;
    }

}
