package reika.dragonapi.instantiable;

import net.minecraft.core.BlockPos;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

import java.awt.*;
import java.util.Comparator;

public class Comparators {

    public static class CoordinateDistanceComparator implements Comparator<BlockPos> {

        private final BlockPos target;

        public CoordinateDistanceComparator(BlockPos p) {
            target = p;
        }

        @Override
        public int compare(BlockPos o1, BlockPos o2) {
            return (int) Math.signum(ReikaMathLibrary.py3d(o1.getX() - target.getX(), o1.getY() - target.getY(), o1.getZ() - target.getZ()));
        }

    }


    public static class PointDistanceComparator implements Comparator<Point> {

        private final Point target;

        public PointDistanceComparator(Point p) {
            target = p;
        }

        @Override
        public int compare(Point o1, Point o2) {
            return (int) Math.signum(ReikaMathLibrary.py3d(o1.x - target.x, 0, o1.y - target.y));
        }

    }
}
