/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.libraries;

import reika.dragonapi.instantiable.data.maps.PluralMap;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;
import reika.dragonapi.libraries.mathsci.ReikaPhysicsHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.awt.*;
import java.util.*;

public class ReikaDirectionHelper {

	public static Direction getLeftBy90(Direction dir) {
		switch (dir) {
			case EAST:
				return Direction.NORTH;
			case NORTH:
				return Direction.WEST;
			case SOUTH:
				return Direction.EAST;
			case WEST:
				return Direction.SOUTH;
			default:
				return dir;
		}
	}

	public static Direction getRightBy90(Direction dir) {
		switch (dir) {
			case EAST:
				return Direction.SOUTH;
			case NORTH:
				return Direction.EAST;
			case SOUTH:
				return Direction.WEST;
			case WEST:
				return Direction.NORTH;
			default:
				return dir;
		}
	}
	public static Direction getDirectionBetween(BlockPos from, BlockPos to) {
		return getDirectionBetween(from.getX(), from.getY(), from.getZ(), to.getX(), to.getY(), to.getZ());
	}

	public static Direction getDirectionBetween(int x1, int y1, int z1, int x2, int y2, int z2) {
		int dx = x2 - x1;
		int dy = y2 - y1;
		int dz = z2 - z1;
		for (int i = 0; i < 6; i++) {
			Direction dir = Direction.values()[i];
			if (Math.signum(dir.getStepX()) == Math.signum(dx) && Math.signum(dir.getStepY()) == Math.signum(dy) && Math.signum(dir.getStepZ()) == Math.signum(dz))
				return dir;
		}
		//ReikaJavaLibrary.pConsole(x1+","+y1+","+z1+" > "+x2+","+y2+","+z2+" ("+dx+":"+dy+":"+dz+")");
		return null;
	}

	public static Direction getDirectionBetween(Point from, Point to) {
		return getDirectionBetween(from.x, 0, from.y, to.x, 0, to.y);
	}

	/**
	 * Returns the two positive direction vectors perpendicular to the supplied direction.
	 */
	public static ArrayList<Direction> getPerpendicularDirections(Direction dir) {
		ArrayList<Direction> dirs = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			Direction d = Direction.values()[i];
			if (d != dir && d != dir.getOpposite())
				dirs.add(d);
		}
		dirs.remove(Direction.WEST);
		dirs.remove(Direction.NORTH);
		dirs.remove(Direction.DOWN);
		return dirs;
	}

	public static Direction getRandomDirection(boolean vertical, Random rand) {
		int idx = vertical ? rand.nextInt(6) : 2 + rand.nextInt(4);
		return Direction.values()[idx];
	}

	public static Direction getOpposite(Direction facing) {
		int val = facing.ordinal() % 2 != 0 ? facing.ordinal() - 1 : facing.ordinal() + 1;
		return Direction.values()[val];
	}

	public static Direction getSideOfBox(int i, int j, int k, boolean vertical, int size) {
		if (i == size)
			return Direction.EAST;
		else if (i == 0)
			return Direction.WEST;
		else if (k == size)
			return Direction.SOUTH;
		else if (k == 0)
			return Direction.NORTH;
		else if (vertical && j == size)
			return Direction.UP;
		else if (vertical && j == 0)
			return Direction.DOWN;
		else
			return null;
	}

	public static int getRelativeAngle(Direction from, Direction to) {
		int rel = getHeading(to) - getHeading(from);
		return (360 + rel % 360) % 360;
	}

	public static int getHeading(Direction dir) {
		switch (dir) {
			case NORTH:
				return 0;
			case EAST:
				return 90;
			case SOUTH:
				return 180;
			case WEST:
				return 270;
			default:
				return 0;
		}
	}

	public static Direction getByHeading(double ang) {
		ang += 360;
		ang %= 360;
		int a = (int) (ang / 90);
		switch (a) {
			case 0:
				return Direction.NORTH;
			case 1:
				return Direction.EAST;
			case 2:
				return Direction.SOUTH;
			case 3:
				return Direction.WEST;
			default:
				return null;
		}
	}

	public static double getCompassHeading(double dx, double dz) {
		double phi = ReikaPhysicsHelper.cartesianToPolar(dx, 0, -dz)[2];
		phi += 90; //since phi=0 is EAST
		return (phi % 360D + 360D) % 360D;
	}

	public static ArrayList<Direction> getRandomOrderedDirections(boolean vertical) {
		ArrayList<Direction> li = ReikaJavaLibrary.makeListFromArray(Direction.values());
		if (!vertical) {
			li.remove(Direction.UP.ordinal());
			li.remove(Direction.DOWN.ordinal());
		}
		Collections.shuffle(li);
		return li;
	}

	public static Direction getImpactedSide(Level world, BlockPos pos, Entity e) {
		int dx = (int) Math.round((e.getX() - pos.getX() - 0.5) * 2);
		int dz = (int) Math.round((e.getZ() - pos.getZ() - 0.5) * 2);
		return getByDirection(dx, dz);
	}

	public static Direction getByDirection(int dx, int dz) {
		if (dx > 0)
			return Direction.EAST;
		else if (dx < 0)
			return Direction.WEST;
		else if (dz > 0)
			return Direction.SOUTH;
		else if (dz < 0)
			return Direction.NORTH;
		throw new IllegalStateException();
	}

	public static HashSet<Direction> setDirections(boolean vertical) {
		HashSet<Direction> ret = new HashSet();
		ret.add(Direction.EAST);
		ret.add(Direction.WEST);
		ret.add(Direction.NORTH);
		ret.add(Direction.SOUTH);
		if (vertical) {
			ret.add(Direction.UP);
			ret.add(Direction.DOWN);
		}
		return ret;
	}
/*
	public static Direction getFromLookDirection(LivingEntity ep, boolean vertical) {
		if (!vertical || Mth.abs(ep.rotationPitch) < 60) {
			int i = Mth.floor((ep.rotationYaw * 4F) / 360F + 0.5D);
			while (i > 3)
				i -= 4;
			while (i < 0)
				i += 4;
			switch (i) {
				case 0:
					return Direction.SOUTH;
				case 1:
					return Direction.WEST;
				case 2:
					return Direction.NORTH;
				case 3:
					return Direction.EAST;
			}
			return Direction.valueOf("unknown");
		} else {
			if (ep.getRotationVector().y > 0) //length -TODO look direction
				return Direction.DOWN;
			else
				return Direction.UP;
		}
	}
*/

	public static boolean arePerpendicular(Direction d1, Direction d2) {
		return !areCoaxial(d1, d2);
	}

	public static boolean areCoaxial(Direction d1, Direction d2) {
		if (d1.getStepX() != 0)
			return Math.abs(d1.getStepX()) == Math.abs(d2.getStepX());
		if (d1.getStepY() != 0)
			return Math.abs(d1.getStepY()) == Math.abs(d2.getStepY());
		if (d1.getStepZ() != 0)
			return Math.abs(d1.getStepZ()) == Math.abs(d2.getStepZ());
		return false;
	}

	public enum CubeDirections {
		NORTH(0, -1, 90),
		NORTHEAST(1, -1, 45),
		EAST(1, 0, 0),
		SOUTHEAST(1, 1, 315),
		SOUTH(0, 1, 270),
		SOUTHWEST(-1, 1, 225),
		WEST(-1, 0, 180),
		NORTHWEST(-1, -1, 135);

		public static final CubeDirections[] list = values();
		private static final PluralMap<CubeDirections> dirMap = new PluralMap(2);

		static {
			for (int i = 0; i < list.length; i++) {
				dirMap.put(list[i], list[i].directionX, list[i].directionZ);
			}
		}

		public final int directionX;
		public final int directionZ;
		public final double offsetX;
		public final double offsetZ;
		public final int angle;
		/**
		 * 1 for cardinal directions and sqrt(2) for angle directions.
		 */
		public final double projectionFactor;

		CubeDirections(int x, int z, int a) {
			directionX = x;
			directionZ = z;
			angle = a;

			offsetX = Math.cos(Math.toRadians(angle));
			offsetZ = Math.sin(Math.toRadians(angle));

			projectionFactor = ReikaMathLibrary.py3d(directionX, 0, directionZ);
		}

		private static CubeDirections getShiftedIndex(int i, int d) {
			int o = ((i + d) % list.length + list.length) % list.length;
			return list[o];
		}

		public static CubeDirections getFromVectors(double dx, double dz) {
			return dirMap.get((int) Math.signum(dx), (int) Math.signum(dz));
		}

		public static CubeDirections getFromDirection(Direction dir) {
			return getFromVectors(dir.getStepX(), dir.getStepZ());
		}

		public CubeDirections getRotation(boolean clockwise) {
			return this.getRotation(clockwise, 1);
		}

		public CubeDirections getRotation(boolean clockwise, int num) {
			int d = clockwise ? num : -num;
			return getShiftedIndex(this.ordinal(), d);
		}

		public CubeDirections getOpposite() {
			return getShiftedIndex(this.ordinal(), 4);
		}

		public boolean isCardinal() {
			return directionX == 0 || directionZ == 0;
		}

		public Direction getCardinal() {
			return this.isCardinal() ? ReikaDirectionHelper.getByDirection(directionX, directionZ) : null;
		}
	}

	public enum FanDirections {
		N(0, -2, 90),
		NNE(1, -2, 67.5),
		NE(1, -1, 45),
		ENE(2, -1, 22.5),
		E(2, 0, 0),
		ESE(2, 1, 337.5),
		SE(1, 1, 315),
		SSE(1, 2, 292.5),
		S(0, 2, 270),
		SSW(-1, 2, 247.5),
		SW(-1, 1, 225),
		WSW(-2, 1, 202.5),
		W(-2, 0, 180),
		WNW(-2, -1, 157.5),
		NW(-1, -1, 135),
		NNW(-1, -2, 112.5);

		public static final FanDirections[] list = values();
		private static final PluralMap<FanDirections> dirMap = new PluralMap(2);
		private static final HashMap<Double, FanDirections> angleMap = new HashMap();

		static {
			for (int i = 0; i < list.length; i++) {
				dirMap.put(list[i], list[i].directionX, list[i].directionZ);
				angleMap.put(list[i].angle, list[i]);
			}
		}

		public final int directionX;
		public final int directionZ;
		public final int normalizedX;
		public final int normalizedZ;
		public final double offsetX;
		public final double offsetZ;
		public final double angle;

		FanDirections(int x, int z, double a) {
			directionX = x;
			directionZ = z;

			normalizedX = z == 0 ? x : x / 2;
			normalizedZ = x == 0 ? z : z / 2;

			angle = a;

			offsetX = Math.cos(Math.toRadians(angle));
			offsetZ = Math.sin(Math.toRadians(angle));
		}

		private static FanDirections getShiftedIndex(int i, int d) {
			int o = ((i + d) % list.length + list.length) % list.length;
			return list[o];
		}

		public static FanDirections getFromVectors(int dx, int dz) {
			if (dx == 0 && Math.abs(dz) == 1)
				dz *= 2;
			else if (dz == 0 && Math.abs(dx) == 1)
				dx *= 2;
			return dirMap.get(dx, dz);
		}

		public static FanDirections getFromPlayerLook(Player ep) {
			return getFromAngle(-ep.yHeadRot - 90);
		}

		public static FanDirections getFromAngle(double angle) {
			angle = (angle + 360) % 360;
			angle = ReikaMathLibrary.roundToNearestFraction(angle, 22.5);
			angle = (angle + 360) % 360;
			return angleMap.get(angle);
		}

		public FanDirections getRotation(boolean clockwise) {
			return this.getRotation(clockwise, 1);
		}

		public FanDirections getRotation(boolean clockwise, int num) {
			int d = clockwise ? num : -num;
			return getShiftedIndex(this.ordinal(), d);
		}

		public FanDirections getOpposite() {
			return getShiftedIndex(this.ordinal(), 4);
		}

		public boolean isCardinal() {
			return directionX == 0 || directionZ == 0;
		}

		public boolean isOctagonal() {
			return this.name().length() <= 2;
		}
	}

}
