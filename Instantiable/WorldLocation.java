package Reika.DragonAPI.Instantiable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeDirection;
import Reika.DragonAPI.Libraries.World.ReikaWorldHelper;

public final class WorldLocation {

	public final int xCoord;
	public final int yCoord;
	public final int zCoord;
	public final int dimensionID;

	public WorldLocation(World world, int x, int y, int z) {
		this(world.provider.dimensionId, x, y, z);
	}

	private WorldLocation(int dim, int x, int y, int z) {
		xCoord = x;
		yCoord = y;
		zCoord = z;
		dimensionID = dim;
	}

	public int getBlockID() {
		World world = this.getWorld();
		return world != null ? world.getBlockId(xCoord, yCoord, zCoord) : -1;
	}

	public int getBlockMetadata() {
		World world = this.getWorld();
		return world != null ? world.getBlockId(xCoord, yCoord, zCoord) : -1;
	}

	public TileEntity getTileEntity() {
		World world = this.getWorld();
		return world != null ? world.getBlockTileEntity(xCoord, yCoord, zCoord) : null;
	}

	public int getRedstone() {
		World world = this.getWorld();
		return world != null ? world.getBlockPowerInput(xCoord, yCoord, zCoord) : 0;
	}

	public int getRedstoneOnSide(ForgeDirection dir) {
		ForgeDirection opp = dir.getOpposite();
		int s = dir.ordinal();
		World world = this.getWorld();
		return world != null ? world.getIndirectPowerLevelTo(xCoord+opp.offsetX, yCoord+opp.offsetY, zCoord+opp.offsetZ, s) : 0;
	}

	public boolean isRedstonePowered() {
		World world = this.getWorld();
		return world != null ? world.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord) : false;
	}

	public void triggerBlockUpdate(boolean adjacent) {
		World world = this.getWorld();
		if (world != null) {
			world.markBlockForUpdate(xCoord, yCoord, zCoord);
			if (adjacent) {
				ReikaWorldHelper.causeAdjacentUpdates(world, xCoord, yCoord, zCoord);
			}
		}
	}

	public WorldLocation move(int dx, int dy, int dz) {
		return new WorldLocation(dimensionID, xCoord+dx, yCoord+dy, zCoord+dz);
	}

	public WorldLocation move(ForgeDirection dir, int dist) {
		return this.move(dir.offsetX*dist, dir.offsetY*dist, dir.offsetZ*dist);
	}

	public WorldLocation changeWorld(World world) {
		return new WorldLocation(world, xCoord, yCoord, zCoord);
	}

	public World getWorld() {
		return DimensionManager.getWorld(dimensionID);
	}

	public void writeToNBT(String tag, NBTTagCompound NBT) {
		NBTTagCompound data = new NBTTagCompound();
		data.setInteger("dim", dimensionID);
		data.setInteger("x", xCoord);
		data.setInteger("y", yCoord);
		data.setInteger("z", zCoord);
		NBT.setTag(tag, data);
	}

	public static WorldLocation readFromNBT(String tag, NBTTagCompound NBT) {
		if (!NBT.hasKey(tag))
			return null;
		NBTTagCompound data = NBT.getCompoundTag(tag);
		if (data != null) {
			int x = data.getInteger("x");
			int y = data.getInteger("y");
			int z = data.getInteger("z");
			int dim = data.getInteger("dim");
			return new WorldLocation(dim, x, y, z);
		}
		return null;
	}

	public WorldLocation copy() {
		return new WorldLocation(dimensionID, xCoord, yCoord, zCoord);
	}

	@Override
	public String toString() {
		return xCoord+", "+yCoord+", "+zCoord+" in DIM"+dimensionID;
	}

	@Override
	public int hashCode() {
		return xCoord + zCoord << 8 + yCoord << 16 + dimensionID << 24;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof WorldLocation) {
			WorldLocation w = (WorldLocation)o;
			return this.equals(w.dimensionID, w.xCoord, w.yCoord, w.zCoord);
		}
		return false;
	}

	private boolean equals(int dim, int x, int y, int z) {
		return dim == dimensionID && x == xCoord && y == yCoord && z == zCoord;
	}

	public boolean equals(World world, int x, int y, int z) {
		return this.equals(world.provider.dimensionId, x, y, z);
	}

}
