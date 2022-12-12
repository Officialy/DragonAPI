package reika.dragonapi.instantiable.data.blockstruct;

import net.minecraft.core.BlockPos;
import reika.dragonapi.instantiable.data.SphericalVector;
import reika.dragonapi.instantiable.data.immutable.BlockBox;
import reika.dragonapi.instantiable.data.immutable.DecimalPosition;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

public class CurvedTrajectory {

    private final DecimalPosition origin;

    public int trailCount = 1;
    public float trailForkChance = 0;
    public BlockBox bounds = BlockBox.infinity();

    private final HashSet<BlockPos> locations = new HashSet<>();

    private final Collection<Trail> trails = new ArrayList<>();

    public CurvedTrajectory(int x, int y, int z) {
        origin = new DecimalPosition(x+0.5, y+0.5, z+0.5);
    }
    public CurvedTrajectory(BlockPos pos) {
        origin = new DecimalPosition(pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5);
    }
    public void generatePaths(long seed, TrailShape ts) {
        this.generatePaths(seed, ts, null);
    }

    public void generatePaths(long seed, TrailShape ts, InitialAngleProvider p) {
        Random rand = new Random(seed);
        for (int i = 0; i < trailCount; i++) {
            trails.add(new Trail(origin, seed, p));
        }
        while (!trails.isEmpty()) {
            ArrayList<Trail> newTrails = trailForkChance > 0 ? new ArrayList<>() : null;
            Iterator<Trail> it = trails.iterator();
            while (it.hasNext()) {
                Trail t = it.next();
                t.step();
                if (bounds.asAABB().intersects(t.position.xCoord, t.position.yCoord, t.position.zCoord, t.position.xCoord, t.position.yCoord, t.position.zCoord)) {
                    Collection<BlockPos> li = ts.getBlocks(t.position);
                    if (li.isEmpty()) {
                        it.remove();
                    }
                    else {
                        boolean flag = false;
                        for (BlockPos c : li) {
                            if (locations.add(c))
                                flag = true;
                        }
                        if (!flag)
                            ;//it.remove();
                        if (rand.nextFloat() < trailForkChance) {
                            Trail t2 = t.copy();
                            t2.updateVelocity();
                            t2.targetPhi += -10+rand.nextDouble()*20;
                            t2.targetTheta += -10+rand.nextDouble()*20;
                            newTrails.add(t2);
                        }
                        t.updateVelocity();
                    }
                }
                else {
                    it.remove();
                }
            }
            if (newTrails != null)
                trails.addAll(newTrails);
        }
    }

    public HashSet<BlockPos> getLocations() {
        return new HashSet<>(locations);
    }

    public interface TrailShape {

        Collection<BlockPos> getBlocks(DecimalPosition pos);

    }

    public interface InitialAngleProvider {

        double getInitialTheta(Random rand, int trail);
        double getInitialPhi(Random rand, int trail);

    }

    private class Trail {

        private DecimalPosition position;
        private final long seed;
        private final Random rand;
        private final SphericalVector velocity;

        private double targetTheta;
        private double targetPhi;

        private Trail(DecimalPosition pos, long seed, InitialAngleProvider p) {
            this.seed = seed;
            rand = new Random(seed);
            position = pos;
            velocity = new SphericalVector(0.25, p != null ? p.getInitialTheta(rand, trails.size()) : rand.nextInt(360), p != null ? p.getInitialPhi(rand, trails.size()) : rand.nextInt(360));
        }

        public Trail copy() {
            Trail t = new Trail(position, seed, null);
            t.velocity.inclination = velocity.inclination;
            t.velocity.rotation = velocity.magnitude;
            t.targetPhi = targetPhi;
            t.targetTheta = targetTheta;
            return t;
        }

        private void step() {
            double[] xyz = velocity.getCartesian();
            position = position.offset(xyz[0], xyz[1], xyz[2]);
        }

        private void updateVelocity() {
            if (ReikaMathLibrary.approxr(velocity.inclination, targetTheta, 2)) {
                targetTheta = rand.nextInt(360);
            }
            else {
                if (targetTheta > velocity.inclination)
                    velocity.inclination += 2;
                else
                    velocity.inclination -= 2;
            }

            if (ReikaMathLibrary.approxr(velocity.rotation, targetPhi, 2)) {
                targetPhi = rand.nextInt(360);
            }
            else {
                if (targetPhi > velocity.rotation)
                    velocity.rotation += 2;
                else
                    velocity.rotation -= 2;
            }
        }

    }

}
