///*******************************************************************************
// * @author Reika Kalseki
// *
// * Copyright 2017
// *
// * All rights reserved.
// * Distribution of the software in any form is only allowed with
// * explicit, prior permission from the owner.
// ******************************************************************************/
//package reika.dragonapi.auxiliary.trackers;
//
//import com.mojang.blaze3d.platform.Window;
//import com.mojang.blaze3d.systems.RenderSystem;
//import com.mojang.blaze3d.vertex.PoseStack;
//import com.mojang.math.Axis;
//import net.minecraft.client.player.AbstractClientPlayer;
//import net.minecraft.client.renderer.MultiBufferSource;
//import net.minecraft.client.renderer.entity.EntityRendererProvider;
//import net.minecraftforge.client.event.ScreenEvent;
//import org.joml.Quaternionf;
//import reika.dragonapi.DragonAPI;
//import reika.dragonapi.DragonOptions;
//import reika.dragonapi.extras.ModifiedPlayerModel;
//import reika.dragonapi.extras.ReikaModel;
//import reika.dragonapi.instantiable.data.maps.MultiMap;
//import reika.dragonapi.interfaces.PlayerRenderObj;
//import reika.dragonapi.io.ReikaFileReader;
//import reika.dragonapi.libraries.java.ReikaObfuscationHelper;
//import reika.dragonapi.libraries.rendering.ReikaRenderHelper;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.renderer.entity.player.PlayerRenderer;
//import net.minecraft.world.entity.LivingEntity;
//import net.minecraft.world.entity.player.Player;
//import org.lwjgl.opengl.GL11;
//
//import javax.imageio.ImageIO;
//import java.awt.image.BufferedImage;
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.util.*;
//
//
//public final class PlayerSpecificRenderer {
//
//    public static final PlayerSpecificRenderer instance = new PlayerSpecificRenderer();
//
//    private final MultiMap<UUID, PlayerRenderObj> renders = new MultiMap().setNullEmpty().setOrdered(new RenderComparator());
//    private final HashMap<UUID, String> glows = new HashMap();
//    private final HashMap<UUID, String> customGlows = new HashMap();
//
//    private final ReikaModel modelReika = new ReikaModel();
//    //private final SamakiModel modelSamaki = new SamakiModel();
//
//    private PlayerSpecificRenderer() {
//        this.registerRenderer(DragonAPI.Reika_UUID, new PlayerModelRenderer(modelReika));
//        //this.registerRenderer(UUID.fromString("bca741d8-d934-4785-9c26-f6a4141be124"), new PlayerModelRenderer(modelSamaki));
//
//        this.registerGlow(DragonAPI.Reika_UUID, "reika_glow");
//        this.registerGlow(UUID.fromString("bca741d8-d934-4785-9c26-f6a4141be124"), "samaki_glow");
//        this.registerGlow(UUID.fromString("d859c5ea-37e9-43d7-b3b9-523e448bfda0"), "frey_glow");
//        this.registerGlow(UUID.fromString("bb5029b7-9381-4d99-aaa9-106da41aa659"), "officialy_glow");
//    }
//
//    public void registerIntercept() {
//        Map<Class, Render> map = RenderManager.instance.entityRenderMap;
//        map.put(Player.class, new CustomPlayerRenderer(map.get(Player.class)));
//    }
//
//    public void registerRenderer(UUID uuid, PlayerRenderObj r) {
//        renders.addValue(uuid, r);
//
//        //If anyone flips out over this and complains "OMG REIKA GIVES HIMSELF ALL THE RENDERS IN THE DEV ENVIRONMENT! DRM!",
//        //You are:
//        //If you cannot understand Java: making wild accusations based on your own ignorance
//        //If you can understand Java: A disgrace to other programmers for harassing a developer over what you should understand is harmless
//        //Update: Someone did it. Congratulations. Now go eat the contents of your toilet.
//        if (/*DragonAPI.isReikasComputer() && */ReikaObfuscationHelper.isDeObfEnvironment()) {
//            renders.addValue(DragonAPI.Reika_UUID, r);
//        }
//    }
//
//    private void registerGlow(UUID uuid, String s) {
//        glows.put(uuid, "/assets/dragonapi/" + s + ".png");
//    }
//
//    public void loadGlowFiles() {
//        File f = new File(DragonAPI.getMinecraftDirectory(), "config/Reika/glowrenders.dat");
//        if (f.exists()) {
//            ArrayList<String> li = ReikaFileReader.getFileAsLines(f, true);
//            for (String s : li) {
//                try {
//                    String[] parts = s.split("=");
//                    UUID uid = UUID.fromString(parts[0]);
//                    File img = new File(DragonAPI.getMinecraftDirectory(), "config/Reika/glowrenders/" + parts[1]);
//                    if (!img.exists())
//                        throw new FileNotFoundException();
//                    BufferedImage im = ImageIO.read(img);
//                    if (im != null) {
//                        customGlows.put(uid, "*" + img.getAbsolutePath());
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    DragonAPI.LOGGER.error("Could not load glow render entry " + s);
//                }
//            }
//        }
//    }
//
//    public String getGlow(UUID uid) {
//        String s = glows.get(uid);
//        if (s == null)
//            s = customGlows.get(uid);
//        return s;
//    }
//
//    private void renderAdditionalObjects(PoseStack stack, Player ep, float ptick) {
//        if (ep.getUUID() == DragonAPI.Reika_UUID) {
////     todo       ReikaShader.instance.prepareRender(ep);
//        }
//        if (ep == Minecraft.getInstance().player && !DragonOptions.CUSTOMRENDER.getState())
//            return;
//        Collection<PlayerRenderObj> c = renders.get(ep.getUUID());
//        if (c != null) {
//            for (PlayerRenderObj r : c) {
//                r.render(stack, ep, ptick, new PlayerRotationData(ep, ptick));
//            }
//        }
//    }
//
//    private static class ModelReikaEars extends ModelRenderer {
//
//        public ModelReikaEars(ModelBase b, int x, int y) {
//            super(b, x, y);
//            this.addBox(3.2F, -5F, 3.5F, 2, 1, 5);
//            this.addBox(-3.2F, -5F, 3.5F, 2, 1, 5);
//            this.addBox(3.2F, -4F, 3.5F, 2, 1, 5);
//            this.addBox(-3.2F, -4F, 3.5F, 2, 1, 5);
//            this.setRotationPoint(-1, -0.5F, -0.2F);
//            rotateAngleX = 35;
//            rotateAngleY = 25; //-25 for R
//            rotateAngleZ = 30; //-30 for R
//
//            //-35, -35, 22 for DR, -35, 35, -22 for DL
//            this.setTextureSize(64, 32);
//        }
//
//    }
//
//    private static final class CustomPlayerRenderer extends PlayerRenderer {
//
////        private CustomPlayerRenderer(Render original) {
////            super();
////            renderManager = RenderManager.instance;
////
////            //modelBipedMain.bipedHeadwear = new ModelReikaEars(modelBipedMain, 40, 25);
////        }
//
//        public CustomPlayerRenderer(EntityRendererProvider.Context p_174557_, boolean p_174558_) {
//            super(p_174557_, p_174558_);
//        }
//
//        @Override
//        protected void rotateCorpse(LivingEntity ep, float par2, float par3, float partialTick) {
//            super.rotateCorpse(ep, par2, par3, partialTick);
//            if (ep.isInvisibleToPlayer(Minecraft.getInstance().player))
//                return;
//            if (MinecraftForgeClient.getRenderPass() == 1 || MinecraftForgeClient.getRenderPass() == -1)
//                PlayerSpecificRenderer.instance.renderAdditionalObjects((Player) ep, partialTick);
//        }
//
//        @Override
//        public void render(AbstractClientPlayer pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {
//            super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
//        }
//
//        @Override
//        protected void renderModel(LivingEntity ep, float f1, float f2, float f3, float f4, float f5, float f6) {
//            if (MinecraftForgeClient.getRenderPass() == 0 || MinecraftForgeClient.getRenderPass() == -1)
//                super.renderModel(ep, f1, f2, f3, f4, f5, f6);
//            if (ep.isInvisibleToPlayer(Minecraft.getInstance().player))
//                return;
//            String glow = PlayerSpecificRenderer.instance.getGlow(ep.getUUID());
//            if (glow != null) {
////        todo        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
////                todo       GL11.glDisable(GL11.GL_LIGHTING);
//                ReikaRenderHelper.disableEntityLighting();
//                RenderSystem.enableBlend();
////todo                ReikaGLHelper.RenderSystem.defaultBlendFunc();
//                if (glow.charAt(0) == '*') {
//                    ReikaTextureHelper.bindRawTexture(glow.substring(1));
//                } else {
//                    ReikaTextureHelper.bindTexture(DragonAPI.class, glow);
//                }
//                this.renderWithoutTextureBind(ep, f1, f2, f3, f4, f5, f6);
////           todo     GL11.glPopAttrib();
//            }
//        }
//
//        @Override
//        public void renderRightHand(PoseStack pMatrixStack, MultiBufferSource pBuffer, int pCombinedLight, AbstractClientPlayer pPlayer) {
//            super.renderRightHand(pMatrixStack, pBuffer, pCombinedLight, pPlayer);
//            String glow = PlayerSpecificRenderer.instance.getGlow(pPlayer.getUUID());
//            if (glow != null) {
////        todo        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
////   todo       GL11.glDisable(GL11.GL_LIGHTING);
//                ReikaRenderHelper.disableEntityLighting();
//                RenderSystem.enableBlend();
////   todo             ReikaGLHelper.RenderSystem.defaultBlendFunc();
//                if (glow.charAt(0) == '*') {
//                    ReikaTextureHelper.bindRawTexture(glow.substring(1));
//                } else {
//                    ReikaTextureHelper.bindTexture(DragonAPI.class, glow);
//                }
//                super.renderRightHand(pMatrixStack, pBuffer, pCombinedLight, pPlayer);
//            //todo    GL11.glPopAttrib();
//            }
//        }
//
//        @Override
//        public void renderLeftHand(PoseStack pMatrixStack, MultiBufferSource pBuffer, int pCombinedLight, AbstractClientPlayer pPlayer) {
//            super.renderLeftHand(pMatrixStack, pBuffer, pCombinedLight, pPlayer);
//            String glow = PlayerSpecificRenderer.instance.getGlow(pPlayer.getUUID());
//            if (glow != null) {
////           todo     GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
////todo                GL11.glDisable(GL11.GL_LIGHTING);
//                ReikaRenderHelper.disableEntityLighting();
//                RenderSystem.enableBlend();
////   todo             ReikaGLHelper.RenderSystem.defaultBlendFunc();
//                if (glow.charAt(0) == '*') {
//                    ReikaTextureHelper.bindRawTexture(glow.substring(1));
//                } else {
//                    ReikaTextureHelper.bindTexture(DragonAPI.class, glow);
//                }
//                super.renderLeftHand(pMatrixStack, pBuffer, pCombinedLight, pPlayer);
//                //todo GL11.glPopAttrib();
//            }
//        }
//
//        private void renderWithoutTextureBind(PoseStack stack, LivingEntity ep, float f1, float f2, float f3, float f4, float f5, float f6) {
//            if (!ep.isInvisible()) {
//                mainModel.render(ep, f1, f2, f3, f4, f5, f6);
//            } else if (!ep.isInvisibleTo(Minecraft.getInstance().player)) {
//                stack.pushPose();
//                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.15F);
//                GL11.glDepthMask(false);
//                RenderSystem.enableBlend();
//                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
//                GL11.glAlphaFunc(GL11.GL_GREATER, 0.003921569F);
//                mainModel.render(ep, f1, f2, f3, f4, f5, f6);
//                RenderSystem.disableBlend();
//                GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
//                stack.popPose();
//                GL11.glDepthMask(true);
//            } else {
//                mainModel.setRotationAngles(f1, f2, f3, f4, f5, f6, ep);
//            }
//        }
//
//    }
//
//    public static class PlayerRotationData {
//
//        public final float rotationYaw;
//        public final float rotationYawHead;
//        public final float rotationPitch;
//        public final float interpYaw;
//        public final float interpYawHead;
//        public final float interpPitch;
//        private final float prevRotationYaw;
//        private final float prevRotationYawHead;
//        private final float prevRotationPitch;
//        private final float partialTick;
//        /**
//         * Compensates for in-inventory rendering
//         */
//        ScreenEvent.MouseButtonPressed event; //todo see if this works or is fucked cause its not initialized?
//        private float renderYaw;
//        private float renderYawHead;
//        private float renderPitch;
//
////            pitch = x
////            yaw = y
////            roll = z
////            prev = O
//        private PlayerRotationData(Player ep, float ptick) {
//            rotationPitch = ep.getXRot();
//            rotationYaw = ep.getYRot();
//            rotationYawHead = ep.getYHeadRot();
//            partialTick = ptick;
//            prevRotationPitch = ep.xRotO;
//            prevRotationYaw = ep.yRotO;
//            prevRotationYawHead = ep.yHeadRotO;
//
//            renderPitch = -ep.getXRot();
//            renderYawHead = -ep.getYRot() % 360 - partialTick * (ep.getYRot() - ep.yRotO);
//            renderYaw = -ep.yBodyRot % 360 - partialTick * (ep.yBodyRot - ep.yBodyRotO) + 180;
//
//            interpYaw = prevRotationYaw + (rotationYaw - prevRotationYaw) * partialTick;
//            interpYawHead = prevRotationYawHead + (rotationYawHead - prevRotationYawHead) * partialTick;
//            interpPitch = prevRotationPitch + (rotationPitch - prevRotationPitch) * partialTick;
//
//            this.compensateAngles();
//        }
//
//        private void compensateAngles() {
//            //ReikaJavaLibrary.pConsole(yc/RADIAN);
//            if (partialTick == 1.0F) {
//
//                int ySize = 136;
//                int xSize = 195;
//
//                Minecraft mc = Minecraft.getInstance();
//                Window scr = Minecraft.getInstance().getWindow();
//
//                int width = scr.getGuiScaledWidth();
//                int height = scr.getGuiScaledHeight();
//
//                int guiLeft = (width - xSize) / 2;
//                int guiTop = (height - ySize) / 2;
//
//
//                double par1 = event.getMouseX() * width / mc.getWindow().getWidth();
//                double par2 = height - event.getMouseY() * height / mc.getWindow().getHeight() - 1;
//                double par3 = guiLeft + 43 - par1;
//                double par4 = guiTop + 45 - 30 - par2;
//
//                renderYaw = -(float) Math.atan(par3 / 40.0F) * 20.0F;
//                renderYawHead = -(float) Math.atan(par3 / 40.0F) * 40.0F;
//                renderPitch = -((float) Math.atan(par4 / 40.0F)) * 20.0F;
//
//                renderYawHead += 180;
//                renderYaw += 180;
//                //renderPitch = -90;
//            }
//        }
//
//        public float getRenderYaw() {
//            return renderYaw;
//        }
//
//        public float getRenderYawHead() {
//            return renderYawHead;
//        }
//
//        public float getRenderPitch() {
//            return renderPitch;
//        }
//
//    }
//
//    private static class RenderComparator implements Comparator<PlayerRenderObj> {
//
//        @Override
//        public int compare(PlayerRenderObj o1, PlayerRenderObj o2) {
//            int p1 = o1.getRenderPriority();
//            int p2 = o2.getRenderPriority();
//            return Integer.compare(p1, p2);
//        }
//
//    }
//
//    private static class PlayerModelRenderer implements PlayerRenderObj {
//
//        private final ModifiedPlayerModel model;
//
//        private PlayerModelRenderer(ModifiedPlayerModel m) {
//            model = m;
//        }
//
//        public void render(PoseStack stack, Player ep, float tick, PlayerRotationData dat) {
//            if (ep != null) {
//                stack.pushPose();
//                //render.setRenderPassModel(modelReika);
//                model.bindTexture();
//                stack.translate(0, 1.6, 0);
//                stack.scale(1, -1, 1);
//                if (ep.isCrouching()) {
//                    stack.mulPose(new Quaternionf(Axis.XP.rotationDegrees(22.5f)));
//                    stack.translate(-0.02, 0.1, -0.05);
//                }
////todo                GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
////        todo        GL11.glFrontFace(GL11.GL_CW);
//                model.renderBodyParts(stack, ep, tick);
////          todo     GL11.glPopAttrib();
//                stack.popPose();
//            }
//        }
//
//        @Override
//        public int getRenderPriority() {
//            return Integer.MIN_VALUE;
//        }
//    }
//
//}
