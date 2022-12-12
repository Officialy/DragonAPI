package reika.dragonapi.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.realmsclient.util.LevelType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;
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


//todo FIX COMMAND ARGUMENTS
public class BiomeMapCommand {

    public static final int PACKET_COMPILE = 2048; //packet size in bytes = 4*(1+n*3)

    private static BiomeMapCommand instance;

    private static final Random rand = new Random();
    private final static HashMap<Integer, BiomeMap> activeMaps = new HashMap<>();

    public BiomeMapCommand() {
        instance = this;
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("biomepng")
//                .then(Commands.argument("seed", StringArgumentType.string()))
//                .then(Commands.argument("range", IntegerArgumentType.integer(1)))
//                .then(Commands.argument("resolution", IntegerArgumentType.integer(1)))
//                .then(Commands.argument("grid", IntegerArgumentType.integer(1)))
//                .then(Commands.argument("fullgrid", BoolArgumentType.bool()))
                .executes((context) -> {
                    /*String[] args = {
                            String.valueOf(StringArgumentType.getString(context, "seed")),
                            String.valueOf(IntegerArgumentType.getInt(context, "range")),
                            String.valueOf(IntegerArgumentType.getInt(context, "resolution")),
                            String.valueOf(IntegerArgumentType.getInt(context, "grid")),
                            String.valueOf(BoolArgumentType.getBool(context, "fullgrid"))
                    };*/

                    String[] args = {"seed=-8335656470636700638", "1000", "1", "1", "false"};
                    return processCommand(context.getSource(), args);
                }));
    }

    public static int processCommand(CommandSourceStack sourceStack, String[] args) throws CommandSyntaxException {
        Object[] ret = getPlayer(sourceStack, args);
        Collection<BiomeProvider> set = new ArrayList<>();
        DragonAPI.LOGGER.info("starting biomepng");
        if (args.length < 2) {
            ReikaChatHelper.sendChatToPlayer(sourceStack.getPlayerOrException(), ChatFormatting.RED.toString() + "Illegal arguments. Use [seed=<seed>] [range] [resolution] <grid> <fullGrid>.");
            return 0;
        }
        if (args[0].toLowerCase(Locale.ENGLISH).startsWith("seed=")) {
            args[0] = args[0].substring(5);
            /*todo if (args[0].contains(",")) {
                String[] parts = args[0].split(",");
                for (String s : parts) {
                    set.add(new SeedBiomes(Long.parseLong(s)));
                }
            } else if (args[0].charAt(0) != '-' && args[0].contains("-")) {
                String[] parts = args[0].split("\\-");
                long s1 = Long.parseLong(parts[0]);
                long s2 = Long.parseLong(parts[1]);
                for (long seed = s1; seed <= s2; seed++) {
                    set.add(new SeedBiomes(seed));
                }
            } else {
                set.add(new SeedBiomes(Long.parseLong(args[0])));
            }*/
            String[] nargs = new String[args.length - 1];
            System.arraycopy(args, 1, nargs, 0, nargs.length);
            args = nargs;
        }
        ServerPlayer ep = (ServerPlayer) ret[0];
        if ((boolean) ret[1]) {
            String[] nargs = new String[args.length - 1];
            System.arraycopy(args, 1, nargs, 0, nargs.length);
            args = nargs;
        }
        int range = Integer.parseInt(args[0]);
        int res = Integer.parseInt(args[1]);
        int grid = args.length >= 3 ? Integer.parseInt(args[2]) : -1;
        boolean fullGrid = grid > 0 && args.length >= 4 && Boolean.parseBoolean(args[3]);
        int x = Mth.floor(ep.getX());
        int z = Mth.floor(ep.getZ());
        long start = System.currentTimeMillis();

        if (set.isEmpty())
            set.add(new WorldBiomes(ep.level));

        for (BiomeProvider bp : set)
            generateMap(bp, ep, start, x, z, range, res, grid, fullGrid, null);

        return 1;
    }

    public static void triggerBiomeMap(ServerPlayer ep, int x, int z, int range, int res, int grid, MapCompleteCallback call) {
        instance.generateMap(new WorldBiomes(ep.level), ep, System.currentTimeMillis(), x, z, range, res, grid, false, call);
    }

  /*  public static void triggerBiomeMap(ServerPlayer ep, int range, int res, int grid) {
        instance.processCommand(ep, new String[]{String.valueOf(range), String.valueOf(res), String.valueOf(grid)});
    }*/

    private static void generateMap(BiomeProvider bp, ServerPlayer ep, long start, int x, int z, int range, int res, int grid, boolean fullGrid, MapCompleteCallback callback) {
        int hash = rand.nextInt();

        ResourceKey<Level> dim = bp instanceof WorldBiomes ? Level.OVERWORLD : ep.level.dimension();
        if (DragonAPI.isSinglePlayer()) {
            startCollecting(hash, bp.getName(), dim, x, z, range, res, grid, fullGrid);
        } else {
            /*todo fix packets needing dimension to be an int, find a way to convert dims to ints?*/
            ReikaPacketHelper.sendStringIntPacket(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.BIOMEPNGSTART.ordinal(), ep, bp.getName(), hash, dim.hashCode(), x, z, range, res, grid, fullGrid ? 1 : 0);
        }

        ArrayList<Integer> dat = new ArrayList<>();
        dat.add(hash);
        int n = 0;
        for (int dx = x - range; dx <= x + range; dx += res) {
            for (int dz = z - range; dz <= z + range; dz += res) {
                Biome biome = bp.getBiome(dx, dz);
//                ReikaPacketHelper.sendDataPacket(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.BIOMEPNGDAT.ordinal(), ep, hash, dx, dz, b.biomeID);
                n++;
                if (DragonAPI.isSinglePlayer()) {
                    addBiomePoint(hash, dx, dz, biome.hashCode());
                } else {
                    dat.add(dx);
                    dat.add(dz);
                    dat.add(biome.hashCode());
                }
                if (n >= PACKET_COMPILE) {
                    if (DragonAPI.isSinglePlayer()) {

                    } else {
                        ReikaPacketHelper.sendDataPacket(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.BIOMEPNGDAT.ordinal(), ep, dat);
                        n = 0;
                        dat.clear();
                        dat.add(hash);
                    }
                }
            }
        }
        //in case leftover
        if (dat.size() > 1) {
            //pad to fit normal packet size expectation
            int m = (dat.size() - 1) / 3;
            Biome biome = bp.getBiome(x, z);
            if (DragonAPI.isSinglePlayer()) {
                addBiomePoint(hash, x, z, biome.hashCode());
            } else {
                for (int i = m; i < PACKET_COMPILE; i++) {
                    dat.add(x);
                    dat.add(z);
                    dat.add(biome.hashCode());
                }
                ReikaPacketHelper.sendDataPacket(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.BIOMEPNGDAT.ordinal(), ep, dat);
                n = 0;
                dat.clear();
                dat.add(hash);
            }
        }
        if (callback != null) {
            callback.onComplete();
            callback = null;
        }
        if (DragonAPI.isSinglePlayer()) {
            finishCollectingAndMakeImage(hash);
        } else {
            ReikaPacketHelper.sendDataPacket(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.BIOMEPNGEND.ordinal(), ep, hash);
        }
    }

    private static Object[] getPlayer(CommandSourceStack ics, String[] args) throws CommandSyntaxException {
        try {
            return new Object[]{ics.getPlayerOrException(), false};
        } catch (Exception e) {
//          todo  ServerPlayer ep = ReikaPlayerAPI.getPlayerByNameAnyWorld(args[0]);
            if (ics.getPlayerOrException() == null) {
                ReikaChatHelper.sendChatToPlayer(ics.getPlayerOrException(), "If you specify a player, they must exist.");
                throw new IllegalArgumentException(e);
            }
            return new Object[]{ics.getPlayerOrException(), true};
        }
    }

    public static void startCollecting(int hash, String world, ResourceKey<Level> dim, int x, int z, int range, int res, int grid, boolean fullGrid) {
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
                ReikaChatHelper.sendChatToPlayer(Minecraft.getInstance().player, ChatFormatting.GREEN + "File created in " + dur + " ms: " + path);
            } catch (IOException e) {
                ReikaChatHelper.sendChatToPlayer(Minecraft.getInstance().player, ChatFormatting.RED + "Failed to create file: " + e);
                e.printStackTrace();
            }
        }
    }

    private interface BiomeProvider {

        //public String getFileName(long seed, String name, int x, int z, int range, int res, int grid, boolean fullGrid);
        Biome getBiome(int x, int z);

        String getName();

    }

    private static class WorldBiomes implements BiomeProvider {

        private final Level world;

        private WorldBiomes(Level world) {
            this.world = world;
        }
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
            return world.getLevelData().toString();//todo.getWorldName() + "/[" + world.getSaveHandler().getWorldDirectoryName() + "]";
        }

    }

/*    private static class SeedBiomes implements BiomeProvider {

        private final long seed;
        private final WorldChunkManager world;

        private SeedBiomes(long seed) {
            this.seed = seed;
            world = new WorldChunkManager(seed, LevelType.DEFAULT);
        }
		*//*
		@Override
		public String getFileName(long seed, String name, int x, int z, int range, int res, int grid, boolean fullGrid) {
			return "BiomeMap/Forced/"+worldName+"; "+x+", "+z+" ("+sr+"x"+sr+"; [R="+res+" b-px, G="+grid+"-"+fullGrid+"]).png";
		}*//*

        @Override
        public Biome getBiome(int x, int z) {
            return world.getBiomeGenAt(x, z).biomeID;
        }

        @Override
        public String getName() {
            return "SEED=" + seed;
        }

    }*/

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
            var b = (Biome) ForgeRegistries.BIOMES.getValues().toArray()[data];
            var key = ForgeRegistries.BIOMES.getResourceKey(b);
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
            graphics.setFont(new Font(ft.getName(), ft.getStyle(), ft.getSize() * 1));
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
            Biome biome = (Biome) ForgeRegistries.BIOMES.getValues().toArray()[b];
            ResourceKey<Biome> key = ForgeRegistries.BIOMES.getResourceKey(biome).get();
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

        if (b instanceof CustomMapColorBiome)
            return ((CustomMapColorBiome) b).getMapColor(Minecraft.getInstance().level, x, z);

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
        if (ForgeRegistries.BIOMES.getHolder(b).toString().equalsIgnoreCase("Shield")) {
            return 0x387F4D;
        } else if (ForgeRegistries.BIOMES.getHolder(b).toString().equalsIgnoreCase("Tropics")) {
            return 0x00ff00;
        } else if (ForgeRegistries.BIOMES.getHolder(b).toString().equalsIgnoreCase("Lush Swamp")) {
            return 0x009000;
        } else if (ForgeRegistries.BIOMES.getHolder(b).toString().equalsIgnoreCase("Bayou")) {
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

       /* if (ForgeRegistries.BIOMES.getHolder(b).get().get().topBlock == Blocks.SAND) {
            return 0xE2C995;
        }
        if (ForgeRegistries.BIOMES.getHolder(b).get().get().topBlock == Blocks.STONE) {
            return 0x808080;
        }*/

        if (ForgeRegistries.BIOMES.getHolder(b).toString().equalsIgnoreCase("Coniferous Forest")) {
            return 0x007F42;
        }
        if (ForgeRegistries.BIOMES.getHolder(b).toString().equalsIgnoreCase("Maple Forest")) {
            return 0x3A7F52;
        }

        int c = ForgeRegistries.BIOMES.getHolder(b).get().value().getGrassColor(x, z);

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
