package reika.dragonapi.instantiable.math.hexgrid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.world.phys.Vec3;

import reika.dragonapi.libraries.mathsci.ReikaVectorHelper;

/**
 * Cube-coordinate hex grid, ported from 1.7.10 {@code Instantiable/Math/HexGrid}.
 *
 * <p>Hexes are addressed by the usual {@code (q, r, s)} cube coordinates constrained to
 * {@code q + r + s == 0}, which makes neighbours, distance and line-drawing fall out as plain vector
 * arithmetic. Two orientations are supported — flat-topped and angled — each carrying its own
 * forward/backward basis matrix, so the same grid maths serves either.
 *
 * <p>This port covers the geometry in full. The original also carried four immediate-mode drawing
 * methods ({@code drawHexEdges}, {@code drawFilledHex}, {@code drawTexturedGrid},
 * {@code drawTexturedHex}) built on 1.7.10's {@code Tessellator} and raw {@code GL11} calls. Their
 * only consumers are ChromatiCraft's lore-book screens, which are themselves still unported, and
 * there is no modern render context to target until those land — so they are deliberately absent
 * rather than guessed at. See DRAGONAPI-PORT below.
 */
// DRAGONAPI-PORT: HexGrid's rendering half. V33a drew, per hex, a GL_LINE_LOOP outline at line width
// 3 with texturing disabled, a filled triangle fan, and textured quads whose UVs came from a hex's
// position within the grid's bounding box (see GridProperties). Restore these against the 26.2 submit
// pipeline when the lore-book GUI is ported, and take the geometry from getHexLocation/hexSize as the
// original did rather than re-deriving it.
public class HexGrid {

	private static final List<Hex> DIRECTIONS = List.of(
			new Hex(1, 1, -1, 0), new Hex(1, 0, -1, 1), new Hex(1, -1, 0, 1),
			new Hex(1, -1, 1, 0), new Hex(1, 0, 1, -1), new Hex(1, 1, 0, -1));

	private static final List<Hex> DIAGONALS = List.of(
			new Hex(1, 2, -1, -1), new Hex(1, 1, -2, 1), new Hex(1, -1, -1, 2),
			new Hex(1, -2, 1, 1), new Hex(1, -1, 2, -1), new Hex(1, 1, 1, -2));

	private static final Orientation ANGLED = new Orientation(Math.sqrt(3D), Math.sqrt(3D) / 2D, 0D,
			3D / 2D, Math.sqrt(3D) / 3D, -1D / 3D, 0D, 2D / 3D, 0.5);
	private static final Orientation FLAT = new Orientation(3D / 2D, 0D, Math.sqrt(3D) / 2D,
			Math.sqrt(3D), 2D / 3D, 0D, -1D / 3D, Math.sqrt(3D) / 3D, 0D);

	/** Cube coordinate used as the map key; the original used DragonAPI's Coordinate for this. */
	private record Cell(int q, int r, int s) {}

	private final Map<Cell, Hex> hexes = new HashMap<>();

	public final int size;
	public final double hexSize;

	private final Orientation style;
	private final MapShape shape;

	private GridProperties properties;

	/** Size is a diameter. */
	public HexGrid(int s, double s2, boolean flatTop, MapShape shape) {
		style = flatTop ? FLAT : ANGLED;
		size = s;
		this.shape = shape;
		hexSize = s2;
	}

	public GridProperties getGridProperties() {
		if (properties == null)
			properties = new GridProperties(this);
		return properties;
	}

	public HexGrid addHex(Hex h) {
		return this.addHex(h.q, h.r, h.s);
	}

	public HexGrid addHex(int q, int r, int s) {
		if (q + r + s != 0)
			throw new IllegalArgumentException("Q, R, and S must sum to zero!");
		if (!shape.isInGrid(q, r, s, size))
			throw new IllegalArgumentException("Position outside grid!");
		hexes.computeIfAbsent(new Cell(q, r, s), k -> new Hex(hexSize, q, r, s));
		properties = null;
		return this;
	}

	/** Expands in rings; ideal for creating hexagonal shapes. */
	public HexGrid flower() {
		this.addHex(0, 0, 0);
		for (int i = 1; i < size; i += 2) {
			Collection<Hex> copy = new HashSet<>(this.getAllHexes());
			for (Hex h : copy)
				for (Hex neighbour : h.getNeighbors())
					if (shape.isInGrid(neighbour, size))
						this.addHex(neighbour);
		}
		return this;
	}

	public int cellCount() {
		return hexes.size();
	}

	public Collection<Hex> getAllHexes() {
		return Collections.unmodifiableCollection(hexes.values());
	}

	public Hex getHex(int q, int r, int s) {
		return hexes.get(new Cell(q, r, s));
	}

	public Hex getRandomEdgeCell(Random rand) {
		int d = rand.nextInt(6);
		Hex h = new Hex(hexSize, 0, 0, 0);
		while (this.containsHex(h.getNeighbor(d)))
			h = h.getNeighbor(d);
		return h;
	}

	public boolean isHexAtEdge(Hex h) {
		return Math.abs(h.q) + Math.abs(h.r) + Math.abs(h.s) == size - 1;
	}

	public Point getHexLocation(Hex h) {
		double x = (style.f0 * h.q + style.f1 * h.r) * hexSize / 2D;
		double y = (style.f2 * h.q + style.f3 * h.r) * hexSize / 2D;
		return new Point(x, y);
	}

	public Hex getHexAtLocation(int x, int y) {
		Hex h = this.getFractionalHexAtLocation(x, y).hexRound();
		return this.containsHex(h) ? h : null;
	}

	public boolean containsHex(Hex h) {
		return hexes.containsKey(new Cell(h.q, h.r, h.s));
	}

	public boolean containsHex(int q, int r, int s) {
		return hexes.containsKey(new Cell(q, r, s));
	}

	private FractionalHex getFractionalHexAtLocation(int x, int y) {
		double dx = x * 2D / hexSize;
		double dy = y * 2D / hexSize;
		double q = style.b0 * dx + style.b1 * dy;
		double r = style.b2 * dx + style.b3 * dy;
		return new FractionalHex(hexSize, q, r, -q - r);
	}

	public int getNeighborDirection(double angle) {
		return (int)Math.floor(((angle + 360D) % 360D) / 60D);
	}

	public List<Integer> getValidMovementDirections(Hex location) {
		List<Integer> ret = new ArrayList<>();
		for (int i = 0; i < 6; i++)
			if (this.containsHex(location.getNeighbor(i)))
				ret.add(i);
		return ret;
	}

	public int getOppositeDirection(int dir) {
		return (dir + 3) % 6;
	}

	public Collection<Hex> getRegion(Hex start, Collection<Hex> exclusions) {
		return this.getRegion(start, new HashSet<>(exclusions));
	}

	private Collection<Hex> getRegion(Hex start, HashSet<Hex> exclusions) {
		exclusions.add(start);
		Collection<Hex> ret = new HashSet<>();
		ret.add(start);
		for (Hex h : start.getNeighbors())
			if (this.containsHex(h) && !exclusions.contains(h))
				ret.addAll(this.getRegion(h, exclusions));
		return ret;
	}

	public boolean dividesGrid(Hex start, Collection<Hex> exclusions) {
		return this.dividesGrid(start, new HashSet<>(exclusions));
	}

	private boolean dividesGrid(Hex start, HashSet<Hex> exclusions) {
		Collection<Hex> region = null;
		for (Hex h : start.getNeighbors()) {
			if (this.containsHex(h) && !exclusions.contains(h)) {
				Collection<Hex> other = this.getRegion(start, exclusions);
				if (region == null)
					region = other;
				if (!region.equals(other))
					return true;
			}
		}
		return false;
	}

	/** Which shapes a grid may take. Only HEXAGON was ever implemented upstream. */
	public enum MapShape {
		RECTANGLE,
		TRIANGLE,
		HEXAGON,
		RHOMBUS;

		public boolean isInGrid(Hex h, int diameter) {
			return this.isInGrid(h.q, h.r, h.s, diameter);
		}

		public boolean isInGrid(int q, int r, int s, int diameter) {
			// The other three branches are abandoned upstream, marked TODO and falling through to
			// false; that is preserved rather than invented here.
			return this == HEXAGON && Math.abs(q) + Math.abs(r) + Math.abs(s) <= diameter - 1;
		}
	}

    public record Hex(double size, int q, int r, int s) {

        public Hex(double size, int q, int r, int s) {
            this.size = size;
            this.q = q;
            this.r = r;
            this.s = s;
            if (q + r + s != 0)
                throw new IllegalArgumentException("Q, R, and S must sum to zero!");
        }

        public Hex scale(int k) {
            return new Hex(size, q * k, r * k, s * k);
        }

        public Hex getNeighbor(int direction) {
            return add(size, this, DIRECTIONS.get(direction));
        }

        public Hex diagonalNeighbor(int direction) {
            return add(size, this, DIAGONALS.get(direction));
        }

        public int length() {
            return (Math.abs(q) + Math.abs(r) + Math.abs(s)) / 2;
        }

        public Collection<Hex> getNeighbors() {
            Collection<Hex> c = new HashSet<>();
            for (Hex h : DIRECTIONS)
                c.add(add(size, this, h));
            return c;
        }

        public static int distance(double size, Hex a, Hex b) {
            return subtract(size, a, b).length();
        }

        public Hex offset(Hex h) {
            return add(size, this, h);
        }

        public Hex offset(int q, int r, int s) {
            return add(size, this, new Hex(size, q, r, s));
        }

        public Hex subtract(Hex h) {
            return subtract(size, this, h);
        }

        public static Hex add(double size, Hex a, Hex b) {
            return new Hex(size, a.q + b.q, a.r + b.r, a.s + b.s);
        }

        public static Hex subtract(double size, Hex a, Hex b) {
            return new Hex(size, a.q - b.q, a.r - b.r, a.s - b.s);
        }

        @Override
        public int hashCode() {
            return (-q * 17 ^ r * 77) * s * 37;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Hex h && h.q == q && h.r == r && h.s == s;
        }

        @Override
        public String toString() {
            return q + "," + r + "," + s;
        }
    }

    private record FractionalHex(double size, double q, double r, double s) {

        /**
         * Rounds to the nearest cube coordinate, repairing whichever axis drifted furthest.
         */
        private Hex hexRound() {
            int rq = (int) Math.round(q);
            int rr = (int) Math.round(r);
            int rs = (int) Math.round(s);
            double dq = Math.abs(rq - q);
            double dr = Math.abs(rr - r);
            double ds = Math.abs(rs - s);
            if (dq > dr && dq > ds)
                rq = -rr - rs;
            else if (dr > ds)
                rr = -rq - rs;
            else
                rs = -rq - rr;
            return new Hex(size, rq, rr, rs);
        }

        private static FractionalHex lerp(double size, FractionalHex a, FractionalHex b, double t) {
            return new FractionalHex(size, a.q * (1 - t) + b.q * t, a.r * (1 - t) + b.r * t,
                    a.s * (1 - t) + b.s * t);
        }

        @Override
        public String toString() {
            return q + ", " + r + ", " + s;
        }
    }

	/** V33a hexLinedraw: the hexes a straight line between two cells passes through. */
	public static List<Hex> lineDraw(double size, Hex a, Hex b) {
		int n = Hex.distance(size, a, b);
		// The nudge avoids ties landing exactly on a cell boundary during rounding.
		FractionalHex an = new FractionalHex(a.size, a.q + 0.000001, a.r + 0.000001, a.s - 0.000002);
		FractionalHex bn = new FractionalHex(a.size, b.q + 0.000001, b.r + 0.000001, b.s - 0.000002);
		List<Hex> results = new ArrayList<>();
		double step = 1.0 / Math.max(n, 1);
		for (int i = 0; i <= n; i++)
			results.add(FractionalHex.lerp(size, an, bn, step * i).hexRound());
		return results;
	}

    /**
     * Square-grid offset coordinates, for interoperating with rectangular storage.
     */
    public record OffsetCoord(int col, int row) {

        public static final int EVEN = 1;
        public static final int ODD = -1;

        public Hex roffsetToCube(double size, int offset) {
            int q = col - (row + offset * (row & 1)) / 2;
            return new Hex(size, q, row, -q - row);
        }

        public Hex qoffsetToCube(double size, int offset) {
            int r = row - (col + offset * (col & 1)) / 2;
            return new Hex(size, col, r, -col - r);
        }

        public static OffsetCoord roffsetFromCube(int offset, Hex h) {
            return new OffsetCoord(h.q + (h.r + offset * (h.r & 1)) / 2, h.r);
        }

        public static OffsetCoord qoffsetFromCube(int offset, Hex h) {
            return new OffsetCoord(h.q, h.r + (h.q + offset * (h.q & 1)) / 2);
        }
    }

    private record Orientation(double f0, double f1, double f2, double f3, double b0, double b1, double b2, double b3,
                               double startAngle) {


    }

    public record Point(double x, double y) {

        public Point translate(double dx, double dy) {
            return new Point(x + dx, y + dy);
        }

        public Point scale(double d) {
            return this.scale(d, d);
        }

        public Point scale(double sx, double sy) {
            return new Point(x * sx, y * sy);
        }

        /**
         * Rotates about a pivot in the XZ plane, as upstream did through ReikaVectorHelper.
         */
        public Point rotate(double r, int ox, int oz) {
            Vec3 ret = ReikaVectorHelper.rotateVector(new Vec3(x - ox, 0, y - oz), 0, r, 0);
            return new Point(ox + ret.x, oz + ret.z);
        }
    }

	public static final class GridProperties {

		public final double minX;
		public final double maxX;
		public final double minY;
		public final double maxY;
		public final double sizeX;
		public final double sizeY;

		private GridProperties(HexGrid g) {
			double nx = Double.POSITIVE_INFINITY;
			double px = Double.NEGATIVE_INFINITY;
			double ny = Double.POSITIVE_INFINITY;
			double py = Double.NEGATIVE_INFINITY;
			for (Hex h : g.hexes.values()) {
				Point p = g.getHexLocation(h);
				nx = Math.min(nx, p.x);
				ny = Math.min(ny, p.y);
				px = Math.max(px, p.x);
				py = Math.max(py, p.y);
			}
			minX = nx - g.hexSize / 2D;
			minY = ny - g.hexSize / 2D;
			maxX = px + g.hexSize / 2D;
			maxY = py + g.hexSize / 2D;
			sizeX = maxX - minX;
			sizeY = maxY - minY;
		}
	}
}
