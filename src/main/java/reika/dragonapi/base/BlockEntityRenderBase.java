package reika.dragonapi.base;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.debug.StructureRenderer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.awt.image.BufferedImage;
import java.util.HashMap;

import static reika.dragonapi.DragonAPI.MODID;

public abstract class BlockEntityRenderBase<TE extends BlockEntity> implements BlockEntityRenderer<TE> {

    protected final Direction[] dirs = Direction.values();

    private final HashMap<String, String> textureOverrides = new HashMap<>();

    public final boolean isValidMachineRenderPass(BlockEntityBase te) {
//     todo   if (!te.isInWorld() || StructureRenderer.isRenderingTiles())
//            return true;

//        if (!ModLockController.instance.verify(this.getOwnerMod()))
//            return false;

		int b = 0;
		for (int i = 0; i < 6; i++) {
			Direction dir = dirs[i];
			int c = te.getLevel().getMaxLocalRawBrightness(new BlockPos(te.getBlockPos().getX()+dir.getStepX(), te.getBlockPos().getY()+dir.getStepY(), te.getBlockPos().getZ()+dir.getStepZ()), 0);
			b = Math.max(c, b);
		}
		if (te.hasLevel()) {
			int j = b % 65536;
			int k = b / 65536;
//			OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, j / 1.0F, k / 1.0F); todo old opengl code lightmaptexturecoords
		}
//todo        int pass = MinecraftForgeClient.getRenderPass();
        return true;//(te.shouldRenderInPass(pass));
    }

    protected abstract boolean doRenderModel(PoseStack stack, BlockEntityBase te);

/*    public final int bindTextureByName(String tex) {
        String over = textureOverrides.get(tex);
        if (over != null) {
            return Minecraft.getInstance().textureManager.getTexture(new ResourceLocation(this.getModID(), over)).getId();
        }
        if (this.loadXmasTextures()) {
            String xmas = tex.replace(".png", "").replace("_xmas", "")+"_xmas.png";
//            BufferedImage ret = ReikaImageLoader.readImage(this.getModID(), xmas, null);
//            String bind = ret != null && ret != MissingTextureAtlasSprite.getTexture() ? xmas : tex;
            textureOverrides.put(tex, xmas);
            return this.bindTextureByName(xmas);
        }
        return Minecraft.getInstance().textureManager.getTexture(new ResourceLocation(this.getModID(), tex)).getId();
    }*/

    protected boolean loadXmasTextures() {
        return false;
    }

    protected abstract DragonAPIMod getOwnerMod();
    protected abstract Class<?> getModClass();
    protected abstract String getModID();

    protected final Font getFontRenderer() {
        return /*todo what were these for? this.func_147498_b() != null ? this.func_147498_b() :*/ Minecraft.getInstance().font;
    }

}
