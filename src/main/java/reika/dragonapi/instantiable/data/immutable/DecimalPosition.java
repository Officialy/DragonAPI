/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.data.immutable;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import reika.dragonapi.interfaces.Location;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

import java.util.Random;

public final class DecimalPosition implements Location, Comparable<DecimalPosition> {

    private static final Random rand = new Random();

    public final double xCoord;
    public final double yCoord;
    public final double zCoord;

    public DecimalPosition(double x, double y, double z) {
        xCoord = x;
        yCoord = y;
        zCoord = z;
    }

    public DecimalPosition(BlockEntity te) {
        this(te.getBlockPos().getX() + 0.5, te.getBlockPos().getY() + 0.5, te.getBlockPos().getZ() + 0.5);
    }

    public DecimalPosition(Entity e) {
        this(e.getX(), e.getY(), e.getZ());
    }

    public DecimalPosition(Entity e, float ptick) {
        this(e.xOld + ptick * (e.getX() - e.xOld), e.yOld + ptick * (e.getY() - e.yOld), e.zOld + ptick * (e.getZ() - e.zOld));
    }

    public DecimalPosition(DecimalPosition loc) {
        this(loc.xCoord, loc.yCoord, loc.zCoord);
    }

    public DecimalPosition(HitResult hit) {
    	this(hit.getLocation().x() + 0.5, hit.getLocation().y() + 0.5, hit.getLocation().z() + 0.5);
    }
    public DecimalPosition(WorldLocation src) {
    	this(src.pos.getX() + 0.5, src.pos.getY() + 0.5, src.pos.getZ() + 0.5);
    }

    public DecimalPosition(Vec3 vec) {
        this(vec.x, vec.y, vec.z);
    }

    public static DecimalPosition load(String tag, CompoundTag NBT) {
        if (!NBT.contains(tag))
            return null;
        CompoundTag data = NBT.getCompound(tag);
        if (data != null) {
            double x = data.getDouble("x");
            double y = data.getDouble("y");
            double z = data.getDouble("z");
            return new DecimalPosition(x, y, z);
        }
        return null;
    }

    public static DecimalPosition readTag(CompoundTag data) {
        double x = data.getDouble("x");
        double y = data.getDouble("y");
        double z = data.getDouble("z");
        return new DecimalPosition(x, y, z);
    }

    public static DecimalPosition interpolate(DecimalPosition p1, DecimalPosition p2, double f) {
        return interpolate(p1.xCoord, p1.yCoord, p1.zCoord, p2.xCoord, p2.yCoord, p2.zCoord, f);
    }

    public static DecimalPosition interpolate(double x1, double y1, double z1, double x2, double y2, double z2, double f) {
        return new DecimalPosition(x1 + (x2 - x1) * f, y1 + (y2 - y1) * f, z1 + (z2 - z1) * f);
    }

    public static DecimalPosition readFromBuf(ByteBuf buf) {
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        return new DecimalPosition(x, y, z);
    }


    public static DecimalPosition getRandomWithin(BlockPos c, Random rand) {
        return new DecimalPosition(c.getX() + rand.nextDouble(), c.getY() + rand.nextDouble(), c.getZ() + rand.nextDouble());
    }

    public static DecimalPosition average(DecimalPosition... pos) {
        double sx = 0;
        double sy = 0;
        double sz = 0;
        int n = pos.length;
        for (int i = 0; i < n; i++) {
            sx += pos[i].xCoord;
            sy += pos[i].yCoord;
            sz += pos[i].zCoord;
        }
        return new DecimalPosition(sx / n, sy / n, sz / n);
    }

    public DecimalPosition offset(double dx, double dy, double dz) {
        return new DecimalPosition(xCoord + dx, yCoord + dy, zCoord + dz);
    }

    public DecimalPosition offset(Direction dir, double dist) {
        return this.offset(dir.getStepX() * dist, dir.getStepY() * dist, dir.getStepZ() * dist);
    }

    public DecimalPosition offset(DecimalPosition p) {
        return this.offset(p.xCoord, p.yCoord, p.zCoord);
    }

    public boolean sharesBlock(DecimalPosition dec) {
        return this.sharesBlock(dec.xCoord, dec.yCoord, dec.zCoord);
    }

    public boolean sharesBlock(double x, double y, double z) {
        return this.matchX(x) && this.matchY(y) && this.matchZ(z);
    }

    private boolean matchX(double x) {
        return Mth.floor(x) == Mth.floor(xCoord);
    }

    private boolean matchY(double y) {
        return Mth.floor(y) == Mth.floor(yCoord);
    }

    private boolean matchZ(double z) {
        return Mth.floor(z) == Mth.floor(zCoord);
    }

    public void saveAdditional(String tag, CompoundTag NBT) {
        CompoundTag data = new CompoundTag();
        this.writeToTag(data);
        NBT.put(tag, data);
    }

    public void writeToTag(CompoundTag data) {
        data.putDouble("x", xCoord);
        data.putDouble("y", yCoord);
        data.putDouble("z", zCoord);
    }

    public CompoundTag writeToTag() {
        CompoundTag data = new CompoundTag();
        data.putDouble("x", xCoord);
        data.putDouble("y", yCoord);
        data.putDouble("z", zCoord);
        return data;
    }

    public DecimalPosition copy() {
        return new DecimalPosition(xCoord, yCoord, zCoord);
    }

    @Override
    public String toString() {
        return "[" + xCoord + ", " + yCoord + ", " + zCoord + "]";
    }

    @Override
    public int hashCode() {
        return 1; //BlockPos.coordHash((int) (xCoord * 1000), (int) (yCoord * 1000), (int) (zCoord * 1000));//(int)(xCoord + (zCoord * 256) + (yCoord * 65536));
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DecimalPosition w) {
            return this.equals(w.xCoord, w.yCoord, w.zCoord);
        }
        return false;
    }

    public boolean equals(double x, double y, double z) {
        return x == xCoord && y == yCoord && z == zCoord;
    }

    public double getDistanceTo(DecimalPosition src) {
        return this.getDistanceTo(src.xCoord, src.yCoord, src.zCoord);
    }

    public double getDistanceTo(double x, double y, double z) {
        return ReikaMathLibrary.py3d(x - xCoord, y - yCoord, z - zCoord);
    }

    public boolean isWithinSquare(BlockPos c, double d) {
        return this.isWithinSquare(c, d, d, d);
    }

    public boolean isWithinSquare(BlockPos c, double dx, double dy, double dz) {
        return Math.abs(c.getX() - xCoord) <= dx && Math.abs(c.getY() - yCoord) <= dy && Math.abs(c.getZ() - zCoord) <= dz;
    }

    public double[] toArray() {
        double[] a = new double[3];
        a[0] = xCoord;
        a[1] = yCoord;
        a[2] = zCoord;
        return a;
    }

    public BlockPos getCoordinate() {
        return new BlockPos(Mth.floor(xCoord), Mth.floor(yCoord), Mth.floor(zCoord));
    }

    public Block getBlock(BlockGetter world) {
        return world != null ? world.getBlockState(this.getCoordinate()).getBlock() : null;
    }

    public boolean isEmpty(BlockGetter world) {
        return world.getBlockState(new BlockPos(Mth.floor(xCoord), Mth.floor(yCoord), Mth.floor(zCoord))).getBlock() == Blocks.AIR;
    }

    public void dropItem(Level world, ItemStack is) {
        this.dropItem(world, is, 1);
    }

    public void dropItem(Level world, ItemStack is, double vscale) {
        if (world != null && !world.isClientSide()) {
            //ReikaItemHelper.dropItem(world, xCoord + rand.nextDouble(), yCoord + rand.nextDouble(), zCoord + rand.nextDouble(), is, vscale);
        }
    }

    public boolean setBlock(Level world, Block b) {
        return this.setBlock(world, b);
    }

    public boolean setBlock(Level world, ItemStack is) {
        return this.setBlock(world, Block.byItem(is.getItem()));
    }

    public DecimalPosition negate() {
        return new DecimalPosition(xCoord, yCoord, zCoord);
    }

    public DecimalPosition to2D() {
        return new DecimalPosition(xCoord, 0, zCoord);
    }

    public void writeToBuf(ByteBuf buf) {
        buf.writeDouble(xCoord);
        buf.writeDouble(yCoord);
        buf.writeDouble(zCoord);
    }

    public String formattedString(int decimal) {
        String part = "%." + decimal + "f";
        return String.format(part + ", " + part + ", " + part, xCoord, yCoord, zCoord);
    }

    public AABB getAABB(double radius) {
        //return AABB.of(new BoundingBox(xCoord - radius, yCoord - radius, zCoord - radius, xCoord + radius, yCoord + radius, zCoord + radius));
        return AABB.of(new BoundingBox(1, 1, 1, 1, 1, 1));
    }

    @Override
    public int compareTo(DecimalPosition o) {
        int val = Integer.compare(this.hashCode(), o.hashCode());
        if (val != 0)
            return val;
        val = Double.compare(xCoord, o.xCoord);
        if (val != 0)
            return val;
        val = Double.compare(yCoord, o.yCoord);
        if (val != 0)
            return val;
        val = Double.compare(zCoord, o.zCoord);
        return val;
    }

    @Override
    public HitResult asMovingPosition(Direction s, Vec3 vec) {
        //return new HitResult(Mth.floor(xCoord), Mth.floor(yCoord), Mth.floor(zCoord), s, vec);
        return new BlockHitResult(vec, s, new BlockPos(Mth.floor(xCoord), Mth.floor(yCoord), Mth.floor(zCoord)), false);
    }

    @Override
    public BlockEntity getBlockEntity(BlockGetter world) {
        return world != null ? world.getBlockEntity(this.getCoordinate()) : null;
    }

}
//TODO BASICALLY THIS WHOLE SHIT BOI