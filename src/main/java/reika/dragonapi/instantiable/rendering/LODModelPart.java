//package reika.dragonapi.instantiable.rendering;
//
//import com.google.common.collect.Lists;
//import com.mojang.blaze3d.vertex.BufferBuilder;
//import com.mojang.blaze3d.vertex.PoseStack;
//import com.mojang.blaze3d.vertex.Tesselator;
//import com.mojang.blaze3d.vertex.VertexConsumer;
//import com.mojang.math.*;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.gui.screens.Screen;
//import net.minecraft.client.model.Model;
//import net.minecraft.client.model.geom.ModelPart;
//import net.minecraft.client.model.geom.builders.CubeDefinition;
//import net.minecraft.client.model.geom.builders.CubeDeformation;
//import net.minecraft.client.model.geom.builders.CubeListBuilder;
//import net.minecraft.world.entity.player.Player;
//import net.minecraft.world.level.block.entity.BlockEntity;
//import org.jetbrains.annotations.Nullable;
//import reika.dragonapi.exception.MisuseException;
//import reika.dragonapi.libraries.java.ReikaJVMParser;
//
//import java.util.List;
//
//public class LODModelPart extends CubeListBuilder {
//    private static final String JVM_FLAG = "-DragonAPI_CompileLODModels";
//    public static boolean allowCompiling = ReikaJVMParser.isArgumentPresent(JVM_FLAG);
//    private final List<CubeDefinition> cubeList = Lists.newArrayList();
//    private double renderDistanceSqr = -1;
//    private double lastDistance;
//    private int lastTileX;
//    private int lastTileY;
//    private int lastTileZ;
//    private double lastPlayerX;
//    private double lastPlayerY;
//    private double lastPlayerZ;
//
//    private int textureX;
//    private int textureY;
//    private double renderDistanceScalar = 1;
//
//    @Override
//    public CubeListBuilder texOffs(int x, int y) {
//        super.texOffs(x, y);
//        textureX = x;
//        textureY = y;
//        return this;
//    }
//
///*    @Override
//    public final ModelPart setTextureSize(int w, int h) {
//        super.setTextureSize(w, h);
//        if (allowCompiling) {
//            for (MovableBox b : ((List<MovableBox>) cubeList)) {
//                b.textureWidth = w;
//                b.textureHeight = h;
//            }
//        }
//        return this;
//    }
//
//    @Override
//    public final void setRotationPoint(float x, float y, float z) {
//        super.setRotationPoint(x, y, z);
//        if (allowCompiling) {
//            for (MovableBox b : ((List<MovableBox>) cubeList)) {
//                b.offsetX = offsetX + x;
//                b.offsetY = offsetY + y;
//                b.offsetZ = offsetZ + z;
//            }
//        }
//    }*/
//
//    @Override
//    public ModelPart addBox(float par1, float par2, float par3, int par4, int par5, int par6) {
//        if (allowCompiling) {
//            this.getCubes().add(new MovableBox(this, par1, par2, par3, par4, par5, par6));
//        } else {
//            if (getCubes().size() > 0) {
//                throw new MisuseException("You cannot have multiple pieces per model unless model compiling is enabled (jvm arg '" + JVM_FLAG + "')!");
//            } else {
//                super.addBox(par1, par2, par3, par4, par5, par6);
//            }
//        }
//        renderDistanceSqr = -1;
//        return this;
//    }
//
//    public final void addBox(LODModelPart model) {
//        if (!allowCompiling) {
//            throw new MisuseException("You cannot have multiple pieces per model unless model compiling is enabled (jvm arg '" + JVM_FLAG + "')!");
//        }
//        for (MovableBox b : (List<MovableBox>) cubeList) {
//            MovableBox b2 = b.move(model, model.rotationPointX, model.rotationPointY, model.rotationPointZ);
//			/*
//			if (b.rotationX != 0 || b.rotationY != 0 || b.rotationZ != 0) {
//				if (b2.rotationX == 0 && b2.rotationY == 0 && b2.rotationZ == 0) {
//					ReikaJavaLibrary.pConsole("Undoing rotation!!");
//				}
//			}
//			 */
//            //ReikaJavaLibrary.pConsole(b+" moves to "+b2+" rot "+b2.rotationX+", "+b2.rotationY+", "+b2.rotationZ, (model.rotateAngleX != 0 || model.rotateAngleY != 0 || model.rotateAngleZ != 0));
//            cubeList.add(b2);
//        }
//    }
//
//    protected final CubeDefinition getBox(int idx) {
//        return getCubes().get(idx);
//    }
//
//    public final LODModelPart setRenderDistanceScalar(double d) {
//        renderDistanceScalar = d;
//        renderDistanceSqr = -1;
//        return this;
//    }
//
//    private float calculateVolume() {
//        ModelPart.Cube box = this.getBox(cubeList.size() - 1);
//        float x = box.minX - box.maxX;
//        float y = box.minY - box.maxY;
//        float z = box.minZ - box.maxZ;
//        return Math.abs(x * y * z);
//    }
//
//    private double calculateRenderDistance() {
//        float size = this.calculateVolume();
//        int d = 0;
//        if (size > 1024) {
//            d = 16384;
//        } else if (size > 512) {
//            d = 4096;
//        } else if (size > 128) {
//            d = 2048;
//        } else if (size > 32) {
//            d = 1024;
//        } else if (size > 8) {
//            d = 256;
//        } else if (size > 4) {
//            d = 128;
//        } else if (size > 0) {
//            d = 96;
//        } else {
//            d = 0;
//        }
//        return Math.max(renderDistanceSqr, d) * renderDistanceScalar;
//    }
//
//    public final boolean shouldRender(double dist_squared) {
//        if (renderDistanceSqr < 0)
//            renderDistanceSqr = this.calculateRenderDistance();
//        return Screen.hasControlDown() || renderDistanceSqr * this.getDistanceMultiplier() >= dist_squared;
//    }
//
//    private double getDistanceMultiplier() {
//        return Minecraft.getInstance().options.getEffectiveRenderDistance();
//    }
//
//    public final void render(PoseStack stack, BlockEntity te, float pixelSize) {
//        if (te.hasLevel())
//            this.calcAndCacheRenderDistance(te);
//        if (!te.hasLevel() /*|| MinecraftForgeClient.getRenderPass() == -1*/ || this.shouldRender(lastDistance)) {
//            super.render(stack, pixelSize);
//        }
//    }
//
//    public final void render(PoseStack stack, int pixelSize) {
//        if (allowCompiling) {
//            if (Screen.hasControlDown()) {
//                Tesselator tesselator = Tesselator.getInstance();
//                BufferBuilder bb = tesselator.getBuilder();
//                for (MovableBox box : ((List<MovableBox>) cubeList)) {
//                    box.bake(pixelSize, pixelSize);
//                }
//            }
////       todo     else {
////                if (displayList == -1 || Keyboard.isKeyDown(Keyboard.KEY_GRAVE)) {
////                    this.compileDisplayList(pixelSize);
////                }
////                GL11.glCallList(displayList);
////            }
//        } else {
//            super.render(stack, pixelSize);
//        }
//    }
////
//// todo   private void compileDisplayList(float pixelSize)  {
////        if (!allowCompiling) {
////            throw new MisuseException("You may not use GL lists with non-compilable models!");
////        }
//////        displayList = GLAllocation.generateDisplayLists(1);
////        GL11.glNewList(displayList, GL11.GL_COMPILE);
////        Tesselator tess = Tesselator.getInstance();
//			BufferBuilder v5 = tess.getBuilder();
////
//////        for (MovableBox b : (cubeList)) {
//////            b.render(v5, pixelSize);
//////        }
////
////        GL11.glEndList();
////    }
//
//    private void calcAndCacheRenderDistance(BlockEntity te) {
//        Player ep = Minecraft.getInstance().player;
//        if (lastTileX == te.getBlockPos().getX() && lastTileY == te.getBlockPos().getY() && lastTileZ == te.getBlockPos().getZ() && lastPlayerX == ep.getX() && lastPlayerY == ep.getY() && lastPlayerZ == ep.getZ()) {
//
//        } else {
//            lastTileX = te.getBlockPos().getX();
//            lastTileY = te.getBlockPos().getY();
//            lastTileZ = te.getBlockPos().getZ();
//            lastPlayerX = ep.getX();
//            lastPlayerY = ep.getY();
//            lastPlayerZ = ep.getZ();
//            lastDistance = ep.distanceToSqr(lastTileX + 0.5, lastTileY + 0.5, lastTileZ + 0.5);
//        }
//    }
//
//    public static class MovableBox extends CubeDefinition {
//
//        private final float textureX;
//        private final float textureZ;
//        private float textureWidth;
//        private float textureHeight;
//
//        private final float originX;
//        private final float originY;
//        private final float originZ;
//
//        private final float sizeX;
//        private final float sizeY;
//        private final float sizeZ;
//
///*        private final float rotationX;
//        private final float rotationY;
//        private final float rotationZ;
//
//        private final float rotationOriginX;
//        private final float rotationOriginY;
//        private final float rotationOriginZ;*/
//
//        private float offsetX;
//        private float offsetY;
//        private float offsetZ;
//
////        private MovableBox(LODModelPart model, float pX, float pY, float pZ, int sX, int sY, int sZ) {
////            this(model, model.textureX, model.textureZ, model.textureWidth, model.textureHeight, pX, pY, pZ, sX, sY, sZ);
////        }
//
//        protected MovableBox(LODModelPart model, @Nullable String pComment, float pTexCoordU, float pTexCoordV, float pOriginX, float pOriginY, float pOriginZ, float pDimensionX, float pDimensionY, float pDimensionZ, CubeDeformation pGrow, boolean pMirror, float pTexScaleU, float pTexScaleV) {
//            super(pComment, pTexCoordU, pTexCoordV, pOriginX, pOriginY, pOriginZ, pDimensionX, pDimensionY, pDimensionZ, pGrow, pMirror, pTexScaleU, pTexScaleV);
//            if (!allowCompiling) {
//                throw new MisuseException("You cannot have dynamic model boxes unless model compiling is enabled (jvm arg '" + JVM_FLAG + "')!");
//            }
//
//            textureX = pTexCoordU;
//            textureZ = pTexCoordV;
//            textureWidth = pTexScaleU;
//            textureHeight = pTexScaleV;
//            originX = pOriginX;
//            originY = pOriginY;
//            originZ = pOriginZ;
//            sizeX = pDimensionX;
//            sizeY = pDimensionY;
//            sizeZ = pDimensionZ;
//
//           /* rotationX = model.rotateAngleX;
//            rotationY = model.rotateAngleY;
//            rotationZ = model.rotateAngleZ;
//
//            rotationOriginX = model.rotationPointX;
//            rotationOriginY = model.rotationPointY;
//            rotationOriginZ = model.rotationPointZ;*/
//
////            offsetX = model.offsetX;
////            offsetY = model.offsetY;
////            offsetZ = model.offsetZ;
//        }
//
//        private MovableBox move(LODModelPart p, float x, float y, float z) {
//            return new MovableBox(p,null, textureX, textureZ, originX + x, originY + y, originZ + z, sizeX, sizeY, sizeZ, null,false, textureWidth, textureHeight);
//        }
//
//        @Override
//        public String toString() {
//            return originX + ", " + originY + ", " + originZ + " > " + sizeX + "x" + sizeY + "x" + sizeZ + " tex " + textureX + "," + textureZ + " > " + textureWidth + "x" + textureHeight;
//        }
//
//        @Override
//        public ModelPart.Cube bake(int pTexWidth, int pTexHeight) {
//            PoseStack stack = new PoseStack();
//            int f5 = 0;
//            //GL11.glPushMatrix();
//            //GL11.glTranslated(offsetX*f5, offsetY*f5, offsetZ*f5);
//            //v5.addTranslation(offsetX*f5, offsetY*f5, offsetZ*f5);
//            //ReikaJavaLibrary.pConsole(this+" > "+rotationX+" / "+rotationY+" / "+rotationZ);
//            /*if (rotationX != 0 || rotationY != 0 || rotationZ != 0) {
//                stack.pushPose();
//                //ReikaJavaLibrary.pConsole(this+" > "+rotationX+" / "+rotationY+" / "+rotationZ);
//                stack.translate(rotationOriginX * f5, rotationOriginY * f5, rotationOriginZ * f5);
//
//                if (rotationZ != 0)
//                    stack.mulPose(new Quaternion(rotationZ * (180F / (float) Math.PI), 0F, 0F, 1F));
//                if (rotationY != 0)
//                    stack.mulPose(new Quaternion(rotationY * (180F / (float) Math.PI), 0F, 1F, 0F));
//                if (rotationX != 0)
//                    stack.mulPose(new Quaternion(rotationX * (180F / (float) Math.PI), 1F, 0F, 0F));
//
//                stack.translate(-rotationOriginX * f5, -rotationOriginY * f5, -rotationOriginZ * f5);*/
//                stack.popPose();
//                return super.bake(pTexWidth, pTexHeight);
//
//            /*} else {
//                return super.bake(pTexWidth, pTexHeight);
//            }*/
//            //GL11.glPopMatrix();
//            //v5.addTranslation(-offsetX*f5, -offsetY*f5, -offsetZ*f5);
//        }
//
//
//    }
//}
