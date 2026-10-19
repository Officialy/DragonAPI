package reika.dragonapi.client;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
/** Screen quads retain native storage while being submitted in a separate late phase. */
public interface ScreenParticleStateAccess {
    void dragonapi$markScreenStorage();
    QuadParticleRenderState dragonapi$getScreenStorage();
}
