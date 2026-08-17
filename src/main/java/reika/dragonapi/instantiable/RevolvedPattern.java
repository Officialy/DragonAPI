package reika.dragonapi.instantiable;

import net.minecraft.world.level.Level;

import reika.dragonapi.instantiable.data.blockstruct.BlockArray;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;
import reika.dragonapi.instantiable.data.immutable.BlockKey;

/**
 * Builds a four-fold symmetric shape from one quadrant.
 *
 * <p>Authoring a round or cross-shaped structure a quadrant at a time and mirroring it is far less
 * error-prone than writing all four out, and it guarantees the result really is symmetric. Add cells
 * with {@link #addBlock} or a whole {@link #addLayer}, call {@link #calculate} to mirror them across
 * both axes, then {@link #populate} to copy the finished shape into the array being built.
 *
 * <p>{@code coreSize} is what makes an even-width trunk work. With a core of one the mirrors meet on a
 * shared centre line; with a larger core each mirror is pulled back by {@code coreSize - 1} so the
 * quadrants sit either side of a trunk that many blocks across instead of overlapping it.
 */
public class RevolvedPattern {

	public final int coreSize;
	public final int height;

	private final int size;

	private final FilledBlockArray data;

	public RevolvedPattern(Level world, int coreSize, int height, int size) {
		this.coreSize = coreSize;
		this.height = height;
		this.size = size;
		this.data = new FilledBlockArray(world);
	}

	public RevolvedPattern addBlock(BlockKey block, int layer, int main, int side) {
		data.setBlock(main, layer, side, block);
		return this;
	}

	/** @throws IllegalArgumentException if the layer is not the square this pattern was sized for */
	public RevolvedPattern addLayer(BlockKey[][] layerBlocks, int layer) {
		if (layerBlocks.length != size || layerBlocks[0].length != size)
			throw new IllegalArgumentException("Layer does not fit: expected " + size + "x" + size
					+ " but got " + layerBlocks.length + "x" + layerBlocks[0].length);
		for (int i = 0; i < layerBlocks.length; i++)
			for (int k = 0; k < layerBlocks[i].length; k++)
				this.addBlock(layerBlocks[i][k], layer, i, k);
		return this;
	}

	/** Mirrors the authored quadrant across both horizontal axes, offset for the core width. */
	public RevolvedPattern calculate() {
		BlockArray flippedX = data.flipX();
		BlockArray flippedZ = data.flipZ();
		BlockArray flippedBoth = data.flipX().flipZ();
		if (coreSize > 1) {
			int inset = coreSize - 1;
			flippedX.offset(-inset, 0, 0);
			flippedZ.offset(0, 0, -inset);
			flippedBoth.offset(-inset, 0, -inset);
		}
		data.addAll(flippedX);
		data.addAll(flippedZ);
		data.addAll(flippedBoth);
		return this;
	}

	public void populate(FilledBlockArray target) {
		target.addAll(data);
	}
}
