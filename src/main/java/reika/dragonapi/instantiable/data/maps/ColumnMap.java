package reika.dragonapi.instantiable.data.maps;

import net.minecraft.core.BlockPos;
import reika.dragonapi.instantiable.data.immutable.Column;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class ColumnMap {

    private final HashMap<BlockPos, ColumnSet> data = new HashMap<>();

    public void addColumn(int x, int z, int y1, int y2) {
        this.addColumn(new BlockPos(x, 0, z), y1, y2);
    }

    public void addColumn(BlockPos loc, int y1, int y2) {
        BlockPos key = new BlockPos(loc.getX(), 0, loc.getZ());
        ColumnSet c = data.get(key);
        if (c == null) {
            c = new ColumnSet();
            data.put(key, c);
        }
        c.addColumn(y1, y2);
    }

    public void addBlock(BlockPos loc) {
        this.addColumn(loc, loc.getY(), loc.getY());
    }

    public Collection<Column> getColumns(int x, int z) {
        return this.getColumns(new BlockPos(x, 0, z));
    }

    public Collection<Column> getColumns(BlockPos c) {
        ColumnSet set = data.get(new BlockPos(c.getX(), 0, c.getZ()));
        return set != null ? set.getColumns() : new ArrayList<>();
    }

    private static class ColumnSet {

        //private final ArrayList<Column> columns = new ArrayList<>();

        private boolean[] data = new boolean[256];

        private int minY = Integer.MAX_VALUE;
        private int maxY = Integer.MIN_VALUE;

        private void addColumn(int y1, int y2) {
            //columns.add(new Column(y1, y2));
            for (int y = y1; y <= y2; y++) {
                data[y] = true;
            }
            minY = Math.min(minY, y1);
            maxY = Math.max(maxY, y2);
        }

        public Collection<Column> getColumns() {
            Collection<Column> li = new ArrayList<>();
            boolean active = true;
            int startY = minY;
            for (int y = minY; y <= maxY; y++) {
                if (active && !data[y]) {
                    li.add(new Column(startY, y-1));
                    active = false;
                }
                else if (data[y]) {
                    if (!active)
                        startY = y;
                    active = true;
                }
            }
            li.add(new Column(startY, maxY));
            return li;
        }

    }
}
