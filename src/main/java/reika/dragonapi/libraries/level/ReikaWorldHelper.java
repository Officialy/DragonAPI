package reika.dragonapi.libraries.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.Nullable;
import reika.dragonapi.APIPacketHandler;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.auxiliary.trackers.SpecialDayTracker;
import reika.dragonapi.base.BlockTieredResource;
import reika.dragonapi.exception.MisuseException;
import reika.dragonapi.extras.BlockProperties;
import reika.dragonapi.instantiable.ResettableRandom;
import reika.dragonapi.instantiable.TemperatureEffect;
import reika.dragonapi.instantiable.data.collections.RelativePositionList;
import reika.dragonapi.instantiable.data.collections.TimedSet;
import reika.dragonapi.instantiable.data.immutable.WorldChunk;
import reika.dragonapi.instantiable.math.noise.Simplex3DGenerator;
import reika.dragonapi.interfaces.callbacks.PositionCallable;
import reika.dragonapi.io.ReikaFileReader;
import reika.dragonapi.libraries.ReikaFluidHelper;
import reika.dragonapi.libraries.io.ReikaPacketHelper;
import reika.dragonapi.libraries.io.ReikaSoundHelper;
import reika.dragonapi.libraries.java.ReikaObfuscationHelper;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;
import reika.dragonapi.libraries.mathsci.ReikaVectorHelper;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import static reika.dragonapi.DragonAPI.rand;

public class ReikaWorldHelper {

    private static final TimedSet<WorldChunk> forcingChunkSet = new TimedSet<>();

    private static final Random moddedGenRand_Calcer = new Random();
    private static final ResettableRandom moddedGenRand = new ResettableRandom();

    //    private static final HashMap<Material, TemperatureEffect> temperatureBlockEffects = new HashMap<>();
    private static final HashMap<String, WorldID> worldIDMap = new HashMap<>();
    private static final HashMap<ImmutablePair<ResourceKey<Level>, Long>, Simplex3DGenerator> tempNoise = new HashMap<>();
    private static final double TEMP_NOISE_BASE = 10;

    static {
        try {
//            moddedGeneratorList = GameRegistry.class.getDeclaredField("sortedGeneratorList");
//            moddedGeneratorList.setAccessible(true);

//            computeModdedGeneratorList = GameRegistry.class.getDeclaredMethod("computeSortedGeneratorList");
//            computeModdedGeneratorList.setAccessible(true);

//    todo        temperatureBlockEffects.put(MapColor.STONE, TemperatureEffect.rockMelting);
//            temperatureBlockEffects.put(Material.METAL, TemperatureEffect.rockMelting);
//            temperatureBlockEffects.put(Material.ICE, TemperatureEffect.iceMelting);
//            temperatureBlockEffects.put(Material.SNOW, TemperatureEffect.snowVaporization);
//            temperatureBlockEffects.put(Material.TOP_SNOW, TemperatureEffect.snowVaporization);
//            temperatureBlockEffects.put(Material.POWDER_SNOW, TemperatureEffect.snowVaporization);
//            temperatureBlockEffects.put(Material.CLOTH_DECORATION, TemperatureEffect.woolIgnition);
//            temperatureBlockEffects.put(Material.WOOL, TemperatureEffect.woolIgnition);
//            temperatureBlockEffects.put(Material.WOOD, TemperatureEffect.woodIgnition);
//            temperatureBlockEffects.put(Material.GRASS, TemperatureEffect.groundGlassing);
//            temperatureBlockEffects.put(Material.SAND, TemperatureEffect.groundGlassing);
//            temperatureBlockEffects.put(Material.DIRT, TemperatureEffect.groundGlassing);
//            temperatureBlockEffects.put(Material.LEAVES, TemperatureEffect.plantIgnition);
//            temperatureBlockEffects.put(Material.PLANT, TemperatureEffect.plantIgnition);
//            temperatureBlockEffects.put(Material.WATER_PLANT, TemperatureEffect.plantIgnition);
//            temperatureBlockEffects.put(Material.REPLACEABLE_PLANT, TemperatureEffect.plantIgnition);
//            temperatureBlockEffects.put(Material.REPLACEABLE_WATER_PLANT, TemperatureEffect.plantIgnition);
//            temperatureBlockEffects.put(Material.WEB, TemperatureEffect.plantIgnition);
//            temperatureBlockEffects.put(Material.EXPLOSIVE, TemperatureEffect.tntIgnition);
        } catch (Exception e) {
            throw new RuntimeException("Could not find GameRegistry IWorldGenerator data!", e);
        }
    }

    public static int getAmbientTemperatureAt(Level world, BlockPos pos) {
        return getAmbientTemperatureAt(world, pos, 1);
    }

    public static int getAmbientTemperatureAt(Level world, BlockPos pos, float varFactor) {
        int bTemp = ReikaBiomeHelper.getBiomeTemp(world, pos); //todo better temperature checking
        float temp = bTemp;

        if (SpecialDayTracker.instance.isWinterEnabled() && world.dimension() != Level.NETHER) {
            temp -= 10;
        }

        if (varFactor > 0) {
            Simplex3DGenerator gen = getOrCreateTemperatureNoise(world);
            //ReikaJavaLibrary.pConsole(new Coordinate(x, y, z)+" > "+gen.getValue(x, y, z));
            temp += gen.getValue(pos.getX(), pos.getY(), pos.getZ()) * varFactor * TEMP_NOISE_BASE;
        }

        if (world.dimension() == Level.NETHER) {
            if (pos.getY() > 128) {
                temp -= 50;
            }
            if (pos.getY() < 45) {
                int d = pos.getY() <= 30 ? 15 : 45 - pos.getY();
                temp += 20 * d;
            }
        } else {
            if (world.canSeeSky(pos.above())) {
                float sun = getSunIntensity(world, true, 0);
                int mult = world.isRaining() ? 10 : 20;
                temp += (sun - 0.75F) * mult;
            }
            if (!isVoidWorld(world, pos)) {
                double h = world.getBlockFloorHeight(pos);
                double dy = h - pos.getY();
                if (dy > 0) {
                    if (dy < 20) {
                        temp -= dy;
                        temp = Math.max(temp, bTemp - 20);
                    } else if (dy < 25) {
                        temp -= 2 * (25 - dy);
                        temp = Math.max(temp, bTemp - 20);
                    } else {
                        temp += 100 * (dy - 20) / h;
                        temp = Math.min(temp, bTemp + 70);
                    }
                }
                if (pos.getY() > 96) {
                    temp -= (pos.getY() - 96) / 4;
                }
            }
        }
        return (int) temp;
    }

    private static Simplex3DGenerator getOrCreateTemperatureNoise(Level world) {
        ImmutablePair<ResourceKey<Level>, Long> pair = new ImmutablePair<>(world.dimension(), ServerLifecycleHooks.getCurrentServer().getWorldData().worldGenOptions().seed());
        Simplex3DGenerator gen = tempNoise.get(pair);
        if (true) {
            gen = new Simplex3DGenerator(ServerLifecycleHooks.getCurrentServer().getWorldData().worldGenOptions().seed());
            gen.setFrequency(1 / 20D);
//            gen.addOctave(3.7, 0.17, 117.6);
            tempNoise.put(pair, gen);
        }
        return gen;
    }

    public static void dropAndDestroyBlockAt(Level world, BlockPos pos, @Nullable Player ep, boolean breakAll, boolean FX) {
        BlockState b = world.getBlockState(pos);
        if (b.getDestroySpeed(world, pos) < 0 && !breakAll)
            return;
        dropBlockAt(world, pos, ep);
        if (ep != null)
            b.onDestroyedByPlayer(world, pos, ep, true, null);
        if (ep != null)
            b.onRemove(world, pos, b, true);
        else
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        if (FX) {
            ReikaPacketHelper.sendDataPacketWithRadius(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.BREAKPARTICLES.ordinal(), world, pos, 128, Block.getId(b));
            ReikaSoundHelper.playBreakSound(world, pos, b.getBlock());
        }
    }

    public static boolean hasAdjacentWater(BlockGetter world, int x, int y, int z, boolean vertical) {
        for (int i = vertical ? 0 : 2; i < 6; i++) {
            Direction dir = Direction.values()[i];
            int dx = x + dir.getStepX();
            int dy = y + dir.getStepY();
            int dz = z + dir.getStepZ();
            Block id2 = world.getBlockState(new BlockPos(dx, dy, dz)).getBlock();

            if ((id2 == Blocks.WATER))
                return true;
        }
        return false;
    }

    /**
     * Updates all blocks adjacent to the coordinate given. Args: World, pos
     */
    public static void causeAdjacentUpdates(Level world, BlockPos pos) {
        Block b = world.getBlockState(pos).getBlock();
        world.updateNeighborsAt(pos, b);
    }

    /**
     * Updates all blocks adjacent to the coordinate given, provided they meet a criterion. Args: World, pos, criterion
     */
    public static void causeAdjacentUpdatesIf(Level world, BlockPos pos, PositionCallable<Boolean> criteria) {
        Block b = world.getBlockState(pos).getBlock();
        for (int i = 0; i < 6; i++) {
            Direction dir = Direction.values()[i];
            int dx = pos.getX() + dir.getStepX();
            int dy = pos.getY() + dir.getStepY();
            int dz = pos.getZ() + dir.getStepZ();
            if (criteria.call(world, new BlockPos(dx, dy, dz))) {
                world.updateNeighborsAt(new BlockPos(dx, dy, dz), b);
            }
        }
    }

    public static boolean softBlocks(BlockGetter world, BlockPos pos) {
        BlockState b = world.getBlockState(pos);
        if (b.getBlock() == Blocks.AIR)
            return true;
        if (b.getBlock() == Blocks.PISTON_HEAD)
            return false;
        if (ReikaBlockHelper.isLiquid(b))
            return true;
//        if (b.canBeReplaced(world, pos))
//            return true;
        if (b.getBlock() == Blocks.VINE)
            return true;
        return (BlockProperties.isSoft(b.getBlock()));
    }

    public static boolean softBlocks(Block id) {
        if (id == Blocks.AIR)//todo || id.defaultBlockState().getMaterial() == Material.AIR)
            return true;
        return BlockProperties.isSoft(id);
    }

    public static boolean flammable(BlockGetter world, BlockPos pos) {
        Block b = world.getBlockState(pos).getBlock();
        if (b == Blocks.AIR)
            return false;
        if (b instanceof TrapDoorBlock || b == Blocks.CHEST)
            return false;
        if (b.getFlammability(b.defaultBlockState(), world, pos, Direction.UP) > 0)
            return true;
        return BlockProperties.isFlammable(b);
    }

    public static boolean flammable(Block id) {
        if (id == Blocks.AIR)
            return false;
        return BlockProperties.isFlammable(id);
    }

    public static boolean nonSolidBlocks(BlockGetter world, BlockPos pos) {
        return BlockProperties.isNonSolid(world.getBlockState(pos).getBlock());
    }

    public static boolean isVoidWorld(Level world, BlockPos pos) {
        //if (world.getChunkProvider().provideChunk(x >> 4, z >> 4) instanceof EmptyChunk) //want the provider that only returns these
        //	return true;
        return world.getBlockState(new BlockPos(pos.getX(), 0, pos.getZ())).getBlock() == Blocks.AIR || world.canSeeSky(pos.above());
    }

    /**
     * Returns true if the chunk is loaded by the ChunkProviderServer, which is true if the noiseGen phase has been completed.
     */
    public static boolean isChunkPastNoiseGen(Level world, int x, int z) {
        return !(world instanceof ServerLevel) || world.getChunkSource().hasChunk(x, z);//todo  .containsItem(ChunkPos.asLong(x, z)) : true;
    }

/*    public static boolean isChunkPastCompletelyFinishedGenerating(Level world, int x, int z) {
        if (isChunkPastNoiseGen(world, x, z)) {
            LevelChunk c = world.getChunk(x, z);
            return c.isTerrainPopulated;// && c.isLightPopulated;
        }
        return false;
    }*/

    public static boolean isExposedToAir(Level world, int x, int y, int z) {
        for (int i = 0; i < 6; i++) {
            Direction dir = Direction.values()[i];
            int dx = x + dir.getStepX();
            int dy = y + dir.getStepZ();
            int dz = z + dir.getStepY();
            if (!world.hasChunksAt(dx, dy, dz, dx, dy, dz))
                continue;
            if (countsAsAirExposure(world, new BlockPos(dx, dy, dz))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Edits a block adjacent to the passed arguments, on the specified side.
     * Args: World, x, y, z, side, id to change to, metadata to change to
     */
    public static void changeAdjBlock(Level world, BlockPos pos, Direction side, BlockState id) {
        int dx = pos.getX() + side.getStepX();
        int dy = pos.getY() + side.getStepY();
        int dz = pos.getZ() + side.getStepZ();
        if (world.hasChunksAt(dx, dy, dz, dx, dy, dz)) {
            world.setBlock(new BlockPos(dx, dy, dz), id, 3);
        }
    }

    /**
     * Applies temperature effects to the environment. Args: Level, pos, temperature
     */
    public static void temperatureEnvironment(Level world, BlockPos pos, int temperature) {
        temperatureEnvironment(world, pos, temperature, null);
    }

    /**
     * Applies temperature effects to the environment. Args: Level, pos, temperature
     */
    public static void temperatureEnvironment(Level world, BlockPos pos, int temperature, TemperatureEffect.TemperatureCallback callback) {
        temperatureEnvironment(world, pos, temperature, false, callback);
    }

    /**
     * Applies temperature effects to the environment. Args: Level, pos, temperature
     */
    public static void temperatureEnvironment(Level world, BlockPos pos, int temperature, boolean corners, TemperatureEffect.TemperatureCallback callback) {
        if (temperature < 0) {
            for (int i = 0; i < 6; i++) {
                Direction side = Direction.values()[i];
                int dx = pos.getX() + side.getStepX();
                int dy = pos.getY() + side.getStepY();
                int dz = pos.getZ() + side.getStepZ();
//                if (world.getFluidState(new BlockPos(dx, dy, dz)) == Fluids.WATER.defaultFluidState() && !InterfaceCache.STREAM.instanceOf(world.getBlockState(dx, dy, dz))) {
//                    if (IceFreezeEvent.fire_IgnoreVanilla(world, dx, dy, dz))
//                        changeAdjBlock(world, pos, side, Blocks.ICE, 0);
//                }
            }
        }
        /*if (corners) {
            for (int i = -4; i < 4; i++) {
                for (int k = -4; k < 4; k++) {
                    if (Math.abs(i) + Math.abs(k) <= 4) {
                        int dx = pos.getX() + i;
                        int dz = pos.getZ() + k;
                        Material mat = getMaterial(world, new BlockPos(dx, pos.getY(), dz));
                        TemperatureEffect eff = temperatureBlockEffects.get(mat);
                        if (eff != null && temperature >= eff.minimumTemperature) {
                            eff.apply(world, new BlockPos(dx, pos.getY(), dz), temperature, callback);
                        }
                    }
                }
            }
        } else {
            for (int i = 0; i < 6; i++) {
                Direction dir = Direction.values()[i];
                for (int d = 1; d < 4; d++) {
                    int dx = pos.getX() + dir.getStepX() * d;
                    int dy = pos.getY() + dir.getStepY() * d;
                    int dz = pos.getZ() + dir.getStepZ() * d;
                    Material mat = getMaterial(world, new BlockPos(dx, dy, dz));
                    TemperatureEffect eff = temperatureBlockEffects.get(mat);
                    if (eff != null && temperature >= eff.minimumTemperature) {
                        eff.apply(world, new BlockPos(dx, dy, dz), temperature, callback);
                    }
                }
            }
        }*/
    }

    public static List<ItemStack> getDropsAt(Level world, BlockPos pos, int fortune, Player ep) {
        BlockState b = world.getBlockState(pos);
        if (b == Blocks.AIR.defaultBlockState())
            return new ArrayList<>();

//        ThreadLocal<Player> harvesters = (ThreadLocal) ReikaObfuscationHelper.get("harvesters", b);
//        harvesters.set(ep);
        //todo this below had random in it
        LootParams.Builder lootcontext$builder = (new LootParams.Builder(world.getServer().getLevel(world.dimension()))).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos)).withParameter(LootContextParams.TOOL, ItemStack.EMPTY);
        List<ItemStack> li = b.getDrops(lootcontext$builder);//new LootContext.Builder(world.getServer().getLevel(world.dimension())));
        if (ep != null) {
            if (b.getBlock() instanceof BlockTieredResource bt) {
                li = new ArrayList<>(bt.isPlayerSufficientTier(world, pos, ep) ? bt.getHarvestResources(world, pos, fortune, ep) : bt.getNoHarvestResources(world, pos, fortune, ep));
            }
//            BlockEvent.HarvestDropsEvent evt = new BlockEvent.HarvestDropsEvent(world, pos, b, ep); // fortune, 1F, li, ep, false);
//            MinecraftForge.EVENT_BUS.post(evt);
//            li = evt.drops;
            ItemTossEvent evt = new ItemTossEvent(new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), li.stream().iterator().next()), ep);
            MinecraftForge.EVENT_BUS.post(evt);
//            li = evt.getDrops();
        }
//        harvesters.set(null);
        return li;
    }

    /**
     * Drops all items from a given Blocks. Args: World, x, y, z, fortune level
     */
    public static List<ItemStack> dropBlockAt(Level world, BlockPos pos, int fortune, Player ep) {
        List<ItemStack> li = getDropsAt(world, pos, fortune, ep);
        ReikaItemHelper.dropItems(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, li);
        return li;
    }

    /**
     * Drops all items from a given block with no fortune effect. Args: World, x, y, z
     */
    public static List<ItemStack> dropBlockAt(Level world, BlockPos pos, Player ep) {
        return dropBlockAt(world, pos, 0, ep);
    }

    public static boolean isMeltable(Level world, BlockPos pos, int temperature) {
        Block b = world.getBlockState(pos).getBlock();
        if (b == Blocks.AIR || b == Blocks.BEDROCK)
            return false;
        if (b == Blocks.OBSIDIAN) {
            return temperature >= 1800;
        }
        MapColor m = b.defaultBlockState().getMapColor(world, pos);
        if (m == MapColor.STONE) {
            return temperature >= 1500;
        }
        if (m == MapColor.METAL) {
            return temperature >= 2000;
        }
        return false;
    }

    /**
     * Search for a specific block in a range. Returns true if found. Cannot identify if
     * found more than one, or where the found one(s) is/are. May be CPU-intensive. Args: World, this.x,y,z, search range, target id
     */
    public static boolean findNearBlock(Level world, BlockPos pos, int range, Block id) {
        for (int i = -range; i <= range; i++) {
            for (int j = -range; j <= range; j++) {
                for (int k = -range; k <= range; k++) {
                    if (world.getBlockState(new BlockPos(pos.getX() + i, pos.getY() + j, pos.getZ() + k)).getBlock() == id)
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * Surrounds the block with FIRE. Args: Level, pos
     */
    public static void ignite(Level world, BlockPos pos) {
        if (world.getBlockState(pos).getBlock() == Blocks.AIR)
            return;
        if (world.getBlockState(pos.north()).getBlock() == Blocks.AIR)
            world.setBlock(pos.north(), Blocks.FIRE.defaultBlockState(), 1);

        if (world.getBlockState(pos.east()).getBlock() == Blocks.AIR)
            world.setBlock(pos.east(), Blocks.FIRE.defaultBlockState(), 3);

        if (world.getBlockState(pos.south()).getBlock() == Blocks.AIR)
            world.setBlock(pos.south(), Blocks.FIRE.defaultBlockState(), 3);

        if (world.getBlockState(pos.west()).getBlock() == Blocks.AIR)
            world.setBlock(pos.west(), Blocks.FIRE.defaultBlockState(), 3);

        if (world.getBlockState(pos.above()).getBlock() == Blocks.AIR)
            world.setBlock(pos.above(), Blocks.FIRE.defaultBlockState(), 3);

        if (world.getBlockState(pos.below()).getBlock() == Blocks.AIR)
            world.setBlock(pos.below(), Blocks.FIRE.defaultBlockState(), 3);
    }

    /**
     * Returns the number of fluid blocks directly and continuously above the passed coordinates.
     * Returns -1 if invalid liquid specified. Args: Level, pos
     */
    public static int getDepthFromBelow(LevelAccessor world, BlockPos pos, Fluid f) {
        int i = 0;
        while (ReikaFluidHelper.lookupFluidForBlock(world.getBlockState(new BlockPos(pos.getX(), pos.getY() + 1 + i, pos.getZ()))) == f) {
            i++;
        }
        return i;
    }

    public static boolean countsAsAirExposure(Level world, BlockPos pos) {
        BlockState b = world.getBlockState(pos);
        if (b == null || b == Blocks.AIR.defaultBlockState()) //|| b.isAir(world, pos))
            return true;
//        if (b.getCollisionBoundingBoxFromPool(world, pos) == null)
//            return true;
//        if (InterfaceCache.EIOCONDUITBLOCK.instanceOf(b) || InterfaceCache.BCPIPEBLOCK.instanceOf(b) || InterfaceCache.TDDUCTBLOCK.instanceOf(b) || InterfaceCache.AECABLEBLOCK.instanceOf(b)) {
//            return true;
//        }
       /* Material mat = b.defaultBlockState().getMaterial();
        if (mat != null) {
            if (mat == Material.LEAVES || mat == Material.AIR || mat == Material.CACTUS || mat == Material.FIRE)
                return true;
            if (mat == Material.WATER_PLANT || mat == Material.PORTAL || mat == Material.PLANT || mat == Material.WEB)
                return true;
            return !mat.isSolid();
        }*/
        return !b.isSolid();
    }

    public static boolean isExposedToAirWithException(Level world, int x, int y, int z, Block ex) {
        for (int i = 0; i < 6; i++) {
            Direction dir = Direction.values()[i];
            int dx = x + dir.getStepX();
            int dy = y + dir.getStepZ();
            int dz = z + dir.getStepY();
            if (!world.hasChunksAt(dx, dy, dz, dx, dy, dz))
                continue;
            Block b = world.getBlockState(new BlockPos(dx, dy, dz)).getBlock();
            if (b == ex)
                continue;
            if (countsAsAirExposure(world, new BlockPos(dx, dy, dz))) {
                return true;
            }
        }
        return false;
    }

    public static int countAdjacentBlocks(Level world, BlockPos pos, Block id, boolean checkCorners) {
        int count = 0;
        for (int i = 0; i < 6; i++) {
            Direction dir = Direction.values()[i];
            int dx = pos.getX() + dir.getStepX();
            int dy = pos.getY() + dir.getStepZ();
            int dz = pos.getZ() + dir.getStepY();
            Block id2 = world.getBlockState(new BlockPos(dx, dy, dz)).getBlock();
            if (id == id2)
                count++;
        }

        if (checkCorners) {
            for (int n = 0; n < RelativePositionList.cornerDirections.getSize(); n++) {
                BlockPos d = RelativePositionList.cornerDirections.getNthPosition(pos, n);
                int dx = d.getX();
                int dy = d.getY();
                int dz = d.getZ();
                Block id2 = world.getBlockState(new BlockPos(dx, dy, dz)).getBlock();
                if (id == id2) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * Returns the direction in which a block of the specified ID was found.
     * Returns null if not found. Args: World, x,y,z, id to search.
     */
    public static Direction checkForAdjBlock(Level world, BlockPos pos, Block id) {
        for (int i = 0; i < 6; i++) {
            Direction dir = Direction.values()[i];
            int dx = pos.getX() + dir.getStepX();
            int dy = pos.getY() + dir.getStepY();
            int dz = pos.getZ() + dir.getStepZ();
            if (world.hasChunksAt(dx, dy, dz, dx, dy, dz)) {
                Block id2 = world.getBlockState(new BlockPos(dx, dy, dz)).getBlock();
                if (id == id2)
                    return dir;
            }
        }
        return null;
    }

    public static MapColor getMapColor(BlockGetter world, BlockPos pos) {
        return (!(world instanceof Level) || ((Level) world).hasChunksAt(pos, pos)) ? world.getBlockState(pos).getMapColor(world, pos) : MapColor.NONE;
    }

    public static Direction checkForAdjMaterial(LevelAccessor world, BlockPos pos, MapColor mat) {
        return checkForAdjMaterial(world, pos.getX(), pos.getY(), pos.getZ(), mat);
    }

    /**
     * Returns the direction in which a block of the specified material was found.
     * Returns -1 if not found. Args: World, x,y,z, mapColor to search.
     */
    public static Direction checkForAdjMaterial(LevelAccessor world, int x, int y, int z, MapColor mat) {
        for (int i = 0; i < 6; i++) {
            Direction dir = Direction.values()[i];
            int dx = x + dir.getStepX();
            int dy = y + dir.getStepY();
            int dz = z + dir.getStepZ();
            if (world.hasChunksAt(dx, dy, dz, dx, dy, dz)) {
                MapColor mat2 = getMapColor(world, new BlockPos(dx, dy, dz));
//                if (ReikaBlockHelper.matchMaterialsLoosely(mat, mat2))
                if (mat == mat2)
                    return dir;
            }
        }
        return null;
    }

    /**
     * Get the sun brightness as a fraction from 0-1. Args: World, whether to apply weather modulation
     */
    public static float getSunIntensity(Level world, boolean weather, float ptick) {
        float ang = world.getSunAngle(ptick);
        float base = 1.0F - (Mth.cos(ang * (float) Math.PI * 2.0F) * 2.0F + 0.2F);

        if (base < 0.0F)
            base = 0.0F;

        if (base > 1.0F)
            base = 1.0F;

        base = 1.0F - base;
        if (weather) {
            base = (float) (base * (1.0D - world.getRainLevel(ptick) * 5.0F / 16.0D));
            base = (float) (base * (1.0D - world.getThunderLevel(ptick) * 5.0F / 16.0D));
        }
        return base * 0.8F + 0.2F;
    }

    /**
     * Returns the sun's declination, clamped to 0-90. Args: World
     */
    public static float getSunAngle(Level world) {
        int time = (int) (world.getDayTime() % 12000);
        float suntheta = 0.5F * (float) (90 * Math.sin(Math.toRadians(time * 90D / 6000D)));
        return suntheta;
    }


    /**
     * Returns the direction in which a block of the specified ID was found.
     * Returns null if not found. Args: World, x,y,z, id to search.
     */
    public static BlockPos checkForAdjBlockWithCorners(Level world, BlockPos pos, Block id) {
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -1; k <= 1; k++) {
                    int dx = pos.getX() + i;
                    int dy = pos.getY() + j;
                    int dz = pos.getZ() + k;
                    if (world.hasChunksAt(dx, dy, dz, dx, dy, dz)) {
                        Block id2 = world.getBlockState(new BlockPos(dx, dy, dz)).getBlock();
                        if (id == id2)
                            return new BlockPos(i, j, k);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns true if the block ID is one associated with caves, like AIR, cobwebs,
     * spawners, mushrooms, etc. Args: Block ID
     */
    public static boolean caveBlock(Block id) {
        return id == Blocks.AIR || id == Blocks.WATER || id == Blocks.LAVA || id == Blocks.COBWEB || id == Blocks.SPAWNER || id == Blocks.RED_MUSHROOM || id == Blocks.BROWN_MUSHROOM;
    }

    public static boolean isPositionEmpty(Level world, BlockPos pos) {
        Block b = world.getBlockState(pos).getBlock();
        return b.defaultBlockState().isAir();
    }

    public static double getAmbientPressureAt(Level world, BlockPos pos, boolean checkLiquidColumn) {
        double Pamb = 101.3;
        if (world.dimension() == Level.NETHER) {
            Pamb = 20000;
        } else if (world.dimension() == Level.OVERWORLD)
            Pamb = 24; //0.2atm
        if (world.isThundering())
            Pamb -= 5;
        else if (world.isRaining())
            Pamb -= 2.5;

        /*if (pos.getY() < 30 && world.getWorldInfo().getTerrainType() != LevelType.FLAT) {
            double f = (30 - pos.getY()) / 30D;
            Pamb *= 1 + 0.5 * f;
        }
        if (pos.getY() > 64 && world.getWorldInfo().getTerrainType() == LevelType.FLAT) {
            double f = (pos.getY() - 64) / (128D - 64D);
            Pamb *= 1 - 0.2 * f;
        }*/
        if (pos.getY() > 128) {
            double f = (pos.getY() - 128) / (192D - 128D);
            Pamb *= 1 - 0.2 * f;
        }
        if (pos.getY() > 192) {
            double f = (pos.getY() - 128) / (256D - 192D);
            Pamb *= 1 - 0.4 * f;
        }
        //todo new world height is 319
        if (pos.getY() > 319) {
            double f = (pos.getY() - 128) / (256D - 319D);
            Pamb *= 1 - 0.4 * f;
        }
        /*Pamb *= AtmosphereHandler.getAtmoDensity(world); //higher for thin atmo
        if (checkLiquidColumn) {
            double fluid = getFluidColumnPressure(world, pos.above());
            fluid *= 1 + PlanetDimensionHandler.getExtraGravity(world) - 0.03125;
            Pamb += fluid;
        }*/
        return Pamb;
    }


    /**
     * Radius is in CHUNKS!
     */
    public static boolean isRadiusLoaded(Level world, BlockPos pos, int radius) {
        int x0 = (pos.getX() >> 4) - radius;
        int x1 = (pos.getX() >> 4) + radius;
        int z0 = (pos.getZ() >> 4) - radius;
        int z1 = (pos.getZ() >> 4) + radius;
        for (int dx = x0; dx <= x1; dx++) {
            for (int dz = z0; dz <= z1; dz++) {
                if (!world.hasChunk(dx, dz)) {
//    todo log spam                DragonAPI.LOGGER.info("Chunk not loaded: " + dx + ", " + dz);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Takes a specified amount of XP and splits it randomly among a bunch of orbs.
     * Args: World, x, y, z, amount
     */
    public static void splitAndSpawnXP(Level world, double x, double y, double z, int xp) {
        splitAndSpawnXP(world, x, y, z, xp, 6000);
    }

    /**
     * Takes a specified amount of XP and splits it randomly among a bunch of orbs.
     * Args: World, x, y, z, amount, life
     */
    public static void splitAndSpawnXP(Level world, double x, double y, double z, int xp, int life) {
        int max = xp / 5 + 1;

        while (xp > 0) {
            int value = rand.nextInt(max) + 1;
            while (value > xp)
                value = rand.nextInt(max) + 1;
            xp -= value;
            ExperienceOrb orb = new ExperienceOrb(world, x, y, z, value);
            orb.setDeltaMovement(-0.2 + 0.4 * rand.nextFloat(), 0.3 * rand.nextFloat(), -0.2 + 0.4 * rand.nextFloat());
            CompoundTag nbt = new CompoundTag();
            orb.addAdditionalSaveData(nbt);
            nbt.putInt("Age", 6000 - life);
            orb.readAdditionalSaveData(nbt);
            if (!world.isClientSide) {
//                orb.velocityChanged = true; no longer a thing
                world.addFreshEntity(orb);
            }
        }
    }

    public static WorldID getCurrentWorldID(Level world) {
        if (world.isClientSide())
            throw new MisuseException("This cannot be called from the client side!");
        String f = getWorldKey(world);
        WorldID get = worldIDMap.get(f);
        if (get == null) {
            get = calculateWorldID(world);
            worldIDMap.put(f, get);
        }
        return get;
    }

    private static String getWorldKey(Level world) {
        File f = world.getServer().getServerDirectory();
        return ReikaFileReader.getRealPath(f);// return ReikaFileReader.getRelativePath(DragonAPI.getMinecraftDirectory(), f);
    }

    private static WorldID calculateWorldID(Level world) {
        File f = new File(getWorldMetadataFolder(world), "worldID.dat");
        if (!f.exists())
            return WorldID.NONEXISTENT;
        return WorldID.readFile(f);
        //String name = ReikaFileReader.getFileNameNoExtension(f);
        //return ReikaJavaLibrary.safeLongParse(name);
    }

    private static File getWorldMetadataFolder(Level world) {
        if (world.isClientSide())
            throw new MisuseException("This cannot be called from the client side!");
        File ret = new File(world.getServer().getServerDirectory(), "DragonAPI_Data");
        ret.mkdirs();
        return ret;
    }

    public static void onWorldCreation(Level world) {
        if (world.isClientSide())
            throw new MisuseException("This cannot be called from the client side!");
        File folder = getWorldMetadataFolder(world);
        File f = new File(folder, "worldID.dat");
        try {
            f.createNewFile();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        WorldID id = new WorldID(world);
        id.writeToFile(f);
    }

    /**
     * Performs machine overheat effects (primarily intended for RotaryCraft).
     * Args: World, x, y, z, item drop id, item drop metadata, min drops, max drops,
     * spark particles yes/no, number-of-sparks multiplier (default 20-40),
     * flaming explosion yes/no, smoking explosion yes/no, explosion force (0 for none)
     */
    public static void overheat(Level world, int x, int y, int z, ItemStack drop, int mindrops, int maxdrops,
                                boolean sparks, float sparkmultiplier, boolean flaming, boolean smoke, float force) {
        world.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 1);
        if (force > 0 && !world.isClientSide()) {
            if (flaming)
                world.explode(null, x, y, z, force, true, Level.ExplosionInteraction.BLOCK);// DESTROY was smoke
            else
                world.explode(null, x, y, z, force, Level.ExplosionInteraction.BLOCK);//DESTROY was smoke
        }
        int numsparks = rand.nextInt(20) + 20;
        numsparks *= sparkmultiplier;
        if (sparks)
            for (int i = 0; i < numsparks; i++)
                world.addParticle(ParticleTypes.LAVA, x + rand.nextFloat(), y + 1, z + rand.nextFloat(), 0, 0, 0);
        if (drop != null) {
            ItemStack scrap = drop.copy();
            int numdrops = rand.nextInt(1 + maxdrops - mindrops) + mindrops;
            if (!world.isClientSide()) {
                for (int i = 0; i < numdrops; i++) {
                    ItemEntity ent = new ItemEntity(world, x + rand.nextFloat(), y + 0.5, z + rand.nextFloat(), scrap);
                    ent.setDeltaMovement(-0.2 + 0.4 * rand.nextFloat(), 0.5 * rand.nextFloat(), -0.2 + 0.4 * rand.nextFloat());
                    world.addFreshEntity(ent);
                    ent.hurtMarked = true;
                }
            }
        }
    }

    public static FluidState getFluidState(BlockGetter world, BlockPos pos) {
        return world.getFluidState(pos);
    }

    public static FluidState getFluidState(BlockGetter world, int x, int y, int z) {
        return getFluidState(world, new BlockPos(x, y, z));
    }

    public static LivingEntity getClosestLivingEntity(Level world, BlockPos pos, AABB box) {
        List<LivingEntity> li = world.getEntitiesOfClass(LivingEntity.class, box);
        double d = Double.MAX_VALUE;
        LivingEntity index = null;
        for (LivingEntity e : li) {
            if (!e.isDeadOrDying() && e.getHealth() > 0) {
                double dd = ReikaMathLibrary.py3d(e.getX() - pos.getX(), e.getY() - pos.getY(), e.getZ() - pos.getZ());
                if (dd < d) {
                    index = e;
                    d = dd;
                }
            }
        }
        return index;
    }

    public static LivingEntity getClosestLivingEntityNoPlayers(Level world, BlockPos pos, AABB box,
                                                               boolean excludeCreativeOnly) {
        List<LivingEntity> li = world.getEntitiesOfClass(LivingEntity.class, box);
        double d = Double.MAX_VALUE;
        LivingEntity index = null;
        for (LivingEntity e : li) {
            if (!(e instanceof Player) || (excludeCreativeOnly && !((Player) e).isCreative())) {
                if (!e.isDeadOrDying() && e.getHealth() > 0) {
                    double dd = ReikaMathLibrary.py3d(e.getX() - pos.getX(), e.getY() - pos.getY(), e.getZ() - pos.getZ());
                    if (dd < d) {
                        index = e;
                        d = dd;
                    }
                }
            }
        }
        return index;
    }

    /**
     * Sets the biome type at an xz column. Args: World, x, z, biome
     */
    public static void setBiomeForXZ(Level world, int x, int z, Biome biome) { //todo this doesnt work at all right now, pls fix xoxoxo
        ChunkAccess ch = world.getChunk(x, z);

        int ax = x - ch.getPos().x * 16;
        int az = z - ch.getPos().z * 16;

//        ResourceKey<Biome>[] biomes = ch.getBiomeArray();
        int index = az * 16 + ax;
//        if (index < 0 || index >= biomes.length) {
//            DragonAPI.LOGGER.error("BIOME CHANGE ERROR: " + x + "&" + z + " @ " + ch.getPos().x + "&" + ch.getPos().z + ": " + ax + "%" + az + " -> " + index, Dist.DEDICATED_SERVER);
//            return;
//        }
//        biomes[index] = biome;
//        ch.setBiomeArray(biomes);
        ch.setUnsaved(true);//todo check if this is setChunkModified();
        for (int i = 0; i < 256; i++)
            temperatureEnvironment(world, new BlockPos(x, i, z), (int) ReikaBiomeHelper.getBiomeTemp(biome));

        if (!world.isClientSide()) {
            int packet = APIPacketHandler.PacketIDs.BIOMECHANGE.ordinal();
//     todo       ReikaPacketHelper.sendDataPacketWithRadius(DragonAPI.packetChannel, packet, world, x, 0, z, 1024, biome);
        }
    }

    public static boolean isBlockSurroundedBySolid(Level world, BlockPos pos, boolean vertical) {
        for (int i = vertical ? 0 : 2; i < 6; i++) {
            Direction dir = Direction.values()[i];
            int dx = pos.getX() + dir.getStepX();
            int dy = pos.getY() + dir.getStepY();
            int dz = pos.getZ() + dir.getStepZ();
            BlockState id = world.getBlockState(new BlockPos(dx, dy, dz));
            if (!id.isSolid())
                return false;
        }
        return true;
    }

    public static FluidStack getDrainableFluid(Level world, BlockPos pos) {
        BlockState b = world.getBlockState(pos);
        if (b instanceof IFluidBlock) {
            Fluid f = ((IFluidBlock) b).getFluid();
            if (f == null) {
                DragonAPI.LOGGER.error("Found a fluid block " + b + ":" + b.getBlock().getName() + " with a null fluid @ " + pos + "!");
                return null;
            }
            return ((IFluidBlock) b).drain(world, pos, IFluidHandler.FluidAction.EXECUTE);
        } else if (b.getBlock() instanceof LiquidBlock) {
            Fluid f = ReikaFluidHelper.lookupFluidForBlock(b);
            return f != null ? new FluidStack(f, FluidType.BUCKET_VOLUME) : null;
        } else {
            return null;
        }
    }

    /**
     * Returns whether there is a BlockEntity at the specified position. Does not call getBlockEntity().
     */
    public static boolean tileExistsAt(Level world, BlockPos pos) {
        Block b = world.getBlockState(pos).getBlock();
        if (b == Blocks.AIR)
            return false;
        return b != null;
    }

    /**
     * Returns true if a block can see an point. Args: World, block x,y,z, Point x,y,z, Max Range
     */
    public static boolean canBlockSee(Level world, int x, int y, int z, double x0, double y0, double z0, double range) {
        Block locid = world.getBlockState(new BlockPos(x, y, z)).getBlock();
        range += 2;
        for (int k = 0; k < 10; k++) {
            float a = 0;
            float b = 0;
            float c = 0;
            switch (k) {
                case 1 -> a = 1;
                case 2 -> b = 1;
                case 3 -> {
                    a = 1;
                    b = 1;
                }
                case 4 -> c = 1;
                case 5 -> {
                    a = 1;
                    c = 1;
                }
                case 6 -> {
                    b = 1;
                    c = 1;
                }
                case 7 -> {
                    a = 1;
                    b = 1;
                    c = 1;
                }
                case 8 -> {
                    a = 0.5F;
                    b = 0.5F;
                    c = 0.5F;
                }
                case 9 -> b = 0.5F;
            }
            for (float i = 0; i <= range; i += 0.25) {
                Vec3 vec2 = ReikaVectorHelper.getVec2Pt(x0, y0, z0, x + a, y + b, z + c).normalize();
                vec2 = ReikaVectorHelper.scaleVector(vec2, i);
                vec2 = vec2.add(x0, y0, z0);

                //ReikaColorAPI.write(String.format("%f -->  %.3f,  %.3f, %.3f", i, vec2.xCoord, vec2.yCoord, vec2.zCoord));
                int dx = Mth.floor(vec2.x);
                int dy = Mth.floor(vec2.y);
                int dz = Mth.floor(vec2.z);
                Block id = world.getBlockState(new BlockPos(dx, dy, dz)).getBlock();
                if (dx == x && dy == y && dz == z) {
                    //ReikaColorAPI.writeCoords(world, (int)vec2.xCoord, (int)vec2.yCoord, (int)vec2.zCoord);
                    return true;
                } else if (id != locid && ReikaBlockHelper.isCollideable(world, new BlockPos(dx, dy, dz)) && !softBlocks(world, new BlockPos(dx, dy, dz))) {
                    i = (float) (range + 1); //Hard loop break
                }
            }
        }
        return false;
    }

    public static final class WorldID {

        private static final WorldID NONEXISTENT = new WorldID(0, 0, 0, "[NONEXISTENT]", "[NONEXISTENT]", new HashSet());
        private static int worldsThisSession = 0;
        public final long worldCreationTime;
        public final long sourceSessionStartTime;
        public final long sessionWorldIndex;
        public final String originalFolder;
        public final String creatingPlayer;

        private final HashSet<String> modList;

        private WorldID(Level world) {
            this(System.currentTimeMillis(), DragonAPI.getLaunchTime(), worldsThisSession, ReikaFileReader.getRealPath(world.getServer().getServerDirectory()), getSessionName(), getModList());
            worldsThisSession++;
        }

        private WorldID(long time, long session, int index, String folder, String player, HashSet<String> modlist) {
            worldCreationTime = time;
            sourceSessionStartTime = session;
            sessionWorldIndex = index;
            originalFolder = folder;
            creatingPlayer = player;
            modList = modlist;
        }

        private static HashSet<String> getModList() {
            HashSet<String> ret = new HashSet<>();
            for (ModInfo mc : FMLLoader.getLoadingModList().getMods()) {
                ret.add(mc.getModId());
            }
            return ret;
        }

        private static String getSessionName() {
            return DragonAPI.getLaunchingPlayer().getName();
        }

        private static WorldID readFile(File f) {
            try (FileInputStream in = new FileInputStream(f)) {
                CompoundTag data = NbtIo.readCompressed(in);
                long c = data.getLong("creationTime");
                long s = data.getLong("sourceSession");
                String folder = data.getString("originalFolder");
                String player = data.getString("creatingPlayer");
                HashSet<String> modlist = new HashSet<>();
                ListTag li = data.getList("mods", Tag.TAG_STRING);
                for (Object o : li) {
                    modlist.add((String) o);
                }
                return new WorldID(c, s, data.getInt("sessionIndex"), folder, player, modlist);
            } catch (Exception e) {
                e.printStackTrace();
                return NONEXISTENT;
            }
        }

        public boolean isValid() {
            return worldCreationTime > 0 && !originalFolder.equals("[NONEXISTENT]");
        }

        private void writeToFile(File f) {
            CompoundTag data = new CompoundTag();
            data.putLong("creationTime", worldCreationTime);
            data.putLong("sourceSession", sourceSessionStartTime);
            data.putLong("sessionIndex", sessionWorldIndex);
            data.putString("originalFolder", originalFolder);
            data.putString("creatingPlayer", creatingPlayer);
            try (FileOutputStream out = new FileOutputStream(f)) {
                NbtIo.writeCompressed(data, out);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
