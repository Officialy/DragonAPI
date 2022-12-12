package reika.dragonapi.instantiable.effects;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.util.Mth;

public class StringParticleFX extends Particle {

    private final String string;

    public StringParticleFX(ClientLevel world, double x, double y, double z, String s, double xd, double yd, double zd) {
        super(world, x, y, z);
        string = s;
        gravity = 0.225F;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        lifetime = 150;
        this.scale(0.05F);
//        this.noClip = true;
    }

    public void setScale(float f) {
        scale(f);
        yd *= f / 0.025;
    }

    public void setLife(int f) {
        lifetime = f;
    }

    @Override
    public void render(VertexConsumer buffer, Camera pRenderInfo, float pPartialTicks) {
        PoseStack pose = new PoseStack();
//        rotationYaw = (-Minecraft.getInstance().player.getYRot());
//        rotationPitch = Minecraft.getInstance().player.getXRot();
        boolean depth = true;
        boolean shadow = false;

        float locX = ((float) (xo + (x - xo)) * pPartialTicks);//* par2 - interpPosX));
        float locY = ((float) (yo + (y - yo)) * pPartialTicks);//* par2 - interpPosY));
        float locZ = ((float) (zo + (z - zo)) * pPartialTicks);//* par2 - interpPosZ));
        pose.pushPose();
        yd = Math.max(yd, 0);

        if (!depth)
            RenderSystem.disableDepthTest();

        pose.translate(locX, locY, locZ);
//        pose.mulPose(Axis.YP.rotation(180)); //rotationYaw
//        pose.mulPose(Axis.XP.rotation(1)); //rotationPitch

//        pose.scale(-1, -1, 1);
        pose.scale(0.2f, 0.2f, 0.2f); //todo used to be particleScale, changed to 1,1,1 for now?
        Font f = Minecraft.getInstance().font;
//        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 0.003662109F);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableTexture();
//        GL11.glDisable(GL11.GL_LIGHTING);
        RenderSystem.defaultBlendFunc();

        RenderSystem.enableBlend();
//        GL11.glEnable(GL11.GL_ALPHA_TEST);

        RenderSystem.setShaderColor(1, 1, 1, 1);
        int color = 0xffffff;
        int w = -Mth.floor(f.width(string) / 2.0F);
        int h = -Mth.floor(f.lineHeight / 2F);
        if (shadow)
            f.drawShadow(pose, string, 1 + w, 1 + h, color);
        else
            f.draw(pose, string, w, h, color);

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableDepthTest();

        pose.popPose();
//        ReikaTextureHelper.bindParticleTexture();
        Minecraft.getInstance().textureManager.bindForSetup(TextureAtlas.LOCATION_PARTICLES);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }
}
