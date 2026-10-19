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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.client.ParticleStateAccess;
import reika.dragonapi.instantiable.rendering.ReikaParticleEngine;
import reika.dragonapi.instantiable.rendering.ReikaParticleEngine.*;
import reika.dragonapi.interfaces.*;
import reika.dragonapi.interfaces.entity.CustomRenderFX;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

public class EntityBlurFX extends SingleQuadParticle implements CustomRenderFX {

	private float scale;
    private int controllerAge;
    private boolean isCollidedVertically;

	private boolean noSlow = false;
	private boolean rapidExpand = false;
	private boolean alphaFade = false;

	private AABB bounds = null;
	private int bounceAction = 0;
	private double collideAngle;
	private boolean colliding = false;
	private int clearOnCollide = -1;

	private int lifeFreeze;

	private int preColor = -1;
	private int fadeColor = -1;

	private float defaultRed=1;
	private float defaultGreen=1;
	private float defaultBlue=1;

	private double accelerationX;
	private double accelerationY;
	private double accelerationZ;

	private double drag;

	private BlockPos destination;

	private Particle lock;
	private Collection<Particle> locks = new HashSet<>();

	private boolean additiveBlend = true;
	private boolean depthTest = true;
	private boolean alphaTest = false;

	private boolean renderOverLimit = false;

	private RenderMode renderMode;

	private MotionController motionController;
	private PositionController positionController;
	private ColorController colorController;

	public EntityBlurFX(ClientLevel world, double x, double y, double z, TextureAtlasSprite ico) {
		this(world, x, y, z, 0, 0, 0, ico);
	}

	public EntityBlurFX(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, TextureAtlasSprite ico) {
		super(world, x, y, z, vx, vy, vz, ico);
		gravity = 0;
		hasPhysics = false;
		lifetime = 60;
		xd = vx;
		yd = vy;
		zd = vz;
		scale = 1F;
		alphaTest = false;
		additiveBlend = true;
		sprite = ico;
	}

	public final EntityBlurFX setIcon(TextureAtlasSprite ii) {
		sprite = ii;
		return this;
	}

	public final EntityBlurFX setScale(float f) {
		scale = f;
		return this;
	}

	public final EntityBlurFX setLife(int time) {
		lifetime = Math.max(1,time);
		return this;
	}

	public final EntityBlurFX setNoSlowdown() {
		noSlow = true;
		return this;
	}

	public final EntityBlurFX setRapidExpand() {
		rapidExpand = true;
		return this;
	}

	public final EntityBlurFX setAlphaFading() {
		alphaFade = true;
		return this;
	}

	public final EntityBlurFX setGravity(float g) {
		gravity = g;
		return this;
	}

	public final EntityBlurFX setDrag(double d) {
		drag = d;
		return this;
	}

	public final EntityBlurFX setColor(int r, int g, int b) {
		rCol = r/255F;
		gCol = g/255F;
		bCol = b/255F;
		defaultRed = rCol;
		defaultGreen = gCol;
		defaultBlue = bCol;
		return this;
	}

	public final EntityBlurFX setColor(int rgb) {
		return this.setColor(ReikaColorAPI.getRed(rgb), ReikaColorAPI.getGreen(rgb), ReikaColorAPI.getBlue(rgb));
	}

	public final EntityBlurFX fadeColors(int c1, int c2) {
		preColor = c1;
		fadeColor = c2;
		return this.setColor(c1);
	}

	public final EntityBlurFX bound(AABB box, boolean bounce, boolean cull) {
		bounds = box;
		bounceAction = (bounce ? 1 : 0) | (cull ? 2 : 0);
		return this;
	}

	public final EntityBlurFX setColliding() {
		return this.setColliding(-1);
	}

	public final EntityBlurFX setColliding(int clear) {
		return this.setColliding(random.nextDouble()*360, clear);
	}

	public final EntityBlurFX setColliding(double ang) {
		return this.setColliding(ang, -1);
	}

	public final EntityBlurFX setColliding(double ang, int clear) {
		hasPhysics = true;
		colliding = true;
		collideAngle = ang;
		clearOnCollide = clear;
		this.onSetColliding();
		return this;
	}

	protected void onSetColliding() {

	}

	public final EntityBlurFX markDestination(int x, int y, int z) {
		destination = new BlockPos(x, y, z);
		return this;
	}

	public final EntityBlurFX lockTo(Particle fx) {
		if (this == fx) {
			DragonAPI.LOGGER.error("Cannot lock a particle to itself!");
			return this;
		}
		lock = fx;
		if (fx instanceof EntityBlurFX) {
			EntityBlurFX bfx = (EntityBlurFX)fx;
			if (!bfx.getRenderMode().equals(this.getRenderMode()))
				DragonAPI.LOGGER.error("Cannot accurately lock two different particle render types: "+fx+" & "+this);
			bfx.locks.add(this);
		}
		return this;
	}

	public final EntityBlurFX setAcceleration(double x, double y, double z) {
		accelerationX = x;
		accelerationY = y;
		accelerationZ = z;
		return this;
	}

	public final EntityBlurFX setAdditiveBlend() {
		additiveBlend = true;
		renderMode = null;
		return this;
	}

	public final EntityBlurFX setBasicBlend() {
		additiveBlend = false;
		renderMode = null;
		return this;
	}

	public final EntityBlurFX setNoDepthTest() {
		depthTest = false;
		renderMode = null;
		return this;
	}

	public final EntityBlurFX enableAlphaTest() {
		alphaTest = true;
		renderMode = null;
		return this;
	}

	public final EntityBlurFX forceIgnoreLimits() {
		renderOverLimit = true;
		return this;
	}

	public final EntityBlurFX setAge(int age) {
		this.age = age;
		return this;
	}

	public final EntityBlurFX freezeLife(int ticks) {
		lifeFreeze = ticks;
		return this;
	}

	public final EntityBlurFX setMotionController(MotionController m) {
		motionController = m;
		return this;
	}

	public final EntityBlurFX setPositionController(PositionController m) {
		positionController = m;
		return this;
	}

	public final EntityBlurFX setColorController(ColorController m) {
		colorController = m;
		return this;
	}

	protected final boolean isAlphaFade() {
		return alphaFade;
	}

	public final int getMaxAge() {
		return lifetime;
	}

	public final float getGravity() {
		return gravity;
	}

	public final int getMaximumSizeAge() {
		return rapidExpand ? lifetime/12 : lifetime/2;
	}

	@Override
	public void tick() {
		controllerAge = this.age;
		if (this.age < 0) {
			return;
		}
		if (colliding) {
			if (isCollidedVertically) {
				double v = random.nextDouble()*0.0625;
				if (Double.isFinite(collideAngle)) {
					xd = v*Math.sin(Math.toRadians(collideAngle));
					zd = v*Math.cos(Math.toRadians(collideAngle));
				}
				else {
					double vel = Math.hypot(xd,zd);
                    if(vel>0) { xd=xd*v/vel; zd=zd*v/vel; }
				}
				colliding = false;
				this.setNoSlowdown();
				if (clearOnCollide != Integer.MIN_VALUE) {
					lifeFreeze = clearOnCollide >= 0 ? Math.min(clearOnCollide, 20) : 20;
				}
				gravity *= 4;
				if (clearOnCollide >= 0)
					this.setLife(Math.max(1, clearOnCollide-lifeFreeze));
				this.onCollision();
			}

		}

		if (destination != null) {
			BlockPos c = BlockPos.containing(x,y,z);
			if (c.equals(destination)) {
				this.remove();
			}
		}

		xd += accelerationX;
		yd += accelerationY;
		zd += accelerationZ;

		if (noSlow) {
			double mx = xd;
			double my = yd;
			double mz = zd;
			super.tick();
			xd = mx;
			yd = my;
			zd = mz;
		}
		else {
			if (drag != 0) {
				xd *= drag;
				yd *= drag;
				zd *= drag;
			}
			super.tick();
		}

		if (lifeFreeze > 0) {
			lifeFreeze--;
			this.age--;
		}

		int age = Math.max(this.age, 1);

		if (fadeColor != -1) {
			int c = ReikaColorAPI.mixColors(fadeColor, preColor, age/(float)lifetime);
			this.setColor(c);
		}

		if (alphaFade) {
			quadSize = 0.1F*scale;
			float f = 1;
			if (rapidExpand) {
				f = (lifetime/age >= 12 ? age*12F/lifetime : 1-age/(float)lifetime);
			}
			else {
				f = Mth.sin((float)Math.toRadians(180D*age/lifetime));
			}
			if (additiveBlend) {
				rCol = defaultRed*f;
				gCol = defaultGreen*f;
				bCol = defaultBlue*f;
			}
			else {
				alpha = f;
			}
		}
		else {
			if (rapidExpand)
				quadSize = 0.1F*scale*(lifetime/age >= 12 ? age*12F/lifetime : 1-age/(float)lifetime);
			else
				quadSize = 0.1F*scale*Mth.sin((float)Math.toRadians(180D*age/lifetime));
			//if (this.age < 10)
			//	quadSize = 0.1F*scale*(this.age+1)/10F;
			//else if (this.age > 50)
			//	quadSize = 0.1F*scale*(61-this.age)/10F;
			//else
			//	quadSize = 0.1F*scale;
		}

		if (bounds != null) {
			boolean bounce = (bounceAction & 1) != 0;
			boolean cull = (bounceAction & 2) != 0;
			if ((x <= bounds.minX && xd < 0) || (x >= bounds.maxX && xd > 0)) {
				xd = bounce ? -xd : 0;
				if (cull)
					this.remove();
			}
			if ((y <= bounds.minY && yd < 0) || (y >= bounds.maxY && yd > 0)) {
				yd = bounce ? -yd : 0;
				if (cull)
					this.remove();
			}
			if ((z <= bounds.minZ && zd < 0) || (z >= bounds.maxZ && zd > 0)) {
				zd = bounce ? -zd : 0;
				if (cull)
					this.remove();
			}
		}

		if (lock != null) {
            var target=((ParticleStateAccess)lock).dragonapi$controllerTarget();
            setPos(target.x(),target.y(),target.z());
            setParticleSpeed(target.velocity().x,target.velocity().y,target.velocity().z);
		}

		if (!locks.isEmpty()) {
			for (Particle fx : locks) {
				//fx.x = x;
				//fx.y = y;
				//fx.z = z;
                fx.setParticleSpeed(xd,yd,zd);
			}
		}

		if (motionController != null) {
			xd = motionController.getMotionXFor(controllerTarget());
			yd = motionController.getMotionYFor(controllerTarget());
			zd = motionController.getMotionZFor(controllerTarget());
			motionController.updateTarget(controllerTarget());
		}
		if (positionController != null) {
			x = positionController.getPositionXFor(controllerTarget());
			y = positionController.getPositionYFor(controllerTarget());
			z = positionController.getPositionZFor(controllerTarget());
            setPos(x,y,z);
			if (positionController != motionController) //prevent double update
				positionController.updateTarget(controllerTarget());
		}

		if (colorController != null) {
			int rgb = colorController.getColorFor(controllerTarget());
			float f = 1;
			if (alphaFade) {
				if (rapidExpand) {
					f = (lifetime/age >= 12 ? age*12F/lifetime : 1-age/(float)lifetime);
				}
				else {
					f = Mth.sin((float)Math.toRadians(180D*age/lifetime));
				}
			}
			rCol = ReikaColorAPI.getRed(rgb)*f/255F;
			gCol = ReikaColorAPI.getGreen(rgb)*f/255F;
			bCol = ReikaColorAPI.getBlue(rgb)*f/255F;
			colorController.updateTarget(controllerTarget());
		}
	}

    @Override
    public void move(double vx,double vy,double vz) {
        if(!hasPhysics) { super.move(vx,vy,vz); return; }
        double requestedX=vx,requestedY=vy,requestedZ=vz;
        var movement=Entity.collideBoundingBox(CollisionContext.positionContext(y),new Vec3(vx,vy,vz),getBoundingBox(),level,List.of());
        vx=movement.x; vy=movement.y; vz=movement.z;
        setBoundingBox(getBoundingBox().move(vx,vy,vz));
        setLocationFromBoundingbox();
        boolean horizontal=requestedX!=vx || requestedZ!=vz;
        isCollidedVertically=requestedY!=vy;
        onGround=isCollidedVertically && requestedY<0;
        if(horizontal) { xd=0; zd=0; }
        if(isCollidedVertically) yd=0;
    }
    private ControllerTarget controllerTarget() {
        return new ControllerTarget(x,y,z,new Vec3(xd,yd,zd),controllerAge);
    }

	protected void onCollision() {

	}

	@Override
	public final int getLightCoords(float par1) {
		return 0xF000F0;
	}

    @Override protected Layer getLayer() { return Layer.TRANSLUCENT_TERRAIN; }

	@Override
	public final RenderMode getRenderMode() {
		if (renderMode == null)
			renderMode = new RenderMode().setFlag(RenderModeFlags.FOG, false).setFlag(RenderModeFlags.ADDITIVE, additiveBlend).setFlag(RenderModeFlags.DEPTH, depthTest).setFlag(RenderModeFlags.LIGHT, false).setFlag(RenderModeFlags.ALPHACLIP, alphaTest && additiveBlend);//additiveBlend ? RenderMode.ADDITIVEDARK : RenderMode.LIT;
		return renderMode;
	}

	@Override
	public final TextureMode getTexture() {
		return ReikaParticleEngine.blockTex;
	}

	public boolean rendersOverLimit() {
		return renderOverLimit;
	}
    @Override public double getRenderRange() { return 0; }
}
