package reika.dragonapi.extras;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.DragonOptions;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;
import reika.dragonapi.libraries.rendering.ReikaRenderHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Callable;

public class ThrottleableEffectRenderer extends ParticleEngine {

    public static boolean renderParticles = true;

    public final int limit;

    private final ParticleEngine original;

    private final HashMap<Class<?>, ParticleEngine> delegates = new HashMap<>();
    private final HashSet<ParticleEngine> delegateSet = new HashSet<>();

    private boolean isRendering;
    @Deprecated
    private boolean isTicking;

    private ArrayList<ParticleSpawnHandler> particleSpawnHandlers = null;

    private static final ResourceLocation particleTextures = new ResourceLocation("textures/particle/particles.png");
    private static final AABB particleBox = new AABB(0, 0, 0, 0, 0, 0);

    public ThrottleableEffectRenderer(ParticleEngine eff) {
        super(Minecraft.getInstance().level, Minecraft.getInstance().getTextureManager());
//        super(Minecraft.getInstance().level, Minecraft.getInstance().gameRenderer);
        limit = Math.max(250, DragonOptions.PARTICLELIMIT.getValue());
        original = eff;
    }

    public void registerDelegateRenderer(Class<?> fxClass, ParticleEngine renderer) {
        delegates.put(fxClass, renderer);
        delegateSet.add(renderer);
    }

    public void addSpawnHandler(ParticleSpawnHandler p) {
        if (particleSpawnHandlers == null) {
            particleSpawnHandlers = new ArrayList<>();
        }
        particleSpawnHandlers.add(p);
    }

    public void addEffect(Particle fx) {
        if (fx == null)
            throw new IllegalArgumentException("You cannot spawn a null particle! This is a bug in the mod calling this code!");
//        AddParticleEvent evt = AddParticleEvent.getForParticle(fx);
//        if (MinecraftForge.EVENT_BUS.post(evt))
//        	return;
//        if (this.isInWall(fx))
//        	return;
        if (particleSpawnHandlers != null) {
            for (ParticleSpawnHandler p : particleSpawnHandlers) {
                if (p.cancel(fx)) {
                    return;
                }
            }
        }
        ParticleEngine eff = delegates.get(fx.getClass());
        if (eff != null) {
            eff.add(fx);
            return;
        }

        if (isRendering) {
            DragonAPI.LOGGER.error("Tried adding a particle mid-render!");
            Thread.dumpStack();
        }
		/*
		if (isTicking) {
			DragonAPI.LOGGER.error("Tried adding a particle mid-update!");
			Thread.dumpStack();
			return;
		}*/

  /*todo      int i = fx.getFXLayer();
        if (fxLayers[i].size() >= limit) {
            fxLayers[i].remove(0);
        }

        fxLayers[i].add(fx);*/
    }

/* todo   private boolean isInWall(Entity fx) {
        int x = Mth.floor(fx.getX());
        int y = Mth.floor(fx.getY());
        int z = Mth.floor(fx.getZ());
        Block b = fx.level.getBlockState(new BlockPos(x, y, z)).getBlock();
        if (b.isOpaqueCube() && b.renderAsNormalBlock() && b.getRenderType() == 0) {
            double d = 0.4;
            double minX = x+b.getBlockBoundsMinX()+d;
            double minY = y+b.getBlockBoundsMinY()+d;
            double minZ = z+b.getBlockBoundsMinZ()+d;
            double maxX = x+b.getBlockBoundsMaxX()-d;
            double maxY = y+b.getBlockBoundsMaxY()-d;
            double maxZ = z+b.getBlockBoundsMaxZ()-d;
            if (ReikaMathLibrary.isValueInsideBounds(minX, maxX, fx.getX()) && ReikaMathLibrary.isValueInsideBounds(minY, maxY, fx.getY()) && ReikaMathLibrary.isValueInsideBounds(minZ, maxZ, fx.getZ())) {
                //DragonAPI.LOGGER.info("Skipping particle "+fx+"; inside block");
                return true;
            }
        }
        return false;
    }*/

    public void tick() {
//        isTicking = true;
        super.tick();
//        isTicking = false;
        for (ParticleEngine eff : delegateSet) {
            eff.tick();
        }
    }

    public void renderParticles(PoseStack stack, Particle e, MultiBufferSource.BufferSource buffer, float ptick) {
        if (renderParticles) {
            if (renderThroughWalls())
                RenderSystem.disableDepthTest();
//            super.renderParticles(e, ptick);
            this.doRenderParticles(e, ptick);
            for (ParticleEngine eff : delegateSet) {
                eff.render(stack, buffer, Minecraft.getInstance().gameRenderer.lightTexture(), Minecraft.getInstance().gameRenderer.getMainCamera(), ptick);
            }
            RenderSystem.enableDepthTest();
        }
    }

    private void doRenderParticles(Particle e, float ptick) {
/*
   todo     e.interpPosX = e.lastTickPosX + (e.posX - e.lastTickPosX) * ptick;
        e.interpPosY = e.lastTickPosY + (e.posY - e.lastTickPosY) * ptick;
        e.interpPosZ = e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * ptick;
*/

        isRendering = true;

        for (int i = 0; i < 3; i++)  {
            /*todo if (!fxLayers[i].isEmpty())  {
                this.bindTexture(i);
                RenderSystem.setShaderColor(1,1,1,1);
                RenderSystem.depthMask(false);
                RenderSystem.enableBlend();
//                GL11.glAlphaFunc(GL11.GL_GREATER, 0.003921569F);
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                var tess = Tesselator.getInstance();
                var v5 = tess.getBuilder();
                v5.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);

                for (Particle fx : ((Collection<Particle>)fxLayers[i]))  {
                    if (fx != null && isParticleVisible(fx)) {
                        //if (isEntityCloseEnough(fx, Particle.interpPosX, Particle.interpPosY, Particle.interpPosZ)) {
                        v5.setBrightness(fx.getBrightnessForRender(ptick));

                        try {
                            fx.render(v5, Minecraft.getInstance().gameRenderer.getMainCamera(), ptick);//, f1, f5, f2, f3, f4);
                        }
                        catch (Throwable throwable) {
                            this.throwCrash(i, fx, throwable);
                        }
                        if (!v5.building()) {
                            DragonAPI.LOGGER.error("Particle "+fx+" left the tessellator in a bad state, stopped drawing!");
                            v5.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
                        }
                        //}
                    }
                }

                tess.end();
                RenderSystem.disableBlend();
                RenderSystem.depthMask(true);
//                GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            }*/
        }

        isRendering = false;
    }

    private void bindTexture(int i) {
        /*switch (i) {
            case 0, default -> renderer.bindTexture(particleTextures);
            case 1 -> renderer.bindTexture(TextureMap.locationBlocksTexture);
            case 2 -> renderer.bindTexture(TextureMap.locationItemsTexture);
        }*/
    }

    private void throwCrash(final int i, final Particle fx, Throwable throwable) {
        CrashReport crashreport = CrashReport.forThrowable(throwable, "Rendering Particle");
        /*CrashReportCategory crashreportcategory = crashreport.makeCategory("Particle being rendered");
        crashreportcategory.addCrashSectionCallable("Particle", new Callable() {
            private static final String __OBFID = "CL_00000918";
            public String call() {
                return fx.toString();
            }
        });
        crashreportcategory.addCrashSectionCallable("Particle Type", new Callable() {
            private static final String __OBFID = "CL_00000919";
            public String call() {
                return i == 0 ? "MISC_TEXTURE" : (i == 1 ? "TERRAIN_TEXTURE" : (i == 2 ? "ITEM_TEXTURE" : (i == 3 ? "ENTITY_PARTICLE_TEXTURE" : "Unknown - " + i)));
            }
        });*/
        throw new ReportedException(crashreport);
    }

    public int getParticleCount() {
        int base = 0;
//        for (Collection<Particle> fx : fxLayers) {
//            base += fx.size();
//        }
        for (ParticleEngine eff : delegateSet) {
            if (eff instanceof CustomEffectRenderer)
                base += ((CustomEffectRenderer)eff).getParticleCount();
        }
        return base;
    }

    public static ThrottleableEffectRenderer getRegisteredInstance() {
        return (ThrottleableEffectRenderer) Minecraft.getInstance().particleEngine;
    }

    public static boolean renderThroughWalls() {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), InputConstants.KEY_INSERT);
    }

    public static boolean isParticleVisible(Particle fx) {
//        todo return Minecraft.getInstance().levelRenderer.cullingFrustum.isBoundingBoxInFrustum(getBoundingBox(fx));
        return true;
    }

    public static AABB getBoundingBox(Particle fx) {
        return particleBox;
    }

    public interface CustomEffectRenderer {

        int getParticleCount();

    }

    public interface ParticleSpawnHandler {

        boolean cancel(Particle fx);

    }

	/*
	public static boolean isEntityCloseEnough(Particle fx, double x, double y, double z) {
		if (fx instanceof CustomRenderFX) {
			double dx = fx.posX-x;
			double dy = fx.posY-y;
			double dz = fx.posZ-z;
			return ((CustomRenderFX)fx).getRenderRange()*30 >= dx*dx+dy*dy+dz*dz;
		}
		return true;
	}*/
}
