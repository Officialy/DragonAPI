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
		gridSize = size;
		maxDeviation = dev;
		averageSeparation = sep;

		if (sep >= dev / 2) {
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
