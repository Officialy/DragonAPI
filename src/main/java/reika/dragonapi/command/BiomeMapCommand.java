package reika.dragonapi.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.registries.BuiltInRegistries;
import reika.dragonapi.APIPacketHandler;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.instantiable.data.maps.MultiMap;
import reika.dragonapi.instantiable.io.MapOutput;
import reika.dragonapi.interfaces.CustomBiomeDistributionWorld;
import reika.dragonapi.interfaces.CustomMapColorBiome;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.io.ReikaChatHelper;
import reika.dragonapi.libraries.io.ReikaPacketHelper;
import reika.dragonapi.libraries.java.ReikaObfuscationHelper;
import reika.dragonapi.libraries.level.ReikaBiomeHelper;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;


public class BiomeMapCommand {

    public static final int PACKET_COMPILE = 512; //packet size in bytes = 4*(1+n*3)

    private static BiomeMapCommand instance;

    private static final Random rand = new Random();
    private final static HashMap<Integer, BiomeMap> activeMaps = new HashMap<>();
    private static final Map<ResourceKey<Biome>, CustomMapColorBiome> customColors = new HashMap<>();

    /** Biomes are final in 26.3; register custom coloring by registry key instead of subclassing. */
    public static void registerBiomeColor(ResourceKey<Biome> key, CustomMapColorBiome color) {
        customColors.put(Objects.requireNonNull(key), Objects.requireNonNull(color));
    }

    public BiomeMapCommand() {
        instance = this;
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("biomepng").requires(Commands.hasPermission(Commands.LEVEL_ADMINS));
        root.then(mapArguments(false));
        root.then(Commands.literal("seed").then(Commands.argument("seeds", StringArgumentType.string()).then(mapArguments(true))));
        root.then(Commands.argument("seeds", StringArgumentType.string()).then(mapArguments(true)));
        root.then(Commands.literal("player").then(Commands.argument("player", EntityArgument.player())
                .then(mapArguments(false))
                .then(Commands.argument("seeds", StringArgumentType.string()).then(mapArguments(true)))));
        dispatcher.register(root);
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, Integer> mapArguments(boolean seeds) {
        return Commands.argument("range", IntegerArgumentType.integer(1, 100000))
                .then(Commands.argument("resolution", IntegerArgumentType.integer(1, 100000))
                        .executes(context -> runMap(context, seeds, false, false))
                        .then(Commands.argument("grid", IntegerArgumentType.integer(0))
                                .executes(context -> runMap(context, seeds, true, false))
                                .then(Commands.argument("fullgrid", BoolArgumentType.bool())
                                        .executes(context -> runMap(context, seeds, true, true)))));
    }

    private static int runMap(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
                              boolean seeds, boolean grid, boolean fullGrid) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            source = source.withEntity(player);
        } catch (IllegalArgumentException absent) { /* Player command branch was not used. */ }
        ArrayList<String> args = new ArrayList<>();
        if (seeds) {
            String value = StringArgumentType.getString(context, "seeds");
            args.add(value.startsWith("seed=") ? value : "seed=" + value);
        }
        args.add(Integer.toString(IntegerArgumentType.getInteger(context, "range")));
        args.add(Integer.toString(IntegerArgumentType.getInteger(context, "resolution")));
        if (grid) args.add(Integer.toString(IntegerArgumentType.getInteger(context, "grid")));
        if (fullGrid) args.add(Boolean.toString(BoolArgumentType.getBool(context, "fullgrid")));
        return processCommand(source, args.toArray(String[]::new));
    }

    public static int processCommand(CommandSourceStack source, String[] args) throws CommandSyntaxException {
        Object[] target = getPlayer(source, args);
        ServerPlayer player = (ServerPlayer)target[0];
        int index = (boolean)target[1] ? 1 : 0;
        Collection<BiomeProvider> providers = new ArrayList<>();
        try {
            if (index < args.length && args[index].startsWith("seed=")) {
                for (long seed : parseSeeds(args[index++].substring(5))) providers.add(new SeedBiomes(player.level(), seed));
            }
            if (args.length - index < 2 || args.length - index > 4)
                throw new IllegalArgumentException("Use [seed=<seed,list,range>] <range> <resolution> [grid] [fullgrid].");
            int range = Integer.parseInt(args[index++]);
            int resolution = Integer.parseInt(args[index++]);
            validateMapSize(range, resolution);
            int grid = index < args.length ? Integer.parseInt(args[index++]) : 0;
            if (grid < 0) throw new IllegalArgumentException("Grid must be non-negative.");
            boolean fullGrid = index < args.length && Boolean.parseBoolean(args[index]);
            if (providers.isEmpty()) providers.add(new WorldBiomes(player.level()));
            for (BiomeProvider provider : providers)
                generateMap(provider, player, System.currentTimeMillis(), Mth.floor(player.getX()), Mth.floor(player.getZ()), range, resolution, grid, fullGrid, null);
            return providers.size();
        } catch (IllegalArgumentException error) {
            source.sendFailure(Component.literal(error.getMessage()));
            return 0;
        }
    }

    public static List<Long> parseSeeds(String input) {
        List<Long> seeds = new ArrayList<>();
        for (String token : input.split(",", -1)) {
            var range = java.util.regex.Pattern.compile("(-?[0-9]+)-(-?[0-9]+)").matcher(token);
            if (range.matches()) {
                long first = Long.parseLong(range.group(1)), last = Long.parseLong(range.group(2));
                if (last < first || java.math.BigInteger.valueOf(last).subtract(java.math.BigInteger.valueOf(first)).compareTo(java.math.BigInteger.valueOf(15)) > 0)
                    throw new IllegalArgumentException("Seed ranges must be ascending and contain at most 16 seeds.");
                for (long seed = first;; seed++) { seeds.add(seed); if (seed == last) break; }
            } else seeds.add(Long.parseLong(token));
            if (seeds.size() > 16) throw new IllegalArgumentException("At most 16 seed maps may be requested at once.");
        }
        return List.copyOf(seeds);
    }

    public static void validateMapSize(int range, int resolution) {
        if (range < 1 || range > 100000 || resolution < 1 || resolution > 100000)
            throw new IllegalArgumentException("Range and resolution must be between 1 and 100000.");
        long width = 2L * range / resolution + 1;
        if (width * width > 4194304) throw new IllegalArgumentException("Map exceeds 2048 pixels per side; increase resolution.");
    }

    public static void clearClientMaps() { activeMaps.clear(); }

    public static void triggerBiomeMap(ServerPlayer ep, int x, int z, int range, int res, int grid, MapCompleteCallback call) {
        generateMap(new WorldBiomes(ep.level()), ep, System.currentTimeMillis(), x, z, range, res, grid, false, call);
    }

  /*  public static void triggerBiomeMap(ServerPlayer ep, int range, int res, int grid) {
        instance.processCommand(ep, new String[]{String.valueOf(range), String.valueOf(res), String.valueOf(grid)});
    }*/

    private static void generateMap(BiomeProvider bp, ServerPlayer ep, long start, int x, int z, int range, int res, int grid, boolean fullGrid, MapCompleteCallback callback) {
        validateMapSize(range, res);
        int hash = rand.nextInt();

        ResourceKey<Level> dim = ep.level().dimension();
        var biomeRegistry = ep.level().registryAccess().lookupOrThrow(Registries.BIOME);
        // The dimension slot is reserved; the receiver uses its synchronized current dimension.
        ReikaPacketHelper.sendStringIntPacket(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.BIOMEPNGSTART.ordinal(), ep, bp.getName(), hash, 0, x, z, range, res, grid, fullGrid ? 1 : 0);

        ArrayList<Integer> dat = new ArrayList<>();
        dat.add(hash);
        int n = 0;
        for (int dx = x - range; dx <= x + range; dx += res) {
            for (int dz = z - range; dz <= z + range; dz += res) {
                Biome biome = bp.getBiome(dx, dz);
                int biomeId = biomeRegistry.getId(biome);
                if (biomeId < 0) {
                    DragonAPI.LOGGER.warn("Skipping unregistered biome {} at {}, {}", biome, dx, dz);
                    continue;
                }
//                ReikaPacketHelper.sendDataPacket(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.BIOMEPNGDAT.ordinal(), ep, hash, dx, dz, b.biomeID);
                n++;
                dat.add(dx);
                dat.add(dz);
                dat.add(biomeId);
                if (n >= PACKET_COMPILE) {
                    ReikaPacketHelper.sendDataPacket(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.BIOMEPNGDAT.ordinal(), ep, dat);
                    n = 0;
                    dat.clear();
                    dat.add(hash);
                }
            }
        }
        //in case leftover
        if (dat.size() > 1) {
            //pad to fit normal packet size expectation
            int m = (dat.size() - 1) / 3;
            for (int i = m; i < PACKET_COMPILE; i++) {
                dat.add(x - range);
                dat.add(z - range);
                dat.add(biomeRegistry.getId(bp.getBiome(x - range, z - range)));
            }
            ReikaPacketHelper.sendDataPacket(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.BIOMEPNGDAT.ordinal(), ep, dat);
        }
        if (callback != null) {
            callback.onComplete();
        }
        ReikaPacketHelper.sendDataPacket(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.BIOMEPNGEND.ordinal(), ep, hash);
    }

    private static Object[] getPlayer(CommandSourceStack ics, String[] args) throws CommandSyntaxException {
        ServerPlayer executingPlayer = ics.getPlayer();
        if (executingPlayer != null)
            return new Object[]{executingPlayer, false};
        if (args == null || args.length == 0)
            throw EntityArgument.NO_PLAYERS_FOUND.create();
        ServerPlayer target = ics.getServer().getPlayerList().getPlayerByName(args[0]);
        if (target == null) {
            ics.sendFailure(Component.literal("If you specify a player, they must exist."));
            throw EntityArgument.NO_PLAYERS_FOUND.create();
        }
        return new Object[]{target, true};
    }

    public static void startCollecting(int hash, String world, ResourceKey<Level> dim, int x, int z, int range, int res, int grid, boolean fullGrid) {
        validateMapSize(range, res);
        if (activeMaps.size() >= 16) throw new IllegalStateException("Too many unfinished biome maps");
        BiomeMap map = new BiomeMap(world, dim, x, z, range, res, grid, fullGrid);
        activeMaps.put(hash, map);
    }


    public static void addBiomePoint(int hash, int x, int z, int biomeID) {
        BiomeMap map = activeMaps.get(hash);
        if (map != null) {
            map.addPoint(x, z, biomeID);
        }
    }


    public static void finishCollectingAndMakeImage(int hash) {
        BiomeMap map = activeMaps.remove(hash);
        if (map != null) {
            try {
                map.addGrid();
                String path = map.createImage();
                long dur = System.currentTimeMillis() - map.startTime;
                ReikaChatHelper.writeString(ChatFormatting.GREEN + "File created in " + dur + " ms: " + path);
            } catch (IOException e) {
                ReikaChatHelper.writeString(ChatFormatting.RED + "Failed to create file: " + e);
                e.printStackTrace();
            }
        }
    }

    private interface BiomeProvider {

        //public String getFileName(long seed, String name, int x, int z, int range, int res, int grid, boolean fullGrid);
        Biome getBiome(int x, int z);

        String getName();

    }

    private record WorldBiomes(Level world) implements BiomeProvider {

            /*
            @Override
            public String getFileName(long seed, String name, int x, int z, int range, int res, int grid, boolean fullGrid) {
            }*/

            @Override
            public Biome getBiome(int x, int z) {
                if (world instanceof CustomBiomeDistributionWorld) {
                    return ((CustomBiomeDistributionWorld) world).getBiomeID(world, x, z);
                }
                return world.getBiomeManager().getBiome(new BlockPos(x, 100, z)).value();
            }

            @Override
            public String getName() {
                return world.getServer().getWorldData().getLevelName();
            }

        }

    private static final class SeedBiomes implements BiomeProvider {
        private final long seed;
        private final net.minecraft.world.level.biome.BiomeResolver resolver;

        private SeedBiomes(net.minecraft.server.level.ServerLevel world, long seed) {
            this.seed = seed;
            var generator = world.getChunkSource().getGenerator();
            var settings = generator instanceof net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator noise
                    ? noise.generatorSettings().value()
                    : world.registryAccess().lookupOrThrow(Registries.NOISE_SETTINGS)
                            .getOrThrow(net.minecraft.world.level.levelgen.NoiseGeneratorSettings.OVERWORLD).value();
            var random = net.minecraft.world.level.levelgen.RandomState.create(world.registryAccess().lookupOrThrow(Registries.NOISE), seed, settings);
            resolver = generator.getBiomeSource().createCachingResolver(random);
        }

        @Override
        public Biome getBiome(int x, int z) { return resolver.getNoiseBiome(x >> 2, 25, z >> 2).value(); }
        @Override
        public String getName() { return "SEED=" + seed; }
    }

    private static class BiomeMap extends MapOutput<Integer> {

        private BiomeMap(String name, ResourceKey<Level> dim, int x, int z, int r, int res, int grid, boolean fgrid) {
            super(name, dim, x, z, r, res, grid, fgrid);
        }

        @Override
        protected void onImageCreate(File f) throws IOException {
            this.createLegend(f);
        }

        @Override
        protected int getColor(int x, int z, Integer data) {
            // The biome map is a client-side debug render; go through the holder so this class
            // stays loadable where commands are registered.
            var registry = reika.dragonapi.client.ClientEnvironment.level().registryAccess()
                    .lookupOrThrow(Registries.BIOME);
            Biome b = registry.byId(data);
            if (b == null)
                return 0;
            var key = registry.getResourceKey(b);
            return key.map(biomeResourceKey -> getBiomeColor(x, z, biomeResourceKey)).orElse(0);
        }

        private void createLegend(File f) throws IOException {
            File f2 = new File(f.getParentFile(), "!legend.png");
            if (f2.exists() && !ReikaObfuscationHelper.isDeObfEnvironment())
                return;
            f2.createNewFile();

            MultiMap<Integer, Integer> li = ReikaBiomeHelper.getBiomeHierearchy();
            int heightPerBiome = 18;
            int height = (4 + heightPerBiome) * (1 + li.totalSize() + li.keySet().size());
            //height = ReikaMathLibrary.ceil2PseudoExp(height);

            BufferedImage img = new BufferedImage(256, height, BufferedImage.TYPE_INT_ARGB);

            Graphics graphics = img.getGraphics();
            Font ft = graphics.getFont();
            graphics.setFont(new Font(ft.getName(), ft.getStyle(), ft.getSize()));
            graphics.setColor(new Color(0xff000000));
            int y = 2;
            for (Integer b : li.keySet()) {
                this.createLegendEntry(b, 2, y, graphics, img, heightPerBiome);
                y += heightPerBiome + 4;
                for (Integer b2 : li.get(b)) {
                    this.createLegendEntry(b2, 24, y, graphics, img, heightPerBiome);
                    y += heightPerBiome + 4;
                }
            }
            graphics.dispose();

            ImageIO.write(img, "png", f2);
        }

        private void createLegendEntry(int b, int x, int y, Graphics g, BufferedImage img, int hpb) {
            var registry = reika.dragonapi.client.ClientEnvironment.level().registryAccess().lookupOrThrow(Registries.BIOME);
            Biome biome = registry.byId(b);
            if (biome == null)
                return;
            ResourceKey<Biome> key = registry.getResourceKey(biome).orElse(null);
            if (key == null)
                return;
            g.drawString(biome.toString(), x + hpb + 4, y + hpb / 2 + 4);
            for (int i = -1; i <= hpb; i++) {
                for (int k = -1; k <= hpb; k++) {
                    int clr = i == -1 || k == -1 || i == hpb || k == hpb ? 0xff000000 : 0xff000000 | getBiomeColor(i * 12, k * 12, key);
                    img.setRGB(x + i, y + k, clr);
                }
            }
        }

    }


    public static int getBiomeColor(int x, int z, ResourceKey<Biome> b) {
        if (b == null)
            return 0x000000; //should never happen

        var level = reika.dragonapi.client.ClientEnvironment.level();
        CustomMapColorBiome custom = customColors.get(b);
        if (custom != null) return custom.getMapColor(level, x, z);

        /*boolean mutate = b instanceof BiomeGenMutated;
        if (mutate) {
            b = ((BiomeGenMutated) b).baseBiome;
        }*/

        if (b == Biomes.NETHER_WASTES) {
            return 0xC12603;
        }
        if (b == Biomes.THE_END) {
            return 0xFFE9A3;
        }

        if (b == Biomes.FROZEN_OCEAN) {
            return 0x00ffff;
        }
        if (b == Biomes.ICE_SPIKES) {
            return 0x7FFFFF;
        }
//        if (b == Biomes.iceMountains) {
//            return 0xd0d0d0;
//        }

        //Because some BoP forests secretly identify as ocean-kin
        if (b.identifier().getPath().equalsIgnoreCase("Shield")) {
            return 0x387F4D;
        } else if (b.identifier().getPath().equalsIgnoreCase("Tropics")) {
            return 0x00ff00;
        } else if (b.identifier().getPath().equalsIgnoreCase("Lush Swamp")) {
            return 0x009000;
        } else if (b.identifier().getPath().equalsIgnoreCase("Bayou")) {
            return 0x7B7F4F; //Eew
        }/* else if (ReikaBiomeHelper.isOcean(null, b)) { //todo this will crash without a level so its commented out for now, sorry future max
            if (b == Biomes.DEEP_OCEAN)
                return 0x0000b0;
            return 0x0000ff;
        }*/

        if (b == Biomes.RIVER)
            return 0x22aaff;

        if (b == Biomes.BADLANDS) {
            return /*mutate ? 0xCE7352 :*/ 0xC4542B;
        }

        if (b == Biomes.MUSHROOM_FIELDS) {
            return 0x965471;
        }

        if (b == Biomes.TAIGA || b == Biomes.OLD_GROWTH_PINE_TAIGA || b == Biomes.OLD_GROWTH_SPRUCE_TAIGA) {
            return 0x9B6839;
        }

       /* if (net.minecraft.client.reika.dragonapi.client.ClientEnvironment.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME).getHolder(b).get().get().topBlock == Blocks.SAND) {
            return 0xE2C995;
        }
        if (net.minecraft.client.reika.dragonapi.client.ClientEnvironment.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME).getHolder(b).get().get().topBlock == Blocks.STONE) {
            return 0x808080;
        }*/

        if (b.identifier().getPath().equalsIgnoreCase("Coniferous Forest")) {
            return 0x007F42;
        }
        if (b.identifier().getPath().equalsIgnoreCase("Maple Forest")) {
            return 0x3A7F52;
        }

        int c = reika.dragonapi.client.ClientEnvironment.level().registryAccess().lookupOrThrow(Registries.BIOME).get(b).get().value().getGrassColor(x, z);

        if (ReikaBiomeHelper.isSnowBiome(b)) {
            c = 0xffffff;
        }

        if (b == Biomes.SNOWY_TAIGA) {
            c = 0xADFFCB;
        }

        /*if (mutate) {
            c = ReikaColorAPI.getColorWithBrightnessMultiplier(c, 0.875F);
        } else */
        if (ReikaBiomeHelper.isChildBiome(b)) {
            c = c == 0xffffff ? 0xd0d0d0 : ReikaColorAPI.getColorWithBrightnessMultiplier(c, 1.125F);
        }

        return c;
    }

    public interface MapCompleteCallback {

        void onComplete();

    }

}
