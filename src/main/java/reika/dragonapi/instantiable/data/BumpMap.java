/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.data;

import reika.dragonapi.libraries.rendering.ReikaColorAPI;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

public class BumpMap {

	private final Class reference;
	private final String path;
	private final int referenceOffset;
	private int[][] data;

	public BumpMap(Class root, String name) {
		this(root, name, 127);
	}

	public BumpMap(Class root, String name, int offset) {
		reference = root;
		path = name;
		referenceOffset = offset;
		this.load();
	}

	private void load() {
		try (InputStream in = reference.getResourceAsStream(path)) {
			BufferedImage img = ImageIO.read(in);
			data = new int[img.getWidth()][img.getHeight()];
			for (int i = 0; i < img.getWidth(); i++) {
				for (int k = 0; k < img.getHeight(); k++) {
					data[i][k] = ReikaColorAPI.HexToGS(img.getRGB(i, k)) - referenceOffset;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public int getBump(int x, int y) {
		return data != null ? data[x][y] : 0;
	}

	public int getSizeX() {
		return data.length;
	}

	public int getSizeY() {
		return data[0].length;
	}

}
