package reika.dragonapi;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import reika.dragonapi.auxiliary.PopupWriter;
import reika.dragonapi.auxiliary.trackers.EventProfiler;
import reika.dragonapi.instantiable.data.immutable.DecimalPosition;
import reika.dragonapi.instantiable.effects.StringParticleFX;
import reika.dragonapi.libraries.io.ReikaChatHelper;

@Mod.EventBusSubscriber(modid = "DragonAPI", bus = Mod.EventBusSubscriber.Bus.MOD)
public class Tests {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, DragonAPI.MODID);

    public static final RegistryObject<Item> TEST_ITEM = ITEMS.register("test_item", () -> new ItemTest(new Item.Properties()));
    public static void runTests() {

    }

    public static class ItemTest extends Item {

        public ItemTest(Properties p_41383_) {
            super(p_41383_);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            if (!player.isShiftKeyDown()) {
                PopupWriter.instance.addMessage("Popup test with a super duper extra fancy long message, to test spacing!");
            } else if (player.isShiftKeyDown()) {
                PopupWriter.instance.list.remove(0);
            }

            return InteractionResultHolder.pass(this.getDefaultInstance());
        }
    }

}
