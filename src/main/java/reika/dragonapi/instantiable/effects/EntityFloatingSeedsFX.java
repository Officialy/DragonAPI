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

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.multiplayer.ClientLevel;

import reika.dragonapi.libraries.java.ReikaRandomHelper;
import reika.dragonapi.libraries.mathsci.ReikaPhysicsHelper;


public class EntityFloatingSeedsFX extends EntityBlurFX {

	private double angXZ;
	private double angY;

	private double angXZ_vel;
	private double angY_vel;

	private double angXZ_target;
	private double angY_target;

	private final double windAngle;
	private final double climbAngle;

	public double tolerance = 1.5;
	public double freedom = 20;
	public double angleVelocity = 0.75;
	public double particleVelocity = 0.0625;

	/*
	public EntityFloatingSeedsFX(ClientLevel world, double x, double y, double z, double windAng, double climbAng) {
		this(world, x, y, z, windAng, climbAng, null);
	}
	 */

	public EntityFloatingSeedsFX(ClientLevel world, double x, double y, double z, double windAng, double climbAng, TextureAtlasSprite icon) {
		super(world, x, y, z, icon);

		this.setColor(0xffffff);

		windAngle = windAng;
		climbAngle = climbAng;

		angXZ = windAngle;
		angY = climbAngle;

		this.randomizeXZ();
		this.randomizeY();
		this.updateVelocities();

		this.setIcon(icon);
	}

	@Override
	public void tick() {
		super.tick();

		this.updateAnglesAndSpeeds();
	}

	private void updateAnglesAndSpeeds() {
		this.updateAngles();

		angXZ += angXZ_vel;
		angY += angY_vel;

		this.updateVelocities();
	}

	private void updateAngles() {
		if (Math.abs(angXZ_target-angXZ) <= tolerance) {
			this.randomizeXZ();
		}

		if (Math.abs(angY_target-angY) <= tolerance) {
			this.randomizeY();
		}
	}

	private void randomizeXZ() {
		angXZ_target = ReikaRandomHelper.getRandomPlusMinus(windAngle, freedom);

		if (angXZ_target > angXZ) {
			angXZ_vel = angleVelocity;
		}
		else if (angXZ_target < angXZ) {
			angXZ_vel = -angleVelocity;
		}
	}

	private void randomizeY() {
		angY_target = ReikaRandomHelper.getRandomPlusMinus(climbAngle, freedom);

		if (angY_target > angY) {
			angY_vel = angleVelocity;
		}
		else if (angY_target < angY) {
			angY_vel = -angleVelocity;
		}
	}

	private void updateVelocities() {
		double[] xyz = ReikaPhysicsHelper.polarToCartesianFast(particleVelocity, angY, angXZ);
		xd = xyz[0];
		yd = xyz[1];
		zd = xyz[2];
	}

}
