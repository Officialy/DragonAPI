package reika.dragonapi.modregistry;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.RandomSource;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.DragonOptions;
import reika.dragonapi.ModList;
import reika.dragonapi.interfaces.registry.OreType;

import java.util.*;

public enum ModOreList implements OreType {

    TIN("Tin", 0xB2D5E9, OreRarity.COMMON, "ingotTin", 1, "oreTin", "oreCassiterite"),
    COPPER("Copper", 0xBC6C01, OreRarity.EVERYWHERE, "ingotCopper", 1, "oreCopper", "oreTetrahedrite"),
    LEAD("Lead", 0x697597, OreRarity.AVERAGE, "ingotLead", 1, "oreLead"),
    NICKEL("Nickel", 0xD0CCAD, OreRarity.SCATTERED, "ingotNickel", 1, "oreNickel", "orePentlandite"),
    SILVER("Silver", 0xA4D0DA, OreRarity.AVERAGE, "ingotSilver", 1, "oreSilver"),
    GALENA("Galena", 0x7F6E95, OreRarity.SCATTERED, "dustGalena", 1, "oreGalena"),
    ALUMINUM("Aluminum", 0xF1F1F1, OreRarity.COMMON, getAluminumName(), 1, "oreAluminum", "oreAluminium", "oreNaturalAluminum"), //...Why??
    IRIDIUM("Iridium", 0xC1E2D3, OreRarity.RARE, "ingotIridium", 1, "oreIridium"),
    FIRESTONE("Firestone", 0xE19636, OreRarity.RARE, "shardFirestone", 1, "oreFirestone"),
    CERTUSQUARTZ("Certus Quartz", 0xC4CEFF, OreRarity.AVERAGE, "crystalCertusQuartz", 2, "oreCertusQuartz"),
    URANIUM("Uranium", 0x4CFF00, OreRarity.SCATTERED, "ingotUranium", 1, "oreUranium", "oreYellorite", "oreUraninite"),
    CINNABAR("Mercury", 0x811A1A, OreRarity.SCATTERED, "itemQuicksilver", 1, "oreCinnabar"),
    AMBER("Amber", 0xB17F17, OreRarity.SCATTERED, "gemAmber", 1, "oreAmber"),
    INFUSEDAIR("Air Infused", 0xA88C32, OreRarity.SCATTERED, "shardAir", 2, "oreInfusedAir"),
    INFUSEDFIRE("Fire Infused", 0xB52100, OreRarity.SCATTERED, "shardFire", 2, "oreInfusedFire"),
    INFUSEDWATER("Water Infused", 0x007C99, OreRarity.AVERAGE, "shardWater", 2, "oreInfusedWater"),
    INFUSEDEARTH("Earth Infused", 0x008200, OreRarity.SCATTERED, "shardEarth", 2, "oreInfusedEarth"),
    INFUSEDENTROPY("Entropy Infused", 0x555576, OreRarity.SCARCE, "shardEntropy", 2, "oreInfusedEntropy"),
    INFUSEDORDER("Order Infused", 0xDCDCFF, OreRarity.SCARCE, "shardOrder", 2, "oreInfusedOrder"),
    APATITE("Apatite", 0x3296C5, OreRarity.COMMON, "gemApatite", 3, "oreApatite"),
    SALTPETER("Saltpeter", 0xFFFFFF, OreRarity.AVERAGE, "dustSaltpeter", 2, "oreSaltpeter"),
    TUNGSTEN("Tungsten", 0x1E1E1E, OreRarity.COMMON, "dustTungsten", 1, "oreTungsten", "oreTungstate"),
    NIKOLITE("Nikolite", 0x2DABB8, OreRarity.COMMON, "dustElectrotine", 5, "oreNikolite", "oreElectrotine"),
    PERIDOT("Peridot", 0x00C416, OreRarity.SCARCE, "gemPeridot", 1, "orePeridot"),
    RUBY("Ruby", 0xBC0000, OreRarity.SCARCE, "gemRuby", 1, "oreRuby"),
    SAPPHIRE("Sapphire", 0x0019AA, OreRarity.SCARCE, "gemSapphire", 1, "oreSapphire"),
    //AMETHYST("Amethyst", 0xFF72F3, OreRarity.RARE, "gemAmethyst", 1, "oreAmethyst"),
    MONAZIT("Monazit", 0x3C6E3C, OreRarity.AVERAGE, "ForciciumItem", 4, "MonazitOre"),
    FORCE("Force", 0xFFC700, OreRarity.AVERAGE, "gemForce", 3, "oreForce"),
    NETHERCOAL("Nether Coal", 0x262626, OreRarity.EVERYWHERE, "itemCoal", 1, "oreNetherCoal"),
    NETHERIRON("Nether Iron", 0xD8AD91, OreRarity.COMMON, "ingotIron", 1, "oreNetherIron"),
    NETHERGOLD("Nether Gold", 0xD6A400, OreRarity.AVERAGE, "ingotGold", 1, "oreNetherGold"),
    NETHERREDSTONE("Nether Redstone", 0xBC0000, OreRarity.COMMON, "dustRedstone", 4, "oreNetherRedstone"),
    NETHERLAPIS("Nether Lapis", 0x2B4BFF, OreRarity.AVERAGE, "dyeBlue", 6, "oreNetherLapis"),
    NETHERDIAMOND("Nether Diamond", 0x68FFE3, OreRarity.SCARCE, "gemDiamond", 1, "oreNetherDiamond"),
    NETHEREMERALD("Nether Emerald", 0x12DB00, OreRarity.SCARCE, "gemEmerald", 1, "oreNetherEmerald"),
    NETHERTIN("Nether Tin", 0xA9CCE2, OreRarity.COMMON, "ingotTin", 1, "oreNetherTin"),
    NETHERCOPPER("Nether Copper", 0xC8850C, OreRarity.COMMON, "ingotCopper", 1, "oreNetherCopper"),
    NETHERLEAD("Nether Lead", 0x6F7DA3, OreRarity.AVERAGE, "ingotLead", 1, "oreNetherLead"),
    NETHERNICKEL("Nether Nickel", 0xB6AE85, OreRarity.AVERAGE, "ingotNickel", 1, "oreNetherNickel"),
    NETHERSILVER("Nether Silver", 0xA7E4EC, OreRarity.AVERAGE, "ingotSilver", 1, "oreNetherSilver"),
    NETHERNIKOLITE("Nether Nikolite", 0xA7E4EC, OreRarity.AVERAGE, "dustNikolite", 5, "oreNetherNikolite"),
    COBALT("Cobalt", 0x1C30A5, OreRarity.SCARCE, "ingotCobalt", 1, "oreCobalt"),
    ARDITE("Ardite", 0xF48A00, OreRarity.SCARCE, "ingotArdite", 1, "oreArdite"),
    PLATINUM("Platinum", 0x00A0DB, OreRarity.RARE, "ingotPlatinum", 1, "orePlatinum", "oreCooperite"), //WTF Greg..."Cooper" == "Sheldon"??
    NETHERPLATINUM("Nether Platinum", 0x0892DE, OreRarity.RARE, "ingotPlatinum", 1, "oreNetherPlatinum"),
    ZINC("Zinc", 0x9B9B9B, OreRarity.COMMON, "ingotZinc", 1, "oreZinc", "oreSphalerite"),
    OSMIUM("Osmium", 0xA8B0E7, OreRarity.COMMON, "ingotOsmium", 1, "oreOsmium"),
    NETHERPIGIRON("Pig Iron", 0xC5B3AA, OreRarity.SCATTERED, "ingotSteel", 1, "oreNetherSteel"), //...
    SULFUR("Sulfur", 0xFFFF00, OreRarity.COMMON, "dustSulfur", 3, "oreSulfur"),
    PITCHBLENDE("Pitchblende", 0x4E4E7B, OreRarity.AVERAGE, "ingotUranium", 1, "orePitchblende"),
    CADMIUM("Cadmium", 0x87A6E8, OreRarity.AVERAGE, "ingotCadmium", 1, "oreCadmium"),
    INDIUM("Indium", 0xB3BFD8, OreRarity.SCATTERED, "ingotIndium", 1, "oreIndium"),
    FLUORITE("Fluorite", 0xDFD4AA, OreRarity.EVERYWHERE, "gemFluorite", 6, "oreFluorite"),
    BAUXITE("Bauxite", 0x72332B, OreRarity.AVERAGE, "dustBauxite", 1, "oreBauxite"),
    SODALITE("Sodalite", 0x133CAD, OreRarity.EVERYWHERE, "dyeBlue", 1, "oreSodalite"),
    PYRITE("Pyrite", 0xE1B531, OreRarity.COMMON, "dustPyrite", 1, "orePyrite"),
    AMMONIUM("Ammonium Chloride", 0xFFF9E0, OreRarity.SCATTERED, "dustAmmonium", 1, "oreAmmonium"),
    CALCITE("Calcite", 0xFF6A00, OreRarity.SCATTERED, "gemCalcite", 1, "oreCalcite"),
    CHIMERITE("Chimerite", 0xCBE5F5, OreRarity.SCATTERED, "gemChimerite", 2, "oreChimerite"),
    VINTEUM("Vinteum", 0x507BBC, OreRarity.SCATTERED, "dustVinteum", 1, "oreVinteum"),
    BLUETOPAZ("Blue Topaz", 0x92B1F0, OreRarity.SCATTERED, "gemBlueTopaz", 1, "oreBlueTopaz"),
    MOONSTONE("Moonstone", 0x6592CD, OreRarity.RARE, "gemMoonstone", 1, "oreMoonstone"),
    SUNSTONE("Sunstone", 0xD15106, OreRarity.RARE, "gemSunstone", 1, "oreSunstone"),
    TITANIUM("Titanium", 0x809797, OreRarity.SCARCE, "ingotTitanium", 1, "oreTitanium"),
    MAGMANITE("Magmanite", 0xF95D00, OreRarity.SCATTERED, "dropMagma", 1, "oreMagmanite"),
    MAGNETITE("Magnetite", 0x44442D, OreRarity.AVERAGE, "gemMagnetite", 1, "oreMagnetite"),
    ESSENCE("Essence", 0x158215, OreRarity.AVERAGE, "itemEssence", 4, "oreEssence", "oreNetherEssence", "oreEndEssence"),
    MIMICHITE("Mimichite", 0x8900FF, OreRarity.SCATTERED, "gemMimichite", 1, "oreMimichite"),
    NETHERURANIUM("Nether Uranium", 0x00aa00, OreRarity.SCARCE, "ingotUranium", 1, "oreNetherUranium"),
    QUANTUM("Quantum", 0x5BA642, OreRarity.SCATTERED, "dustQuantum", 2, "oreQuantum"),
    NETHERIRIDIUM("Nether Iridium", 0xb6f6c7, OreRarity.RARE, "ingotIridium", 1, "oreNetherIridium"),
    DARKIRON("Dark Iron", 0x4F00D1, OreRarity.AVERAGE, "ingotFzDarkIron", 1, "oreFzDarkIron"),
    CHROMITE("Chromite", 0x94E0F5, OreRarity.AVERAGE, "ingotChrome", 1, "oreChromite"),
    NETHERSAPPHIRE("Nether Sapphire", 0x00977, OreRarity.SCARCE, "gemSapphire", 1, "oreNetherSapphire"),
    NETHERPERIDOT("Nether Peridot", 0x0a408, OreRarity.SCARCE, "gemPeridot", 1, "oreNetherGreenSapphire", "oreNetherPeridot"),
    NETHERTITANIUM("Nether Titanium", 0x0a408, OreRarity.SCATTERED, "ingotTitanium", 1, "oreNetherTitanium"),
    NETHERSULFUR("Nether Sulfur", 0x0a408, OreRarity.COMMON, "dustSulfur", 3, "oreNetherSulfur"),
    NETHEROSMIUM("Nether Osmium", 0x98A0D7, OreRarity.AVERAGE, "ingotOsmium", 1, "oreNetherOsmium"),
    SILICON("Silicon", 0x777777, OreRarity.AVERAGE, "itemSilicon", 1, "oreSilicon"),
    RUTILE("Rutile", 0x615E57, OreRarity.SCATTERED, "ingotRutile", 1, "oreRutile"),
    AMETHYST("Amethyst", 0xFF72F3, OreRarity.RARE, "gemAmethyst", 1, "oreAmethyst"),
    TESLATITE("Teslatite", 0x2F81F1, OreRarity.COMMON, "dustTeslatite", 3, "oreTeslatite"),
    MANA("Mana", 0x70DAFC, OreRarity.SCARCE, "ingotMana", 1, "oreMana"),
    NETHERSALTPETER("Nether Saltpeter", 0xdddddd, OreRarity.SCARCE, "dustSaltpeter", 4, "oreNetherSaltpeter"),
    THORIUM("Thorium", 0x595959, OreRarity.SCARCE, "dustThorium", 1, "oreThorium", "oreThorite", "oreThorianite"),
    DRACONIUM("Draconium", 0x9F01E2, OreRarity.SCATTERED, "dustDraconium", 2, "oreDraconium"),
    ENDIUM("Endium", 0x372356, OreRarity.SCATTERED, "ingotEndium", 1, "oreEndium", "oreHeeEndium"),
    DILITHIUM("Dilithium", 0xC7BAB7, OreRarity.SCATTERED, "dustDilithium", 1, "oreDilithium"),
    EXIMITE("Eximite", 0x7C5A96, OreRarity.SCARCE, "ingotEximite", 1, "oreEximite"),
    MEUTOITE("Meutoite", 0x5F5269, OreRarity.SCARCE, "ingotMeutoite", 1, "oreMeutoite"),
    ;

    public final String displayName;
    private volatile String[] oreLabel;
    public final int dropCount;
    public final int oreColor;
    private final String product;
    public final OreRarity rarity;

    //private static final ArrayList<ItemStack> blocks = new ArrayList<ItemStack>();

    public static final ModOreList[] oreList = values();

    private static volatile OreCatalogue catalogue;
    private static final HashMap<String, ModOreList> enumNames = new HashMap<>();

    ModOreList(String n, int color, OreRarity r, String prod, int count, String... ore) {
        //if (!DragonAPIInit.canLoadHandlers())
        //	throw new MisuseException("Accessed registry enum too early! Wait until postInit!");
        dropCount = count;
        oreColor = color;
        displayName = n;
        product = prod;
        oreLabel = new String[ore.length];
        System.arraycopy(ore, 0, oreLabel, 0, ore.length);
        rarity = r;

        DragonAPI.LOGGER.info("Adding ore entries for "+ this +" (Ore Names: "+ Arrays.toString(ore)+")");
    }

    private static String getAluminumName() {
        return ModList.GREGTECH.isLoaded() ? "dustAluminum" : "ingotAluminum";
    }

    public static void initializeAll() { initializeAll(BuiltInRegistries.ITEM); }

    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.shouldUpdateStaticData()) initializeAll(event.getRegistries().lookupOrThrow(Registries.ITEM));
    }

    private static synchronized void initializeAll(HolderLookup.RegistryLookup<Item> registry) {
        var definitions = Arrays.stream(values()).map(ore -> {
            var names = new LinkedHashSet<>(List.of(ore.getOreDictNames()));
            for (var name : List.copyOf(names))
                OreCatalogueTags.resolve(name).forEach(tag -> names.add(tag.location().toString()));
            return new OreCatalogue.Definition(ore, List.copyOf(names), ore.product);
        }).toList();
        var next = OreCatalogue.build(definitions, name -> {
            var items = new LinkedHashSet<Item>();
            for (var tag : OreCatalogueTags.resolve(name))
                registry.get(tag).ifPresent(members -> members.forEach(holder -> items.add(holder.value())));
            return List.copyOf(items);
        });
        catalogue = next;
        DragonAPI.LOGGER.debug("Rebuilt ore/product catalogue for {} definitions", definitions.size());
    }

    private static OreCatalogue catalogue() {
        var snapshot = catalogue;
        if (snapshot != null) return snapshot;
        synchronized (ModOreList.class) {
            if (catalogue == null) initializeAll();
            return catalogue;
        }
    }

    /** Native tag registration; legacy category/material aliases remain supported below. */
    public void addOreTag(TagKey<Item> tag) {
        Objects.requireNonNull(tag);
        if (!tag.registry().equals(Registries.ITEM)) throw new IllegalArgumentException("Expected an item tag");
        addOreDictName(tag.location().toString());
    }

    public void addOreDictName(String name) {
        OreCatalogueTags.resolve(name); // Validate before changing any catalogue definition.
        synchronized (ModOreList.class) {
            var previous = oreLabel;
            if (!Arrays.asList(previous).contains(name)) {
                var extended = Arrays.copyOf(previous, previous.length + 1);
                extended[previous.length] = name;
                oreLabel = extended;
            }
            try { initializeAll(); }
            catch (RuntimeException failure) { oreLabel = previous; throw failure; }
        }
    }

    /** Rebuild the complete snapshot so old reverse entries disappear and shared memberships survive. */
    public void initialize() { initializeAll(); }

    @Override
    public String toString() {
        //sb.append(" (Ore Names: ");
        //for (int i = 0; i < oreLabel.length; i++) {
        //	sb.append(oreLabel[i]);
        //	if (i < oreLabel.length-1)
        //		sb.append(" ");
        //}
        //sb.append(")");
        //sb.append(" ("+this.getRarity().name().toLowerCase()+")");
        return displayName
                //sb.append(" (Ore Names: ");
                //for (int i = 0; i < oreLabel.length; i++) {
                //	sb.append(oreLabel[i]);
                //	if (i < oreLabel.length-1)
                //		sb.append(" ");
                //}
                //sb.append(")");
                //sb.append(" ("+this.getRarity().name().toLowerCase()+")");
                ;
    }

    public boolean isThaumcraft() {
        return this.name().contains("INFUSED") || this.name().equals("CINNABAR") || this.name().equals("AMBER");
    }

    public boolean isNetherOres() {
        return this.name().startsWith("NETHER");
    }

    public boolean isGregtech() {
        return switch (this) {
            case BAUXITE, GALENA, PYRITE, SODALITE, TUNGSTEN, ZINC -> true;
            default -> false;
        };
    }

    public static boolean isModOre(ItemStack is) {
        return getModOreFromOre(is) != null;
    }

    public static boolean isModOre(Block b) {
        return getModOreFromOre(b) != null;
    }

    public static ModOreList getModOreFromOre(Block b) {
        return first(getAllModOresFromOre(b));
    }

    public static ModOreList getModOreFromOre(BlockState state) {
        return state == null ? null : getModOreFromOre(state.getBlock());
    }

    public static boolean isModOre(BlockState state) { return getModOreFromOre(state) != null; }

    /** Common item tags classify every native state of each registered ore block. */
    public List<BlockState> getOreStates() {
        return catalogue().blocks(this).stream().map(item -> ((BlockItem)item).getBlock())
                .flatMap(block -> block.getStateDefinition().getPossibleStates().stream()).distinct().toList();
    }

    // DRAGONAPI-PORT: The original MekanismHandler.getModOre(item, damage) special case
    // discriminated its shared 1.7.10 ore block. Its modern port must provide native
    // item tags or explicit state associations; numeric damage cannot select a species.
    /** Singular compatibility lookups choose the first original enum entry. Use the plural API for all matches. */
    public static ModOreList getModOreFromOre(ItemStack stack) { return first(getAllModOresFromOre(stack)); }

    public static List<ModOreList> getAllModOresFromOre(ItemStack stack) {
        return stack == null || stack.isEmpty() ? List.of() : catalogue().ores(stack.getItem());
    }

    public static List<ModOreList> getAllModOresFromOre(Block block) {
        return block == null ? List.of() : catalogue().ores(block.asItem());
    }

    public static List<ModOreList> getAllModOresFromOre(BlockState state) {
        return state == null ? List.of() : getAllModOresFromOre(state.getBlock());
    }

    private static ModOreList first(List<ModOreList> matches) { return matches.isEmpty() ? null : matches.getFirst(); }

    /** Decode an old saved catalogue ordinal; this has never classified block/item damage. */
    public static ModOreList getByLegacyOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < oreList.length ? oreList[ordinal] : null;
    }

    /** @deprecated Use state/item lookup for live ores, or getByLegacyOrdinal for old saved data. */
    @Deprecated
    public static ModOreList getEntryFromDamage(int damage) { return getByLegacyOrdinal(damage); }

    public String[] getOreDictNames() {
        synchronized (ModOreList.class) {
            boolean charged = DragonAPI.config == null ? DragonOptions.CHARGEDCERTUS.getDefaultState() : DragonOptions.CHARGEDCERTUS.getState();
            return oreNames(charged).toArray(String[]::new);
        }
    }

    List<String> oreNames(boolean chargedCertus) {
        var names = new ArrayList<>(List.of(oreLabel));
        if (this == CERTUSQUARTZ && chargedCertus && !names.contains("oreChargedCertusQuartz"))
            names.add("oreChargedCertusQuartz");
        return List.copyOf(names);
    }

    public String getProductOreDictName() {
        return product;
    }

    public int getDropCount() {
        return dropCount;
    }

    public boolean isIngotType() {
        return product.contains("ingot");
    }

    public boolean isDustType() {
        return product.contains("dust");
    }

    public boolean isGemType() {
        return product.contains("gem") || product.contains("crystal");
    }

    public String getTypeName() {
        if (this.isIngotType())
            return "Ingot";
        if (this.isDustType())
            return "Dust";
        if (this.isGemType())
            return "Gem";
        return "Item";
    }

    public static ArrayList<ItemStack> getAllRegisteredOreBlocks() {
        var snapshot = catalogue();
        var result = new ArrayList<ItemStack>();
        for (var ore : values()) snapshot.blocks(ore).forEach(item -> result.add(new ItemStack(item)));
        return result;
    }

    public ItemStack getFirstOreBlock() {
        var items = catalogue().blocks(this);
        return items.isEmpty() ? null : new ItemStack(items.getFirst());
    }

    public ItemStack getRandomOreBlock() {
        var items = catalogue().blocks(this);
        return items.isEmpty() ? null : new ItemStack(items.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(items.size())));
    }

    public ItemStack getRandomOreBlock(RandomSource random) {
        Objects.requireNonNull(random);
        var items = catalogue().blocks(this);
        return items.isEmpty() ? null : new ItemStack(items.get(random.nextInt(items.size())));
    }

    public Collection<ItemStack> getAllOreBlocks() {
        return catalogue().blocks(this).stream().map(ItemStack::new).toList();
    }

    /** Declared product tag members, rather than guessed recipes or possible native loot drops. */
    public List<ItemStack> getProductItems() {
        return catalogue().products(this).stream().map(ItemStack::new).toList();
    }

    public boolean existsInGame() { return !catalogue().blocks(this).isEmpty(); }

	/*
	public boolean isRare() {
		return this == PLATINUM || this == NETHERPLATINUM || this == IRIDIUM ||	this == MOONSTONE || this == NETHERIRIDIUM;
	}*/

    public OreRarity getRarity() {
        return rarity;
    }

    public boolean isNether() {
        if (this.isNetherOres())
            return true;
        if (this == ARDITE || this == COBALT)
            return true;
        if (this == SUNSTONE)
            return true;
        if (this == FIRESTONE)
            return true;
        if (this == PYRITE)
            return true;
        if (this == AMMONIUM)
            return true;
        return this == THORIUM;
    }

    public boolean isEnd() {
        return this == SODALITE;
    }

    public EnumSet<OreLocation> getOreLocations() {
        if (this == MIMICHITE)
            return EnumSet.of(OreLocation.OVERWORLD, OreLocation.NETHER, OreLocation.END);
        if (this == PITCHBLENDE)
            return EnumSet.of(OreLocation.OVERWORLD, OreLocation.END);
        return this.isEnd() ? EnumSet.of(OreLocation.END) : this.isNether() ? EnumSet.of(OreLocation.NETHER) : EnumSet.of(OreLocation.OVERWORLD);
    }

    public boolean isArsMagica() {
        return switch (this) {
            case CHIMERITE, VINTEUM, BLUETOPAZ, MOONSTONE, SUNSTONE -> true;
            default -> false;
        };
    }

    //todo metallurgy? reika never did it clearly
    public boolean isMetallurgy() {
        return false;
    }

    public boolean canGenerateIn(Block b) {
        for (OreLocation loc : this.getOreLocations())
            if (loc.genBlock == b || loc == OreLocation.OVERWORLD && (b.defaultBlockState().is(net.neoforged.neoforge.common.Tags.Blocks.ORE_BEARING_GROUND_STONE)
                    || b.defaultBlockState().is(net.neoforged.neoforge.common.Tags.Blocks.ORE_BEARING_GROUND_DEEPSLATE))
                    || loc == OreLocation.NETHER && b.defaultBlockState().is(net.neoforged.neoforge.common.Tags.Blocks.ORE_BEARING_GROUND_NETHERRACK))
                return true;
        return false;
    }

    public Collection<ItemStack> getGennableIn(Block b) {
        var host = b.defaultBlockState();
        var ground = host.is(net.neoforged.neoforge.common.Tags.Blocks.ORE_BEARING_GROUND_DEEPSLATE)
                ? net.neoforged.neoforge.common.Tags.Blocks.ORES_IN_GROUND_DEEPSLATE
                : host.is(net.neoforged.neoforge.common.Tags.Blocks.ORE_BEARING_GROUND_STONE)
                ? net.neoforged.neoforge.common.Tags.Blocks.ORES_IN_GROUND_STONE
                : host.is(net.neoforged.neoforge.common.Tags.Blocks.ORE_BEARING_GROUND_NETHERRACK)
                ? net.neoforged.neoforge.common.Tags.Blocks.ORES_IN_GROUND_NETHERRACK : null;
        return catalogue().blocks(this).stream().filter(item -> {
            var state = ((BlockItem)item).getBlock().defaultBlockState();
            boolean classified = state.is(net.neoforged.neoforge.common.Tags.Blocks.ORES_IN_GROUND_STONE)
                    || state.is(net.neoforged.neoforge.common.Tags.Blocks.ORES_IN_GROUND_DEEPSLATE)
                    || state.is(net.neoforged.neoforge.common.Tags.Blocks.ORES_IN_GROUND_NETHERRACK);
            return classified ? ground != null && state.is(ground) : this.canGenerateIn(b);
        }).map(ItemStack::new).toList();
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    public boolean contains(ItemStack is) {
        return getAllModOresFromOre(is).contains(this);
    }

    public static ModList getOreModFromItemStack(ItemStack is) {
        if (is == null || is.isEmpty()) return null;
        boolean ore = is.is(net.neoforged.neoforge.common.Tags.Items.ORES) || getModOreFromOre(is) != null;
        if (!ore) return null;
        if (!(is.getItem() instanceof net.minecraft.world.item.BlockItem item)) {
            DragonAPI.LOGGER.error("{} is registered as an ore but is not a block item", is);
            return null;
        }
        return ModList.getModFromID(BuiltInRegistries.BLOCK.getKey(item.getBlock()).getNamespace());
    }

    public static boolean isModOreType(String name) { return !getAllByOreName(name).isEmpty(); }

    public static ModOreList getByOreName(String name) { return first(getAllByOreName(name)); }

    public static List<ModOreList> getAllByOreName(String name) {
        return name == null ? List.of() : catalogue().names(name);
    }

    /** Declared product associations plus the original ore-block fallback, in enum order. */
    public static ModOreList getByDrop(ItemStack stack) { return first(getAllByDrop(stack)); }

    public static List<ModOreList> getAllByDrop(ItemStack stack) {
        return stack == null || stack.isEmpty() ? List.of() : catalogue().drops(stack.getItem());
    }

    public static ModOreList getByEnumName(String name) {
        return enumNames.get(name);
    }

    static {
        for (ModOreList modOreList : oreList) {
            enumNames.put(modOreList.name(), modOreList);
        }
    }

    @Override
    public int getDisplayColor() {
        return oreColor;
    }
}
