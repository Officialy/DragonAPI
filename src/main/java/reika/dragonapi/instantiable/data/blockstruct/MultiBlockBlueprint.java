/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.data.blockstruct;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class MultiBlockBlueprint {

	protected static final Random rand = new Random();
	public final int xSize;
	public final int ySize;
	public final int zSize;
	private final List<Integer> overrides = new ArrayList<>();
	protected Block[][][] IDs;

	public MultiBlockBlueprint(int x, int y, int z) {
		xSize = x;
		ySize = y;
		zSize = z;
		IDs = new Block[x][y][z];
		//Arrays.fill(IDs, -1);
	}

	public MultiBlockBlueprint addBlockAt(int x, int y, int z, Block id) {
		IDs[x][y][z] = id;
		return this;
	}

	public MultiBlockBlueprint addCenteredBlockAt(BlockPos pos, Block id) {
		return this.addBlockAt(pos.getX() + xSize / 2, pos.getY(), pos.getZ() + zSize / 2, id);
	}

	public boolean isMatch(Level world, int x0, int y0, int z0) {
		for (int i = 0; i < xSize; i++) {
			for (int j = 0; j < ySize; j++) {
				for (int k = 0; k < zSize; k++) {
					Block b = world.getBlockState(new BlockPos(x0 + i, y0 + j, z0 + k)).getBlock();
					if (b != IDs[i][j][k])
						return false;
				}
			}
		}
		return true;
	}

	public void createInWorld(Level world, int x0, int y0, int z0) {
		for (int i = 0; i < xSize; i++) {
			for (int j = 0; j < ySize; j++) {
				for (int k = 0; k < zSize; k++) {
					Block id = IDs[i][j][k];
					if (id != null) {
						if (this.canPlaceBlockAt(world, new BlockPos(x0 + i, y0 + j, z0 + k))) {
							//ReikaJavaLibrary.pConsole("Creating "+id+":"+meta+" @ "+(x0+i)+", "+(y0+j)+", "+(z0+k));
							world.setBlock(new BlockPos(x0 + i, y0 + j, z0 + k), id.defaultBlockState(), 3);
						}
					}
				}
			}
		}
	}

	protected boolean canPlaceBlockAt(Level world, BlockPos pos) {
		Block b = world.getBlockState(pos).getBlock();
		return overrides.contains(b);
	}

	public MultiBlockBlueprint addOverwriteableID(int id) {
		overrides.add(id);
		return this;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < xSize; i++) {
			for (int j = 0; j < ySize; j++) {
				for (int k = 0; k < zSize; k++) {
					Block id = IDs[i][j][k];
					sb.append("[" + id + "]"); //TODO Intelij says to do something here idfk
				}
			}
		}
		return sb.toString();
	}

	public void clear() {
		IDs = new Block[xSize][ySize][zSize];

	}
}
