package reika.dragonapi.instantiable.data;


import reika.dragonapi.DragonAPI;
import reika.dragonapi.libraries.java.ReikaRandomHelper;

import java.util.Random;

public class ShuffledGrid {

	public final int gridSize;
	public final int maxDeviation;
	public final int averageSeparation;

	private final boolean[][] data;

	public ShuffledGrid(int size, int dev, int sep) {
		if (size <= 0)
			throw new IllegalArgumentException("Grid size must be positive: "+size);
		if (dev < 0)
			throw new IllegalArgumentException("Grid deviation cannot be negative: "+dev);
		if (sep <= 0)
			throw new IllegalArgumentException("Grid separation must be positive: "+sep);
		gridSize = size;
		maxDeviation = dev;
		averageSeparation = sep;

		// Adjacent rows can overlap only when both may move toward one another far enough
		// to consume their separation. The legacy comparison was reversed and warned for
		// practically every healthy grid (including the 55 separation / 20 deviation warp grid).
		if ((long)dev * 2 >= sep) {
			DragonAPI.LOGGER.info("Warning, shuffled grid may have row overlap!");
			Thread.dumpStack();
		}

		data = new boolean[size][size];
	}

	public void calculate(Random rand) {
		rand.nextBoolean();
		rand.nextBoolean();
		for (int x = maxDeviation; x < gridSize - maxDeviation; x += averageSeparation) {
			for (int z = maxDeviation; z < gridSize - maxDeviation; z += averageSeparation) {
				int x2 = ReikaRandomHelper.getRandomPlusMinus(x, maxDeviation, rand);
				int z2 = ReikaRandomHelper.getRandomPlusMinus(z, maxDeviation, rand);
				data[x2][z2] = true;
			}
		}
	}

	public boolean isValid(int x, int z) {
		/*
		while (x < 0)
			x += gridSize;
		while (z < 0)
			z += gridSize;
		 */
		x = ((x % gridSize) + gridSize) % gridSize;
		z = ((z % gridSize) + gridSize) % gridSize;
		return data[x][z];
	}
}
