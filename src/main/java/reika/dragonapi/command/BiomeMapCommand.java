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
import reika.dragonapi.instantiable.io.MapOutput;
import reika.dragonapi.interfaces.CustomBiomeDistributionWorld;
import reika.dragonapi.interfaces.CustomMapColorBiome;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.io.ReikaChatHelper;
import reika.dragonapi.libraries.io.ReikaPacketHelper;
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
        ReikaPacketHelper.sendStringIntPacket(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.BIOMEPNGSTART.wireId(), ep, bp.getName(), hash, 0, x, z, range, res, grid, fullGrid ? 1 : 0);

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
//                ReikaPacketHelper.sendDataPacket(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.BIOMEPNGDAT.wireId(), ep, hash, dx, dz, b.biomeID);
                n++;
                dat.add(dx);
                dat.add(dz);
                dat.add(biomeId);
                if (n >= PACKET_COMPILE) {
                    ReikaPacketHelper.sendDataPacket(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.BIOMEPNGDAT.wireId(), ep, dat);
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
            ReikaPacketHelper.sendDataPacket(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.BIOMEPNGDAT.wireId(), ep, dat);
        }
        if (callback != null) {
            callback.onComplete();
        }
        ReikaPacketHelper.sendDataPacket(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.BIOMEPNGEND.wireId(), ep, hash);
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
        private final BitSet received = new BitSet();
        private final Map<ResourceKey<Biome>, Integer> biomeCounts = new HashMap<>();

        private BiomeMap(String name, ResourceKey<Level> dim, int x, int z, int r, int res, int grid, boolean fgrid) {
            super(name, dim, x, z, r, res, grid, fgrid);
        }

        @Override
        protected void onImageCreate(File f) throws IOException {
            this.createLegend(f);
            StringBuilder csv = new StringBuilder("biome,samples,percentage,hex_at_origin\n");
            int total = biomeCounts.values().stream().mapToInt(Integer::intValue).sum();
            for (var key : biomeCounts.keySet().stream().sorted(Comparator.comparing(k -> k.identifier().toString())).toList()) {
                int count = biomeCounts.get(key);
                csv.append(key.identifier()).append(',').append(count).append(',')
                        .append(String.format(Locale.ROOT, "%.6f", 100D * count / total)).append(',')
                        .append(String.format(Locale.ROOT, "#%06X", getBiomeColor(originX, originZ, key) & 0xffffff)).append('\n');
            }
            java.nio.file.Files.writeString(new File(f.getParentFile(), f.getName().replace(".png", ".biomes.csv")).toPath(),
                    csv, java.nio.charset.StandardCharsets.UTF_8);
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
            int width = 2 * range / resolution + 1;
            int index = (range + x - originX) / resolution * width + (range + z - originZ) / resolution;
            if (key.isPresent() && !received.get(index)) {
                received.set(index);
                biomeCounts.merge(key.get(), 1, Integer::sum);
            }
            return key.map(biomeResourceKey -> getBiomeColor(x, z, biomeResourceKey)).orElse(0);
        }

        private void createLegend(File f) throws IOException {
            writeBiomeLegend(reika.dragonapi.client.ClientEnvironment.level(), f.getParentFile());
        }
    }

    /** Registry-backed legend, also usable by offline/debug exports without a client renderer. */
    public static void writeBiomeLegend(Level level, File directory) throws IOException {
        var registry = level.registryAccess().lookupOrThrow(Registries.BIOME);
        var keys = registry.listElements().map(holder -> holder.key()).sorted(
                Comparator.comparing(key -> key.identifier().toString())).toList();
        if (keys.isEmpty()) throw new IOException("The synchronized biome registry is empty");
        java.nio.file.Files.createDirectories(directory.toPath());
        int rowHeight = 24;
        BufferedImage img = new BufferedImage(800, 32 + rowHeight * keys.size(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = img.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, img.getWidth(), img.getHeight());
        graphics.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        graphics.setColor(Color.BLACK);
        graphics.drawString("Biome registry key / RGB at (0,0); swatches show spatial colour variation", 8, 20);
        StringBuilder csv = new StringBuilder("biome,red,green,blue,hex\n");
        for (int row = 0; row < keys.size(); row++) {
            var key = keys.get(row);
            int y = 32 + row * rowHeight;
            graphics.setColor(Color.BLACK);
            graphics.drawRect(7, y - 1, 19, 19);
            for (int x = 0; x < 18; x++) for (int z = 0; z < 18; z++)
                img.setRGB(8 + x, y + z, 0xff000000 | getBiomeColor(level, x * 12, z * 12, key));
            int color = getBiomeColor(level, 0, 0, key) & 0xffffff;
            String hex = String.format(Locale.ROOT, "#%06X", color);
            graphics.drawString(key.identifier() + "  " + hex, 36, y + 14);
            csv.append(key.identifier()).append(',').append(color >> 16).append(',')
                    .append((color >> 8) & 255).append(',').append(color & 255).append(',').append(hex).append('\n');
        }
        graphics.dispose();
        ImageIO.write(img, "png", new File(directory, "!legend.png"));
        java.nio.file.Files.writeString(new File(directory, "!legend.csv").toPath(), csv,
                java.nio.charset.StandardCharsets.UTF_8);
    }


    public static int getBiomeColor(int x, int z, ResourceKey<Biome> b) {
        return getBiomeColor(reika.dragonapi.client.ClientEnvironment.level(), x, z, b);
    }

    public static int getBiomeColor(Level level, int x, int z, ResourceKey<Biome> b) {
        if (b == null) return 0x000000;

        CustomMapColorBiome custom = customColors.get(b);
        if (custom != null) return custom.getMapColor(level, x, z);

        // Modern oceans used to fall through to grass colours, hiding both water and climate.
        // Give active mod biomes their own debug colours rather than identical forest swatches.
        int modernColor = switch (b.identifier().toString()) {
            case "minecraft:ocean" -> 0x0000FF;
            case "minecraft:deep_ocean" -> 0x0000B0;
            case "minecraft:cold_ocean" -> 0x315FA1;
            case "minecraft:deep_cold_ocean" -> 0x25357A;
            case "minecraft:lukewarm_ocean" -> 0x30A2DC;
            case "minecraft:deep_lukewarm_ocean" -> 0x1463AC;
            case "minecraft:warm_ocean" -> 0x36D4E8;
            case "minecraft:deep_frozen_ocean" -> 0x00B7D4;
            case "minecraft:frozen_river" -> 0x77CFFF;
            case "minecraft:frozen_peaks", "minecraft:jagged_peaks", "minecraft:grove" -> 0xE6EFFF;
            case "minecraft:dripstone_caves" -> 0x905B36;
            case "minecraft:lush_caves" -> 0x00D48A;
            case "minecraft:deep_dark" -> 0x133E4C;
            case "chromaticraft:rainbow_forest" -> 0xD969E8;
            case "chromaticraft:rainbow_stream" -> 0xAF76F5;
            case "chromaticraft:ender_forest" -> 0x3D2466;
            case "chromaticraft:luminous_cliffs" -> 0xF4D35E;
            case "chromaticraft:luminous_cliffs_shores" -> 0xB8A85A;
            case "geostrata:arctic_spires" -> 0xD6F4FF;
            case "geostrata:kelp_forest" -> 0x18785D;
            default -> -1;
        };
        if (modernColor >= 0) return modernColor;

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
        }

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

        int c = level.registryAccess().lookupOrThrow(Registries.BIOME).get(b).get().value().getGrassColor(x, z);

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
