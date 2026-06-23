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

import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import reika.dragonapi.exception.MisuseException;
import reika.dragonapi.instantiable.data.blockstruct.filledblockarray.BlockMatchFailCallback;
import reika.dragonapi.instantiable.data.immutable.BlockKey;

/**
 * 26.2 port: redesigned to be blockstate-native. Legacy DragonAPI stored a {@code Block}+int-metadata
 * per cell; the int metadata is gone, so mapped cells now hold a specific {@link BlockState} (exact match)
 * and anti-cells hold a {@link Block} (block-level "must not be"). The legacy {@code putInto(FilledBlockArray...)}
 * is dropped (FilledBlockArray.setBlock is not ported and no consumer uses it).
 */
public class SlicedBlockBlueprint {

	private int width;
	private int ySize;

	private final ArrayList<BlockState[][]> slices = new ArrayList();
	private final ArrayList<Block[][]> antiSlices = new ArrayList();

	private final HashMap<Character, BlockState> mappings = new HashMap();
	private final HashMap<Character, Block> antiMappings = new HashMap();

	public void addMapping(char c, Block id) {
		this.addMapping(c, id.defaultBlockState());
	}

	public void addMapping(char c, BlockState state) {
		this.verifyArg(c);
		mappings.put(c, state);
	}

	public void addAntiMapping(char c, Block id) {
		this.verifyArg(c);
		antiMappings.put(c, id);
	}

	private void verifyArg(char c) {
		if (c == 'x')
			throw new MisuseException("Character 'x' is reserved for \"don't care\"!");
		if (c == '-')
			throw new MisuseException("Character '-' is reserved for empty space!");
	}

	public void addSlice(String... array) {
		int l = array.length;
		for (int i = 0; i < l; i++) {
			if (i > 0)
				if (array[i].length() != array[i-1].length())
					throw new MisuseException("You must only register properly shaped slices!");
		}
		int w = array[0].length();

		if (l > ySize)
			ySize = l;
		if (w > width)
			width = w;

		BlockState[][] ids = new BlockState[l][w];
		Block[][] antis = new Block[l][w];

		for (int i = 0; i < l; i++) {
			char[] cs = array[i].toCharArray();
			for (int k = 0; k < cs.length; k++) { //cs.length == w
				char c = cs[k];
				if (c == '-') {
					ids[i][k] = Blocks.AIR.defaultBlockState();
					antis[i][k] = null;
				}
				else if (c == 'x') {
					ids[i][k] = null;
					antis[i][k] = null;
				}
				else if (mappings.containsKey(c)) {
					ids[i][k] = mappings.get(c);
					antis[i][k] = null;
				}
				else if (antiMappings.containsKey(c)) {
					ids[i][k] = null;
					antis[i][k] = antiMappings.get(c);
				}
				else {
					throw new MisuseException("Unspecified mapping '"+c+"'!");
				}
			}
		}
		slices.add(ids);
		antiSlices.add(antis);
	}

	public int getLength() {
		return slices.size();
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return ySize;
	}

	public int getWidth(int slice) {
		return slices.get(slice).length;
	}

	public int getHeight(int slice) {
		return slices.get(slice)[0].length;
	}

	public boolean checkAgainst(Level world, int x, int y, int z, int xref, int yref, Direction plane, int slice, BlockMatchFailCallback call) {
		BlockState[][] ids = slices.get(slice);
		Block[][] antis = antiSlices.get(slice);
		for (int i = 0; i < ids.length; i++) {
			for (int k = 0; k < ids[i].length; k++) {
				int dx = plane.getStepX() == 0 ? x-xref+i : x;
				int dz = plane.getStepZ() == 0 ? z-xref+i : z;
				int dy = y+yref-k;
				BlockState id = ids[k][i];
				BlockPos p = new BlockPos(dx, dy, dz);
				BlockState id2 = world.getBlockState(p);
				if (id == null) {
					Block anti = antis[k][i];
					if (anti != null && id2.is(anti)) {
						if (call != null)
							call.onBlockFailure(world, dx, dy, dz, BlockKey.AIR);
						return false;
					}
				}
				else {
					if (id != id2) {
						if (call != null)
							call.onBlockFailure(world, dx, dy, dz, new BlockKey(id));
						return false;
					}
				}
			}
		}
		return true;
	}

	public SlicedBlockBlueprint copy() {
		SlicedBlockBlueprint cp = new SlicedBlockBlueprint();
		cp.width = width;
		cp.ySize = ySize;
		cp.slices.addAll(slices);
		cp.antiSlices.addAll(antiSlices);
		cp.mappings.putAll(mappings);
		cp.antiMappings.putAll(antiMappings);
		return cp;
	}
}
