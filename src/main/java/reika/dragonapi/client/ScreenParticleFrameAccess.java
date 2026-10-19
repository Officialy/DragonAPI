package reika.dragonapi.client;
import com.mojang.renderpearl.api.commands.RenderPass;
/** Prepared native groups retain their normal frame ownership and staged vertex buffers. */
public interface ScreenParticleFrameAccess {
    boolean dragonapi$hasScreenParticles();
    void dragonapi$executeScreenParticles(RenderPass pass,boolean improvedTransparency);
}
