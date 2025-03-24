package reika.dragonapi.instantiable.rendering;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.extras.ThrottleableEffectRenderer;
import reika.dragonapi.instantiable.data.maps.PluralMap;
import reika.dragonapi.interfaces.entity.CustomRenderFX;
import reika.dragonapi.libraries.java.ReikaArrayHelper;
import reika.dragonapi.libraries.rendering.ReikaRenderHelper;

import java.util.*;

public abstract class ReikaParticleEngine extends ParticleEngine implements ThrottleableEffectRenderer.CustomEffectRenderer {

    private final HashMap<RenderKey, ParticleList> particles = new HashMap<>();
    private final PluralMap<RenderKey> keyMap = new PluralMap<>(2);
    private final Collection<Particle> queuedParticles = new ArrayList<>();

    private final Random rand = new Random();

    public static final int MAX_PARTICLES = ThrottleableEffectRenderer.getRegisteredInstance().limit;

    public static final TextureMode blockTex = new VanillaTextureMode(InventoryMenu.BLOCK_ATLAS);
//    public static final TextureMode itemTex = new VanillaTextureMode(TextureMap.locationItemsTexture);
    public static final TextureMode particleTex = new VanillaTextureMode(ResourceLocation.parse("textures/particle/particles.png"));

    private final RenderKey DEFAULT_RENDER = new RenderKey(particleTex, new RenderMode());

    private boolean isRendering;
    private boolean isTicking;

    public static final ReikaParticleEngine defaultCustomEngine = new ReikaParticleEngine() {

        @Override
        protected void registerClasses() {
//        todo    ThrottleableEffectRenderer.getRegisteredInstance().registerDelegateRenderer(EntityBlurFX.class, this);
//            ThrottleableEffectRenderer.getRegisteredInstance().registerDelegateRenderer(EntityFloatingSeedsFX.class, this);
        }

    };

    protected ReikaParticleEngine() {
        super(Minecraft.getInstance().level, Minecraft.getInstance().getTextureManager());
    }

    public final void register() {
        MinecraftForge.EVENT_BUS.register(this);
//        FMLCommonHandler.instance.bus().register(this);
        this.registerClasses();
    }

    protected abstract void registerClasses();

    public final void registerAdditionalClass(Class<? extends Particle> c) {
        ThrottleableEffectRenderer.getRegisteredInstance().registerDelegateRenderer(c, this);
    }

    public final String getStatistics() {
        return this.getParticleCount()+" Particles, "+keyMap.size()+" keys";
    }

    public final void renderParticles(PoseStack stack, Entity entity, float frame) {
        stack.pushPose();
//        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
//        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
//        GL11.glAlphaFunc(GL11.GL_GREATER, 1/255F);

        isRendering = true;

        for (ParticleList parts : particles.values()) {
            parts.render(stack, entity, frame);
        }

        isRendering = false;

//        GL11.glPopAttrib();
        stack.popPose();
    }


    public final void addEffect(Particle fx) {
        //int layer = fx.getFXLayer();
        //int dim = world.provider.dimensionId;
        if (isRendering) {
            DragonAPI.LOGGER.error("Tried adding a particle mid-render!");
            Thread.dumpStack();
            return;
        }
        if (isTicking) {
            queuedParticles.add(fx);
            return;
        }
        RenderKey rm = DEFAULT_RENDER;
        if (fx instanceof CustomRenderFX) {
            rm = this.getOrCreateKey(((CustomRenderFX)fx).getTexture(), ((CustomRenderFX)fx).getRenderMode());
        }
        this.addParticle(rm, fx);
    }

    private void addParticle(RenderKey rm, Particle fx) {
        ParticleList li = particles.get(rm);
        if (li == null) {
            li = new ParticleList(rm);
            particles.put(rm, li);
        }
        li.addParticle(fx);
    }

    private RenderKey getOrCreateKey(TextureMode tex, RenderMode rm) {
        RenderKey rk = keyMap.get(tex, rm);
        if (rk == null) {
            rk = new RenderKey(tex, rm);
            keyMap.put(rk, tex, rm);
        }
        return rk;
    }

    public final void updateEffects() {
        var mc = Minecraft.getInstance(); //FMLClientHandler.instance.getClient();
        if (mc.level == null)
            return;
        isTicking = true;
        var dim = mc.level.dimension();
        for (ParticleList li : particles.values()) {
            li.tick();
        }
        isTicking = false;
        if (!queuedParticles.isEmpty()) {
            for (Particle fx : queuedParticles) {
                this.addEffect(fx);
            }
            queuedParticles.clear();
        }
    }

    public final void clearEffects(Level world) {
        particles.clear();
    }


    public final int getParticleCount() {
        int ret = 0;
        for (ParticleList li : particles.values()) {
            ret += li.particles.size(); //not count since this is for debug
        }
        return ret;
    }

    private static class ParticleList {

        private final RenderKey key;
        private final ArrayList<ParticleEntry> particles = new ArrayList<>();
        private int effectiveCount = 0;

        private ParticleList(RenderKey rk) {
            key = rk;
        }

        private void render(PoseStack stack, Entity entity, float frame) {
            if (!particles.isEmpty()) {
                stack.pushPose();
                key.apply();

                /*float yaw = ActiveRenderInfo.rotationX;
                float pitch = ActiveRenderInfo.rotationZ;

                float f3 = ActiveRenderInfo.rotationYZ;
                float f4 = ActiveRenderInfo.rotationXY;
                float f5 = ActiveRenderInfo.rotationXZ;

                Particle.interpPosX = entity.lastTickPosX+(entity.posX-entity.lastTickPosX)*frame;
                Particle.interpPosY = entity.lastTickPosY+(entity.posY-entity.lastTickPosY)*frame;
                Particle.interpPosZ = entity.lastTickPosZ+(entity.posZ-entity.lastTickPosZ)*frame;*/

                var tess = Tesselator.getInstance();
                var v5 = tess.getBuilder();
                v5.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);

                for (ParticleEntry p : particles) {
                    Particle fx = p.effect;
//        todo            if (ThrottleableEffectRenderer.isParticleVisible(fx)) {
                        //if (ThrottleableEffectRenderer.isEntityCloseEnough(fx, Particle.interpPosX, Particle.interpPosY, Particle.interpPosZ)) {
                        if (key.mode.flags[RenderModeFlags.LIGHT.ordinal()]) {
//                   todo         v5.setBrightness(fx.getBrightnessForRender(frame));
                        }
                        else {
//                   todo         v5.setBrightness(240);
                        }
                        fx.render(v5, Minecraft.getInstance().gameRenderer.getMainCamera(), Minecraft.getInstance().getDeltaFrameTime());//1.19 = .getPartialTick()); //todo yaw, f5, pitch, f3, f4);
                        //}
                    }
//                }

                tess.end();

                stack.popPose();
            }
        }

        private boolean isEmpty() {
            return particles.isEmpty();
        }

        private void addParticle(Particle fx) {
            ParticleEntry e = new ParticleEntry(fx);
            particles.add(e);
            if (e.countsToLimit)
                effectiveCount++;

            if (effectiveCount >= MAX_PARTICLES) {
                int i = 0;
                ParticleEntry rfx = particles.get(i);
                while (!rfx.countsToLimit && i < particles.size()-1) {
                    i++;
                    rfx = particles.get(i);
                }
                particles.remove(i);
                effectiveCount--;
            }
        }

        private void tick() {
            Iterator<ParticleEntry> it = particles.iterator();
            while (it.hasNext()) {
                try {
                    ParticleEntry fx = it.next();
                    if (fx != null) {
                        fx.effect.tick();
                    }
                    if (fx.effect == null || fx.effect.isAlive()) {
                        it.remove();
                        effectiveCount--;
                    }
                }
                catch (ConcurrentModificationException e) {
                    DragonAPI.LOGGER.info("CME thrown updating particle type "+key+"!");
                    //e.printStackTrace();
                }
            }
        }

    }

    private static class ParticleEntry {

        private final Particle effect;
        private final boolean countsToLimit;

        private ParticleEntry(Particle fx) {
            effect = fx;
            boolean flag = true;
            if (fx instanceof CustomRenderFX) {
                flag = !((CustomRenderFX)fx).rendersOverLimit();
            }
            countsToLimit = flag;
        }

    }

    private static class RenderKey {

        private final RenderMode mode;
        private final TextureMode texture;

        private RenderKey(TextureMode s, RenderMode rm) {
            texture = s;
            mode = rm;
        }

        private void apply() {
            mode.apply();
            texture.bind();
        }

        @Override
        public int hashCode() {
            return texture.hashCode() ^ mode.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof RenderKey r) {
                return texture.equals(r.texture) && mode.equals(r.mode);
            }
            return false;
        }

    }

    public static abstract class TextureMode {

        protected abstract void bind();

        @Override
        public final boolean equals(Object o) {
            return o.getClass() == this.getClass() && this.isTextureSame((TextureMode)o);
        }

        protected abstract boolean isTextureSame(TextureMode o);

        @Override
        public abstract int hashCode();

    }

    public static final class CustomTextureMode extends TextureMode {

        private final Class reference;
        private final ResourceLocation texture;

        public CustomTextureMode(Class c, ResourceLocation t) {
            reference = c;
            texture = t;
        }

        @Override
        protected void bind() {
//            ReikaTextureHelper.bindFinalTexture(reference, texture);
            RenderSystem.setShaderTexture(0, texture);
        }

        @Override
        protected boolean isTextureSame(TextureMode o) {
            CustomTextureMode cm = (CustomTextureMode)o;
            return cm.reference == reference && texture.equals(cm.texture);
        }

        @Override
        public int hashCode() {
            return reference.hashCode() ^ texture.hashCode();
        }

    }

    private static final class VanillaTextureMode extends TextureMode {

        private final ResourceLocation resource;

        private VanillaTextureMode(ResourceLocation loc) {
            resource = loc;
        }

        @Override
        protected void bind() {
            RenderSystem.setShaderTexture(0, resource);
        }

        @Override
        protected boolean isTextureSame(TextureMode o) {
            return ((VanillaTextureMode)o).resource.equals(resource);
        }

        @Override
        public int hashCode() {
            return resource.hashCode();
        }

    }

    public static final class RenderMode {

        private final boolean[] flags = new boolean[RenderModeFlags.list.length];

        public RenderMode() {
            for (int i = 0; i < flags.length; i++) {
                flags[i] = RenderModeFlags.list[i].defaultValue;
            }
        }

        public RenderMode setFlag(RenderModeFlags f, boolean flag) {
            flags[f.ordinal()] = flag;
            return this;
        }

        @Override
        public int hashCode() {
            return ReikaArrayHelper.booleanToBitflags(flags);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof RenderMode r) {
                return Arrays.equals(flags, r.flags);
            }
            return false;
        }

        @Override
        public String toString() {
            return Arrays.toString(flags);
        }

        private void apply() {
            for (int i = 0; i < flags.length; i++) {
                RenderModeFlags.list[i].apply(flags[i]);
            }
        }

    }

    public enum RenderModeFlags {
        ALPHA(true),
        LIGHT(true),
        ADDITIVE(false),
        DEPTH(true),
        ALPHACLIP(true);

        private final boolean defaultValue;

        private static final RenderModeFlags[] list = values();

        RenderModeFlags(boolean f) {
            defaultValue = f;
        }

        private int getFlag() {
            return 1 << this.ordinal();
        }

        private void apply(boolean set) {
            switch(this) {
                case LIGHT:
                    if (set) {
//                todo        GL11.glEnable(GL11.GL_LIGHTING);
                        ReikaRenderHelper.enableEntityLighting();
                    }
                    else {
//                todo        GL11.glDisable(GL11.GL_LIGHTING);
                        ReikaRenderHelper.disableEntityLighting();
                    }
                    break;

                case ALPHA:
                    if (set) {
                        RenderSystem.enableBlend();
                    }
                    else {
                        RenderSystem.disableBlend();
                    }
                    break;

                case ADDITIVE:
                    if (set) {
//                todo        BlendMode.ADDITIVEDARK.apply();
                        RenderSystem.blendFunc(GlStateManager.SourceFactor.DST_ALPHA, GlStateManager.DestFactor.ONE);
                    }
                    else {
                        RenderSystem.defaultBlendFunc();
                    }
                    break;

                case DEPTH:
                    if (set && !ThrottleableEffectRenderer.renderThroughWalls()) {
                        RenderSystem.enableDepthTest();
                    }
                    else {
                        RenderSystem.disableDepthTest();
                    }
                    break;

                case ALPHACLIP:
                    if (set) {
//                        GL11.glEnable(GL11.GL_ALPHA_TEST);
                    }
                    else {
//                        GL11.glDisable(GL11.GL_ALPHA_TEST);
                    }
                    break;
            }
        }
    }
}
