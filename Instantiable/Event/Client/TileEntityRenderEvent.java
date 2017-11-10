/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.DragonAPI.Instantiable.Event.Client;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@Cancelable
@SideOnly(Side.CLIENT)
public class TileEntityRenderEvent extends Event {

	public final TileEntitySpecialRenderer tesr;
	public final TileEntity tileEntity;
	public final double renderPosX;
	public final double renderPosY;
	public final double renderPosZ;
	public final float partialTickTime;

	public TileEntityRenderEvent(TileEntitySpecialRenderer tesr, TileEntity te, double par2, double par4, double par6, float par8) {
		tileEntity = te;
		renderPosX = par2;
		renderPosY = par4;
		renderPosZ = par6;
		partialTickTime = par8;
		this.tesr = tesr;
	}

	public static void fire(TileEntitySpecialRenderer tesr, TileEntity te, double par2, double par4, double par6, float par8) {
		if (!MinecraftForge.EVENT_BUS.post(new TileEntityRenderEvent(tesr, te, par2, par4, par6, par8))) {
			tesr.renderTileEntityAt(te, par2, par4, par6, par8);
		}
	}

}
