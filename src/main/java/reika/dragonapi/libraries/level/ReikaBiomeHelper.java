package reika.dragonapi.libraries.level;


import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;
import reika.dragonapi.instantiable.data.maps.MultiMap;
import reika.dragonapi.interfaces.registry.TreeType;
import reika.dragonapi.libraries.java.ReikaStringParser;
import reika.dragonapi.libraries.registry.ReikaTreeHelper;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

import java.util.*;

public class ReikaBiomeHelper {

    public static final Comparator<Biome> biomeNameSorter = new Comparator<Biome>() {

        @Override
        public int compare(Biome o1, Biome o2) {
            return ForgeRegistries.BIOMES.getKey(o1).getNamespace().compareTo(ForgeRegistries.BIOMES.getKey(o2).getNamespace());
        }

    };
    private static final MultiMap<ResourceKey<Biome>, ResourceKey<Biome>> children = new MultiMap<>();
    private static final MultiMap<ResourceKey<Biome>, ResourceKey<Biome>> similarity = new MultiMap<>();
    private static final HashMap<ResourceKey<Biome>, ResourceKey<Biome>> parents = new HashMap<>();
    //    private static final ResourceKey<Biome> biomeColors = new int[40];
    private static final HashMap<String, Biome> nameMap = new HashMap<>();
    private static final HashMap<ResourceKey<Biome>, BiomeTemperatures> temperatures = new HashMap<>();
    private static final HashMap<ResourceKey<Biome>, TreeType> biomeTrees = new HashMap<>();

    static {
//        addChildBiome(Biomes.DESERT, Biomes.DESERTHILLS);

        addChildBiome(Biomes.FOREST, Biomes.WINDSWEPT_FOREST);

        addChildBiome(Biomes.TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA);
        addChildBiome(Biomes.TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA);
        addChildBiome(Biomes.TAIGA, Biomes.SNOWY_TAIGA, false);
        addChildBiome(Biomes.TAIGA, Biomes.SNOWY_TAIGA, false);

//        addChildBiome(Biomes.JUNGLE, Biomes.JUNGLEHILLS);
//        addChildBiome(Biomes.JUNGLE, Biomes.JUNGLEEDGE);
//
//        addChildBiome(Biomes.MEGA_TAIGA, Biomes.MEGATAIGAHILLS);
//
//        addChildBiome(Biomes.COLD_TAIGA, Biomes.COLDTAIGAHILLS);
//
//        addChildBiome(Biomes.ICE_PLAINS, Biomes.ICEMOUNTAINS);
//
//        addChildBiome(Biomes.BIRCH_FOREST, Biomes.BIRCHFORESTHILLS);
//
//        addChildBiome(Biomes.EXTREMEHILLS, Biomes.EXTREMEHILLSEDGE);
//        addChildBiome(Biomes.EXTREMEHILLS, Biomes.EXTREMEHILLSPLUS);
//
//        addChildBiome(Biomes.MUSHROOM_FIELDS, Biomes.MUSHROOMISLANDSHORE);

        addChildBiome(Biomes.BADLANDS, Biomes.ERODED_BADLANDS);
        addChildBiome(Biomes.BADLANDS, Biomes.WOODED_BADLANDS);

        addChildBiome(Biomes.BEACH, Biomes.SNOWY_BEACH, false);

        addChildBiome(Biomes.OCEAN, Biomes.DEEP_OCEAN, false);
        addChildBiome(Biomes.OCEAN, Biomes.FROZEN_OCEAN, false);

        addChildBiome(Biomes.RIVER, Biomes.FROZEN_RIVER, false);

        addChildBiome(Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU);

//        biomeColors[Biomes.OCEAN] = 0x0000FF;
//        biomeColors[Biomes.DEEP_OCEAN.biomeID] = 0x0000B0;
//        biomeColors[Biomes.RIVER.biomeID] = 0x005AFF;
//        biomeColors[Biomes.FROZENOCEAN.biomeID] = 0x0094FF;
//        biomeColors[Biomes.FROZENRIVER.biomeID] = 0x00C6FF;
//
//        biomeColors[Biomes.ICEPLAINS.biomeID] = 0x608772;
//        biomeColors[Biomes.PLAINS.biomeID] = 0x6B8C42;
//        biomeColors[Biomes.TAIGA.biomeID] = 0x62886F;
//        biomeColors[Biomes.SWAMPLAND.biomeID] = 0x444E3A;
//        biomeColors[Biomes.EXTREMEHILLS.biomeID] = 0x668766;
//        biomeColors[Biomes.JUNGLE.biomeID] = 0x3E9829;
//        biomeColors[Biomes.FOREST.biomeID] = 0x5B9144;
//        biomeColors[Biomes.SAVANNA.biomeID] = 0xBCB555;
//        biomeColors[Biomes.BIRCHFOREST.biomeID] = 0x78BC63;
//        biomeColors[Biomes.ROOFEDFOREST.biomeID] = 0x387F24;
//
//        biomeColors[Biomes.DESERT.biomeID] = 0xEEE7B1;
//        biomeColors[Biomes.MUSHROOMISLAND.biomeID] = 0x726D81;
//        biomeColors[Biomes.MESA.biomeID] = 0x9C6247;
//        biomeColors[Biomes.BEACH.biomeID] = 0xEDE28E;
//        biomeColors[Biomes.STONEBEACH.biomeID] = 0x949494;
//        biomeColors[Biomes.COLDBEACH.biomeID] = 0xACB6D3;
//
//        biomeColors[Biomes.MEGATAIGA.biomeID] = 0x62886A;
//        biomeColors[Biomes.MEGATAIGAHILLS.biomeID] = 0x82B28B;
//        biomeColors[Biomes.COLDTAIGA.biomeID] = 0x628878;
//        biomeColors[Biomes.COLDTAIGAHILLS.biomeID] = 0x82B29D;
//        biomeColors[Biomes.SAVANNAPLATEAU.biomeID] = 0xD3CA61;
//        biomeColors[Biomes.ICE_SPIKES.biomeID] = 0x80B297;
//        biomeColors[Biomes.MUSHROOM_FIELDS.biomeID] = 0x726D96;
//
//        biomeColors[Biomes.MESAPLATEAU.biomeID] = 0xCC805D;
//        biomeColors[Biomes.MESAPLATEAU_F.biomeID] = 0xFF9E75;
//
//        biomeColors[Biomes.HELL.biomeID] = 0x8F5353;
//        biomeColors[Biomes.SKY.biomeID] = 0xD6D99B;

        temperatures.put(Biomes.TAIGA, BiomeTemperatures.COOL);

        temperatures.put(Biomes.FOREST, BiomeTemperatures.TEMPERATE);
        temperatures.put(Biomes.PLAINS, BiomeTemperatures.TEMPERATE);
        temperatures.put(Biomes.BIRCH_FOREST, BiomeTemperatures.TEMPERATE);
        temperatures.put(Biomes.OCEAN, BiomeTemperatures.TEMPERATE);
        temperatures.put(Biomes.DEEP_OCEAN, BiomeTemperatures.TEMPERATE);
        temperatures.put(Biomes.MUSHROOM_FIELDS, BiomeTemperatures.TEMPERATE);
        temperatures.put(Biomes.SWAMP, BiomeTemperatures.TEMPERATE);
        temperatures.put(Biomes.RIVER, BiomeTemperatures.TEMPERATE);

        temperatures.put(Biomes.DARK_FOREST, BiomeTemperatures.WARM);
        temperatures.put(Biomes.SAVANNA, BiomeTemperatures.WARM);

        temperatures.put(Biomes.DESERT, BiomeTemperatures.HOT);
        temperatures.put(Biomes.BADLANDS, BiomeTemperatures.HOT);
        temperatures.put(Biomes.JUNGLE, BiomeTemperatures.HOT);

        temperatures.put(Biomes.SNOWY_TAIGA, BiomeTemperatures.ICY);
        temperatures.put(Biomes.SNOWY_BEACH, BiomeTemperatures.ICY);
        temperatures.put(Biomes.ICE_SPIKES, BiomeTemperatures.ICY);
        temperatures.put(Biomes.FROZEN_OCEAN, BiomeTemperatures.ICY);
        temperatures.put(Biomes.FROZEN_RIVER, BiomeTemperatures.ICY);
        temperatures.put(Biomes.FROZEN_PEAKS, BiomeTemperatures.ICY);
        temperatures.put(Biomes.DEEP_FROZEN_OCEAN, BiomeTemperatures.ICY);


        temperatures.put(Biomes.NETHER_WASTES, BiomeTemperatures.FIERY);

        temperatures.put(Biomes.THE_END, BiomeTemperatures.LUNAR);

        biomeTrees.put(Biomes.SNOWY_TAIGA, ReikaTreeHelper.SPRUCE);
        biomeTrees.put(Biomes.TAIGA, ReikaTreeHelper.SPRUCE);
        biomeTrees.put(Biomes.OLD_GROWTH_PINE_TAIGA, ReikaTreeHelper.SPRUCE);
        biomeTrees.put(Biomes.OLD_GROWTH_SPRUCE_TAIGA, ReikaTreeHelper.SPRUCE);

        biomeTrees.put(Biomes.OCEAN, ReikaTreeHelper.OAK);
        biomeTrees.put(Biomes.FOREST, ReikaTreeHelper.OAK);
        biomeTrees.put(Biomes.SWAMP, ReikaTreeHelper.OAK);
        biomeTrees.put(Biomes.RIVER, ReikaTreeHelper.OAK);
        biomeTrees.put(Biomes.ICE_SPIKES, ReikaTreeHelper.OAK);
        biomeTrees.put(Biomes.SNOWY_PLAINS, ReikaTreeHelper.OAK);

        biomeTrees.put(Biomes.ERODED_BADLANDS, ReikaTreeHelper.OAK);
        biomeTrees.put(Biomes.WOODED_BADLANDS, ReikaTreeHelper.OAK);
        biomeTrees.put(Biomes.BADLANDS, ReikaTreeHelper.OAK);

        biomeTrees.put(Biomes.BIRCH_FOREST, ReikaTreeHelper.BIRCH);

        biomeTrees.put(Biomes.JUNGLE, ReikaTreeHelper.JUNGLE);
        biomeTrees.put(Biomes.BAMBOO_JUNGLE, ReikaTreeHelper.JUNGLE);
        biomeTrees.put(Biomes.SPARSE_JUNGLE, ReikaTreeHelper.JUNGLE);

        biomeTrees.put(Biomes.SAVANNA, ReikaTreeHelper.ACACIA);
        biomeTrees.put(Biomes.SAVANNA_PLATEAU, ReikaTreeHelper.ACACIA);

        biomeTrees.put(Biomes.DARK_FOREST, ReikaTreeHelper.DARKOAK);

//        for (int i = 0; i < Biome.biomeList; i++) { //todo list of biomes
//            Biome b = Biome.biomeList[i];
//            if (b != null) {
//                nameMap.put(b.getRegistryName().toString(), b);
//            }
//        }
    }

    public static MultiMap<Integer, Integer> getBiomeHierearchy() {
        MultiMap<Integer, Integer> data = new MultiMap<>(MultiMap.CollectionType.LIST, TreeMap::new);

        /*for (Biome b : getAllBiomes()) {
            for (Biome b2 : getChildBiomes(b)) {
                data.addValue(b.biomeID, b2.biomeID);
            }
        }
        for (Biome b : getAllBiomes()) {
            Biome parent = getParentBiomeType(b, false);
            if (b != parent) {
                data.addValue(parent.biomeID, b.biomeID);
            }
        }
        for (Biome b : getAllBiomes()) {
            if (!data.containsKey(b.biomeID)) {
                data.put(b.biomeID, new ArrayList());
            }
        }*/

        HashSet<Integer> set = new HashSet<>(data.allValues(false));
        for (int id : set) {
            data.remove(id);
        }
        return data;
    }

    public static void addChildBiome(ResourceKey<Biome> parent, ResourceKey<Biome> child) {
        addChildBiome(parent, child, true);
    }

    private static void addChildBiome(ResourceKey<Biome> parent, ResourceKey<Biome> child, boolean isChild) {
        similarity.addValue(parent, child);
        if (isChild) {
            children.addValue(parent, child);
            parents.put(child, parent);
        }
    }

    /**
     * Note that this is affected by other mods, so exclusive calls on this will end up including mod biomes
     */
//    public static List<Biome> getAllBiomes() {
//        List<Biome> li = new ArrayList<Biome>();
//        for (int i = 0; i < Biome.biomeList.length; i++) {
//            if (Biome.biomeList[i] != null)
//                li.add(Biome.biomeList[i]);
//        }
//        return li;
//    }

    /**
     * Returns any associated biomes (eg Desert+DesertHills) to the one supplied. Args: Biome, Whether to match "loosely". Loose matching
     * is defined as similarity between two biomes where one is not a parent of the other but both are similar in nature, eg Taiga+Cold Taiga
     */
    public static Collection<ResourceKey<Biome>> getAllAssociatedBiomes(ResourceKey<Biome> biome, boolean loose) {
        return Collections.unmodifiableCollection(loose ? similarity.get(biome) : children.get(biome));
    }

    public static Collection<ResourceKey<Biome>> getChildBiomes(ResourceKey<Biome> biome) {
        return getAllAssociatedBiomes(biome, false);
    }

    // Returns the biome's parent. Args: ResourceKey<Biome> biome
    public static ResourceKey<Biome> getParentBiomeType(ResourceKey<Biome> biome, boolean onlyDirect) {
        ResourceKey<Biome> b = onlyDirect ? null : parents.get(biome);
        if (b != null)
            return b;
//        if (biome instanceof BiomeGenMutated && ((BiomeGenMutated)biome).baseBiome != null) {
//            parents.put(biome, ((BiomeGenMutated)biome).baseBiome);
//            biome = ((BiomeGenMutated)biome).baseBiome;
//        }
        return biome;
    }

    /**
     * Returns whether the biome is a variant of a parent. Args: Biome
     */
    public static boolean isChildBiome(ResourceKey<Biome> biome) {
        return parents.containsKey(biome);
    }

    /**
     * Converts the given coordinates to an RGB representation of those coordinates' biome's color, for the given material type.
     * Args: World, x, z, material (String)
     */
    public static int[] biomeToRGB(Level world, BlockPos pos, BlockBehaviour.Properties properties) {
        int color = biomeToHex(world, pos, properties);
        return ReikaColorAPI.HexToRGB(color);
    }

    /**
     * Converts the given coordinates to a hex representation of those coordinates' biome's color, for the given material type.
     * Args: World, x, z, material (String)
     */
    public static int biomeToHexColor(Level world, BlockPos pos, BlockBehaviour.Properties properties) {
        int color = biomeToHex(world, pos, properties);
        return color;
    }

    private static int biomeToHex(Level world, BlockPos pos, BlockBehaviour.Properties properties) {
        Biome biome = world.getBiomeManager().getBiome(pos).value();
        int color = 0;
      /*  if (mat == Material.WATER)
            color = biome.getFoliageColor();
        if (mat == Material.GRASS)
            color = biome.getGrassColor(pos.getX(), pos.getZ());
        if (mat == Material.WATER)
            color = biome.getWaterColor();
        if (mat == Material.AIR)
            color = biome.getSkyColor();//(biome.getFloatTemperature(pos));*/
        return color;
    }

    /**
     * Returns true if the passed biome is a cool but not cold biome.  Args: Biome
     */
    public static boolean isCoolBiome(ResourceKey<Biome> biome) {
        return biome == Biomes.TAIGA;
//        if (level.registryAccess().registryOrThrow(Registries.BIOME).getKey(biome).getNamespace().toString().toLowerCase(Locale.ENGLISH).contains("maple woods"))
//            return true;
        /*BiomeDictionary.Type[] types = BiomeDictionary.getTypes(biome).toArray(new BiomeDictionary.Type[0]);
        for (int i = 0; i < types.length; i++) {

        }*/
    }

    /**
     * Returns true if the passed biome is a snow biome.  Args: Biome
     */
    public static boolean isSnowBiome(ResourceKey<Biome> biome) {
        if (biome == Biomes.FROZEN_OCEAN)
            return true;
        if (biome == Biomes.FROZEN_RIVER)
            return true;
        if (biome == Biomes.SNOWY_BEACH)
            return true;
        if (biome == Biomes.SNOWY_PLAINS)
            return true;
        if (biome == Biomes.SNOWY_SLOPES)
            return true;
        if (biome == Biomes.SNOWY_TAIGA)
            return true;
        return biome == Biomes.ICE_SPIKES;
  /*todo       if (level.registryAccess().registryOrThrow(Registries.BIOME).getHolder(biome).getNamespace().toString().toLowerCase(Locale.ENGLISH).contains("maple woods")) //I do NOT live in the Arctic
            return false;
//        if (biome.getEnableSnow())
//            return true;
        if (level.registryAccess().registryOrThrow(Registries.BIOME).getHolder(biome).getNamespace().toString().toLowerCase(Locale.ENGLISH).contains("arctic"))
            return true;
        if (level.registryAccess().registryOrThrow(Registries.BIOME).getHolder(biome).getNamespace().toString().toLowerCase(Locale.ENGLISH).contains("tundra"))
            return true;
        if (level.registryAccess().registryOrThrow(Registries.BIOME).getHolder(biome).getNamespace().toString().toLowerCase(Locale.ENGLISH).contains("alpine"))
            return true;
        if (level.registryAccess().registryOrThrow(Registries.BIOME).getHolder(biome).getNamespace().toString().toLowerCase(Locale.ENGLISH).contains("frozen"))
            return true;
        Set<BiomeDictionary.Type> types = BiomeDictionary.getTypes(biome);
        for (int i = 0; i < types.size(); i++) {
//            if (types[i] == BiomeDictionary.Type.FROZEN)
//                return true;
//            if (types[i] == BiomeDictionary.Type.COLD)
//                return true;
//            if (types[i] == BiomeDictionary.Type.SNOWY)
//                return true;
        }*/
    }

    /**
     * Returns true if the passed biome is a hot biome.  Args: Biome
     */
    public static boolean isHotBiome(ResourceKey<Biome> biome) {
        if (biome == Biomes.DESERT)
            return true;
        if (biome == Biomes.NETHER_WASTES)
            return true;
        if (biome == Biomes.JUNGLE)
            return true;
        return biome == Biomes.BADLANDS;
        /* todo TagKey<Biome>[] types = (TagKey<Biome>[]) level.registryAccess().registryOrThrow(Registries.BIOME).getHolder(biome).get().getTagKeys().toArray();//todo might be casting wrong idk
        for (TagKey<Biome>[] type : types) {
            if (type == BiomeTags.DESERT_LEGACY)
                return true;
            if (type == BiomeTags.DESERT)
                return true;
            if (type == BiomeTags.WARM)
                return true;
        }*/
    }

    /**
     * Returns a broad-stroke biome temperature in degrees centigrade.
     * Args: biome
     */
    public static int getBiomeTemp(LevelAccessor world, ResourceKey<Biome> biome) {
        biome = getParentBiomeType(biome, false);
        BiomeTemperatures temp = temperatures.get(biome);
        if (temp == null) {
            temp = calcBiomeTemp(world, biome);
            temperatures.put(biome, temp);
        }

        int Tamb = temp.ambientTemperature;

//        if (ModSeasonHandler.isLoaded()) { //account for seasons
//            Tamb += getBiomeSeasonStrength(biome, temp)*ModSeasonHandler.getSeasonTemperatureModifier(world);
//        }

        return Tamb;
    }

    public static float getBiomeTemp(Biome biome) {
        return biome.getBaseTemperature();//.getModifiedClimateSettings().temperature();
    }

    private static float getBiomeSeasonStrength(ResourceKey<Biome> biome, BiomeTemperatures temp) {
        if (temp == BiomeTemperatures.FIERY || temp == BiomeTemperatures.LUNAR)
            return 0;
       /* todo if (BiomeManager.hasType(biome, Tags.Biomes.IS_SANDY))
            return 1.5F;
        if (BiomeManager.hasType(biome, BiomeTags.IS_SAVANNA))
            return 1.25F;
        if (BiomeManager.hasType(biome, BiomeTags.IS_JUNGLE))
            return 0.4F;*/
        if (temp == BiomeTemperatures.HOT || temp == BiomeTemperatures.ICY)
            return 0.2F;
        if (temp == BiomeTemperatures.COOL || temp == BiomeTemperatures.WARM)
            return 0.75F;
        if (temp == BiomeTemperatures.TEMPERATE)
            return 1F;
        return 1;
    }

    private static BiomeTemperatures calcBiomeTemp(LevelAccessor level, ResourceKey<Biome> biome) {
        if (biome == Biomes.NETHER_WASTES)
            return BiomeTemperatures.FIERY;
        else if (biome == Biomes.THE_END)
            return BiomeTemperatures.LUNAR;
        var holder = level.registryAccess().registryOrThrow(Registries.BIOME).getHolder(biome);
        if (holder.isPresent()) {
            List<TagKey<Biome>> types = holder.get().tags().toList();//todo might be broken
            for (TagKey<Biome> type : types) {
                if (type == BiomeTags.IS_NETHER)
                    return BiomeTemperatures.FIERY;
                else if (type == BiomeTags.IS_END)
                    return BiomeTemperatures.LUNAR;
            }
        } else {
            return BiomeTemperatures.TEMPERATE; // Returns temperate if the biome holder is not present
        }

        if (isSnowBiome(biome))
            return BiomeTemperatures.ICY;
        else if (isHotBiome(biome))
            return BiomeTemperatures.HOT;
        else if (isCoolBiome(biome))
            return BiomeTemperatures.COOL;
        else
            return BiomeTemperatures.TEMPERATE;

    }

    /**
     * Returns a broad-stroke biome temperature in degrees centigrade.
     * Args: World, x, z
     */
    public static int getBiomeTemp(LevelAccessor world, BlockPos pos) {
        ResourceKey<Biome> biome = world.getBiomeManager().getBiome(pos).unwrapKey().get(); //todo check if this isnt null
        return getBiomeTemp(world, biome);
    }

    public static float getBiomeHumidity(LevelAccessor level, ResourceKey<Biome> biome) {
        biome = getParentBiomeType(biome, false);
        if (biome == Biomes.JUNGLE)
            return 0.95F;
        if (biome == Biomes.OCEAN)
            return 1F;
        if (biome == Biomes.DEEP_OCEAN)
            return 1F;
        if (biome == Biomes.SWAMP)
            return 0.85F;
        if (biome == Biomes.FOREST)
            return 0.6F;
        if (biome == Biomes.BIRCH_FOREST)
            return 0.55F;
        if (biome == Biomes.DARK_FOREST)
            return 0.7F;
        if (biome == Biomes.PLAINS)
            return 0.4F;
        if (biome == Biomes.SAVANNA)
            return 0.3F;
        if (biome == Biomes.DESERT)
            return 0.2F;
        if (biome == Biomes.BADLANDS)
            return 0.2F;
        if (biome == Biomes.NETHER_WASTES)
            return 0.1F;
        if (biome == Biomes.THE_END)
            return 0.1F;
        if (biome == Biomes.BEACH)
            return 0.98F;
        if (biome == Biomes.ICE_SPIKES)
            return 0.4F;
        if (biome == Biomes.MUSHROOM_FIELDS)
            return 0.75F;

        TagKey<Biome>[] types = (TagKey<Biome>[]) level.registryAccess().registryOrThrow(Registries.BIOME).getHolder(biome).get().getTagKeys().toArray();//todo might be casting wrong idk //BiomeDictionary.getTypes(biome).toArray(new BiomeDictionary.Type[0]);
        float val = 0.5F;
        for (TagKey<Biome> type : types) {
            if (type == BiomeTags.IS_BEACH)
                val = Math.max(val, 0.95F);
            if (type == BiomeTags.IS_OCEAN || type == BiomeTags.IS_RIVER) // || type == Tags.Biomes.WATER)
                val = Math.max(val, 1F);
            if (type == Tags.Biomes.IS_SWAMP || type == BiomeTags.IS_JUNGLE)
                val = Math.max(val, 0.95F);
            if (type == Tags.Biomes.IS_HOT || type == Tags.Biomes.IS_SANDY)
                val = Math.min(val, 0.2F);
            if (type == Tags.Biomes.IS_COLD_NETHER || type == Tags.Biomes.IS_WET_NETHER || type == Tags.Biomes.IS_HOT_NETHER || type == Tags.Biomes.IS_DRY_NETHER
                    || type == BiomeTags.IS_END || type == Tags.Biomes.IS_COLD_END || type == Tags.Biomes.IS_WET_END || type == Tags.Biomes.IS_HOT_END || type == Tags.Biomes.IS_DRY_END)
                val = Math.min(val, 0.1F);
            if (type == Tags.Biomes.IS_WASTELAND)
                val = Math.min(val, 0.1F);
            if (type == Tags.Biomes.IS_LUSH)
                val = Math.max(val, 0.6F);
            if (type == Tags.Biomes.IS_WET || type == Tags.Biomes.IS_WET_OVERWORLD || type == Tags.Biomes.IS_WET_NETHER || type == Tags.Biomes.IS_WET_END)
                val = Math.max(val, 0.7F);
            if (type == Tags.Biomes.IS_DRY || type == Tags.Biomes.IS_DRY_OVERWORLD || type == Tags.Biomes.IS_DRY_NETHER || type == Tags.Biomes.IS_DRY_END || type == BiomeTags.IS_SAVANNA)
                val = Math.min(val, 0.3F);
        }
        return val;
    }

    public static float getBiomeHumidity(LevelAccessor world, BlockPos pos) {
        return getBiomeHumidity(world, world.getBiomeManager().getBiome(pos).unwrapKey().get());
    }

    public static boolean isOcean(LevelAccessor level, ResourceKey<Biome> b) {
        if (b == Biomes.OCEAN || b == Biomes.DEEP_OCEAN || b == Biomes.FROZEN_OCEAN || b == Biomes.DEEP_FROZEN_OCEAN || b == Biomes.COLD_OCEAN
                || b == Biomes.DEEP_COLD_OCEAN || b == Biomes.WARM_OCEAN || b == Biomes.LUKEWARM_OCEAN || b == Biomes.DEEP_LUKEWARM_OCEAN)
            return true;

        if (level.registryAccess().registryOrThrow(Registries.BIOME).getHolder(b).get().is(BiomeTags.IS_OCEAN))
            return true;
        if (level.registryAccess().registryOrThrow(Registries.BIOME).getHolder(b).get().is(BiomeTags.IS_DEEP_OCEAN))
            return true;
        /*if (b == Biomes.OCEAN || b == Biomes.FROZEN_OCEAN || b == Biomes.DEEP_OCEAN)
            return true;
        if (BiomeDictionary.hasType(b, BiomeDictionary.Type.FOREST))
            return false;
        if (BiomeDictionary.hasType(b, BiomeDictionary.Type.DRY))
            return false;
        if (BiomeDictionary.hasType(b, BiomeDictionary.Type.DENSE))
            return false;
        if (BiomeDictionary.hasType(b, BiomeDictionary.Type.OCEAN))
            return true;*/
        return ReikaStringParser.containsWord(level.registryAccess().registry(Registries.BIOME).get().getKey(level.registryAccess().registry(Registries.BIOME).get().getHolder(b).get().value()).getNamespace().toLowerCase(Locale.ENGLISH), "ocean");
    }

    public static boolean isOcean(LevelAccessor level, BlockPos pos) {
        var biome = level.getBiome(pos).unwrapKey().get();
        if (biome == Biomes.OCEAN || biome == Biomes.DEEP_OCEAN || biome == Biomes.FROZEN_OCEAN || biome == Biomes.DEEP_FROZEN_OCEAN || biome == Biomes.COLD_OCEAN
                || biome == Biomes.DEEP_COLD_OCEAN || biome == Biomes.WARM_OCEAN || biome == Biomes.LUKEWARM_OCEAN || biome == Biomes.DEEP_LUKEWARM_OCEAN)
            return true;

        if (level.getBiome(pos).is(BiomeTags.IS_OCEAN))
            return true;
        if (level.getBiome(pos).is(BiomeTags.IS_DEEP_OCEAN))
            return true;
        /*if (biome == Biomes.OCEAN || biome == Biomes.FROZEN_OCEAN || biome == Biomes.DEEP_OCEAN)
            return true;
        if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.FOREST))
            return false;
        if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.DRY))
            return false;
        if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.DENSE))
            return false;
        if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN))
            return true;*/
        return ReikaStringParser.containsWord(level.registryAccess().registry(Registries.BIOME).get().getKey(level.getBiome(pos).value()).getNamespace().toLowerCase(Locale.ENGLISH), "ocean");
    }

//    public static int getBiomeUniqueColor(Biome b) {
//        return biomeColors[b.getFoliageColor()]; //todo biome colors
//    }

//    public static void removeBiomeWithAssociates(Biome biome) {
//        BiomeManager.removeSpawnBiome(biome);
//        Collection<Biomes> c = getChildBiomes(biome);
//        for (Biome b : c)
//            BiomeManager.removeSpawnBiome(b);
//    }

//    public static void removeAllBiomesBut(Collection<Biomes> biomes) {
//        for (int i = 0; i < Biomes.biomeList.length; i++) {
//            Biomes b = Biomes.biomeList[i];
//            if (!biomes.contains(b))
//                BiomeManager.removeSpawnBiome(b);
//        }
//    }

//    public static void removeAllBiomesBut(Biomes... biomes) {
//        removeAllBiomesBut(ReikaJavaLibrary.makeListFromArray(biomes));
//    }

//    public static void removeAllBiomesBut(Biome biome) {
//        for (int i = 0; i < Biomes.biomeList.length; i++) {
//            Biomes b = Biomes.biomeList[i];
//            if (b != biome)
//                BiomeManager.removeBiome(b, null);
//        }
//    }

//    public static int getBiomeNaturalColor(ResourceKey<Biome> b) {
//        BlockState top = b.topBlock;
//        if (BiomeDictionary.hasType(b, BiomeDictionary.Type.WATER))
//            top = Blocks.WATER.defaultBlockState();
//        int mat = top.getMaterial().getColor().col;
//        if (top == Blocks.GRASS.defaultBlockState()) {
//            mat = b.getBiomeGrassColor(0, 0, 0);
//        }
//        RGB rgb = new RGB(mat);
//        return rgb.getInt();
//    }

    public static Biome getBiomeByName(String s) {
        return nameMap.get(s);
    }

    public static boolean doesBiomeHavePrecipitation(Biome b, BlockPos pos) {
        return b.warmEnoughToRain(pos) || b.coldEnoughToSnow(pos);
    }

    public enum BiomeTemperatures {
        LUNAR(-100),
        ICY(-20),
        COOL(10),
        TEMPERATE(25),
        WARM(30),
        HOT(40),
        FIERY(300);

        public final int ambientTemperature;

        BiomeTemperatures(int t) {
            ambientTemperature = t;
        }
    }

}