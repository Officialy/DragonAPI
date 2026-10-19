package reika.dragonapi.instantiable.particlecontroller;

import reika.dragonapi.instantiable.data.immutable.DecimalPosition;
import reika.dragonapi.interfaces.PositionController;
import reika.dragonapi.libraries.mathsci.ReikaPhysicsHelper;
import net.minecraft.world.entity.Entity;
import reika.dragonapi.interfaces.ControllerTarget;

public class SpringMotionController implements PositionController {

	private final double xComponent;
	private final double yComponent;
	private final double zComponent;

	private final double maxAmplitude;
	private final double angularSpeed;
	private final double damping;

	private final DecimalPosition origin;

	private double linearPosition;
	private int tick;

	public SpringMotionController(double theta, double phi, double a, double v, double d, DecimalPosition p) {
		this(ReikaPhysicsHelper.polarToCartesian(1, theta, phi), a, v, d, p);
	}

	public SpringMotionController(double[] xyz, double a, double v, double d, DecimalPosition p) {
		xComponent = xyz[0];
		yComponent = xyz[1];
		zComponent = xyz[2];

		maxAmplitude = a;
		angularSpeed = v;
		damping = d;

		origin = p;
	}

	@Override
	public void updateTarget(ControllerTarget e) {
		tick++;
		linearPosition = maxAmplitude * Math.pow(Math.E, -damping * tick) * Math.cos(angularSpeed * tick);
	}

	@Override
	public double getPositionXFor(ControllerTarget e) {
		return origin.xCoord() + linearPosition * xComponent;
	}

	@Override
	public double getPositionYFor(ControllerTarget e) {
		return origin.yCoord() + linearPosition * yComponent;
	}

	@Override
	public double getPositionZFor(ControllerTarget e) {
		return origin.zCoord() + linearPosition * zComponent;
	}

}
