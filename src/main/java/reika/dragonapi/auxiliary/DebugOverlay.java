package reika.dragonapi.auxiliary;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.DragonOptions;
import reika.dragonapi.libraries.ReikaNBTHelper;
import reika.dragonapi.libraries.ReikaPlayerAPI;

import java.util.ArrayList;

import static reika.dragonapi.DragonAPI.MODID;

@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DebugOverlay {

    @SubscribeEvent
    public static void drawDebugOverlay(RenderGuiOverlayEvent event) {
        if (event.getOverlay() == VanillaGuiOverlay.TITLE_TEXT.type()) {
            var stack = new PoseStack();
            var mc = Minecraft.getInstance();
            if (DragonAPI.debugtest) {
                var f = mc.font;
                float d = 3;
                stack.scale(d, d, d);
                var s = "Debug Mode Enabled!";
                f.draw(stack, s, 1, 1, 0xffffff);
                stack.scale(1/d, 1/d, 1/d);
                RenderSystem.setShaderTexture(0, new ResourceLocation("textures/gui/icons.png"));
            }

            if (DragonOptions.TABNBT.getState() && InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), DragonOptions.DEBUGKEY.getValue())) {
                //if (APIProxyClient.key_nbt.isPressed()) {
                var ep = mc.player;
                var f = mc.font;
                if (mc.screen == null) {
                    float reach = 4;
                    var hit = ReikaPlayerAPI.getLookedAtBlockClient(4, false);
                    if (hit != null) {
                        Block b = ep.level.getBlockState(hit.getBlockPos()).getBlock();
//                        if (b.hasBlockEntity(ep.level.getBlockMetadata(hit.getBlockPos()))) {
                            BlockEntity te = ep.level.getBlockEntity(hit.getBlockPos());
                            if (te != null) {
                                CompoundTag NBT = new CompoundTag();
                                ArrayList<String> li = new ArrayList<>();
                                try {
                                    te.load(NBT); //todo could break things, why would blank NBT be added?
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
                                    f.draw(stack, s, 1+event.getWindow().getGuiScaledWidth()/2*(i/24), 1+f.lineHeight*(i%24), 0xffffff);
                                    RenderSystem.setShaderTexture(0, new ResourceLocation("textures/gui/icons.png"));
                                }
                            }
                        }
//                    }
                }
            }
        }
    }
}
