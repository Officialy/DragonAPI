package reika.dragonapi;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reika.dragonapi.auxiliary.trackers.CommandableUpdateChecker;
import reika.dragonapi.auxiliary.trackers.PlayerChunkTracker;
import reika.dragonapi.auxiliary.trackers.RemoteAssetLoader;
import reika.dragonapi.auxiliary.trackers.TickRegistry;
import reika.dragonapi.base.DragonAPIMod;
import reika.dragonapi.command.*;
import reika.dragonapi.exception.RegistrationException;
import reika.dragonapi.instantiable.effects.ReikaParticleTypes;
import reika.dragonapi.instantiable.io.ControlledConfig;
import reika.dragonapi.libraries.io.ReikaPacketHelper;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;
import reika.dragonapi.libraries.registry.ReikaDyeHelper;
import reika.dragonapi.modregistry.ModOreList;
import reika.dragonapi.modregistry.PowerTypes;
import reika.dragonapi.trackers.PatreonController;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;
import java.util.UUID;

import static net.minecraftforge.fml.loading.FMLLoader.isProduction;

@Mod.EventBusSubscriber(modid = DragonAPI.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
@Mod(DragonAPI.MODID)
public class DragonAPI extends DragonAPIMod {
    public static final String last_API_Version = "1" + "0";
    public static final String MODID = "dragonapi";
    public static final Logger LOGGER = LogManager.getLogger(DragonAPI.MODID);
    public static final String NAME = "DragonAPI";
    public static final Random rand = new Random();
    public static DragonAPI instance;

    public static ControlledConfig config;
    public static final GameProfile serverProfile = new GameProfile(UUID.fromString("b9a1b954-6651-4bb8-af54-452a4d9fd5a4"), "[SERVER]");
    public static final String FORUM_PAGE = "http://www.minecraftforum.net/topic/1969694-";
    public static final String GITHUB_PAGE = "https://github.com/ReikaKalseki/Reika_Mods_Issues/issues?q=";
    public static final UUID Reika_UUID = UUID.fromString("e5248026-6874-4954-9a02-aa8910d08f31");
    //public static final ControlledConfig config = new ControlledConfig(instance, DragonOptions.optionList, null);
    private static final long launchTime = ManagementFactory.getRuntimeMXBean().getStartTime();
    private static final GameProfile sessionUser = serverProfile;
    public static boolean debugtest = false;
    public static final String packetChannel = "DragonAPIData";

    public DragonAPI() {
        this.startTiming(DragonAPIMod.LoadProfiler.LoadPhase.PRELOAD);
        LOGGER.warn("****************************************");
        LOGGER.warn("DragonAPI Loading");
        LOGGER.warn("****************************************");
        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        MinecraftForge.EVENT_BUS.register(this);
        instance = this;

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
//        modEventBus.addListener(ReikaParticleTypes::registerParticleFactories);
//        modEventBus.addListener(this::serverStarting);
//        modEventBus.addListener(this::serverStarted);

//        DragonOptions.register(ModLoadingContext.get());

        Tests.ITEMS.register(modEventBus);
        ReikaParticleTypes.REGISTRY.register(modEventBus);

        this.finishTiming();
    }

//    todo update overlays to new system
//     @SubscribeEvent
//     public static void registerOverlays(RegisterGuiOverlaysEvent event) {
//        event.registerAboveAll("DebugOverlay", DebugOverlay.getOverlay());
//    }

    @Override
    public String getModId() {
        return MODID;
    }

    public static File getMinecraftDirectory() {
        return Minecraft.getInstance().gameDirectory;
    }

    protected static Dist getSide() {
        return FMLLoader.getDist();
    }

    private static GameProfile loadSessionProfile() {
        return Minecraft.getInstance().getUser().getGameProfile();
    }

    public static boolean isOnActualServer() {
        return getSide() == Dist.DEDICATED_SERVER && FMLLoader.getDist().isDedicatedServer();
    }

    public static boolean isSinglePlayerFromClient() {
        return Minecraft.getInstance().isLocalServer();
    }

    public static boolean isSinglePlayer() {
//        DragonAPI.LOGGER.info("The side is" + getSide() + "Am I a dedicated server?" + FMLLoader.getDist().isDedicatedServer());
        return false;//getSide() == Dist.DEDICATED_SERVER && !FMLLoader.getDist().isDedicatedServer();
    }

    public static long getLaunchTime() {
        return launchTime;
    }

    public static GameProfile getLaunchingPlayer() {
        return sessionUser;
    }

    public static int getSystemTimeAsInt() {
        long t = System.currentTimeMillis();
        return (int) (t % (Integer.MAX_VALUE + 1));
    }

    public static void openURL(String url) {
        Minecraft.getInstance().setScreen(new ConfirmLinkScreen((p_170143_) -> {
            if (p_170143_) {
                Util.getPlatform().openUri(url);
            }
            Minecraft.getInstance().setScreen(null);
        }, url, true));
    }

    public static void debugPrint(Object o) {
        ReikaJavaLibrary.pConsole(o);
        if (!isProduction())
            Thread.dumpStack();
    }

    public void commonSetup(final FMLCommonSetupEvent evt) {
        instance.startTiming(LoadProfiler.LoadPhase.LOAD);

        config = new ControlledConfig(this, DragonOptions.optionList, null);
        config.loadSubfolderedConfigFile();
        config.initProps();

        instance.loadHandlers();
        Tests.runTests();

        TickRegistry.instance.registerTickHandler(PlayerChunkTracker.instance);
        ReikaPacketHelper.registerPacketHandler(instance, packetChannel, new APIPacketHandler());

        PatreonController.instance.registerMod("Reika", PatreonController.reikaURL);

        LOGGER.info("DRAGONAPI: Credit to Techjar for hosting the version file and remote asset server.");

        instance.finishTiming();
    }

    public void clientSetup(final FMLClientSetupEvent evt) {
        RemoteAssetLoader.instance.checkAndStartDownloads();
        CommandDispatcher<CommandSourceStack> commandDispatcher = ClientCommandHandler.getDispatcher();

//            ClientCommandHandler.instance.registerCommand(new ToggleBlockChangePacketCommand());
//            ClientCommandHandler.instance.registerCommand(new GetLatencyCommand());
//            ClientCommandHandler.instance.registerCommand(new ClearParticlesCommand());
//            ClientCommandHandler.instance.registerCommand(new ExportEnvironmentCommand());
    }

    @SubscribeEvent
    public void onRegisterCommandEvent(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> commandDispatcher = event.getDispatcher();
        DonatorCommand.register(commandDispatcher);
        GetUUIDCommand.register(commandDispatcher);
        GuideCommand.register(commandDispatcher);
        EntityListCommand.register(commandDispatcher);
        TestControlCommand.register(commandDispatcher);
        BiomeMapCommand.register(commandDispatcher);
        EventProfilerCommand.register(commandDispatcher);
    }

    public String getModAuthorName() {
        return "Reika";
    }

    @Override
    public Logger getModLogger() {
        return LOGGER;
    }

    @Override
    public URL getDocumentationSite() {
        return DragonAPI.getReikaForumPage();
    }

    @Override
    public URL getBugSite() {
        try {
            return new URL(GITHUB_PAGE);
        }
        catch (MalformedURLException e) {
            throw new RegistrationException(DragonAPI.instance, "Reika's mods provided a malformed URL for their github site!", e);
        }
    }

    @Override
    public File getConfigFolder() {
        return config.getConfigFolder();
    }

    @Override
    public String getUpdateCheckURL() {
        return CommandableUpdateChecker.reikaURL;
    }

    public static URL getReikaGithubPage() {
        try {
            return new URL(GITHUB_PAGE);
        } catch (MalformedURLException e) {
            throw new RegistrationException(DragonAPI.instance, "Reika's mods provided a malformed URL for their github site!", e);
        }
    }

    public static URL getReikaForumPage() {
        try {
            return new URL(FORUM_PAGE);
        } catch (MalformedURLException e) {
            throw new RegistrationException(instance, "Reika's mods provided a malformed URL for their documentation site!", e);
        }
    }

    public void serverStarting(ServerAboutToStartEvent evt) {
        ModOreList.initializeAll();
        ReikaDyeHelper.buildItemCache();
        if (ModList.FORESTRY.isLoaded()) {
            //ReikaBeeHelper.buildSpeciesList();
        }
    }

    public void serverStarted(ServerStartingEvent evt) {
        LOGGER.info("Server Started.");
        //DragonAPI.LOGGER.info("Total Crafting Recipes: "+CraftingManager.getInstance().getRecipeList().size());
        //DragonAPI.LOGGER.info("Dimensions Present: "+ Arrays.toString(DimensionManager.getStaticDimensionIDs()));
        DragonAPI.LOGGER.info("Mods Present: " + FMLLoader.modLauncherModList().size());

        //if (MinecraftServer.getServer() != null)
        //   DragonAPI.LOGGER.info("Commands Loaded: "+ ReikaCommandHelper.getCommandList().size());
    }

    private void loadHandlers() {
        ReikaJavaLibrary.initClass(PowerTypes.class);
    }

    public String getDisplayName() {
        return "DragonAPI";
    }

}