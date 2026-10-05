package reika.dragonapi.instantiable.data.immutable;

public record Column(int minY, int maxY) {

    public Column topSlice(int y) {
        if (y > maxY)
            return null;
        return new Column(y, maxY);
    }

    public Column bottomSlice(int y) {
        if (y < minY)
            return null;
        return new Column(minY, y);
    }

    public Column extendTo(int y) {
        int y1 = Math.min(y, minY);
        int y2 = Math.max(y, maxY);
        return new Column(y1, y2);
    }

    public Column merge(Column c) {
        int y1 = Math.min(c.minY, minY);
        int y2 = Math.max(c.maxY, maxY);
        return new Column(y1, y2);
    }

    public boolean contains(int y) {
        return y >= minY && y <= maxY;
    }

}
