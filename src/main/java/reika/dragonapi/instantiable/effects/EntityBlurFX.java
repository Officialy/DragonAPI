//package reika.dragonapi.instantiable.effects;
//
//import com.mojang.blaze3d.vertex.VertexConsumer;
//import net.minecraft.client.Camera;
//import net.minecraft.client.multiplayer.ClientLevel;
//import net.minecraft.client.particle.Particle;
//import net.minecraft.client.particle.ParticleRenderType;
//import net.minecraft.client.particle.SpriteSet;
//import net.minecraft.client.particle.TextureSheetParticle;
//import net.minecraft.core.BlockPos;
//import net.minecraft.util.Mth;
//import net.minecraft.world.phys.AABB;
//import net.minecraft.world.phys.shapes.VoxelShape;
//import reika.dragonapi.DragonAPI;
//import reika.dragonapi.instantiable.rendering.ReikaParticleEngine;
//import reika.dragonapi.interfaces.ColorController;
//import reika.dragonapi.interfaces.MotionController;
//import reika.dragonapi.interfaces.PositionController;
//import reika.dragonapi.interfaces.entity.CustomRenderFX;
//import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;
//import reika.dragonapi.libraries.rendering.ReikaColorAPI;
//
//import java.util.Collection;
//import java.util.HashSet;
//import java.util.List;
//
//public class EntityBlurFX extends TextureSheetParticle implements CustomRenderFX {
//
//    private float scale;
//
//    private boolean noSlow = false;
//    private boolean rapidExpand = false;
//    private boolean alphaFade = false;
//
//    private AABB bounds = null;
//    private int bounceAction = 0;
//    private double collideAngle;
//    private boolean colliding = false;
//    private int clearOnCollide = -1;
//
//    private int lifeFreeze;
//
//    private int preColor = -1;
//    private int fadeColor = -1;
//
//    private float defaultRed;
//    private float defaultGreen;
//    private float defaultBlue;
//
//    private double accelerationX;
//    private double accelerationY;
//    private double accelerationZ;
//
//    private double drag;
//
//    private BlockPos destination;
//
//    private Particle lock;
//    private Collection<Particle> locks = new HashSet<>();
//
//    private boolean additiveBlend = true;
//    private boolean depthTest = true;
//    private boolean alphaTest = false;
//
//    private boolean renderOverLimit = false;
//
//    private ReikaParticleEngine.RenderMode renderMode;
//
//    private MotionController motionController;
//    private PositionController positionController;
//    private ColorController colorController;
//
//    public EntityBlurFX(ClientLevel world, double x, double y, double z, SpriteSet ico) {
//        this(world, x, y, z, 0, 0, 0, ico);
//    }
//
//    public EntityBlurFX(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet ico) {
//        super(world, x, y, z, vx, vy, vz);
//        gravity = 0;
////        noClip = true;
//        lifetime = 60;
//        xd = vx;
//        yd = vy;
//        zd = vz;
//        scale = 1F;
//        alphaTest = false;
//        additiveBlend = true;
//        this.setSpriteFromAge(ico);
//    }
//
//    public final EntityBlurFX setIcon(SpriteSet ico) {
//        this.setSpriteFromAge(ico);
//        return this;
//    }
//
//    public final EntityBlurFX setScale(float f) {
//        scale = f;
//        return this;
//    }
//
//    public final EntityBlurFX setLife(int time) {
//        lifetime = time;
//        return this;
//    }
//
//    public final EntityBlurFX setNoSlowdown() {
//        noSlow = true;
//        return this;
//    }
//
//    public final EntityBlurFX setRapidExpand() {
//        rapidExpand = true;
//        return this;
//    }
//
//    public final EntityBlurFX setAlphaFading() {
//        alphaFade = true;
//        return this;
//    }
//
//    public final EntityBlurFX setGravity(float g) {
//        gravity = g;
//        return this;
//    }
//
//    public final EntityBlurFX setDrag(double d) {
//        drag = d;
//        return this;
//    }
//
//    public final EntityBlurFX setColor(int r, int g, int b) {
//        rCol = r/255F;
//        gCol = g/255F;
//        bCol = b/255F;
//        defaultRed = rCol;
//        defaultGreen = gCol;
//        defaultBlue = bCol;
//        return this;
//    }
//
//    public final EntityBlurFX setColor(int rgb) {
//        return this.setColor(ReikaColorAPI.getRed(rgb), ReikaColorAPI.getGreen(rgb), ReikaColorAPI.getBlue(rgb));
//    }
//
//    public final EntityBlurFX fadeColors(int c1, int c2) {
//        preColor = c1;
//        fadeColor = c2;
//        return this.setColor(c1);
//    }
//
//    public final EntityBlurFX bound(AABB box, boolean bounce, boolean cull) {
//        bounds = box;
//        bounceAction = (bounce ? 1 : 0) | (cull ? 2 : 0);
//        return this;
//    }
//
//    public final EntityBlurFX setColliding() {
//        return this.setColliding(-1);
//    }
//
//    public final EntityBlurFX setColliding(int clear) {
//        return this.setColliding(DragonAPI.rand.nextDouble()*360, clear);
//    }
//
//    public final EntityBlurFX setColliding(double ang) {
//        return this.setColliding(ang, -1);
//    }
//
//    public final EntityBlurFX setColliding(double ang, int clear) {
////        noClip = false;
//        colliding = true;
//        collideAngle = ang;
//        clearOnCollide = clear;
//        this.onSetColliding();
//        return this;
//    }
//
//    protected void onSetColliding() {
//
//    }
//
//    public final EntityBlurFX markDestination(int x, int y, int z) {
//        destination = new BlockPos(x, y, z);
//        return this;
//    }
//
//    public final EntityBlurFX lockTo(Particle fx) {
//        lock = fx;
//        if (this == fx) {
//            DragonAPI.LOGGER.error("Cannot lock a particle to itself!");
//            return this;
//        }
//        if (fx instanceof EntityBlurFX) {
//            EntityBlurFX bfx = (EntityBlurFX)fx;
//            if (!bfx.getRenderMode().equals(this.getRenderMode()))
//                DragonAPI.LOGGER.error("Cannot accurately lock two different particle render types: "+fx+" & "+this);
//            bfx.locks.add(this);
//        }
//        return this;
//    }
//
//    public final EntityBlurFX setAcceleration(double x, double y, double z) {
//        accelerationX = x;
//        accelerationY = y;
//        accelerationZ = z;
//        return this;
//    }
//
//    public final EntityBlurFX setAdditiveBlend() {
//        additiveBlend = true;
//        renderMode = null;
//        return this;
//    }
//
//    public final EntityBlurFX setBasicBlend() {
//        additiveBlend = false;
//        renderMode = null;
//        return this;
//    }
//
//    public final EntityBlurFX setNoDepthTest() {
//        depthTest = false;
//        renderMode = null;
//        return this;
//    }
//
//    public final EntityBlurFX enableAlphaTest() {
//        alphaTest = true;
//        renderMode = null;
//        return this;
//    }
//
//    public final EntityBlurFX forceIgnoreLimits() {
//        renderOverLimit = true;
//        return this;
//    }
//
//    public final EntityBlurFX setAge(int age) {
//        this.age = age;
//        return this;
//    }
//
//    public final EntityBlurFX freezeLife(int ticks) {
//        lifeFreeze = ticks;
//        return this;
//    }
//
//    public final EntityBlurFX setMotionController(MotionController m) {
//        motionController = m;
//        return this;
//    }
//
//    public final EntityBlurFX setPositionController(PositionController m) {
//        positionController = m;
//        return this;
//    }
//
//    public final EntityBlurFX setColorController(ColorController m) {
//        colorController = m;
//        return this;
//    }
//
//    protected final boolean isAlphaFade() {
//        return alphaFade;
//    }
//
//    public final int getMaxAge() {
//        return lifetime;
//    }
//
//    public final int getMaximumSizeAge() {
//        return rapidExpand ? lifetime/12 : lifetime/2;
//    }
//
//    public void tick() {
//        ticksExisted = age;
//        if (age < 0) {
//            return;
//        }
//        if (colliding) {
//            if (isCollidedVertically) {
//                double v = DragonAPI.rand.nextDouble()*0.0625;
//                if (Double.isFinite(collideAngle)) {
//                    xd = v*Math.sin(Math.toRadians(collideAngle));
//                    zd = v*Math.cos(Math.toRadians(collideAngle));
//                }
//                else {
//                    double vel = ReikaMathLibrary.py3d(xd, 0, zd);
//                    xd = xd*v/vel;
//                    zd = zd*v/vel;
//                }
//                colliding = false;
//                this.setNoSlowdown();
//                if (clearOnCollide != Integer.MIN_VALUE) {
//                    lifeFreeze = clearOnCollide >= 0 ? Math.min(clearOnCollide, 20) : 20;
//                }
//                gravity *= 4;
//                if (clearOnCollide >= 0)
//                    this.setLife(Math.max(1, clearOnCollide-lifeFreeze));
//                this.onCollision();
//            }
//        }
//
//        if (destination != null) {
//            BlockPos c = postion;
//            if (c.equals(destination)) {
//                this.remove();
//            }
//        }
//
//        xd += accelerationX;
//        yd += accelerationY;
//        zd += accelerationZ;
//
//        if (noSlow) {
//            double mx = xd;
//            double my = yd;
//            double mz = zd;
//            super.tick();
//            xd = mx;
//            yd = my;
//            zd = mz;
//        }
//        else {
//            if (drag != 0) {
//                xd *= drag;
//                yd *= drag;
//                zd *= drag;
//            }
//            super.tick();
//        }
//
//        if (lifeFreeze > 0) {
//            lifeFreeze--;
//            this.age--;
//        }
//
//        int age = Math.max(this.age, 1);
//
//        if (fadeColor != -1) {
//            int c = ReikaColorAPI.mixColors(fadeColor, preColor, age/(float)lifetime);
//            this.setColor(c);
//        }
//
//        if (alphaFade) {
//            particleScale = scale;
//            float f = 1;
//            if (rapidExpand) {
//                f = (lifetime/age >= 12 ? age*12F/lifetime : 1-age/(float)lifetime);
//            }
//            else {
//                f = Mth.sin((float)Math.toRadians(180D*age/lifetime));
//            }
//            if (additiveBlend) {
//                rCol = defaultRed*f;
//                gCol = defaultGreen*f;
//                bCol = defaultBlue*f;
//            }
//            else {
//                alpha = f;
//            }
//        }
//        else {
//            if (rapidExpand)
//                particleScale = scale*(lifetime/age >= 12 ? age*12F/lifetime : 1-age/(float)lifetime);
//            else
//                particleScale = scale*Mth.sin((float)Math.toRadians(180D*age/lifetime));
//            //if (particleAge < 10)
//            //	particleScale = scale*(particleAge+1)/10F;
//            //else if (particleAge > 50)
//            //	particleScale = scale*(61-particleAge)/10F;
//            //else
//            //	particleScale = scale;
//        }
//
//        if (bounds != null) {
//            boolean bounce = (bounceAction & 1) != 0;
//            boolean cull = (bounceAction & 2) != 0;
//            if ((x <= bounds.minX && xd < 0) || (x >= bounds.maxX && xd > 0)) {
//                xd = bounce ? -xd : 0;
//                if (cull)
//                    this.remove();
//            }
//            if ((y <= bounds.minY && yd < 0) || (y >= bounds.maxY && yd > 0)) {
//                yd = bounce ? -yd : 0;
//                if (cull)
//                    this.remove();
//            }
//            if ((z <= bounds.minZ && zd < 0) || (z >= bounds.maxZ && zd > 0)) {
//                zd = bounce ? -zd : 0;
//                if (cull)
//                    this.remove();
//            }
//        }
//
//        if (lock != null) {
//            x = lock.x;
//            y = lock.y;
//            z = lock.z;
//            xd = lock.xd;
//            yd = lock.yd;
//            zd = lock.zd;
//        }
//
//        if (!locks.isEmpty()) {
//            for (Particle fx : locks) {
//                //fx.posX = posX;
//                //fx.posY = posY;
//                //fx.posZ = posZ;
//                fx.xd = xd;
//                fx.yd = yd;
//                fx.zd = zd;
//            }
//        }
//
//        if (motionController != null) {
//            xd = motionController.getMotionX(this);
//            yd = motionController.getMotionY(this);
//            zd = motionController.getMotionZ(this);
//            motionController.update(this);
//        }
//        if (positionController != null) {
//            x = positionController.getPositionX(this);
//            y = positionController.getPositionY(this);
//            z = positionController.getPositionZ(this);
//            if (positionController != motionController) //prevent double update
//                positionController.update(this);
//        }
//
//        if (colorController != null) {
//            int rgb = colorController.getColor(this);
//            float f = 1;
//            if (alphaFade) {
//                if (rapidExpand) {
//                    f = (lifetime/age >= 12 ? age*12F/lifetime : 1-age/(float)lifetime);
//                }
//                else {
//                    f = Mth.sin((float)Math.toRadians(180D*age/lifetime));
//                }
//            }
//            rCol = ReikaColorAPI.getRed(rgb)*f/255F;
//            gCol = ReikaColorAPI.getGreen(rgb)*f/255F;
//            bCol = ReikaColorAPI.getBlue(rgb)*f/255F;
//            colorController.update(this);
//        }
//    }
//
//    public void move(double vx, double vy, double vz) {
//        if (noClip) {
//            super.move(vx, vy, vz);
//        }
//        else { //streamlined, removed everything that was never applicable or desirable for a particle
//            ySize *= 0.4F;
//
//            double vxCopy = vx;
//            double vyCopy = vy;
//            double vzCopy = vz;
//
//            List<VoxelShape> list = level.getEntityCollisions(this, bounds.addCoord(vx, vy, vz));
//
//            if (!list.isEmpty()) {
//                for (VoxelShape box : list) {
//                    vx = box.calculateXOffset(bounds, vx);
//                    vy = box.calculateYOffset(bounds, vy);
//                    vz = box.calculateZOffset(bounds, vz);
//                }
//            }
//
//            bounds.move(vx, vy, vz);
//
//            x = (getBoundingBox().minX + getBoundingBox().maxX) * 0.5;
//            y = getBoundingBox().minY + yOffset - ySize;
//            z = (getBoundingBox().minZ + getBoundingBox().maxZ) * 0.5;
//            isCollidedHorizontally = vxCopy != vx || vzCopy != vz;
//            isCollidedVertically = vyCopy != vy;
//            onGround = vyCopy != vy && vyCopy < 0.0D;
////            isCollided = isCollidedHorizontally || isCollidedVertically;
//
//            if (isCollidedHorizontally) {
//                xd = 0.0D;
//                zd = 0.0D;
//            }
//
//            if (isCollidedVertically) {
//                yd = 0.0D;
//            }
//        }
//    }
//
//    protected void onCollision() {
//
//    }
//	/*
//	@Override
//	public void renderParticle(Tessellator v5, float par2, float par3, float par4, float par5, float par6, float par7)
//	{
//		v5.draw();
//		ReikaTextureHelper.bindTerrainTexture();
//		if (additiveBlend)
//			BlendMode.ADDITIVEDARK.apply();
//		GL11.glColor4f(1, 1, 1, 1);
//		v5.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
//		v5.setBrightness(this.getBrightnessForRender(0));
//		super.renderParticle(v5, par2, par3, par4, par5, par6, par7);
//		v5.draw();
//		RenderSystem.defaultBlendFunc();
//		v5.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
//	}
//	 */
//
//    @Override
//    public final ReikaParticleEngine.RenderMode getRenderMode() {
//        if (renderMode == null)
//            renderMode = new ReikaParticleEngine.RenderMode().setFlag(ReikaParticleEngine.RenderModeFlags.ADDITIVE, additiveBlend).setFlag(ReikaParticleEngine.RenderModeFlags.DEPTH, depthTest).setFlag(ReikaParticleEngine.RenderModeFlags.LIGHT, false).setFlag(ReikaParticleEngine.RenderModeFlags.ALPHACLIP, alphaTest && additiveBlend);//additiveBlend ? RenderMode.ADDITIVEDARK : RenderMode.LIT;
//        return renderMode;
//    }
//
//    @Override
//    public final ReikaParticleEngine.TextureMode getTexture() {
//        return ReikaParticleEngine.blockTex;
//    }
//
//    public boolean rendersOverLimit() {
//        return renderOverLimit;
//    }
//
//    @Override
//    public double getRenderRange() {
//        return 0;
//    }
//
//    @Override
//    public void render(VertexConsumer p_107261_, Camera p_107262_, float p_107263_) {
//
//    }
//
//    @Override
//    public ParticleRenderType getRenderType() {
//        return ParticleRenderType.CUSTOM;
//    }
//	/*
//	@Override
//	public double getRenderRange() {
//		return scale*96;
//	}
//	 */
//}
