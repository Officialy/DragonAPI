package reika.dragonapi.modregistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.ForgeRegistries;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.ModList;
import reika.dragonapi.instantiable.data.immutable.BlockBox;
import reika.dragonapi.instantiable.data.immutable.BlockKey;
import reika.dragonapi.instantiable.data.maps.BlockMap;
import reika.dragonapi.instantiable.data.maps.MultiMap;
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
    LIGHTED(ModList.CHROMATICRAFT,	0xA05F36, 0xFFD793, 10, 14, "GLOWLOG", "GLOWLEAF", "GLOWSAPLING",  VarType.INSTANCE),
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

    private ModList mod;
    private Block blockID = null;
    private Block leafID = null;
    private Block saplingID;

    private boolean hasPlanks;

    public final int logColor;
    public final int leafColor;
    public final BlockBox bounds;

    private String varName;
    private Class containerClass;

    private boolean exists = false;

    public static final ModWoodList[] woodList = values();

    private static final BlockMap<ModWoodList> logMappings = new BlockMap<>();
    private static final BlockMap<ModWoodList> leafMappings = new BlockMap<>();
    private static final BlockMap<ModWoodList> saplingMappings = new BlockMap<>();

    private static final MultiMap<ModList, ModWoodList> modMappings = new MultiMap<>(MultiMap.CollectionType.HASHSET);

    ModWoodList(ModList req, int color, int leafcolor, int w, int h, String blockVar, String leafVar, String saplingVar, VarType type) {
        //if (!DragonAPI.canLoadHandlers())
        //    throw new MisuseException("Accessed registry enum too early! Wait until postInit!");
        mod = req;
        leafColor = leafcolor;
        logColor = color;
        bounds = BlockBox.origin().expand(w, h, w);
        if (!mod.isLoaded()) {
            DragonAPI.LOGGER.info("DRAGONAPI: Not loading "+this.getLabel()+": Mod not present.");
            return;
        }
        Class cl = req.getBlockClass();
        //DragonAPI.LOGGER.info("DRAGONAPI: Attempting to load "+this.getLabel()+". Data parameters:");
        //DragonAPI.LOGGER.info(cl+", "+blockVar+", "+leafVar+", "+saplingVar+", "+type);
        if (cl == null) {
            DragonAPI.LOGGER.error("Error loading wood "+this.getLabel()+": Empty block class");
            return;
        }
        if (blockVar == null || blockVar.isEmpty()) {
            DragonAPI.LOGGER.error("Error loading wood "+this.getLabel()+": Empty variable name");
            return;
        }
        if (leafVar == null || leafVar.isEmpty()) {
            DragonAPI.LOGGER.error("Error loading leaves for wood "+this.getLabel()+": Empty variable name");
            return;
        }
        try {
            Block id;
            Block idleaf;
            Block idsapling;
            switch (type) {
                case ITEMSTACK -> {
                    ItemStack wood = this.loadItemStack(cl, blockVar);
                    ItemStack leaf = this.loadItemStack(cl, leafVar);
                    ItemStack sapling = saplingVar == null ? null : this.loadItemStack(cl, saplingVar);
                    if (wood == null || leaf == null || (saplingVar != null && sapling == null)) {
                        DragonAPI.LOGGER.error("Error loading " + this.getLabel() + ": Block not instantiated!");
                        return;
                    }
                    id = Block.byItem(wood.getItem());
                    idleaf = Block.byItem(leaf.getItem());
                    idsapling = Block.byItem(sapling.getItem());
                }
                case INSTANCE -> {
                    Block wood_b = this.loadBlock(cl, blockVar);
                    Block leaf_b = this.loadBlock(cl, leafVar);
                    Block sapling_b = saplingVar == null ? null : this.loadBlock(cl, saplingVar);
                    if (wood_b == null || leaf_b == null || (saplingVar != null && sapling_b == null)) {
                        DragonAPI.LOGGER.error("Error loading " + this.getLabel() + ": Block not instantiated!");
                        return;
                    }
                    id = wood_b;
                    idleaf = leaf_b;
                    idsapling = sapling_b;
                }
                case REGISTRY -> {
                    Block wood_b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(mod.modid, blockVar));
                    Block leaf_b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(mod.modid, leafVar)); //todo check if this this is correct
                    Block sapling_b = saplingVar == null ? null : ForgeRegistries.BLOCKS.getValue(new ResourceLocation(mod.modid, saplingVar));
                    if (wood_b == null || leaf_b == null || (saplingVar != null && sapling_b == null)) {
                        DragonAPI.LOGGER.error("Error loading " + this.getLabel() + ": Block not instantiated!");
                        return;
                    }
                    id = wood_b;
                    idleaf = leaf_b;
                    idsapling = sapling_b;
                }
                default -> {
                    DragonAPI.LOGGER.error("Error loading wood " + this.getLabel());
                    DragonAPI.LOGGER.error("Invalid variable type " + type);
                    return;
                }
            }
            blockID = id;
            leafID = idleaf;
            saplingID = idsapling;
            DragonAPI.LOGGER.info("Successfully loaded wood "+this.getLabel());
            exists = true;
        } catch (SecurityException | IllegalArgumentException | NullPointerException | ReflectiveOperationException e) {
            DragonAPI.LOGGER.error("Error loading wood "+this.getLabel());
            e.printStackTrace();
        }
    }

    private ItemStack loadItemStack(Class cl, String field) throws ReflectiveOperationException {
        Object ins = this.getFieldInstance();
        Field f = cl.getField(field);
        return (ItemStack) f.get(ins);
    }

    private Block loadBlock(Class cl, String field) throws ReflectiveOperationException {
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
            Class c = Class.forName("com.emoniph.witchery.Witchery");
            Field f = c.getField("Blocks");
            return f.get(null);
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getName());
        sb.append(" from ");
        sb.append(mod);
        if (exists) {
            sb.append(" (LOG "+blockID+";");
            sb.append(" ");
            sb.append("LEAF "+leafID+";");
            if (saplingID != null) {
                sb.append(" ");
                sb.append("SAPLING "+saplingID);
            }
            sb.append(")");
        }
        else {
            sb.append(" (Not loaded)");
        }
        return sb.toString();
    }

    public String getBasicInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getName());
        sb.append(" from ");
        sb.append(mod);
        return sb.toString();
    }

    public String getLabel() {
        return this.name()+" from "+this.getParentMod();
    }

    public boolean exists() {
        return exists && this.getParentMod().isLoaded();
    }

    public BlockKey getItem() {
        return new BlockKey(blockID);
    }

    public BlockKey getLogItemWithOffset(int i) {
        return new BlockKey(blockID);
    }

//    public boolean isLogBlock(ItemStack block) {
//        if (blockMeta == null)
//            return false;
//        if (this == SEQUOIA) {
//            return Block.byItem(block.getItem()) == blockID;
//        }
//        for (int i = 0; i < blockMeta.length; i++) {
//            if (ReikaItemHelper.matchStacks(block, this.getLogItemWithOffset(i)))
//                return true;
//        }
//        return false;
//    }

    public Block getBlock() {
        return blockID;
    }

    public String getName() {
        return ReikaStringParser.capFirstChar(this.name());
    }

    @Override
    public Block getLogID() {
        return blockID;
    }

    @Override
    public Block getLeafID() {
        return leafID;
    }

    @Override
    public Block getSaplingID() {
        return saplingID;
    }

    @Override
    public Block getPlankID() {
        return null; //todo fix null
    }


    public static ModWoodList getModWood(Block id) {
        return logMappings.get(id);
    }

    public static ModWoodList getModWood(ItemStack block) {
        return getModWood(Block.byItem(block.getItem()));
    }

    public static ModWoodList getModWoodFromSapling(Block id) {
        return saplingMappings.get(id);
    }

    public static ModWoodList getModWoodFromSapling(ItemStack block) {
        return getModWoodFromSapling(Block.byItem(block.getItem()));
    }

    public static ModWoodList getModWoodFromLeaf(ItemStack block) {
        return getModWoodFromLeaf(Block.byItem(block.getItem()));
    }

    public static ModWoodList getModWoodFromLeaf(Block id) {
        return leafMappings.get(id);
    }

    public static boolean isModWood(ItemStack block) {
        return getModWood(block) != null;
    }

    public static boolean isModWood(Block id) {
        return getModWood(id) != null;
    }

    public static boolean isModLeaf(Block id) {
        return getModWoodFromLeaf(id) != null;
    }

    public static boolean isModLeaf(ItemStack block) {
        return getModWoodFromLeaf(block) != null;
    }

    public static boolean isModSapling(ItemStack block) {
        return getModWoodFromSapling(block) != null;
    }

    public static boolean isModSapling(Block id) {
        return getModWoodFromSapling(id) != null;
    }


    public FallingBlockEntity getFallingBlock(Level world, int x, int y, int z) {
        FallingBlockEntity e = FallingBlockEntity.fall(world, new BlockPos(x+0.5, y+0.5, z+0.5), blockID.defaultBlockState());
        return e;
    }

    public BlockKey getBasicLeaf() {
        return new BlockKey(leafID);
    }

    public ArrayList<ItemStack> getAllLeaves() {
        ArrayList<ItemStack> li = new ArrayList<>();
//        for (int i = 0; i < leafMeta.length; i++) { //todo meta
            li.add(new ItemStack(leafID, 1));
//        }
        return li;
    }

    public ItemStack getCorrespondingSapling() {
        return saplingID == null ? null : new ItemStack(saplingID, 1);
    }

    public ModList getParentMod() {
        return mod;
    }

    public static ModWoodList getRandomWood(Random rand) {
        ModWoodList wood = woodList[rand.nextInt(woodList.length)];
        while (!wood.exists) {
            wood = woodList[rand.nextInt(woodList.length)];
        }
        return wood;
    }

    public boolean isRareTree() {
        if (this.isMagicTFTree())
            return true;
        if (this == SILVERWOOD)
            return true;
        return false;
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

//    public boolean canBePlacedSideways() {
//        return this.getLogMetadatas().size() == 3;
//    }

    @Override
    public boolean canBePlacedSideways() {
        return false;
    }

    public boolean isNaturalLeaf(Level world, BlockPos pos) {
        if (this.getParentMod() == ModList.BOP || this.getParentMod() == ModList.THAUMCRAFT || this.getParentMod() == ModList.NATURA || this.getParentMod() == ModList.TWILIGHT)
        return true;
            return world.getBlockState(pos).getMaterial() == Material.LEAVES; //todo fix this leaf check
    }

    @Override
    public BlockBox getTypicalMaximumSize() {
        return bounds;
    }

    public static Collection<ModWoodList> getAllWoodsByMod(ModList mod) {
        return modMappings.get(mod);
    }

    public static enum VarType {
        ITEMSTACK(),
        INSTANCE(),
        REGISTRY();
        //INT();

        @Override
        public String toString() {
            return "Variable Type "+ ReikaStringParser.capFirstChar(this.name());
        }
    }

    static {
        for (ModWoodList w : woodList) {
            if (w.exists()) {
                Block id = w.blockID;
                Block leaf = w.leafID;
                Block sapling = w.saplingID;
                if (sapling != null)
                    saplingMappings.put(sapling, w);

                modMappings.addValue(w.mod, w);
            }
        }
    }
}
