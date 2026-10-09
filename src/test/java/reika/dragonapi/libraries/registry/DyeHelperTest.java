package reika.dragonapi.libraries.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DyeHelperTest {
    @Test void everyNativeDyeAndWoolKeepsOriginalDamageOrder() {
        for (var color : ReikaDyeHelper.dyes) {
            assertSame(color, ReikaDyeHelper.getColorFromItem(pigment(color)));
            assertTrue(ReikaDyeHelper.isDyeItem(pigment(color)));
            assertEquals(color.ordinal(), color.getDamage());
            assertSame(color, ReikaDyeHelper.getColorFromDamage(color.getDamage()));
            assertEquals(color.getWoolMeta(), color.getDye().getId());
        }
        assertSame(ReikaDyeHelper.BLACK, ReikaDyeHelper.getColorFromDamage(-1));
        assertSame(ReikaDyeHelper.BLACK, ReikaDyeHelper.getColorFromDamage(16));
        assertSame(ReikaDyeHelper.LIGHTGRAY, ReikaDyeHelper.getByName("Light Gray"));
        assertEquals("dyeLightBlue", ReikaDyeHelper.LIGHTBLUE.getOreDictName());
    }

    @Test void typedDyeOverridesItemDefaultWithoutCachingStackComponents() {
        ItemStack stack = pigment(ReikaDyeHelper.RED);
        stack.set(DataComponents.DYE, DyeColor.BLACK);
        assertSame(ReikaDyeHelper.BLACK, ReikaDyeHelper.getColorFromItem(stack));
        stack.set(DataComponents.DYE, DyeColor.LIGHT_BLUE);
        assertSame(ReikaDyeHelper.LIGHTBLUE, ReikaDyeHelper.getColorFromItem(stack));
        assertSame(ReikaDyeHelper.RED, ReikaDyeHelper.getColorFromItem(pigment(ReikaDyeHelper.RED)));
        ItemStack custom = stack(Items.APPLE);
        custom.set(DataComponents.DYE, DyeColor.GREEN);
        assertSame(ReikaDyeHelper.GREEN, ReikaDyeHelper.getColorFromItem(custom));
    }

    @Test void nullEmptyAndUncolouredItemsAreNotDyes() {
        for (ItemStack stack : new ItemStack[]{null, ItemStack.EMPTY, stack(Items.DIAMOND), stack(Items.LEATHER_HELMET)}) {
            assertFalse(ReikaDyeHelper.isDyeItem(stack));
            assertNull(ReikaDyeHelper.getColorFromItem(stack));
            assertNull(ReikaDyeHelper.getColorsFromItem(stack));
        }
    }

    @Test void lookupDoesNotMutateStacksAndResultsCannotPoisonCache() {
        ItemStack stack = pigment(ReikaDyeHelper.MAGENTA);
        stack.setCount(32);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("named pigment"));
        ItemStack original = stack.copy();
        var colors = ReikaDyeHelper.getColorsFromItem(stack);
        assertThrows(UnsupportedOperationException.class, colors::clear);
        ReikaDyeHelper.buildItemCache();
        assertSame(ReikaDyeHelper.MAGENTA, ReikaDyeHelper.getColorFromItem(stack));
        assertTrue(ItemStack.isSameItemSameComponents(stack, original));
        assertEquals(32, stack.getCount());
        assertEquals(0x6699D8, ReikaDyeHelper.LIGHTBLUE.getJavaColor().getRGB() & 0xFFFFFF);
    }

    // JUnit registers items but does not reload 26.3 components. Native GameTests check real item prototypes.
    private static ItemStack stack(Item item) {
        return new ItemStack(new Holder.Direct<>(item, DataComponentMap.EMPTY), 1);
    }

    private static ItemStack pigment(ReikaDyeHelper color) {
        return new ItemStack(new Holder.Direct<>(Items.DYE.pick(color.dye),
                DataComponentMap.builder().set(DataComponents.DYE, color.dye).build()), 1);
    }
}
