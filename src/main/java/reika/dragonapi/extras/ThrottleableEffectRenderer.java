package reika.dragonapi.extras;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.AABB;
import reika.dragonapi.DragonOptions;
import reika.dragonapi.client.ParticleEngineAccess;
import reika.dragonapi.client.ParticleLayerAccess;
import net.minecraft.resources.Identifier;

/** Admission/lifecycle controller attached to the existing native engine; never replaces its resource manager. */
public final class ThrottleableEffectRenderer {
    public static boolean renderParticles=true;
    public final int limit;
    private final ParticleEngine original;
    private final State state;
    private static final class State {
        final Thread owner=Thread.currentThread();
        final Map<Class<?>,ParticleEngine> delegates=new LinkedHashMap<>();
        final List<ParticleSpawnHandler> handlers=new ArrayList<>();
        final Map<Identifier,Identifier> atlasKeys=new java.util.HashMap<>();
        ThrottleableEffectRenderer parent;
        int changingLevel;
        boolean clearing;
        Frustum frustum;
    }

    /** Repeated facades share the attached engine's controller state instead of orphaning its registrations. */
    public ThrottleableEffectRenderer(ParticleEngine engine) {
        original=Objects.requireNonNull(engine,"engine");
        var access=access(engine);
        var existing=access.dragonapi$getController();
        if(existing!=null) { state=existing.state; limit=existing.limit; }
        else {
            state=new State(); limit=Math.max(250,DragonOptions.PARTICLELIMIT.getValue());
            access.dragonapi$attachController(this);
        }
    }
    public static ParticleEngineAccess access(ParticleEngine engine) {
        if(engine instanceof ParticleEngineAccess access) return access;
        throw new IllegalStateException("Native particle controller mixin is unavailable");
    }
    public static ThrottleableEffectRenderer forEngine(ParticleEngine engine) {
        return Objects.requireNonNull(access(engine).dragonapi$getController(),"Native engine controller is not initialized");
    }
    public static ThrottleableEffectRenderer getRegisteredInstance() {
        var engine=Minecraft.getInstance().particleEngine;
        if(engine==null) throw new IllegalStateException("The native particle engine has not been created yet");
        return forEngine(engine);
    }
    public ParticleEngine getOriginalEngine() { return original; }
    private void requireOwner() {
        if(Thread.currentThread()!=state.owner) throw new IllegalStateException("Particle engine access must stay on its owning thread");
    }
    public void checkOwner() { requireOwner(); }
    public void registerDelegateRenderer(Class<?> particleClass,ParticleEngine renderer) {
        requireOwner(); Objects.requireNonNull(particleClass,"particle class"); Objects.requireNonNull(renderer,"delegate");
        if(!Particle.class.isAssignableFrom(particleClass)) throw new IllegalArgumentException("Delegate key must be a particle class");
        var child=forEngine(renderer); child.requireOwner(); var canonical=forEngine(original);
        for(var ancestor=canonical;ancestor!=null;ancestor=ancestor.state.parent)
            if(ancestor.original==renderer) throw new IllegalArgumentException("Particle delegate ownership cannot contain a cycle");
        if(renderer==Minecraft.getInstance().particleEngine) throw new IllegalArgumentException("The global engine cannot become a delegate");
        if(child.state.parent!=null && child.state.parent!=canonical) throw new IllegalArgumentException("A particle delegate has exactly one lifecycle owner");
        if(state.delegates.get(particleClass)==renderer) return;
        if(access(renderer).dragonapi$getLevel()!=access(original).dragonapi$getLevel()) renderer.setLevel(access(original).dragonapi$getLevel());
        var previous=state.delegates.put(particleClass,renderer); child.state.parent=canonical;
        releaseIfUnused(previous);
    }
    public boolean unregisterDelegateRenderer(Class<?> particleClass,ParticleEngine renderer) {
        requireOwner();
        if(state.delegates.get(particleClass)!=renderer || !state.delegates.containsKey(particleClass)) return false;
        state.delegates.remove(particleClass); releaseIfUnused(renderer); return true;
    }
    private void releaseIfUnused(ParticleEngine renderer) {
        if(renderer!=null && state.delegates.values().stream().noneMatch(value->value==renderer)) {
            forEngine(renderer).state.parent=null; renderer.clearParticles();
        }
    }
    private List<ParticleEngine> delegates() {
        var seen=new IdentityHashMap<ParticleEngine,Boolean>(); var result=new ArrayList<ParticleEngine>();
        for(var renderer:state.delegates.values()) if(seen.put(renderer,true)==null) result.add(renderer);
        return result;
    }
    public void addSpawnHandler(ParticleSpawnHandler handler) { requireOwner(); state.handlers.add(Objects.requireNonNull(handler,"handler")); }
    public boolean removeSpawnHandler(ParticleSpawnHandler handler) {
        requireOwner();
        for(int i=0;i<state.handlers.size();i++) if(state.handlers.get(i)==handler) { state.handlers.remove(i); return true; }
        return false;
    }
    public void addEffect(Particle particle) { original.add(particle); }
    /** Called before native admission: handlers can cancel without reserving a ParticleLimit slot. */
    public boolean dispatch(Particle particle) {
        requireOwner();
        if(particle==null) throw new IllegalArgumentException("You cannot spawn a null particle");
        for(var owner=forEngine(original);owner!=null;owner=owner.state.parent) if(owner.state.clearing) return true;
        for(var handler:List.copyOf(state.handlers)) if(handler.cancel(particle)) return true;
        var delegate=state.delegates.get(particle.getClass());
        if(delegate==null) return false;
        delegate.add(particle); return true;
    }
    /** Native SINGLE_QUADS combines three old layers; retain an independent budget for each texture sheet. */
    public Object bucket(Particle particle) {
        if(particle instanceof SingleQuadParticle) {
            var texture=((ParticleLayerAccess)particle).dragonapi$getLayer().textureAtlasLocation();
            return state.atlasKeys.computeIfAbsent(texture,key->key);
        }
        return particle.getGroup();
    }
    /** Native group admission uses its owned bounded FIFO rather than vanilla reservoir rejection. */
    public boolean addToGroup(Queue<Particle> particles,Particle particle) {
        requireOwner();
        particles.add(particle); return true;
    }
    public void retire(Particle particle) {
        particle.getParticleLimit().ifPresent(bound->access(original).dragonapi$changeCount(bound,-1));
        particle.remove();
    }
    public void tickDelegates() { requireOwner(); for(var renderer:delegates()) renderer.tick(); }
    public void beginLevelChange() { requireOwner(); state.changingLevel++; }
    public void finishLevelChange() { state.changingLevel--; }
    public boolean beginClear() {
        requireOwner(); if(state.clearing) return false; state.clearing=true; return true;
    }
    public void finishClear() { state.clearing=false; }
    public void levelChanged(ClientLevel level) {
        state.frustum=null;
        for(var renderer:delegates()) renderer.setLevel(level);
    }
    public void clearDelegates() {
        requireOwner(); state.frustum=null;
        state.atlasKeys.clear();
        if(state.changingLevel==0) for(var renderer:delegates()) renderer.clearParticles();
    }
    public void rememberFrustum(Frustum frustum) { state.frustum=new Frustum(frustum); }
    public void extractDelegates(ParticlesRenderState output,Frustum frustum,Camera camera,float partialTick) {
        for(var renderer:delegates()) renderer.extract(output,frustum,camera,partialTick);
    }
    public int getParticleCount() {
        requireOwner(); int count=access(original).dragonapi$countOwnedParticles();
        for(var renderer:delegates()) count+=forEngine(renderer).getParticleCount();
        return count;
    }
    public String statistics(String base) {
        var text=new StringBuilder(base);
        for(var renderer:delegates()) text.append("; ").append(renderer.countParticles());
        return text.toString();
    }
    public String getStatistics() { return original.countParticles(); }
    public static boolean renderThroughWalls() { return InputConstants.isKeyDown(InputConstants.KEY_INSERT); }
    public static boolean isParticleVisible(Particle particle) {
        return isParticleVisible(particle,getRegisteredInstance().state.frustum);
    }
    public static boolean isParticleVisible(Particle particle,Frustum frustum) {
        Objects.requireNonNull(particle,"particle");
        return particle.isAlive() && (frustum==null || frustum.isVisible(getBoundingBox(particle)));
    }
    public static AABB getBoundingBox(Particle particle) {
        Objects.requireNonNull(particle,"particle");
        var bounds=particle.getBoundingBox();
        if(particle instanceof SingleQuadParticle quad) {
            double radius=Math.max(Math.abs(quad.getQuadSize(0)),Math.abs(quad.getQuadSize(1)))*Math.sqrt(2);
            if(!Double.isFinite(radius)) throw new IllegalArgumentException("Particle visual size must be finite");
            var pos=particle.getPos();
            bounds=bounds.minmax(new AABB(pos.x-radius,pos.y-radius,pos.z-radius,pos.x+radius,pos.y+radius,pos.z+radius));
        }
        return bounds;
    }
    public interface CustomEffectRenderer { int getParticleCount(); }
    @FunctionalInterface public interface ParticleSpawnHandler { boolean cancel(Particle particle); }
}
