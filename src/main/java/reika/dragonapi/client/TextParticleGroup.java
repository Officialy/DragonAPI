package reika.dragonapi.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.network.chat.Component;
import reika.dragonapi.instantiable.effects.StringParticleFX;

/** Extracts immutable text and positions; drawing never reads a live particle. */
public final class TextParticleGroup extends ParticleGroup<StringParticleFX> {
    public TextParticleGroup(ParticleEngine engine) { super(engine); }

    @Override
    public ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float partialTick) {
        return new State(particles.stream().filter(particle -> frustum.isVisible(particle.getBoundingBox()))
                .map(particle -> particle.extract(partialTick)).toList());
    }

    private record State(List<StringParticleFX.TextState> texts) implements ParticleGroupRenderState {
        @Override
        public void submit(SubmitNodeCollector collector, CameraRenderState camera) {
            var font = Minecraft.getInstance().font;
            for (var text : texts) {
                PoseStack pose = new PoseStack();
                pose.translate(text.x() - camera.pos.x, text.y() - camera.pos.y, text.z() - camera.pos.z);
                pose.rotate(camera.orientation);
                float scale = 0.025F * text.scale();
                pose.scale(scale, -scale, scale);
                collector.submitText(pose, -font.width(text.text()) / 2F, 0,
                        Component.literal(text.text()).getVisualOrderText(), true, Font.DisplayMode.NORMAL,
                        15728880, 0xFFFFFFFF, 0, 0);
            }
        }
    }
}
