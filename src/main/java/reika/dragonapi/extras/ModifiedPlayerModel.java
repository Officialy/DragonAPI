///*******************************************************************************
// * @author Reika Kalseki
// *
// * Copyright 2017
// *
// * All rights reserved.
// * Distribution of the software in any form is only allowed with
// * explicit, prior permission from the owner.
// ******************************************************************************/
//package reika.dragonapi.extras;
//
//
//import com.mojang.blaze3d.platform.Window;
//import com.mojang.blaze3d.vertex.PoseStack;
//import net.minecraft.client.Minecraft;
//import net.minecraft.world.entity.player.Player;
//import reika.dragonapi.libraries.java.ReikaJavaLibrary;
//
//public abstract class ModifiedPlayerModel extends ModelBiped {
//
//	public ModifiedPlayerModel() {
//		super();
//		textureWidth = 64;
//		textureHeight = 32;
//
//		this.init();
//	}
//
//	protected float pc;
//	protected float yc;
//	protected float yhc;
//
//	public static final float RADIAN = toRadians();
//
//	private static float toRadians() {
//		return (float)(Math.PI/180);
//	}
//
//	protected abstract void setPositions();
//
//	protected abstract void init();
//
//	protected abstract void setPartAngles(Player ep, float tick);
//
//	public abstract void bindTexture();
//
//	public abstract void renderBodyParts(PoseStack stack, Player ep, float tick);
//
//	protected final void setRotation(ModelRenderer model, float x, float y, float z) {
//		model.rotateAngleX = x;
//		model.rotateAngleY = y;
//		model.rotateAngleZ = z;
//	}
//
//	protected final void compensateAngles(float tick) {
//		ReikaJavaLibrary.pConsole(yc/RADIAN);
//		if (tick == 1.0F) {
//
//			int ySize = 136;
//			int xSize = 195;
//
//			Minecraft mc = Minecraft.getInstance();
//			Window scr = Minecraft.getInstance().getWindow();
//
//			int width = scr.getGuiScaledWidth();
//			int height = scr.getGuiScaledHeight();
//
//			int guiLeft = (width - xSize) / 2;
//			int guiTop = (height - ySize) / 2;
//
//			float par1 = 150f /*todo Mouse.getX()*/ * width / mc.getWindow().getWidth();
//			float par2 = height - 150f /*todo Mouse.getY()*/ * height / mc.getWindow().getHeight() - 1;
//
//			float par3 = guiLeft + 43 - par1;
//			float par4 = guiTop + 45 - 30 - par2;
//
//			yc = (float)Math.atan(par3 / 40.0F) * 20.0F;
//			yhc = (float)Math.atan(par3 / 40.0F) * 40.0F;
//			pc = -((float)Math.atan(par4 / 40.0F)) * 20.0F;
//
//			yhc = -yhc;
//			yc = -yc;
//
//			yhc += 180;
//			yc += 180;
//			//pc = -90;
//
//			yhc *= RADIAN;
//			yc *= RADIAN;
//
//			pc *= RADIAN;
//		}
//	}
//}