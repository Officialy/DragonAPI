package reika.dragonapi.base;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;
import reika.dragonapi.interfaces.registry.TileEnum;

/**
 * Base for a multiblock structure definition: subclasses build a {@link FilledBlockArray} layout in
 * {@link #getArray}, which callers then match against / place in the world. Ported to 26.2 on the
 * BlockState-based {@link FilledBlockArray}/{@link TileEnum} APIs (the {@code setTile}/{@code addTile}
 * helpers now place a tile's {@link TileEnum#getBlockState()} rather than a block+metadata pair).
 */
public abstract class StructureBase {

	private boolean isDisplayCall;

	@OnlyIn(Dist.CLIENT)
	public final synchronized FilledBlockArray getStructureForDisplay() {
		isDisplayCall = true;
		this.initDisplayData();
		FilledBlockArray ret = this.getArray(Minecraft.getInstance().level, 0, 0, 0);
		isDisplayCall = false;
		this.finishDisplayCall();
		return ret;
	}

	protected void initDisplayData() {

	}

	protected void finishDisplayCall() {

	}

	public abstract FilledBlockArray getArray(Level world, int x, int y, int z);

	protected final void setTile(FilledBlockArray f, int x, int y, int z, TileEnum te) {
		f.setBlock(x, y, z, te.getBlockState());
	}

	protected final void addTile(FilledBlockArray f, int x, int y, int z, TileEnum te) {
		f.addBlock(x, y, z, te.getBlockState());
	}

	protected final boolean isDisplay() {
		return isDisplayCall;
	}

}
