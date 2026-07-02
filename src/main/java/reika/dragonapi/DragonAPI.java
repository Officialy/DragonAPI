package reika.dragonapi;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import static net.neoforged.fml.loading.FMLEnvironment.isProduction;
import net.neoforged.neoforge.client.ClientCommandHandler;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

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
import reika.dragonapi.instantiable.rendering.ReikaRenderDispatcher;
import reika.dragonapi.libraries.io.ReikaPacketHelper;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;
import reika.dragonapi.libraries.registry.ReikaDyeHelper;
//import reika.dragonapi.modregistry.ModOreList;
//import reika.dragonapi.modregistry.PowerTypes;
import reika.dragonapi.trackers.PatreonController;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;
import java.util.UUID;

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
    public static final UUID Officialy_UUID = UUID.fromString("bb5029b7-9381-4d99-aaa9-106da41aa659");
    public static final UUID Dev_UUID = UUID.fromString("380df991-f603-344c-a090-369bad2a924a");

    //public static final ControlledConfig config = new ControlledConfig(instance, DragonOptions.optionList, null);
    private static final long launchTime = ManagementFactory.getRuntimeMXBean().getStartTime();
    private static final GameProfile sessionUser = serverProfile;
    public static boolean debugtest = false;
    public static final String packetChannel = "DragonAPIData";

    public DragonAPI(final IEventBus modEventBus) {
        this.startTiming(DragonAPIMod.LoadProfiler.LoadPhase.PRELOAD);
        LOGGER.warn("****************************************");
        LOGGER.warn("DragonAPI Loading");
        LOGGER.warn("****************************************");
        NeoForge.EVENT_BUS.register(this);
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
        return FMLEnvironment.getDist();
    }

    private static GameProfile loadSessionProfile() {
        return Minecraft.getInstance().getGameProfile();
    }

    public static boolean isOnActualServer() {
        return getSide() == Dist.DEDICATED_SERVER;
    }

    public static boolean isSinglePlayerFromClient() {
        return Minecraft.getInstance().isLocalServer();
    }

    public static boolean isSinglePlayer() {
//        DragonAPI.LOGGER.info("The side is" + getSide() + "Am I a dedicated server?" + FMLEnvironment.getDist().isDedicatedServer());
        return false;//getSide() == Dist.DEDICATED_SERVER && !FMLEnvironment.getDist().isDedicatedServer();
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
        Minecraft.getInstance().gui.setScreen(new ConfirmLinkScreen((p_170143_) -> {
            if (p_170143_) {
                Util.getPlatform().openUri(url);
            }
            Minecraft.getInstance().gui.setScreen(null);
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

        // instance.loadHandlers(); // Method removed - handlers are loaded automatically
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
        // ReikaRenderDispatcher.init();
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
        ReikaDyeHelper.buildItemCache();
        if (ModList.FORESTRY.isLoaded()) {
            //ReikaBeeHelper.buildSpeciesList();
        }
    }

    public String getDisplayName() {
        return "DragonAPI";
    }

}


