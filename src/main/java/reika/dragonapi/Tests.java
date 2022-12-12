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
    public static final RegistryObject<Item> RENDER_TESTER = ITEMS.register("render_tester", () -> new RenderTester(new Item.Properties()));
    public static final RegistryObject<Item> ASSET_DOWNLOAD = ITEMS.register("asset_download", () -> new VectorHelperTester(new Item.Properties()));
    public static final RegistryObject<Item> VECTORHELPERTESTER = ITEMS.register("vector_helper_tester", () -> new VectorHelperTester(new Item.Properties()));
    public static final RegistryObject<Item> PARTICLE_TESTER = ITEMS.register("particle_tester", () -> new ParticleTester(new Item.Properties()));


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

    public static class RenderTester extends Item {

        public RenderTester(Properties p_41383_) {
            super(p_41383_);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            DragonAPI.LOGGER.info(EventProfiler.getProfilingData());
            return InteractionResultHolder.pass(this.getDefaultInstance());
        }
    }

    public static class AssetDownload extends Item {

        public AssetDownload(Properties p_41383_) {
            super(p_41383_);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            MusicLoader.instance.registerAssets();
            return InteractionResultHolder.pass(this.getDefaultInstance());
        }
    }

    public static class VectorHelperTester extends Item {

        public VectorHelperTester(Properties p_41383_) {
            super(p_41383_);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            for (float i = 0; i <= 5; i += 0.2) {
                DecimalPosition xyz = getPlayerLookCoords(player, i);
                Block id = xyz.getBlock(level);
                ReikaChatHelper.write(id.getName());
            }
            return InteractionResultHolder.success(this.getDefaultInstance());
        }

        public static DecimalPosition getPlayerLookCoords(Player ep, double distance) {
            Vec3 look = ep.getLookAngle();
            double dx = ep.getX();
            double dy = ep.getY() + ep.getEyeHeight();
            double dz = ep.getZ();
            double lx = look.x();
            double lz = look.z();
            double ly = look.y();
            lx *= distance;
            ly *= distance;
            lz *= distance;

            return new DecimalPosition(dx + lx, dy + ly, dz + lz); //todo check if this actually works, hope so xoxo
        }
    }

    public static class ParticleTester extends Item {

        public ParticleTester(Properties p_41383_) {
            super(p_41383_);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            if (!player.isShiftKeyDown()) {
                Minecraft.getInstance().particleEngine.add(new StringParticleFX(Minecraft.getInstance().level, player.getX(), player.getY(), player.getZ(), "Hello World!", 0, 0, 0));
            } else if (player.isShiftKeyDown()) {

            }
            return InteractionResultHolder.success(this.getDefaultInstance());
        }
    }
}
