
/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.math;

import com.mojang.blaze3d.shaders.BlendMode;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.lwjgl.opengl.GL11;
import reika.dragonapi.instantiable.data.immutable.DecimalPosition;
import reika.dragonapi.libraries.java.ReikaRandomHelper;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

import java.util.ArrayList;
import java.util.List;

public class Spline {

    public final SplineType type;
    private final List<SplineAnchor> anchors = new ArrayList<>();

    public Spline(SplineType t) {
        type = t;
    }

    public void update() {
        for (SplineAnchor a : anchors) {
            a.update();
        }
    }

    public void addPoint(SplineAnchor a) {
        anchors.add(a);
    }

    public DecimalPosition getLast() {
        return anchors.get(anchors.size() - 1).asPosition();
    }

    public List<DecimalPosition> get(int fineness, boolean closed) {
        if (fineness < 2) {
            throw new IllegalArgumentException("The fineness parameter must be greater than 2, since 2 points is just the linear segment.");
        }
        return this.interpolate(fineness, closed);
    }


    public void render(double x, double y, double z, int color, boolean glow, boolean closed, int fineness, float lineWidthFactor, BlendMode blend) {
        var tesselator = Tesselator.getInstance();
        var renderer = tesselator.getBuilder();

//        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        RenderSystem.depthMask(false);
        var li = this.get(fineness, closed);
        RenderSystem.enableBlend();
        blend.apply();
//        GL11.glDisable(GL11.GL_TEXTURE_2D);
//        RenderSystem.disableTexture();
        float w = 0xB21;//GL11.glGetFloat(GL11.GL_LINE_WIDTH); //todo line width
        renderer.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION); // GL11.GL_LINES
        int a = ReikaColorAPI.getAlpha(color);
        int clr = color & 0xffffff;
//  todo      if (blend.isColorBlending() && a < 255)
//            clr = ReikaColorAPI.getColorWithBrightnessMultiplier(clr, a / 255F / lineWidthFactor);
        renderer.color(clr, clr, clr, a);
        this.renderPoints(renderer, li, x, y, z, closed);

        if (glow) {
            renderer.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION); // GL11.GL_LINES -TODO VertexFormat.Mode.LINES ALSO FIX COLOR PLS MAX
            renderer.color(clr, clr, clr, a / 4);
            RenderSystem.lineWidth(5 * lineWidthFactor);
            this.renderPoints(renderer, li, x, y, z, closed);
            tesselator.end();

            renderer.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION); // GL11.GL_LINES
//    todo        if (blend.isColorBlending())
//                clr = ReikaColorAPI.getColorWithBrightnessMultiplier(clr, a / 4 / 255F);
            renderer.color(clr, clr, clr, a / 4);
            RenderSystem.lineWidth(10 * lineWidthFactor);
            this.renderPoints(renderer, li, x, y, z, closed);
            tesselator.end();
        }
        RenderSystem.lineWidth(w);
//        GL11.glPopAttrib();
    }

    private void renderPoints(BufferBuilder renderer, List<DecimalPosition> li, double x, double y, double z, boolean closed) {
        for (DecimalPosition d : li) {
            renderer.vertex(x + d.xCoord, y + d.yCoord, z + d.zCoord);
        }
        if (closed) {
            DecimalPosition d = li.get(0);
            renderer.vertex(x + d.xCoord, y + d.yCoord, z + d.zCoord);
        }
    }

    /**
     * This method will calculate the Catmull-Rom interpolation curve, returning
     * it as a list of DecimalPosition DecimalPosition objects.  This method in particular
     * adds the first and last control points which are not visible, but required
     * for calculating the spline.
     * <p>
     * //@param s    The list of original straight line points to calculate
     * an interpolation from.
     *
     * @param fine The integer number of equally spaced points to
     *             return along each curve.  The actual distance between each
     *             point will depend on the spacing between the control points.
     * @return The list of interpolated DecimalPosition.
     * @ curveType Chordal (stiff), Uniform(floppy), or Centripetal(medium)
     */
    private List<DecimalPosition> interpolate(int fine, boolean closed) {
        List<DecimalPosition> vertices = new ArrayList<>();
        for (SplineAnchor a : anchors) {
            vertices.add(a.asPosition());
        }

        // Cannot interpolate curves given only two points.  Two points
        // is best represented as a simple line segment.
        if (vertices.size() < 3) {
            return vertices;
        }

        // Test whether the shape is open or closed by checking to see if
        // the first point intersects with the last point.  M and Z are ignored.
        if (closed) {
            /**
             Reika fix: this algorithm above assumes that "closed" is only and always true if start and end points match, rather than just a
             boolean flag indicating "try and close the loop". As such it needs correction, though the general logic still applies.
             */
            vertices.add(vertices.get(0));

            // Use the second and second from last points as control points.
            // get the second point.
            DecimalPosition p2 = vertices.get(1).copy();
            // get the point before the last point
            DecimalPosition pn1 = vertices.get(vertices.size() - 2).copy();

            // insert the second from the last point as the first point in the list
            // because when the shape is closed it keeps wrapping around to
            // the second point.
            vertices.add(0, pn1);
            // add the second point to the end.
            vertices.add(p2);
        } else {
            // The shape is open, so use control points that simply extend
            // the first and last segments

            // Get the change in x and y between the first and second DecimalPositioninates.
            double dx = vertices.get(1).xCoord - vertices.get(0).xCoord;
            double dy = vertices.get(1).yCoord - vertices.get(0).yCoord;
            double dz = vertices.get(1).zCoord - vertices.get(0).zCoord;

            // Then using the change, extrapolate backwards to find a control point.
            double x1 = vertices.get(0).xCoord - dx;
            double y1 = vertices.get(0).yCoord - dy;
            double z1 = vertices.get(0).zCoord - dz;

            // Actaully create the start point from the extrapolated values.
            DecimalPosition start = new DecimalPosition(x1, y1, z1);

            // Repeat for the end control point.
            int n = vertices.size() - 1;
            dx = vertices.get(n).xCoord - vertices.get(n - 1).xCoord;
            dy = vertices.get(n).yCoord - vertices.get(n - 1).yCoord;
            dz = vertices.get(n).zCoord - vertices.get(n - 1).zCoord;
            double xn = vertices.get(n).xCoord + dx;
            double yn = vertices.get(n).yCoord + dy;
            double zn = vertices.get(n).zCoord + dz;
            DecimalPosition end = new DecimalPosition(xn, yn, zn);

            // insert the start control point at the start of the vertices list.
            vertices.add(0, start);

            // append the end control ponit to the end of the vertices list.
            vertices.add(end);
        }

        // Dimension a result list of DecimalPositioninates.
        List<DecimalPosition> result = new ArrayList<>();
        // When looping, remember that each cycle requires 4 points, starting
        // with i and ending with i+3.  So we don't loop through all the points.
        for (int i = 0; i < vertices.size() - 3; i++) {

            // Actually calculate the Catmull-Rom curve for one segment.
            List<DecimalPosition> points = this.interpolatePoints(vertices, i, fine);
            // Since the middle points are added twice, once for each bordering
            // segment, we only add the 0 index result point for the first
            // segment.  Otherwise we will have duplicate points.
            if (result.size() > 0) {
                points.remove(0);
            }

            // Add the DecimalPositioninates for the segment to the result list.
            result.addAll(points);
        }
        return result;

    }

    /**
     * Given a list of control points, this will create a list of pointsPerSegment
     * points spaced uniformly along the resulting Catmull-Rom curve.
     *
     * @param points           The list of control points, leading and ending with a
     *                         DecimalPositioninate that is only used for controling the spline and is not visualized.
     * @param index            The index of control point p0, where p0, p1, p2, and p3 are
     *                         used in order to create a curve between p1 and p2.
     * @param pointsPerSegment The total number of uniformly spaced interpolated
     *                         points to calculate for each segment. The larger this number, the
     *                         smoother the resulting curve.
     *                         curveType Clarifies whether the curve should use uniform, chordal
     *                         or centripetal curve types. Uniform can produce loops, chordal can
     *                         produce large distortions from the original lines, and centripetal is an
     *                         optimal balance without spaces.
     * @return the list of DecimalPositioninates that define the CatmullRom curve
     * between the points defined by index+1 and index+2.
     */
    private List<DecimalPosition> interpolatePoints(List<DecimalPosition> points, int index, int pointsPerSegment) {
        List<DecimalPosition> result = new ArrayList<>();
        double[] x = new double[4];
        double[] y = new double[4];
        double[] z = new double[4];
        double[] time = new double[4];
        for (int i = 0; i < 4; i++) {
            x[i] = points.get(index + i).xCoord;
            y[i] = points.get(index + i).yCoord;
            z[i] = points.get(index + i).zCoord;
            time[i] = i;
        }

        double tstart = 1;
        double tend = 2;
        double power = type.power;
        if (power > 0) {
            double total = 0;
            for (int i = 1; i < 4; i++) {
                double dx = x[i] - x[i - 1];
                double dy = y[i] - y[i - 1];
                double dz = z[i] - z[i - 1];
                total += Math.pow(dx * dx + dy * dy + dz * dz, power);
                time[i] = total;
            }
            tstart = time[1];
            tend = time[2];
        }
        //double z1 = 0.0;
        //double z2 = 0.0;
		/*
		if (!Double.isNaN(points.get(index+1).zCoord)) {
			z1 = points.get(index+1).zCoord;
		}
		if (!Double.isNaN(points.get(index+2).zCoord)) {
			z2 = points.get(index+2).zCoord;
		}
		 */
        //double dz = z2-z1;
        int segments = pointsPerSegment - 1;
        result.add(points.get(index + 1));
        for (int i = 1; i < segments; i++) {
            double xi = this.interpolate(x, time, tstart + (i * (tend - tstart)) / segments);
            double yi = this.interpolate(y, time, tstart + (i * (tend - tstart)) / segments);
            double zi = this.interpolate(z, time, tstart + (i * (tend - tstart)) / segments);//z1+(dz*i)/segments;
            result.add(new DecimalPosition(xi, yi, zi));
        }
        result.add(points.get(index + 2));
        return result;
    }

    /**
     * Unlike the other implementation here, which uses the default "uniform"
     * treatment of t, this computation is used to calculate the same values but
     * introduces the ability to "parameterize" the t values used in the
     * calculation. This is based on Figure 3 from
     * http://www.cemyuksel.com/research/catmullrom_param/catmullrom.pdf
     *
     * @param p    An array of double values of length 4, where interpolation
     *             occurs from p1 to p2.
     * @param time An array of time measures of length 4, corresponding to each
     *             p value.
     * @param t    the actual interpolation ratio from 0 to 1 representing the
     *             position between p1 and p2 to interpolate the value.
     * @return
     */
    private double interpolate(double[] p, double[] time, double t) {
        double L01 = p[0] * (time[1] - t) / (time[1] - time[0]) + p[1] * (t - time[0]) / (time[1] - time[0]);
        double L12 = p[1] * (time[2] - t) / (time[2] - time[1]) + p[2] * (t - time[1]) / (time[2] - time[1]);
        double L23 = p[2] * (time[3] - t) / (time[3] - time[2]) + p[3] * (t - time[2]) / (time[3] - time[2]);
        double L012 = L01 * (time[2] - t) / (time[2] - time[0]) + L12 * (t - time[0]) / (time[2] - time[0]);
        double L123 = L12 * (time[3] - t) / (time[3] - time[1]) + L23 * (t - time[1]) / (time[3] - time[1]);
        double C12 = L012 * (time[2] - t) / (time[2] - time[1]) + L123 * (t - time[1]) / (time[2] - time[1]);
        return C12;
    }

    @Override
    public String toString() {
        return anchors.toString();
    }

    public int length() {
        return anchors.size();
    }

    public enum SplineType {
        UNIFORM(0),
        CENTRIPETAL(0.25),
        CHORDAL(0.5);

        private final double power;

        SplineType(double p) {
            power = p;
        }
    }

    public interface SplineAnchor {

        void update();

        DecimalPosition asPosition();

        String toString();

    }

    public static class BasicSplinePoint implements SplineAnchor {

        protected double posX;
        protected double posY;
        protected double posZ;

        private DecimalPosition relative;

        public BasicSplinePoint(double x, double y, double z) {
            posX = x;
            posY = y;
            posZ = z;
        }

        public BasicSplinePoint(DecimalPosition p) {
            this(p.xCoord, p.yCoord, p.zCoord);
        }

        public BasicSplinePoint setRelativeTo(double x, double y, double z) {
            return this.setRelativeTo(new DecimalPosition(x, y, z));
        }

        public BasicSplinePoint setRelativeTo(DecimalPosition p) {
            relative = p;
            return this;
        }

        @Override
        public void update() {

        }

        @Override
        public final DecimalPosition asPosition() {
            return relative != null ? relative.offset(posX, posY, posZ) : new DecimalPosition(posX, posY, posZ);
        }

        @Override
        public String toString() {
            return this.asPosition().toString();
        }

    }

    public static class BasicVariablePoint extends BasicSplinePoint {

        private final double velocity;
        private final double variance;
        private final DecimalPosition origin;
        public double tolerance = 1;
        private double targetX;
        private double targetY;
        private double targetZ;

        public BasicVariablePoint(DecimalPosition pos, double var, double vel) {
            super(pos.xCoord, pos.yCoord, pos.zCoord);
            origin = pos;
            variance = var;
            velocity = vel;

            this.pickNewTarget();
        }

        @Override
        public void update() {
            double dx = targetX - posX;
            double dy = targetY - posY;
            double dz = targetZ - posZ;

            if (this.atTarget(dx, dy, dz)) {
                this.pickNewTarget();
            }

            this.move(dx, dy, dz);
        }

        private void move(double dx, double dy, double dz) {
            //ReikaJavaLibrary.pConsole(dx+":"+dy+":"+dz+" from "+targetX+":"+targetY+":"+targetZ+" @ "+posX+":"+posY+":"+posZ);
            if (Math.abs(dx) >= tolerance)
                posX += velocity * Math.signum(dx);
            if (Math.abs(dy) >= tolerance)
                posY += velocity * Math.signum(dy);
            if (Math.abs(dz) >= tolerance)
                posZ += velocity * Math.signum(dz);
        }

        private boolean atTarget(double dx, double dy, double dz) {
            return Math.abs(dx) < tolerance && Math.abs(dy) < tolerance && Math.abs(dz) < tolerance;
        }

        private void pickNewTarget() {
            targetX = ReikaRandomHelper.getRandomPlusMinus(origin.xCoord, variance);
            targetY = ReikaRandomHelper.getRandomPlusMinus(origin.yCoord, variance);
            targetZ = ReikaRandomHelper.getRandomPlusMinus(origin.zCoord, variance);
        }

        @Override
        public String toString() {
            return super.toString() + " varies " + variance + "/" + velocity;
        }
    }

}
