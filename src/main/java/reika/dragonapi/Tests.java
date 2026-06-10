package reika.dragonapi;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import reika.dragonapi.auxiliary.PopupWriter;
import reika.dragonapi.auxiliary.trackers.EventProfiler;
import reika.dragonapi.instantiable.data.immutable.DecimalPosition;
import reika.dragonapi.instantiable.effects.StringParticleFX;
import reika.dragonapi.libraries.io.ReikaChatHelper;

@EventBusSubscriber(modid = "DragonAPI")
public class Tests {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, DragonAPI.MODID);

    // 1.21.5: Item.Properties needs setId() before Item.<init> dereferences it via effectiveDescriptionId().
    public static final DeferredHolder<Item, Item> TEST_ITEM = ITEMS.register("test_item",
            rl -> new ItemTest(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, rl))));
    public static void runTests() {

    }

    public static class ItemTest extends Item {

        public ItemTest(Properties p_41383_) {
            super(p_41383_);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!player.isShiftKeyDown()) {
                PopupWriter.instance().addMessage("Popup test with a super duper extra fancy long message, to test spacing!");
            } else if (player.isShiftKeyDown()) {
                PopupWriter.list.remove(0);
            }

            return InteractionResult.PASS;
        }
    }

}

