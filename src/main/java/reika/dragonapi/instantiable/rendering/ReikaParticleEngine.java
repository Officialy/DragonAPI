package reika.dragonapi.instantiable.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.ParticleResources;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import reika.dragonapi.DragonOptions;
import reika.dragonapi.client.CustomParticleLayers;
import reika.dragonapi.client.BoundedParticleQueue;
import reika.dragonapi.client.ParticleAdmissionPolicy;
import reika.dragonapi.extras.ThrottleableEffectRenderer;
import reika.dragonapi.instantiable.effects.EntityBlurFX;
import reika.dragonapi.instantiable.effects.EntityFloatingSeedsFX;
import reika.dragonapi.interfaces.entity.CustomRenderFX;

/** Custom batches backed by an actual native engine, sharing the loaded particle resources.
 * Constructing the facade (including its default instance) never constructs a native engine.
 * Registration attaches the backend to the global engine's ticking/extraction/level lifecycle.
 */
public abstract class ReikaParticleEngine implements ThrottleableEffectRenderer.CustomEffectRenderer {
    public static final int MAX_PARTICLES=Math.max(250,DragonOptions.PARTICLELIMIT.getValue());
    public static final TextureMode blockTex=new VanillaTextureMode(TextureAtlas.LOCATION_BLOCKS);
    public static final TextureMode itemTex=new VanillaTextureMode(TextureAtlas.LOCATION_ITEMS);
    public static final TextureMode particleTex=new VanillaTextureMode(TextureAtlas.LOCATION_PARTICLES);
    public static final ReikaParticleEngine defaultCustomEngine=new ReikaParticleEngine() {
        @Override protected void registerClasses() {
            registerAdditionalClass(EntityBlurFX.class);
            registerAdditionalClass(EntityFloatingSeedsFX.class);
        }
    };
    private Backend backend;
    private boolean registered;
    protected ReikaParticleEngine() {}
    protected abstract void registerClasses();

    public final void register() {
        RenderSystem.assertOnRenderThread();
        if(registered) return;
        registerClasses();
        registered=true;
    }
    /** Exact-class registration; multiple classes share one backend and therefore one native tick. */
    public final void registerAdditionalClass(Class<? extends Particle> type) {
        ThrottleableEffectRenderer.getRegisteredInstance().registerDelegateRenderer(Objects.requireNonNull(type),backend());
    }
    private Backend backend() {
        RenderSystem.assertOnRenderThread();
        if(backend==null) {
            var parent=ThrottleableEffectRenderer.getRegisteredInstance().getOriginalEngine();
            var access=ThrottleableEffectRenderer.access(parent);
            backend=new Backend(access.dragonapi$getLevel(),access.dragonapi$getResources());
        }
        return backend;
    }
    /** Exposes the owned native engine for modern integrations; callers must use the owning render thread. */
    public final ParticleEngine getNativeEngine() { return backend(); }
    public final void addEffect(Particle particle) { backend().add(Objects.requireNonNull(particle)); }
    /** Manual operation for unregistered engines. Registered engines are ticked by their native parent. */
    public final void updateEffects() { backend().tick(); }
    public final void clearEffects(ClientLevel level) { backend().setLevel(level); }
    public final void extract(ParticlesRenderState state,Frustum frustum,Camera camera,float partialTick) {
        backend().extract(state,frustum,camera,partialTick);
    }
    @Override public final int getParticleCount() {
        RenderSystem.assertOnRenderThread();
        return backend==null?0:ThrottleableEffectRenderer.forEngine(backend).getParticleCount();
    }
    public final String getStatistics() {
        return getParticleCount()+" Particles, "+(backend==null?0:backend.keys.size())+" keys";
    }

    /** Values are immutable; RenderMode is mutable only as a caller-side builder. */
    private record RenderKey(ParticleRenderType group,Identifier texture,int flags) {}
    private static final class Entry {
        final Particle particle;
        final RenderKey key;
        final boolean counted;
        boolean budgeted;
        Entry(Particle particle,RenderKey key,boolean counted) { this.particle=particle; this.key=key; this.counted=counted; }
    }
    private static final class Backend extends ParticleEngine implements ParticleAdmissionPolicy {
        private final Map<RenderKey,RenderKey> keys=new HashMap<>();
        private final Map<Particle,Entry> entries=new IdentityHashMap<>();
        private final BoundedParticleQueue<Entry,RenderKey> ownership;
        private final java.util.Set<Particle> deferredRetirements=java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        private boolean ticking;
        private Backend(ClientLevel level,ParticleResources resources) {
            super(level,resources);
            ownership=new BoundedParticleQueue<>(ThrottleableEffectRenderer.forEngine(this).limit,
                    entry->entry.key,entry->entry.counted,this::retireOwnership);
        }
        private void retireOwnership(Entry entry) {
            entry.budgeted=false;
            if(ticking) {
                // Removing an active native queue node during its tick would invalidate its iterator.
                // Mark it now; native death cleanup or the post-tick sweep releases its reservation once.
                deferredRetirements.add(entry.particle);
                if(entry.particle.isAlive()) entry.particle.remove();
            } else {
                if(ThrottleableEffectRenderer.access(this).dragonapi$removeOwnedParticle(entry.particle))
                    ThrottleableEffectRenderer.forEngine(this).retire(entry.particle);
                else entries.remove(entry.particle);
            }
        }
        @Override public void particleAdmitted(Particle particle) {
            // Called after native add has reserved its ParticleLimit slot. Rejected/cancelled
            // particles never reach the pending queue and therefore have no captured entry.
            var entry=entries.get(particle);
            if(entry!=null && !entry.budgeted) { entry.budgeted=true; ownership.add(entry); }
        }
        private Entry entry(Particle particle) {
            return entries.computeIfAbsent(particle,p->{
                var texture=particleTex;
                int flags=RenderMode.DEFAULT_FLAGS;
                boolean counted=true;
                if(p instanceof CustomRenderFX custom) {
                    texture=Objects.requireNonNull(custom.getTexture(),"custom particle texture");
                    flags=Objects.requireNonNull(custom.getRenderMode(),"custom particle render mode").snapshot();
                    counted=!custom.rendersOverLimit();
                }
                var candidate=new RenderKey(p.getGroup(),Objects.requireNonNull(texture.resource(),"custom particle texture resource"),flags);
                var key=keys.computeIfAbsent(candidate,value->value);
                return new Entry(p,key,counted);
            });
        }
        @Override public void validateAdmission(Particle particle) {
            if(entries.containsKey(particle)) throw new IllegalArgumentException("A particle identity cannot be admitted twice to the same engine");
        }
        @Override public Object particleBucket(Particle particle) { return entry(particle).key; }
        @Override public boolean countsToParticleLimit(Particle particle) { return entry(particle).counted; }
        @Override public void retireParticle(Particle particle) {
            var entry=entries.remove(particle);
            if(entry!=null && entry.budgeted) { ownership.remove(entry); entry.budgeted=false; }
            deferredRetirements.remove(particle);
        }
        @Override public SingleQuadParticle.Layer particleLayer(Particle particle,SingleQuadParticle.Layer source) {
            var key=entry(particle).key;
            return CustomParticleLayers.map(source,key.texture,key.flags);
        }
        @Override public void tick() {
            ThrottleableEffectRenderer.forEngine(this).checkOwner();
            ticking=true;
            try { super.tick(); } finally { ticking=false; }
            for(var particle:java.util.List.copyOf(deferredRetirements)) {
                if(ThrottleableEffectRenderer.access(this).dragonapi$removeOwnedParticle(particle))
                    ThrottleableEffectRenderer.forEngine(this).releaseRetired(particle);
                else retireParticle(particle); // Native tick already released the dead particle's slot.
            }
            var owned=new IdentityHashMap<Particle,Boolean>();
            for(var particle:ThrottleableEffectRenderer.access(this).dragonapi$getOwnedParticles()) owned.put(particle,Boolean.TRUE);
            for(var particle:java.util.List.copyOf(entries.keySet()))
                if(!owned.containsKey(particle)) retireParticle(particle);
            var used=new java.util.HashSet<RenderKey>();
            for(var entry:entries.values()) used.add(entry.key);
            keys.keySet().retainAll(used);
        }
        @Override public void clearParticles() {
            super.clearParticles();
            entries.clear(); keys.clear(); deferredRetirements.clear();
            // Detach tickets without invoking retirement twice: native clear already retired them.
            while(ownership.poll()!=null) {}
        }
    }

    /** Resource identities replace global bind calls; native extraction carries texture state to drawing. */
    public abstract static class TextureMode {
        public abstract Identifier resource();
        protected abstract boolean isTextureSame(TextureMode other);
        @Override public final boolean equals(Object other) {
            return this==other || other!=null && other.getClass()==getClass() && isTextureSame((TextureMode)other);
        }
        @Override public abstract int hashCode();
    }
    public static final class CustomTextureMode extends TextureMode {
        private final Class<?> reference;
        private final Identifier texture;
        public CustomTextureMode(Class<?> reference,Identifier texture) {
            this.reference=Objects.requireNonNull(reference); this.texture=Objects.requireNonNull(texture);
        }
        @Override public Identifier resource() { return texture; }
        @Override protected boolean isTextureSame(TextureMode other) {
            var value=(CustomTextureMode)other; return reference==value.reference && texture.equals(value.texture);
        }
        @Override public int hashCode() { return 31*reference.hashCode()+texture.hashCode(); }
    }
    private static final class VanillaTextureMode extends TextureMode {
        private final Identifier texture;
        private VanillaTextureMode(Identifier texture) { this.texture=Objects.requireNonNull(texture); }
        @Override public Identifier resource() { return texture; }
        @Override protected boolean isTextureSame(TextureMode other) { return texture.equals(((VanillaTextureMode)other).texture); }
        @Override public int hashCode() { return texture.hashCode(); }
    }
    public static final class RenderMode {
        public static final int DEFAULT_FLAGS=defaultFlags();
        private int flags=DEFAULT_FLAGS;
        private static int defaultFlags() {
            int result=0;
            for(var flag:RenderModeFlags.values()) if(flag.defaultValue) result|=flag.mask();
            return result;
        }
        public RenderMode setFlag(RenderModeFlags flag,boolean value) {
            int bit=Objects.requireNonNull(flag).mask(); flags=value?flags|bit:flags&~bit; return this;
        }
        public boolean getFlag(RenderModeFlags flag) { return (flags&Objects.requireNonNull(flag).mask())!=0; }
        public int snapshot() { return flags; }
        @Override public int hashCode() { return flags; }
        @Override public boolean equals(Object other) { return other instanceof RenderMode mode && mode.flags==flags; }
        @Override public String toString() {
            var values=new boolean[RenderModeFlags.values().length];
            for(var flag:RenderModeFlags.values()) values[flag.ordinal()]=getFlag(flag);
            return java.util.Arrays.toString(values);
        }
    }
    public enum RenderModeFlags {
        ALPHA(true), LIGHT(true), ADDITIVE(false), DEPTH(true), ALPHACLIP(true), FOG(true);
        private final boolean defaultValue;
        RenderModeFlags(boolean value) { defaultValue=value; }
        public int mask() { return 1<<ordinal(); }
    }
}
