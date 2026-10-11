package reika.dragonapi.modregistry;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.ModList;
import reika.dragonapi.instantiable.data.immutable.BlockBox;
import reika.dragonapi.instantiable.data.immutable.BlockKey;
import reika.dragonapi.interfaces.registry.TreeType;
import reika.dragonapi.libraries.java.ReikaStringParser;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public enum ModWoodList implements TreeType {

    CANOPY(ModList.TWILIGHT, 		0x252517, 0x330464, 18, 30, "log", "leaves", "sapling", VarType.INSTANCE),
    DARKWOOD(ModList.TWILIGHT, 		0x35281A, 0x395F41, 60, 20, "log", "darkleaves", "sapling", VarType.INSTANCE),
    MANGROVE(ModList.TWILIGHT, 		0x8D8980, 0x431445, 10, 18, "log", "leaves", "sapling",VarType.INSTANCE),
    TWILIGHTOAK(ModList.TWILIGHT, 	0x806654, 0x764952, 30, 180, "log", "leaves", "sapling", VarType.INSTANCE),
    //RAINBOWOAK(ModList.TWILIGHT, 	0x806654, 0x764952, ?, ?, "---", "leaves", "sapling", new int[]{0,12}, new int[]{3,11}, 9, VarType.INSTANCE),
    GREATWOOD(ModList.THAUMCRAFT, 	0x4F3E37, 0x71924C, 10, 30, "blockMagicalLog", "blockMagicalLeaves", "blockCustomPlant", VarType.INSTANCE),
    SILVERWOOD(ModList.THAUMCRAFT, 	0xC9C3AC, 0x5782C7, 8, 24, "blockMagicalLog", "blockMagicalLeaves", "blockCustomPlant", VarType.INSTANCE),
    EUCALYPTUS(ModList.NATURA, 		0xE2CEB1, 0x3C9119, 10, 14, "tree", "floraLeaves", "floraSapling", VarType.INSTANCE),
    SEQUOIA(ModList.NATURA, 		0x8C7162, 0x3C9119, 50, 250, "redwood", "floraLeaves", "floraSapling", VarType.INSTANCE),
    SAKURA(ModList.NATURA, 			0x703C02, 0xEB7F98, 16, 30, "tree", "floraLeavesNoColor", "floraSapling", VarType.INSTANCE),
    GHOSTWOOD(ModList.NATURA, 		0xB3B3B3, 0xEEE6D1, 7, 12, "tree", "floraLeavesNoColor", "floraSapling", VarType.INSTANCE),
    HOPSEED(ModList.NATURA, 		0x9F8661, 0x3C9119, 10, 7, "tree", "floraLeaves", "floraSapling", VarType.INSTANCE),
    NATURADARKWOOD(ModList.NATURA, 	0x234D85, 0x061E4C, 7, 12, "darkTree", "darkLeaves", "floraSapling", VarType.INSTANCE),
    BLOODWOOD(ModList.NATURA, 		0x8D4F05, 0xB10000, 14, 16, "bloodwood", "floraLeavesNoColor", "floraSapling", VarType.INSTANCE),
    FUSEWOOD(ModList.NATURA, 		0x2C3E38, 0x28818D, 7, 12, "darkTree", "darkLeaves", "floraSapling",VarType.INSTANCE),
    TIGERWOOD(ModList.NATURA, 		0x544936, 0x5B2900, 7, 12, "rareTree", "rareLeaves", "rareSapling", VarType.INSTANCE),
    SILVERBELL(ModList.NATURA, 		0x939C85, 0x73B849, 7, 12, "rareTree", "rareLeaves", "rareSapling", VarType.INSTANCE),
    MAPLE(ModList.NATURA, 			0x503A23, 0x993412, 7, 12, "rareTree", "rareLeaves", "rareSapling", VarType.INSTANCE),
    WILLOW(ModList.NATURA, 			0x584C30, 0x548941, 9, 14, "willow", "floraLeavesNoColor", "rareSapling", VarType.INSTANCE),
    AMARANTH(ModList.NATURA, 		0x9C8B56, 0x3C9119, 6, 20, "rareTree", "rareLeaves", "rareSapling", VarType.INSTANCE),
    BAMBOO(ModList.BOP, 			0xBBD26C, 0xAFD83B, 5, 20, "bamboo", "leaves1", "saplings",VarType.INSTANCE),
    MAGIC(ModList.BOP, 				0x78839E, 0x5687BE, 7, 12, "logs2", "leaves1", "saplings", VarType.INSTANCE),
    DARK(ModList.BOP, 				0x664848, 0x312F42, 7, 12, "logs1", "leaves1", "saplings", VarType.INSTANCE),
    FIR(ModList.BOP, 				0x675846, 0x518E5F, 12, 60, "logs1", "leaves2", "saplings", VarType.INSTANCE),
    LOFTWOOD(ModList.BOP, 			0x817665, 0x3FD994, 8, 16, "logs2", "leaves2", "saplings", VarType.INSTANCE),
    CHERRY(ModList.BOP, 			0x965441, 0xFFAFE0, 15, 20, "logs1", "leaves3", "saplings", VarType.INSTANCE), //sapling 12 for white cherry
    HELLBARK(ModList.BOP, 			0xB36F43, 0x7B5E1F, 2, 5, "logs4", "leaves4", "saplings", VarType.INSTANCE),
    JACARANDA(ModList.BOP, 			0x998177, 0x644F84, 7, 12, "logs4", "leaves4", "saplings", VarType.INSTANCE),
    SACRED(ModList.BOP, 			0x896B4F, 0x3E981A, 30, 160, "logs1", "colorizedLeaves1", "colorizedSaplings", VarType.INSTANCE),
    BOPMANGROVE(ModList.BOP, 		0xDED1B5, 0x3E981A, 5, 15, "logs2", "colorizedLeaves1", "colorizedSaplings", VarType.INSTANCE),
    PALM(ModList.BOP, 				0x936B40, 0x3E981A, 6, 14, "logs2", "colorizedLeaves1", "colorizedSaplings", VarType.INSTANCE),
    REDWOOD(ModList.BOP, 			0x722F0D, 0x3E981A, 6, 50, "logs3", "colorizedLeaves1", "colorizedSaplings", VarType.INSTANCE),
    BOPWILLOW(ModList.BOP, 			0x767A47, 0x3E981A, 8, 15, "logs3", "colorizedLeaves2", "colorizedSaplings", VarType.INSTANCE),
    PINE(ModList.BOP, 				0x896B4F, 0x3E981A, 8, 25, "logs4", "colorizedLeaves2", "colorizedSaplings", VarType.INSTANCE),
    MAHOGANY(ModList.BOP, 			0x896B4F, 0x3E981A, 9, 16, "logs4", "colorizedLeaves2", "colorizedSaplings", VarType.INSTANCE),
    BXLREDWOOD(ModList.BXL, 		0x000000, 0x000000, -1, -1, null, null, null, VarType.INSTANCE),
    IC2RUBBER(ModList.IC2, 			0x3C2D20, 0x638143, 6, 15, "rubberWood", "rubberLeaves", "rubberSapling", VarType.ITEMSTACK),
    MFRRUBBER(ModList.MINEFACTORY, 	0x7E5C25, 0x5DC123, 30, 90, "rubberWoodBlock", "rubberLeavesBlock", "rubberSaplingBlock", VarType.INSTANCE),
    TIMEWOOD(ModList.TWILIGHT, 		0x4F301D, 6986775, 10, 20, "magicLog", "magicLeaves", "sapling", VarType.INSTANCE),
    TRANSFORMATION(ModList.TWILIGHT, 0x66727F, 7130346, 12, 20, "magicLog", "magicLeaves", "sapling", VarType.INSTANCE),
    MINEWOOD(ModList.TWILIGHT, 		0xC5A982, 16576836, 15, 20, "magicLog", "magicLeaves", "sapling", VarType.INSTANCE),
    SORTING(ModList.TWILIGHT, 		0x705835, 3558403, 12, 20, "magicLog", "magicLeaves", "sapling", VarType.INSTANCE),
    GLOW(ModList.TRANSITIONAL, 		0xE2B87B, 0xFFBC5E, 7, 12, "GlowWood", "GlowLeaf", "GlowSapling",VarType.INSTANCE),
    FORCE(ModList.DARTCRAFT, 		0xE0B749, 0xD9B22C, 7, 12, "forceLog", "forceLeaves", "forceSapling",  VarType.INSTANCE),
    HIGHFIR(ModList.HIGHLANDS, 		0x77ee77, 0x88dd88, 10, 30, "firWood", "firLeaves", "firSapling", VarType.INSTANCE),
    HIGHACACIA(ModList.HIGHLANDS, 	0x77ee77, 0x88dd88, 12, 10, "acaciaWood", "acaciaLeaves", "acaciaSapling", VarType.INSTANCE),
    HIGHREDWOOD(ModList.HIGHLANDS, 	0x77ee77, 0x88dd88, 12, 45, "redwoodWood", "redwoodLeaves", "redwoodSapling", VarType.INSTANCE),
    POPLAR(ModList.HIGHLANDS, 		0x77ee77, 0x88dd88, 8, 12, "poplarWood", "poplarLeaves", "poplarSapling", VarType.INSTANCE),
    HIGHCANOPY(ModList.HIGHLANDS, 	0x77ee77, 0x88dd88, 12, 30, "canopyWood", "canopyLeaves", "canopySapling", VarType.INSTANCE),
    HIGHPALM(ModList.HIGHLANDS, 	0x77ee77, 0x88dd88, 4, 12, "palmWood", "palmLeaves", "palmSapling", VarType.INSTANCE),
    IRONWOOD(ModList.HIGHLANDS, 	0x77ee77, 0x88dd88, 20, 50, "ironWood", "ironwoodLeaves", "ironwoodSapling", VarType.INSTANCE),
    HIGHMANGROVE(ModList.HIGHLANDS, 0x77ee77, 0x88dd88, 6, 10, "mangroveWood", "mangroveLeaves", "mangroveSapling", VarType.INSTANCE),
    ASH(ModList.HIGHLANDS, 			0x77ee77, 0x88dd88, 12, 25, "ashWood", "ashLeaves", "ashSapling", VarType.INSTANCE),
    WITCHWOOD(ModList.ARSMAGICA, 	0x584D32, 0x1F4719, 10, 20, "witchwoodLog", "witchwoodLeaves", "witchwoodSapling", VarType.INSTANCE),
    ROWAN(ModList.WITCHERY, 		0x374633, 0x9E774D, 7, 12, "LOG", "LEAVES", "SAPLING",  VarType.INSTANCE),
    HAWTHORNE(ModList.WITCHERY, 	0x656566, 0xC3EEC3, 10, 16, "LOG", "LEAVES", "SAPLING",  VarType.INSTANCE),
    ALDER(ModList.WITCHERY, 		0x52544C, 0xC3D562, 6, 10, "LOG", "LEAVES", "SAPLING",  VarType.INSTANCE),
    LIGHTED(ModList.CHROMATICRAFT,	0xA05F36, 0xFFD793, 10, 14, "glow_log", "glowing_leaves", "glow_sapling", VarType.REGISTRY),
    SLIME(ModList.TINKERER,			0x68FF7A, 0x8EFFE1, 12, 15, "slimeGel", "slimeLeaves", "slimeSapling", VarType.INSTANCE),
    TAINTED(ModList.FORBIDDENMAGIC,	0x40374B, 0x530D7B,	7, 12, "taintLog", "taintLeaves", "taintSapling", VarType.INSTANCE),
    PINKBIRCH(ModList.SATISFORESTRY,0xE5E4DB, 0xF795B5, 6, 20, "LOG", "LEAVES", "SAPLING",VarType.INSTANCE),
    GIANTPINKTREE(ModList.SATISFORESTRY,0xE5E4DB, 0xF46E9B, 48, 192, "LOG", "LEAVES", "SAPLING", VarType.INSTANCE),
    REDJUNGLE(ModList.SATISFORESTRY,0xE5E4DB, 0xCC515D, 15, 20, "LOG", "LEAVES", "SAPLING", VarType.INSTANCE),
    WEEDWOOD(ModList.BETWEENLANDS,	0x6B7A30, 0x94A843, 8, 14, "weedwoodLog", "weedwoodLeaves", "saplingWeedwood", VarType.INSTANCE),
    SAPTREE(ModList.BETWEENLANDS,	0x6B7A30, 0x94A843, 8, 14, "sapTreeLog", "sapTreeLeaves", "saplingSapTree", VarType.INSTANCE),
    BETWEENRUBBER(ModList.BETWEENLANDS,0x6B7A30, 0x94A843, 8, 14, "weedwoodLog", "rubberTreeLeaves", "saplingRubberTree", VarType.INSTANCE),
    TROPIPALM(ModList.TROPICRAFT,	0x965A33, 0x20c020, 6, 12, "logs", "palmLeaves", "saplings", VarType.INSTANCE),
    TROPIMAHOGANY(ModList.TROPICRAFT,0x7C3631, 0x20c020, 9, 20, "logs", "rainforestLeaves", "saplings", VarType.INSTANCE),
    NETHERPAM(ModList.NETHERPAM,	0xa02020, 0xcf4040, 4, 4, "netherLog", "netherLeaves", "netherSapling", VarType.INSTANCE),
    //SKYROOTG(ModList.AETHER,		0x00ff00, 0x00ff00, 8, 14, "AetherLog", "GreenSkyrootLeaves", "GreenSkyrootSapling", VarType.INSTANCE),
    //SKYROOTB(ModList.AETHER,		0x0000ff, 0x0000ff, 8, 14, "AetherLog", "BlueSkyrootLeaves", "BlueSkyrootSapling", VarType.INSTANCE),
    //SKYROOTD(ModList.AETHER,		0x000070, 0x000070, 8, 14, "AetherLog", "DarkBlueSkyrootLeaves", "DarkBlueSkyrootSapling", VarType.INSTANCE),
    //PURPLECRYSTAL(ModList.AETHER,	0xa000ff, 0xa000ff, 8, 14, "AetherLog", "DarkBlueSkyrootLeaves", "DarkBlueSkyrootSapling", VarType.INSTANCE),,
    GOLDOAK(ModList.AETHER,			0xE4CB64, 0xE4CB64, 8, 14, "AetherLog", "GoldenOakLeaves", "GoldenOakSapling", VarType.INSTANCE),
    BAOBAB(ModList.EREBUS,			0x000000, 0x000000, 12, 14, "Baobab_Log",		"Baobab_Leaves",		"Baobab_Sapling", VarType.INSTANCE),
    EREBEUCALYPTUS(ModList.EREBUS,	0x000000, 0x000000, 20, 60, "Eucalyptus_Log",	"Eucalyptus_Leaves",	"Eucalyptus_Sapling", VarType.INSTANCE),
    EREBMAHOGANY(ModList.EREBUS,	0x000000, 0x000000, 12, 18, "Mahogany_Log",		"Mahogany_Leaves",		"Mahogany_Sapling", VarType.INSTANCE),
    MOSSBARK(ModList.EREBUS,		0x000000, 0x000000, 12, 14, "Mossbark_Log",		"Mossbark_Leaves",		"Mossbark_Sapling", VarType.INSTANCE),
    ASPER(ModList.EREBUS,			0x000000, 0x000000, 12, 14, "Asper_Log",		"Asper_Leaves",			"Asper_Sapling", VarType.INSTANCE),
    CYPRESS(ModList.EREBUS,			0x000000, 0x000000, 8, 16, "Cypress_Log",		"Cypress_Leaves",		"Cypress_Sapling", VarType.INSTANCE),
    SAP(ModList.EREBUS,				0x000000, 0x000000, 12, 14, "Sap_Log",			"Sap_Leaves",			"Sap_Sapling", VarType.INSTANCE),
    MARSHWOOD(ModList.EREBUS,		0x000000, 0x000000, 12, 14, "Marshwood_Log",	"Marshwood_Leaves",		"Marshwood_Sapling", VarType.INSTANCE);

    private final ModList mod;
    private final VarType lookupType;
    private final String registryLog, registryLeaf, registrySapling;
    private volatile WoodSpecies species;
    private boolean attemptedReflection;

    public final int logColor;
    public final int leafColor;
    public final BlockBox bounds;
    public static final ModWoodList[] woodList = values();

    private record Index(Map<BlockState, ModWoodList> logs, Map<BlockState, ModWoodList> leaves,
            Map<BlockState, ModWoodList> saplings) { }
    private static volatile Index mappings = new Index(Map.of(), Map.of(), Map.of());

    ModWoodList(ModList req, int color, int leafcolor, int w, int h, String blockVar,
            String leafVar, String saplingVar, VarType type) {
        mod = req;
        lookupType = type;
        registryLog = blockVar;
        registryLeaf = leafVar;
        registrySapling = saplingVar;
        leafColor = leafcolor;
        logColor = color;
        bounds = BlockBox.origin().expand(w, h, w);
    }

    /** Ports of shared-block species supply their complete state sets after block registration. */
    public void registerSpecies(WoodSpecies association) {
        Objects.requireNonNull(association);
        if (!mod.isLoaded()) throw new IllegalStateException("Cannot associate an absent mod: " + mod);
        synchronized (ModWoodList.class) {
            var logs = new HashMap<BlockState, ModWoodList>();
            var leaves = new HashMap<BlockState, ModWoodList>();
            var saplings = new HashMap<BlockState, ModWoodList>();
            for (var wood : woodList) {
                var entry = wood == this ? association : wood.species;
                if (entry == null) continue;
                index(logs, entry.logs(), wood);
                index(leaves, entry.leaves(), wood);
                index(saplings, entry.saplings(), wood);
            }
            species = association;
            mappings = new Index(Map.copyOf(logs), Map.copyOf(leaves), Map.copyOf(saplings));
        }
    }

    private static void index(Map<BlockState, ModWoodList> into, List<BlockState> states, ModWoodList wood) {
        for (var state : states) {
            var previous = into.putIfAbsent(state, wood);
            if (previous != null && previous != wood)
                throw new IllegalArgumentException("Ambiguous wood state " + state + ": " + previous.name() + " / " + wood.name());
        }
    }

    private void resolve() {
        if (species != null || !mod.isLoaded() || registryLog == null || registryLeaf == null) return;
        try {
            Block log, leaf, sapling;
            if (lookupType == VarType.REGISTRY) {
                log = registered(registryLog);
                leaf = registered(registryLeaf);
                sapling = registrySapling == null ? null : registered(registrySapling);
            } else {
                if (attemptedReflection) return;
                attemptedReflection = true;
                // Legacy field names are retained as an adapter for distinct-block integrations.
                // Shared metadata blocks require registerSpecies from their modern port: assigning
                // every state to each old name would silently turn one species into another.
                for (var other : woodList) {
                    if (other != this && other.mod == mod && (Objects.equals(other.registryLog, registryLog)
                            || Objects.equals(other.registryLeaf, registryLeaf)
                            || registrySapling != null && Objects.equals(other.registrySapling, registrySapling))) {
                        DragonAPI.LOGGER.debug("{} requires an explicit state association from its port", getLabel());
                        return;
                    }
                }
                var type = mod.getBlockClass();
                if (type == null) return;
                if (lookupType == VarType.ITEMSTACK) {
                    registerSpecies(WoodSpecies.ofItems(loadItemStack(type, registryLog), loadItemStack(type, registryLeaf),
                            registrySapling == null ? null : loadItemStack(type, registrySapling), this == LIGHTED));
                    return;
                } else {
                    log = loadBlock(type, registryLog);
                    leaf = loadBlock(type, registryLeaf);
                    sapling = registrySapling == null ? null : loadBlock(type, registrySapling);
                }
            }
            if (log != null && leaf != null && (registrySapling == null || sapling != null))
                registerSpecies(WoodSpecies.ofBlocks(log, leaf, sapling, this == LIGHTED));
        } catch (ReflectiveOperationException | IllegalArgumentException | SecurityException error) {
            DragonAPI.LOGGER.error("Could not associate {}", getLabel(), error);
        }
    }

    private Block registered(String path) {
        return BuiltInRegistries.BLOCK.getOptional(Identifier.fromNamespaceAndPath(mod.modid, path)).orElse(null);
    }

    private ItemStack loadItemStack(Class<?> cl, String field) throws ReflectiveOperationException {
        Object ins = this.getFieldInstance();
        Field f = cl.getField(field);
        return (ItemStack) f.get(ins);
    }

    private Block loadBlock(Class<?> cl, String field) throws ReflectiveOperationException {
        switch (mod) {
            case CHROMATICRAFT, SATISFORESTRY -> {
                Field f = cl.getField(field);
                Method block = cl.getMethod("get");
                Object entry = f.get(null);
                return (Block) block.invoke(entry);
            }
            case EREBUS -> {
                cl = Class.forName("erebus.lib.EnumWood");
                int idx = field.indexOf('_');
                String type = field.substring(idx + 1);
                Field f = cl.getField(field.substring(0, idx));
                Method block = cl.getMethod("get" + type);
                Object entry = f.get(null);
                return (Block) block.invoke(entry);
            }
            default -> {
                Object ins = this.getFieldInstance();
                Field f = cl.getField(field);
                return (Block) f.get(ins);
            }
        }
    }

    private Object getFieldInstance() throws ReflectiveOperationException {
        if (mod == ModList.WITCHERY) {
            Class<?> c = Class.forName("com.emoniph.witchery.Witchery");
            Field f = c.getField("Blocks");
            return f.get(null);
        }
        return null;
    }

    @Override
    public String toString() {
        return getBasicInfo() + (species == null ? " (Not loaded)"
                : " (LOG " + species.logs() + "; LEAF " + species.leaves() + "; SAPLING " + species.saplings() + ")");
    }

    public String getBasicInfo() { return getName() + " from " + mod; }
    public String getLabel() { return name() + " from " + mod; }
    public String getName() { return ReikaStringParser.capFirstChar(name()); }
    public ModList getParentMod() { return mod; }

    @Override
    public boolean exists() {
        resolve();
        return species != null && mod.isLoaded();
    }

    private WoodSpecies requiredSpecies() {
        if (!exists()) throw new IllegalStateException(getLabel() + " has no registered state association");
        return species;
    }

    private static void refreshWoods() {
        for (var wood : woodList) wood.resolve();
    }

    @Override public BlockKey getItem() { return getLogItemWithOffset(0); }

    /** The offset is an index into the declared log states, never an item damage value. */
    public BlockKey getLogItemWithOffset(int index) { return new BlockKey(requiredSpecies().logs().get(index)); }
    public boolean isLogBlock(ItemStack stack) { return getModWood(stack) == this; }
    public Block getBlock() { return getLogID(); }
    @Override public Block getLogID() { return exists() ? species.logs().getFirst().getBlock() : null; }
    @Override public Block getLeafID() { return exists() ? species.leaves().getFirst().getBlock() : null; }
    @Override public Block getSaplingID() {
        return exists() && !species.saplings().isEmpty() ? species.saplings().getFirst().getBlock() : null;
    }
    @Override public List<BlockState> getLogStates() { return exists() ? species.logs() : List.of(); }
    @Override public List<BlockState> getLeafStates() { return exists() ? species.leaves() : List.of(); }
    public List<BlockState> getSaplingStates() { return exists() ? species.saplings() : List.of(); }

    @Override
    public Block getPlankID() {
        if (!this.exists()) return null;
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null; // Recipe-derived associations require the loaded server data.
        var level = server.overworld();
        var logs = requiredSpecies().logItems();
        if (logs.isEmpty()) return null;
        var input = net.minecraft.world.item.crafting.CraftingInput.of(1, 1, List.of(logs.getFirst()));
        return level.recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level)
                .map(recipe -> recipe.value().assemble(input))
                .filter(stack -> !stack.isEmpty() && stack.is(net.minecraft.tags.ItemTags.PLANKS)
                        && stack.getItem() instanceof net.minecraft.world.item.BlockItem)
                .map(stack -> ((net.minecraft.world.item.BlockItem)stack.getItem()).getBlock()).orElse(null);
    }


    public static ModWoodList getModWood(BlockState state) {
        refreshWoods();
        return state == null ? null : mappings.logs().get(state);
    }
    public static ModWoodList getModWood(Block block) { refreshWoods(); return unique(block, mappings.logs()); }
    public static ModWoodList getModWood(ItemStack stack) { return getModWood(WoodSpecies.stateOf(stack)); }
    public static ModWoodList getModWoodFromLeaf(BlockState state) {
        refreshWoods();
        return state == null ? null : mappings.leaves().get(state);
    }
    public static ModWoodList getModWoodFromLeaf(Block block) { refreshWoods(); return unique(block, mappings.leaves()); }
    public static ModWoodList getModWoodFromLeaf(ItemStack stack) { return getModWoodFromLeaf(WoodSpecies.stateOf(stack)); }
    public static ModWoodList getModWoodFromSapling(BlockState state) {
        refreshWoods();
        return state == null ? null : mappings.saplings().get(state);
    }
    public static ModWoodList getModWoodFromSapling(Block block) { refreshWoods(); return unique(block, mappings.saplings()); }
    public static ModWoodList getModWoodFromSapling(ItemStack stack) { return getModWoodFromSapling(WoodSpecies.stateOf(stack)); }

    /** A block-only query is ambiguous when a modern integration shares it between species. */
    private static ModWoodList unique(Block block, Map<BlockState, ModWoodList> mappings) {
        if (block == null) return null;
        ModWoodList found = null;
        for (var entry : mappings.entrySet()) {
            if (!entry.getKey().is(block)) continue;
            if (found != null && found != entry.getValue()) return null;
            found = entry.getValue();
        }
        return found;
    }

    public static boolean isModWood(ItemStack stack) { return getModWood(stack) != null; }
    public static boolean isModWood(Block block) { return getModWood(block) != null; }
    public static boolean isModWood(BlockState state) { return getModWood(state) != null; }
    public static boolean isModLeaf(ItemStack stack) { return getModWoodFromLeaf(stack) != null; }
    public static boolean isModLeaf(Block block) { return getModWoodFromLeaf(block) != null; }
    public static boolean isModLeaf(BlockState state) { return getModWoodFromLeaf(state) != null; }
    public static boolean isModSapling(ItemStack stack) { return getModWoodFromSapling(stack) != null; }
    public static boolean isModSapling(Block block) { return getModWoodFromSapling(block) != null; }
    public static boolean isModSapling(BlockState state) { return getModWoodFromSapling(state) != null; }

    public FallingBlockEntity getFallingBlock(Level world, int x, int y, int z) {
        return FallingBlockEntity.fall(world, new BlockPos(x, y, z), getItem().blockID);
    }
    @Override public BlockKey getBasicLeaf() { return new BlockKey(requiredSpecies().leaves().getFirst()); }
    public ArrayList<ItemStack> getAllLeaves() { return new ArrayList<>(requiredSpecies().leafItems()); }
    public List<ItemStack> getAllLogItems() { return requiredSpecies().logItems(); }
    public List<ItemStack> getAllSaplings() { return requiredSpecies().saplingItems(); }
    public ItemStack getCorrespondingSapling() {
        if (!exists()) return null;
        var saplings = species.saplingItems();
        return saplings.isEmpty() ? null : saplings.getFirst();
    }

    public static ModWoodList getRandomWood(Random rand) {
        List<ModWoodList> available = Arrays.stream(woodList).filter(ModWoodList::exists).toList();
        if (available.isEmpty()) throw new IllegalStateException("No modded woods are available");
        return available.get(rand.nextInt(available.size()));
    }

    public boolean isRareTree() {
        if (this.isMagicTFTree())
            return true;
        return this == SILVERWOOD;
    }

    public boolean isMagicTFTree() {
        if (this == TIMEWOOD)
            return true;
        if (this == SORTING)
            return true;
        if (this == MINEWOOD)
            return true;
        return this == TRANSFORMATION;
    }

    @Override
    public boolean canBePlacedSideways() {
        return getLogStates().stream().anyMatch(state -> state.hasProperty(
                net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS));
    }

    public boolean isNaturalLeaf(Level world, BlockPos pos) { return isNaturalLeaf(world.getBlockState(pos)); }
    public boolean isNaturalLeaf(BlockState state) { return exists() && species.isNaturalLeaf(state); }

    @Override
    public BlockBox getTypicalMaximumSize() {
        return bounds;
    }

    @Override
    public int getRecursiveBreakDepth() {
        return switch (this) {
            case SEQUOIA -> 350;
            case TWILIGHTOAK -> 200;
            case DARKWOOD -> 32;
            case GIANTPINKTREE -> 180;
            default -> TreeType.super.getRecursiveBreakDepth();
        };
    }

    public static Collection<ModWoodList> getAllWoodsByMod(ModList mod) {
        refreshWoods();
        return Arrays.stream(woodList).filter(wood -> wood.mod == mod && wood.exists()).toList();
    }

    public enum VarType {
        ITEMSTACK(),
        INSTANCE(),
        REGISTRY();
        //INT();

        @Override
        public String toString() {
            return "Variable Type "+ ReikaStringParser.capFirstChar(this.name());
        }
    }

}
