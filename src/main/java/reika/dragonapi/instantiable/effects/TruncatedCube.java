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
import net.minecraft.world.phys.Vec3;

/**
 * V33a DragonAPI {@code TruncatedCube}: a cube of half-size {@code mainSize} with every corner cut back to
 * {@code cutSize} from the edges, drawn as six octagonal faces plus eight corner triangles, optionally outlined.
 *
 * <p>1.7.10 drew the faces as triangle fans and the outline as line loops straight to the Tessellator. 26.x has
 * neither immediate mode nor loops in a shared buffer, so {@link #render} emits the faces as a triangle list and
 * the outline as line segments, each into the consumer of whatever render type the caller chose (V33a's callers set
 * ADDITIVEDARK, unlit and without depth writes).
 */
public class TruncatedCube {

	/** V33a {@code ForgeDirection.VALID_DIRECTIONS} order. */
	private static final Direction[] FACES = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH,
			Direction.WEST, Direction.EAST};

	public double mainSize;
	public double cutSize;
	private List<Vec3>[] faces;
	private List<List<Vec3>> corners;

	public TruncatedCube(double s, double s2) {
		mainSize = s2;
		cutSize = s;
	}

	@SuppressWarnings("unchecked")
	public TruncatedCube cache(boolean startCenter, double x0, double y0, double z0) {
		List<Vec3>[] f = new List[6];
		for (int i = 0; i < 6; i++) {
			f[i] = Collections.unmodifiableList(this.getFaceVertices(FACES[i], startCenter, x0, y0, z0));
		}
		List<List<Vec3>> c = Collections.unmodifiableList(this.getCornerVertices(x0, y0, z0));
		faces = f;
		corners = c;
		return this;
	}

	/** One face's octagon; with {@code startCenter} its centre first and the first rim point repeated last (a fan). */
	public List<Vec3> getFaceVertices(Direction face, boolean startCenter, double x0, double y0, double z0) {
		if (faces != null)
			return faces[indexOf(face)];
		double m = mainSize;
		double c = cutSize;
		ArrayList<Vec3> li = new ArrayList<>();
		switch(face) {
			case DOWN:
				if (startCenter)
					li.add(new Vec3(x0, y0-m, z0));
				li.add(new Vec3(x0-c, y0-m, z0-m));
				li.add(new Vec3(x0+c, y0-m, z0-m));
				li.add(new Vec3(x0+m, y0-m, z0-c));
				li.add(new Vec3(x0+m, y0-m, z0+c));
				li.add(new Vec3(x0+c, y0-m, z0+m));
				li.add(new Vec3(x0-c, y0-m, z0+m));
				li.add(new Vec3(x0-m, y0-m, z0+c));
				li.add(new Vec3(x0-m, y0-m, z0-c));
				li.add(new Vec3(x0-c, y0-m, z0-m));
				break;
			case UP:
				if (startCenter)
					li.add(new Vec3(x0, y0+m, z0));
				li.add(new Vec3(x0-c, y0+m, z0-m));
				li.add(new Vec3(x0-m, y0+m, z0-c));
				li.add(new Vec3(x0-m, y0+m, z0+c));
				li.add(new Vec3(x0-c, y0+m, z0+m));
				li.add(new Vec3(x0+c, y0+m, z0+m));
				li.add(new Vec3(x0+m, y0+m, z0+c));
				li.add(new Vec3(x0+m, y0+m, z0-c));
				li.add(new Vec3(x0+c, y0+m, z0-m));
				li.add(new Vec3(x0-c, y0+m, z0-m));
				break;
			case WEST:
				if (startCenter)
					li.add(new Vec3(x0-m, y0, z0));
				li.add(new Vec3(x0-m, y0-c, z0-m));
				li.add(new Vec3(x0-m, y0-m, z0-c));
				li.add(new Vec3(x0-m, y0-m, z0+c));
				li.add(new Vec3(x0-m, y0-c, z0+m));
				li.add(new Vec3(x0-m, y0+c, z0+m));
				li.add(new Vec3(x0-m, y0+m, z0+c));
				li.add(new Vec3(x0-m, y0+m, z0-c));
				li.add(new Vec3(x0-m, y0+c, z0-m));
				li.add(new Vec3(x0-m, y0-c, z0-m));
				break;
			case EAST:
				if (startCenter)
					li.add(new Vec3(x0+m, y0, z0));
				li.add(new Vec3(x0+m, y0-c, z0-m));
				li.add(new Vec3(x0+m, y0+c, z0-m));
				li.add(new Vec3(x0+m, y0+m, z0-c));
				li.add(new Vec3(x0+m, y0+m, z0+c));
				li.add(new Vec3(x0+m, y0+c, z0+m));
				li.add(new Vec3(x0+m, y0-c, z0+m));
				li.add(new Vec3(x0+m, y0-m, z0+c));
				li.add(new Vec3(x0+m, y0-m, z0-c));
				li.add(new Vec3(x0+m, y0-c, z0-m));
				break;
			case SOUTH:
				if (startCenter)
					li.add(new Vec3(x0, y0, z0+m));
				li.add(new Vec3(x0-m, y0-c, z0+m));
				li.add(new Vec3(x0-c, y0-m, z0+m));
				li.add(new Vec3(x0+c, y0-m, z0+m));
				li.add(new Vec3(x0+m, y0-c, z0+m));
				li.add(new Vec3(x0+m, y0+c, z0+m));
				li.add(new Vec3(x0+c, y0+m, z0+m));
				li.add(new Vec3(x0-c, y0+m, z0+m));
				li.add(new Vec3(x0-m, y0+c, z0+m));
				li.add(new Vec3(x0-m, y0-c, z0+m));
				break;
			case NORTH:
				if (startCenter)
					li.add(new Vec3(x0, y0, z0-m));
				li.add(new Vec3(x0-m, y0-c, z0-m));
				li.add(new Vec3(x0-m, y0+c, z0-m));
				li.add(new Vec3(x0-c, y0+m, z0-m));
				li.add(new Vec3(x0+c, y0+m, z0-m));
				li.add(new Vec3(x0+m, y0+c, z0-m));
				li.add(new Vec3(x0+m, y0-c, z0-m));
				li.add(new Vec3(x0+c, y0-m, z0-m));
				li.add(new Vec3(x0-c, y0-m, z0-m));
				li.add(new Vec3(x0-m, y0-c, z0-m));
				break;
		}
		return li;
	}

	/** The eight corner triangles, top four then bottom four. */
	public List<List<Vec3>> getCornerVertices(double x0, double y0, double z0) {
		if (corners != null)
			return corners;
		double m = mainSize;
		double c = cutSize;
		ArrayList<List<Vec3>> li = new ArrayList<>();
		//top corners
		li.add(List.of(new Vec3(x0+m, y0+m, z0-c), new Vec3(x0+m, y0+c, z0-m), new Vec3(x0+c, y0+m, z0-m)));
		li.add(List.of(new Vec3(x0-c, y0+m, z0-m), new Vec3(x0-m, y0+c, z0-m), new Vec3(x0-m, y0+m, z0-c)));
		li.add(List.of(new Vec3(x0+c, y0+m, z0+m), new Vec3(x0+m, y0+c, z0+m), new Vec3(x0+m, y0+m, z0+c)));
		li.add(List.of(new Vec3(x0-m, y0+m, z0+c), new Vec3(x0-m, y0+c, z0+m), new Vec3(x0-c, y0+m, z0+m)));
		//bottom corners
		li.add(List.of(new Vec3(x0+c, y0-m, z0-m), new Vec3(x0+m, y0-c, z0-m), new Vec3(x0+m, y0-m, z0-c)));
		li.add(List.of(new Vec3(x0-m, y0-m, z0-c), new Vec3(x0-m, y0-c, z0-m), new Vec3(x0-c, y0-m, z0-m)));
		li.add(List.of(new Vec3(x0+m, y0-m, z0+c), new Vec3(x0+m, y0-c, z0+m), new Vec3(x0+c, y0-m, z0+m)));
		li.add(List.of(new Vec3(x0-c, y0-m, z0+m), new Vec3(x0-m, y0-c, z0+m), new Vec3(x0-m, y0-m, z0+c)));
		return li;
	}

	/**
	 * V33a {@code render(x, y, z, c1, c2, edge, pdist)}: faces and corners in ARGB {@code c1}, and with {@code edge}
	 * their outlines in {@code c2}. {@code faces} takes position-colour triangles; {@code edges} (may be null when
	 * {@code edge} is false) takes position-colour-normal-width lines, whose width V33a shrank with distance.
	 */
	public void render(PoseStack.Pose pose, VertexConsumer faces, VertexConsumer edges, double x, double y, double z,
			int c1, int c2, boolean edge, float pdist) {
		this.renderFaces(pose, faces, x, y, z, c1);
		if (edge)
			this.renderEdges(pose, edges, x, y, z, c2, pdist);
	}

	/** The fill: each face as a fan from its centre, then the corner triangles, in ARGB {@code c1}. */
	public void renderFaces(PoseStack.Pose pose, VertexConsumer faces, double x, double y, double z, int c1) {
		for (int i = 0; i < 6; i++) {
			List<Vec3> fan = this.getFaceVertices(FACES[i], true, x, y, z);
			Vec3 centre = fan.getFirst();
			for (int k = 1; k < fan.size()-1; k++) {
				vertex(faces, pose, centre, c1);
				vertex(faces, pose, fan.get(k), c1);
				vertex(faces, pose, fan.get(k+1), c1);
			}
		}
		for (List<Vec3> tri : this.getCornerVertices(x, y, z)) {
			for (Vec3 p : tri)
				vertex(faces, pose, p, c1);
		}
	}

	/** The outline of every face and corner in ARGB {@code c2}, V33a's width {@code max(1/8, 2 - pdist/16)}. */
	public void renderEdges(PoseStack.Pose pose, VertexConsumer edges, double x, double y, double z, int c2, float pdist) {
		float w = Math.max(0.125F, 2F-0.0625F*pdist);
		for (int i = 0; i < 6; i++)
			loop(edges, pose, this.getFaceVertices(FACES[i], false, x, y, z), c2, w);
		for (List<Vec3> tri : this.getCornerVertices(x, y, z))
			loop(edges, pose, tri, c2, w);
	}

	private static void loop(VertexConsumer lines, PoseStack.Pose pose, List<Vec3> points, int color, float width) {
		for (int i = 0; i < points.size(); i++) {
			Vec3 a = points.get(i);
			Vec3 b = points.get((i+1)%points.size());
			// A face rim already repeats its first point to close; skip the zero-length closing segment.
			if (a.equals(b))
				continue;
			Vec3 n = b.subtract(a).normalize();
			lines.addVertex(pose, (float)a.x, (float)a.y, (float)a.z).setColor(color)
					.setNormal(pose, (float)n.x, (float)n.y, (float)n.z).setLineWidth(width);
			lines.addVertex(pose, (float)b.x, (float)b.y, (float)b.z).setColor(color)
					.setNormal(pose, (float)n.x, (float)n.y, (float)n.z).setLineWidth(width);
		}
	}

	private static void vertex(VertexConsumer v5, PoseStack.Pose pose, Vec3 p, int color) {
		v5.addVertex(pose, (float)p.x, (float)p.y, (float)p.z).setColor(color);
	}

	private static int indexOf(Direction d) {
		for (int i = 0; i < FACES.length; i++)
			if (FACES[i] == d)
				return i;
		throw new IllegalArgumentException(String.valueOf(d));
	}

}
