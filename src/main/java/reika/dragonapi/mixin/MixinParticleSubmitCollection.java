package reika.dragonapi.mixin;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.feature.phase.TranslucentFeatureRenderPhase;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import reika.dragonapi.client.ScreenParticlePhaseAccess;
/** Include the late forward phase in native grouping, draining, and reset ownership. */
@Mixin(SubmitNodeCollection.class)
public abstract class MixinParticleSubmitCollection implements ScreenParticlePhaseAccess {
    @Shadow @Final @Mutable private List<FeatureRenderPhase<?>> allPhases;
    @Unique private SimpleFeatureRenderPhase dragonapi$screenPhase;
    @Inject(method="<init>",at=@At("RETURN"))
    private void dragonapi$screenPhase(boolean improved,TranslucentFeatureRenderPhase seeThrough,CallbackInfo callback) {
        dragonapi$screenPhase=new SimpleFeatureRenderPhase();
        var phases=new java.util.ArrayList<FeatureRenderPhase<?>>(allPhases); phases.add(dragonapi$screenPhase); allPhases=List.copyOf(phases);
    }
    @Override public SimpleFeatureRenderPhase dragonapi$screenParticles() { return dragonapi$screenPhase; }
}
