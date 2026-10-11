package reika.dragonapi.modregistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import reika.dragonapi.DragonOptions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OreCatalogueTest {
    private static OreCatalogue.Definition definition(ModOreList ore, String product, String... names) {
        return new OreCatalogue.Definition(ore, List.of(names), product);
    }

    @Test void overlapsAndSharedNamesRetainAllMatchesInOriginalOrder() {
        var definitions = List.of(definition(ModOreList.COPPER, "metal", "shared", "copper"),
                definition(ModOreList.TIN, "metal", "shared"));
        var cache = OreCatalogue.build(definitions, name -> switch (name) {
            case "shared", "copper" -> List.of(Items.STONE, Items.STONE, Items.APPLE);
            case "metal" -> List.of(Items.COPPER_INGOT, Items.COPPER_INGOT);
            default -> List.of();
        });
        assertEquals(List.of(ModOreList.TIN, ModOreList.COPPER), cache.ores(Items.STONE));
        assertEquals(List.of(ModOreList.TIN, ModOreList.COPPER), cache.names("shared"));
        assertEquals(List.of(ModOreList.TIN, ModOreList.COPPER), cache.drops(Items.COPPER_INGOT));
        assertEquals(List.of(ModOreList.TIN, ModOreList.COPPER), cache.drops(Items.STONE));
        assertEquals(List.of(Items.STONE), cache.blocks(ModOreList.COPPER));
        assertEquals(List.of(Items.COPPER_INGOT), cache.products(ModOreList.TIN));
        assertTrue(cache.ores(Items.APPLE).isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> cache.names("shared").clear());
        assertThrows(UnsupportedOperationException.class, () -> cache.ores(Items.STONE).clear());
        assertThrows(UnsupportedOperationException.class, () -> cache.products(ModOreList.TIN).clear());
    }

    @Test void rebuiltSnapshotRemovesReassignsAndLeavesPreviousViewsIntact() {
        var definitions = List.of(definition(ModOreList.TIN, "metal", "tin"), definition(ModOreList.COPPER, "copperMetal", "copper"));
        var members = new HashMap<String, List<Item>>();
        var mutable = new ArrayList<Item>(List.of(Items.STONE));
        members.put("tin", mutable); members.put("metal", List.of(Items.APPLE));
        var old = OreCatalogue.build(definitions, name -> members.getOrDefault(name, List.of()));
        var heldView = old.ores(Items.STONE);
        mutable.clear(); members.remove("metal"); members.put("copper", List.of(Items.STONE));
        var current = OreCatalogue.build(definitions, name -> members.getOrDefault(name, List.of()));
        assertTrue(current.blocks(ModOreList.TIN).isEmpty());
        assertTrue(current.drops(Items.APPLE).isEmpty());
        assertEquals(List.of(ModOreList.COPPER), current.ores(Items.STONE));
        assertEquals(List.of(ModOreList.TIN), heldView);
        assertEquals(List.of(Items.STONE), old.blocks(ModOreList.TIN));
        assertThrows(IllegalStateException.class, () -> OreCatalogue.build(definitions, name -> { throw new IllegalStateException("reload failed"); }));
        assertEquals(List.of(ModOreList.COPPER), current.ores(Items.STONE));
    }

    @Test void productAndOreMembershipUnionDoesNotDuplicateOrSuppressEntries() {
        var definitions = List.of(definition(ModOreList.TIN, "tinProduct", "tin"), definition(ModOreList.COPPER, "copperProduct", "copper"));
        var cache = OreCatalogue.build(definitions, name -> name.equals("copper") ? List.of() : List.of(Items.STONE));
        assertEquals(List.of(ModOreList.TIN), cache.ores(Items.STONE));
        assertEquals(List.of(ModOreList.TIN, ModOreList.COPPER), cache.drops(Items.STONE));
        assertTrue(cache.names("unknown").isEmpty());
        assertTrue(cache.drops(Items.APPLE).isEmpty());
    }

    @Test void nativeTagTranslationPreservesCompoundNamesSpecialSourceNamesAndCompatibility() {
        assertEquals(List.of("c:ores/nether_copper", "c:ores/nethercopper"), ids("oreNetherCopper"));
        assertEquals(List.of("c:ingots/fz_dark_iron", "c:ingots/fzdarkiron"), ids("ingotFzDarkIron"));
        assertEquals(List.of("c:ores/monazit_ore"), ids("MonazitOre"));
        assertEquals(List.of("c:items/forcicium_item"), ids("ForciciumItem"));
        assertTrue(ids("crystalCertusQuartz").contains("c:gems/certus_quartz"));
        assertTrue(ids("dyeBlue").contains("c:gems/lapis"));
        assertEquals(List.of("example:ores/custom"), ids("example:ores/custom"));
        for (var ore : ModOreList.values()) {
            for (var name : ore.getOreDictNames()) assertFalse(OreCatalogueTags.resolve(name).isEmpty());
            assertFalse(OreCatalogueTags.resolve(ore.getProductOreDictName()).isEmpty());
        }
        assertThrows(IllegalArgumentException.class, () -> OreCatalogueTags.resolve(""));
        assertThrows(IllegalArgumentException.class, () -> OreCatalogueTags.resolve(null));
        assertThrows(IllegalArgumentException.class, () -> OreCatalogueTags.resolve("BadName"));
    }

    private static List<String> ids(String name) { return OreCatalogueTags.resolve(name).stream().map(tag -> tag.location().toString()).toList(); }

    @Test void chargedCertusKeepsOriginalDefaultOffAndExplicitOptInWithoutChangingOtherDefinitions() {
        assertFalse(DragonOptions.CHARGEDCERTUS.getDefaultState());
        assertEquals(List.of("oreCertusQuartz"), ModOreList.CERTUSQUARTZ.oreNames(false));
        assertEquals(List.of("oreCertusQuartz", "oreChargedCertusQuartz"), ModOreList.CERTUSQUARTZ.oreNames(true));
        assertTrue(ids("oreChargedCertusQuartz").containsAll(List.of("c:ores/charged_certus_quartz", "c:ores/certus_quartz_charged")));
        assertEquals(ModOreList.TIN.oreNames(false), ModOreList.TIN.oreNames(true));
        assertThrows(UnsupportedOperationException.class, () -> ModOreList.CERTUSQUARTZ.oreNames(true).clear());
    }
}
