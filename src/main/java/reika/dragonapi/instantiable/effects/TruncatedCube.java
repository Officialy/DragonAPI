/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.effects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.core.Direction;

import reika.dragonapi.instantiable.data.immutable.DecimalPosition;

/**
 * A cube with its eight corners cut off: six octagonal faces and eight triangles. {@code mainSize} is the
 * half-width; {@code cutSize} is how far from each edge's midpoint the cut starts.
 *
 * <p>1.7.10 drew each face as a GL_TRIANGLE_FAN from the centre and outlined it with GL_LINE_LOOP. 26.2's
 * geometry pipelines are quads and line lists, so {@link #renderFaces} emits each fan triangle as a
 * degenerate quad and {@link #renderEdges} emits each loop as line segments; the shape is the same.
 */
public class TruncatedCube {

	public double mainSize;
	public double cutSize;

	private List<DecimalPosition>[] faces;
	private List<ArrayList<DecimalPosition>> corners;

	/** Args: cut size, main (half) size -- the 1.7.10 argument order. */
	public TruncatedCube(double s, double s2) {
		mainSize = s2;
		cutSize = s;
	}

	@SuppressWarnings("unchecked")
	public TruncatedCube cache(boolean startCenter, double x0, double y0, double z0) {
		faces = new List[6];
		for (Direction dir : Direction.values()) {
			faces[dir.ordinal()] = Collections.unmodifiableList(this.getFaceVertices(dir, startCenter, x0, y0, z0));
		}
		corners = Collections.unmodifiableList(this.getCornerVertices(x0, y0, z0));
		return this;
	}

	/** The face's perimeter, closed (first point repeated), optionally led by its centre for a fan. */
	public List<DecimalPosition> getFaceVertices(Direction face, boolean startCenter, double x0, double y0, double z0) {
		if (faces != null)
			return faces[face.ordinal()];
		ArrayList<DecimalPosition> li = new ArrayList<>();
		double m = mainSize;
		double c = cutSize;
		switch (face) {
			case DOWN -> {
				if (startCenter)
					li.add(new DecimalPosition(x0, y0 - m, z0));
				li.add(new DecimalPosition(x0 - c, y0 - m, z0 - m));
				li.add(new DecimalPosition(x0 + c, y0 - m, z0 - m));
				li.add(new DecimalPosition(x0 + m, y0 - m, z0 - c));
				li.add(new DecimalPosition(x0 + m, y0 - m, z0 + c));
				li.add(new DecimalPosition(x0 + c, y0 - m, z0 + m));
				li.add(new DecimalPosition(x0 - c, y0 - m, z0 + m));
				li.add(new DecimalPosition(x0 - m, y0 - m, z0 + c));
				li.add(new DecimalPosition(x0 - m, y0 - m, z0 - c));
				li.add(new DecimalPosition(x0 - c, y0 - m, z0 - m));
			}
			case UP -> {
				if (startCenter)
					li.add(new DecimalPosition(x0, y0 + m, z0));
				li.add(new DecimalPosition(x0 - c, y0 + m, z0 - m));
				li.add(new DecimalPosition(x0 - m, y0 + m, z0 - c));
				li.add(new DecimalPosition(x0 - m, y0 + m, z0 + c));
				li.add(new DecimalPosition(x0 - c, y0 + m, z0 + m));
				li.add(new DecimalPosition(x0 + c, y0 + m, z0 + m));
				li.add(new DecimalPosition(x0 + m, y0 + m, z0 + c));
				li.add(new DecimalPosition(x0 + m, y0 + m, z0 - c));
				li.add(new DecimalPosition(x0 + c, y0 + m, z0 - m));
				li.add(new DecimalPosition(x0 - c, y0 + m, z0 - m));
			}
			case WEST -> {
				if (startCenter)
					li.add(new DecimalPosition(x0 - m, y0, z0));
				li.add(new DecimalPosition(x0 - m, y0 - c, z0 - m));
				li.add(new DecimalPosition(x0 - m, y0 - m, z0 - c));
				li.add(new DecimalPosition(x0 - m, y0 - m, z0 + c));
				li.add(new DecimalPosition(x0 - m, y0 - c, z0 + m));
				li.add(new DecimalPosition(x0 - m, y0 + c, z0 + m));
				li.add(new DecimalPosition(x0 - m, y0 + m, z0 + c));
				li.add(new DecimalPosition(x0 - m, y0 + m, z0 - c));
				li.add(new DecimalPosition(x0 - m, y0 + c, z0 - m));
				li.add(new DecimalPosition(x0 - m, y0 - c, z0 - m));
			}
			case EAST -> {
				if (startCenter)
					li.add(new DecimalPosition(x0 + m, y0, z0));
				li.add(new DecimalPosition(x0 + m, y0 - c, z0 - m));
				li.add(new DecimalPosition(x0 + m, y0 + c, z0 - m));
				li.add(new DecimalPosition(x0 + m, y0 + m, z0 - c));
				li.add(new DecimalPosition(x0 + m, y0 + m, z0 + c));
				li.add(new DecimalPosition(x0 + m, y0 + c, z0 + m));
				li.add(new DecimalPosition(x0 + m, y0 - c, z0 + m));
				li.add(new DecimalPosition(x0 + m, y0 - m, z0 + c));
				li.add(new DecimalPosition(x0 + m, y0 - m, z0 - c));
				li.add(new DecimalPosition(x0 + m, y0 - c, z0 - m));
			}
			case SOUTH -> {
				if (startCenter)
					li.add(new DecimalPosition(x0, y0, z0 + m));
				li.add(new DecimalPosition(x0 - m, y0 - c, z0 + m));
				li.add(new DecimalPosition(x0 - c, y0 - m, z0 + m));
				li.add(new DecimalPosition(x0 + c, y0 - m, z0 + m));
				li.add(new DecimalPosition(x0 + m, y0 - c, z0 + m));
				li.add(new DecimalPosition(x0 + m, y0 + c, z0 + m));
				li.add(new DecimalPosition(x0 + c, y0 + m, z0 + m));
				li.add(new DecimalPosition(x0 - c, y0 + m, z0 + m));
				li.add(new DecimalPosition(x0 - m, y0 + c, z0 + m));
				li.add(new DecimalPosition(x0 - m, y0 - c, z0 + m));
			}
			case NORTH -> {
				if (startCenter)
					li.add(new DecimalPosition(x0, y0, z0 - m));
				li.add(new DecimalPosition(x0 - m, y0 - c, z0 - m));
				li.add(new DecimalPosition(x0 - m, y0 + c, z0 - m));
				li.add(new DecimalPosition(x0 - c, y0 + m, z0 - m));
				li.add(new DecimalPosition(x0 + c, y0 + m, z0 - m));
				li.add(new DecimalPosition(x0 + m, y0 + c, z0 - m));
				li.add(new DecimalPosition(x0 + m, y0 - c, z0 - m));
				li.add(new DecimalPosition(x0 + c, y0 - m, z0 - m));
				li.add(new DecimalPosition(x0 - c, y0 - m, z0 - m));
				li.add(new DecimalPosition(x0 - m, y0 - c, z0 - m));
			}
		}
		return li;
	}

	/** The eight corner triangles. */
	public List<ArrayList<DecimalPosition>> getCornerVertices(double x0, double y0, double z0) {
		if (corners != null)
			return corners;
		double m = mainSize;
		double c = cutSize;
		ArrayList<ArrayList<DecimalPosition>> li = new ArrayList<>();
		//top corners
		li.add(triangle(x0 + m, y0 + m, z0 - c, x0 + m, y0 + c, z0 - m, x0 + c, y0 + m, z0 - m));
		li.add(triangle(x0 - c, y0 + m, z0 - m, x0 - m, y0 + c, z0 - m, x0 - m, y0 + m, z0 - c));
		li.add(triangle(x0 + c, y0 + m, z0 + m, x0 + m, y0 + c, z0 + m, x0 + m, y0 + m, z0 + c));
		li.add(triangle(x0 - m, y0 + m, z0 + c, x0 - m, y0 + c, z0 + m, x0 - c, y0 + m, z0 + m));
		//bottom corners
		li.add(triangle(x0 + c, y0 - m, z0 - m, x0 + m, y0 - c, z0 - m, x0 + m, y0 - m, z0 - c));
		li.add(triangle(x0 - m, y0 - m, z0 - c, x0 - m, y0 - c, z0 - m, x0 - c, y0 - m, z0 - m));
		li.add(triangle(x0 + m, y0 - m, z0 + c, x0 + m, y0 - c, z0 + m, x0 + c, y0 - m, z0 + m));
		li.add(triangle(x0 - c, y0 - m, z0 + m, x0 - m, y0 - c, z0 + m, x0 - m, y0 - m, z0 + c));
		return li;
	}

	private static ArrayList<DecimalPosition> triangle(double x1, double y1, double z1, double x2, double y2, double z2,
			double x3, double y3, double z3) {
		ArrayList<DecimalPosition> li = new ArrayList<>();
		li.add(new DecimalPosition(x1, y1, z1));
		li.add(new DecimalPosition(x2, y2, z2));
		li.add(new DecimalPosition(x3, y3, z3));
		return li;
	}

	/** The filled faces and corners in {@code c1} (ARGB), on a QUADS position-colour pipeline. */
	public void renderFaces(PoseStack.Pose pose, VertexConsumer out, double x, double y, double z, int c1) {
		for (Direction dir : Direction.values()) {
			List<DecimalPosition> li = this.getFaceVertices(dir, true, x, y, z);
			DecimalPosition centre = li.get(0);
			for (int i = 1; i < li.size() - 1; i++) {
				vertex(pose, out, centre, c1);
				vertex(pose, out, li.get(i), c1);
				vertex(pose, out, li.get(i + 1), c1);
				vertex(pose, out, centre, c1);
			}
		}
		for (List<DecimalPosition> li : this.getCornerVertices(x, y, z)) {
			vertex(pose, out, li.get(0), c1);
			vertex(pose, out, li.get(1), c1);
			vertex(pose, out, li.get(2), c1);
			vertex(pose, out, li.get(0), c1);
		}
	}

	/**
	 * The face and corner outlines in {@code c2} (ARGB), on a LINES pipeline. 1.7.10 widened them as the viewer
	 * came closer: {@code max(0.125, 2 - pdist/16)} pixels.
	 */
	public void renderEdges(PoseStack.Pose pose, VertexConsumer out, double x, double y, double z, int c2, float pdist) {
		float w = Math.max(0.125F, 2F - 0.0625F * pdist);
		for (Direction dir : Direction.values()) {
			List<DecimalPosition> li = this.getFaceVertices(dir, false, x, y, z);
			for (int i = 0; i < li.size() - 1; i++)
				line(pose, out, li.get(i), li.get(i + 1), c2, w);
		}
		for (List<DecimalPosition> li : this.getCornerVertices(x, y, z)) {
			for (int i = 0; i < 3; i++)
				line(pose, out, li.get(i), li.get((i + 1) % 3), c2, w);
		}
	}

	private static void vertex(PoseStack.Pose pose, VertexConsumer out, DecimalPosition p, int color) {
		out.addVertex(pose, (float)p.xCoord, (float)p.yCoord, (float)p.zCoord).setColor(color);
	}

	private static void line(PoseStack.Pose pose, VertexConsumer out, DecimalPosition a, DecimalPosition b,
			int color, float width) {
		float nx = (float)(b.xCoord - a.xCoord);
		float ny = (float)(b.yCoord - a.yCoord);
		float nz = (float)(b.zCoord - a.zCoord);
		float len = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
		if (len > 0) {
			nx /= len;
			ny /= len;
			nz /= len;
		}
		out.addVertex(pose, (float)a.xCoord, (float)a.yCoord, (float)a.zCoord).setColor(color)
				.setNormal(pose, nx, ny, nz).setLineWidth(width);
		out.addVertex(pose, (float)b.xCoord, (float)b.yCoord, (float)b.zCoord).setColor(color)
				.setNormal(pose, nx, ny, nz).setLineWidth(width);
	}

}
