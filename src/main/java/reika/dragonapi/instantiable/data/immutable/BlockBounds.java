package reika.dragonapi.instantiable.data.immutable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import reika.dragonapi.libraries.java.ReikaStringParser;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

import java.util.ArrayList;

public class BlockBounds {

    public final double negativeX;
    public final double negativeY;
    public final double negativeZ;
    public final double positiveX;
    public final double positiveY;
    public final double positiveZ;

    /*public static BlockBounds fromBlock(Block b, BlockGetter world, BlockPos pos) {
        b.setBlockBoundsBasedOnState(world, pos);
        return new BlockBounds(b.getBlockBoundsMinX(), b.getBlockBoundsMinY(), b.getBlockBoundsMinZ(), b.getBlockBoundsMaxX(), b.getBlockBoundsMaxY(), b.getBlockBoundsMaxZ());
    }*/

    public BlockBounds(double nx, double ny, double nz, double px, double py, double pz) {
        negativeX = Math.max(0, nx);
        negativeY = Math.max(0, ny);
        negativeZ = Math.max(0, nz);
        positiveX = Math.min(1, px);
        positiveY = Math.min(1, py);
        positiveZ = Math.min(1, pz);
        this.verify();
    }

    private void verify() {
        if (negativeX > positiveX)
            throw new IllegalArgumentException("Negative X bound is larger than positive bound!");
        if (negativeY > positiveY)
            throw new IllegalArgumentException("Negative Y bound is larger than positive bound!");
        if (negativeZ > positiveZ)
            throw new IllegalArgumentException("Negative Z bound is larger than positive bound!");
    }

    public BlockBounds add(Direction side, double amt) {
        return this.cut(side, -amt);
    }

    public BlockBounds cut(Direction side, double amt) {
        double nx = negativeX;
        double ny = negativeY;
        double nz = negativeZ;
        double px = positiveX;
        double py = positiveY;
        double pz = positiveZ;
        switch (side) {
            case DOWN:
                amt = Math.min(amt, py - ny);
                ny += amt;
                break;
            case UP:
                amt = Math.min(amt, py - ny);
                py -= amt;
                break;
            case EAST:
                amt = Math.min(amt, px - nx);
                px -= amt;
                break;
            case WEST:
                amt = Math.min(amt, px - nx);
                nx += amt;
                break;
            case NORTH:
                amt = Math.min(amt, pz - nz);
                nz += amt;
                break;
            case SOUTH:
                amt = Math.min(amt, pz - nz);
                pz -= amt;
                break;
            default:
                break;
        }
        return new BlockBounds(nx, ny, nz, px, py, pz);
    }

    public BlockBounds fill(Direction side) {
        double nx = negativeX;
        double ny = negativeY;
        double nz = negativeZ;
        double px = positiveX;
        double py = positiveY;
        double pz = positiveZ;
        switch (side) {
            case DOWN:
                ny = 0;
                break;
            case UP:
                py = 1;
                break;
            case EAST:
                px = 1;
                break;
            case WEST:
                nx = 0;
                break;
            case NORTH:
                nz = 0;
                break;
            case SOUTH:
                pz = 1;
                break;
            default:
                break;
        }
        return new BlockBounds(nx, ny, nz, px, py, pz);
    }

    public BlockBounds roundToNearest(double d) {
        double nx = negativeX;
        double ny = negativeY;
        double nz = negativeZ;
        double px = positiveX;
        double py = positiveY;
        double pz = positiveZ;
        nx = ReikaMathLibrary.roundToNearestFraction(nx, d);
        ny = ReikaMathLibrary.roundToNearestFraction(ny, d);
        nz = ReikaMathLibrary.roundToNearestFraction(nz, d);
        px = ReikaMathLibrary.roundToNearestFraction(px, d);
        py = ReikaMathLibrary.roundToNearestFraction(py, d);
        pz = ReikaMathLibrary.roundToNearestFraction(pz, d);
        return new BlockBounds(nx, ny, nz, px, py, pz);
    }

    public void copyToBlock(Block b) {
        //b.setBlockBounds((float)negativeX, (float)negativeY, (float)negativeZ, (float)positiveX, (float)positiveY, (float)positiveZ);
    }

    public static BlockBounds block() {
        return new BlockBounds(0, 0, 0, 1, 1, 1);
    }

    public void saveAdditional(String id, CompoundTag nbt) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("nx", negativeX);
        tag.putDouble("ny", negativeY);
        tag.putDouble("nz", negativeZ);
        tag.putDouble("px", positiveX);
        tag.putDouble("py", positiveY);
        tag.putDouble("pz", positiveZ);
        nbt.put(id, tag);
    }

    public static BlockBounds load(String id, CompoundTag nbt) {
        CompoundTag tag = nbt.getCompound(id);
        double nx = tag.getDouble("nx");
        double ny = tag.getDouble("ny");
        double nz = tag.getDouble("nz");
        double px = tag.getDouble("px");
        double py = tag.getDouble("py");
        double pz = tag.getDouble("pz");
        return new BlockBounds(nx, ny, nz, px, py, pz);
    }

    public AABB asAABB(int x, int y, int z) {
        return new AABB(x + negativeX, y + negativeY, z + negativeZ, x + positiveX, y + positiveY, z + positiveZ);
    }

    public double getBound(Direction dir) {
        switch (dir) {
            case DOWN:
                return negativeY;
            case EAST:
                return positiveX;
            case NORTH:
                return negativeZ;
            case SOUTH:
                return positiveZ;
            case UP:
                return positiveY;
            case WEST:
                return negativeX;
            default:
                break;
        }
        return Double.NaN;
    }

    public boolean isFullDistance(Direction dir) {
        int val = dir.getStepX() + dir.getStepY() + dir.getStepZ() == 1 ? 1 : 0;
        return this.getBound(dir) == val;
    }

    public boolean isFullFace(Direction dir) {
        if (this.isFullDistance(dir))
            return false;
        for (int i = 0; i < 6; i++) {
            Direction dir2 = Direction.values()[i];
            if (dir2 != dir && dir2 != dir.getOpposite()) {
                if (!this.isFullDistance(dir2))
                    return false;
            }
        }
        return true;
    }

    public ArrayList<String> toClearString() {
        ArrayList<String> li = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Direction dir = Direction.values()[i];
            String s = dir == Direction.UP ? "Top" : dir == Direction.DOWN ? "Bottom" : ReikaStringParser.capFirstChar(dir.name());
            li.add(String.format("%s: %.1f px", s, 16 * this.getBound(dir)));
        }
        return li;
    }

    @Override
    public String toString() {
        return negativeX + "," + negativeY + "," + negativeZ + " > " + positiveX + "," + positiveY + "," + positiveZ;
    }

}
