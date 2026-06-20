package reika.dragonapi.auxiliary;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.DragonOptions;
import reika.dragonapi.libraries.ReikaNBTHelper;
import reika.dragonapi.libraries.ReikaPlayerAPI;

import java.util.ArrayList;

import static reika.dragonapi.DragonAPI.MODID;

@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public class DebugOverlay {

    @SubscribeEvent
    public static void drawDebugOverlay(RenderGuiLayerEvent.Post event) {
        if (event.getName().equals(VanillaGuiLayers.TITLE)) {
            var stack = new PoseStack();
            var mc = Minecraft.getInstance();
            if (DragonAPI.debugtest) {
                var f = mc.font;
                float d = 3;
                stack.scale(d, d, d);
                var s = "Debug Mode Enabled!";
                event.getGuiGraphics().text(f, s, 1, 1, 0xffffff);
                stack.scale(1/d, 1/d, 1/d);
                // TODO 1.21+: setShaderTexture now expects a GpuTexture. Use GuiGraphicsExtractor.blit with resource binding instead where needed. Previous code was using setShaderTexture(0, Identifier.parse("textures/gui/icons.png"))
            }

            if (DragonOptions.TABNBT.getState() && InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), DragonOptions.DEBUGKEY.getValue())) {
                //if (APIProxyClient.key_nbt.isPressed()) {
                var ep = mc.player;
                var f = mc.font;
                if (mc.gui.screen() == null) {
                    float reach = 4;
                    var hit = ReikaPlayerAPI.getLookedAtBlockClient(4, false);
                    if (hit != null) {
                        Block b = ep.level().getBlockState(hit.getBlockPos()).getBlock();
//                        if (b.hasBlockEntity(ep.level().getBlockMetadata(hit.getBlockPos()))) {
                            BlockEntity te = ep.level().getBlockEntity(hit.getBlockPos());
                            if (te != null) {
                                CompoundTag NBT = new CompoundTag();
                                ArrayList<String> li = new ArrayList<>();
                                try {
                                    // Note: This is debug code - using saveWithoutMetadata for inspection
                                    te.saveWithoutMetadata(ep.level().registryAccess());
                                    li.addAll(ReikaNBTHelper.parseNBTAsLines(NBT));
                                }
                                catch (Exception e) {
                                    StackTraceElement[] el = e.getStackTrace();
                                    li.add(ChatFormatting.RED.toString()+e.getClass()+": "+e.getLocalizedMessage());
                                    for (int i = 0; i < 4; i++) {
                                        li.add(el[i].toString());
                                    }
                                }
                                for (int i = 0; i < li.size(); i++) {
                                    String s = li.get(i);
                                    int windowWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
                                    event.getGuiGraphics().text(f, s, 1+windowWidth/2*(i/24), 1+f.lineHeight*(i%24), 0xffffff);
                                    // see above note on texture binding API
                                }
                            }
                        }
//                    }
                }
            }
        }
    }
}



