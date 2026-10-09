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
import reika.dragonapi.interfaces.BlockCheck;
import net.minecraft.world.item.ItemStack;
import reika.dragonapi.instantiable.data.blockstruct.filledblockarray.BlockMatchFailCallback;
import reika.dragonapi.instantiable.data.immutable.BlockKey;

/**
 * 26.3 block-state blueprint with mapped, empty, ignored and forbidden-block cells.
 * Retains the original checker (column across / row down) and importer (row left / column up)
 * coordinate conventions, while supporting rectangular slices and every horizontal direction.
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
		if (array == null || array.length == 0)
			throw new MisuseException("Cannot register an empty slice!");
		int l = array.length;
		for (int i = 0; i < l; i++) {
			if (array[i] == null || array[i].isEmpty())
				throw new MisuseException("Slice rows cannot be null or empty!");
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
		verifyHorizontal(plane);
		BlockState[][] ids = slices.get(slice);
		Block[][] antis = antiSlices.get(slice);
        for (int column = 0; column < ids[0].length; column++) {
            for (int row = 0; row < ids.length; row++) {
                int dx = plane.getStepX() == 0 ? x - xref + column : x;
                int dz = plane.getStepZ() == 0 ? z - xref + column : z;
                int dy = y + yref - row;
                BlockState expected = ids[row][column];
                Block forbidden = antis[row][column];
                BlockPos position = new BlockPos(dx, dy, dz);
                BlockState actual = world.getBlockState(position);
                BlockCheck check = expected != null ? new BlockKey(expected)
                        : forbidden != null ? new ForbiddenBlockCheck(forbidden) : null;
                if (check != null && !check.match(actual)) {
                    if (call != null)
                        call.onBlockFailure(world, dx, dy, dz, check);
                    return false;
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
    /** Original import orientation: rows run left of the facing direction, columns run upward. */
    public void putInto(FilledBlockArray array, int x, int y, int z, Direction direction) {
        verifyHorizontal(direction);
        Direction left = direction.getCounterClockWise();
        for (int slice = 0; slice < slices.size(); slice++) {
            BlockState[][] states = slices.get(slice);
            Block[][] forbidden = antiSlices.get(slice);
            for (int row = 0; row < states.length; row++) {
                for (int column = 0; column < states[row].length; column++) {
                    BlockPos position = new BlockPos(x, y + column, z)
                            .relative(direction, slice).relative(left, row);
                    if (states[row][column] != null)
                        array.setBlock(position.getX(), position.getY(), position.getZ(), states[row][column]);
                    else if (forbidden[row][column] != null)
                        array.setBlock(position.getX(), position.getY(), position.getZ(), new ForbiddenBlockCheck(forbidden[row][column]));
                }
            }
        }
    }

    private static void verifyHorizontal(Direction direction) {
        if (direction == null || direction.getAxis() == Direction.Axis.Y)
            throw new MisuseException("Sliced blueprints require a horizontal direction");
    }

    /** Anti-mappings constrain matching without erasing an already permitted existing block. */
    private static final class ForbiddenBlockCheck implements BlockCheck {
        private final Block forbidden;
        private ForbiddenBlockCheck(Block forbidden) { this.forbidden = forbidden; }
        public boolean match(BlockState state) { return !state.is(forbidden); }
        public boolean matchInWorld(Level world, BlockPos position) { return this.match(world.getBlockState(position)); }
        public boolean match(BlockCheck check) { return check instanceof ForbiddenBlockCheck other && forbidden == other.forbidden; }
        public void place(Level world, BlockPos position, int flags) {
            if (!this.matchInWorld(world, position))
                world.setBlock(position, Blocks.AIR.defaultBlockState(), flags);
        }
        public ItemStack asItemStack() { return ItemStack.EMPTY; }
        public ItemStack getDisplay() { return ItemStack.EMPTY; }
        public BlockKey asBlockKey() { return BlockKey.AIR; }
    }

}
