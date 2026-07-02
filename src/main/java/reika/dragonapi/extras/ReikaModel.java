/*******************************************************************************
* @author Reika Kalseki
*
* Copyright 2017
*
* All rights reserved.
* Distribution of the software in any form is only allowed with
* explicit, prior permission from the owner.
******************************************************************************/
package reika.dragonapi.extras;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import reika.dragonapi.DragonAPI;
import net.minecraft.resources.Identifier;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import com.mojang.blaze3d.systems.RenderSystem;

public class ReikaModel extends ModifiedPlayerModel {

//	private final boolean tailMod = Loader.isModLoaded("Tails");

	public final ModelPart hornL;
	public final ModelPart hornR;
	public final ModelPart wingL;
	public final ModelPart wingR;
	public final ModelPart tail;
	public final ModelPart tail2;
	public final ModelPart tail3;
	public final ModelPart back;
	public final ModelPart back2;
	public final ModelPart back3;

	private static final float HORN_Y = -9F;
	private static final float HORN_X = -1F;
	private static final float WING_ANGLE = 0.7853982F;
	private static final float HORN_Z = -3F;

	public ReikaModel(ModelPart root) {
		super(root);
		this.hornL = root.getChild("hornL");
		this.hornR = root.getChild("hornR");
		this.wingL = root.getChild("wingL");
		this.wingR = root.getChild("wingR");
		this.tail = root.getChild("tail");
		this.tail2 = root.getChild("tail2");
		this.tail3 = root.getChild("tail3");
		this.back = root.getChild("back");
		this.back2 = root.getChild("back2");
		this.back3 = root.getChild("back3");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		partdefinition.addOrReplaceChild("hornR", CubeListBuilder.create().texOffs(32, 12).mirror().addBox(HORN_X - 2F, HORN_Y, HORN_Z, 2, 1, 3), PartPose.ZERO);
		partdefinition.addOrReplaceChild("hornL", CubeListBuilder.create().texOffs(32, 12).mirror().addBox(HORN_X + 2F, HORN_Y, HORN_Z, 2, 1, 3), PartPose.ZERO);

		partdefinition.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(32, 0).addBox(-1.5F, 10F, 2F, 3, 5, 3), PartPose.rotation(0.0698132F, 0F, 0F));
		partdefinition.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(32, 0).addBox(-1.5F, 15.8F, -3.4F, 3, 5, 3), PartPose.rotation(0.418879F, 0F, 0F));
		partdefinition.addOrReplaceChild("tail3", CubeListBuilder.create().texOffs(32, 0).addBox(-1.5F, 16.6F, -15.5F, 3, 5, 3), PartPose.rotation(1.047198F, 0F, 0F));

		partdefinition.addOrReplaceChild("back", CubeListBuilder.create().texOffs(32, 8).addBox(-0.5F, 7F, 2F, 1, 2, 1), PartPose.ZERO);
		partdefinition.addOrReplaceChild("back2", CubeListBuilder.create().texOffs(32, 8).addBox(-0.5F, 1F, 2F, 1, 2, 1), PartPose.ZERO);
		partdefinition.addOrReplaceChild("back3", CubeListBuilder.create().texOffs(32, 8).addBox(-0.5F, 4F, 2F, 1, 2, 1), PartPose.ZERO);

		partdefinition.addOrReplaceChild("wingL", CubeListBuilder.create().texOffs(44, 0).addBox(0F, 1F, 3F, 1, 12, 7), PartPose.rotation(0F, WING_ANGLE, 0F));
		partdefinition.addOrReplaceChild("wingR", CubeListBuilder.create().texOffs(44, 0).addBox(-1F, 1F, 3F, 1, 12, 7), PartPose.rotation(0F, -WING_ANGLE, 0F));

		return LayerDefinition.create(meshdefinition, 64, 32);
	}

	@Override
	public void setupAnim(AvatarRenderState state) {
		super.setupAnim(state);
		this.setPartAngles(state);
	}

	@Override
	public Identifier getTexture() {
		return Identifier.fromNamespaceAndPath(DragonAPI.MODID, "textures/reika_tex.png");
	}

	@Override
	public void renderBodyParts(PoseStack stack, VertexConsumer buffer, int packedLight, int packedOverlay, Player ep, float tick) {
		if (ep.equals(Minecraft.getInstance().player) && !ReikaPlayerAPI.isReika(Minecraft.getInstance().player))
			return;
		//this.setPartAngles(ep, tick);

	      stack.pushPose();
	      stack.translate(0, 0, -0.042);

//		if (this.renderTail()) {
//			tail.render(stack, buffer, packedLight, packedOverlay);
//			tail3.render(stack, buffer, packedLight, packedOverlay);
//			tail2.render(stack, buffer, packedLight, packedOverlay);
//		}

		back.render(stack, buffer, packedLight, packedOverlay);
		back2.render(stack, buffer, packedLight, packedOverlay);
		back3.render(stack, buffer, packedLight, packedOverlay);
		/*
		if (ep.isShiftKeyDown()) {
			GL11.glRotated(this.getWingAngle()*180/3.14, 0, 1, 0);
			GL11.glRotated(-20, 0, 0, 1);
		}*/
		wingR.render(stack, buffer, packedLight, packedOverlay);/*
		if (ep.isShiftKeyDown()) {
			GL11.glRotated(20, 0, 0, 1);
			GL11.glRotated(-2*this.getWingAngle()*180/3.14, 0, 1, 0);
			GL11.glRotated(20, 0, 0, 1);
		}*/
		wingL.render(stack, buffer, packedLight, packedOverlay);/*
		if (ep.isShiftKeyDown()) {
			GL11.glRotated(-20, 0, 0, 1);
			GL11.glRotated(this.getWingAngle()*180/3.14, 0, 1, 0);
		}*/

	      stack.popPose();
	      stack.pushPose();

		double d = 0.25;
		if (ep.isCrouching())
			d = 0.3125;
	      stack.translate(0, d, 0);
		hornL.render(stack, buffer, packedLight, packedOverlay);
		hornR.render(stack, buffer, packedLight, packedOverlay);

	      stack.popPose();
	}

//	private boolean renderTail() {
//		return !tailMod;
//	}

	@Override
	protected void setPartAngles(AvatarRenderState ep) {
		float pitch = -ep.xRot;
		float yawHead = -ep.yRot%360;
		float yaw = -ep.bodyRot%360+180;

		pc = pitch*RADIAN;
		yc = yaw*RADIAN;
		yhc = yawHead*RADIAN;

		this.compensateAngles(ep.ageInTicks);

		hornL.xRot = pc;
		//hornR.yRot = yawBody / (180F / (float)Math.PI);
		hornR.xRot = pc;


		hornR.yRot = yhc-yc;
		hornL.yRot = yhc-yc;
		/*
		tail.yRot = yc;
		tail3.yRot = yc;
		tail2.yRot = yc;
		back.yRot = yc;
		back2.yRot = yc;
		back3.yRot = yc;
		wingL.yRot = WING_ANGLE+yc;
		wingR.yRot = -WING_ANGLE+yc;
		 */
		//this.init();
	}

	@Override
	protected void init() {
	}

	@Override
	public void setPositions() {
	}

}

