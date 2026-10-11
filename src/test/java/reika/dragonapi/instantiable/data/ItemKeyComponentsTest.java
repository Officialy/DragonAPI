package reika.dragonapi.instantiable.data;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ItemKeyComponentsTest {
    @Test void durabilityIsSeparateFromAdditionalData() {
        var input = DataComponentMap.builder().set(DataComponents.DAMAGE, 17)
                .set(DataComponents.CUSTOM_NAME, Component.literal("Name")).build();
        var parts = ItemKeyComponents.split(input);
        assertTrue(parts.variants().isEmpty());
        assertFalse(parts.additional().has(DataComponents.DAMAGE));
        assertEquals(Component.literal("Name"), parts.additional().get(DataComponents.CUSTOM_NAME));
    }

    @Test void declaredVariantsRetainIndependentIdentityAndNormalizeOnlyTheirDeclaredDefault() {
        var type = ItemKeyComponents.variant(DataComponentType.<Integer>builder().networkSynchronized(ByteBufCodecs.VAR_INT).build(), 0);
        var zero = ItemKeyComponents.split(DataComponentMap.builder().set(type, 0).build());
        var absent = ItemKeyComponents.split(DataComponentMap.EMPTY);
        assertEquals(absent, zero);
        var one = ItemKeyComponents.split(DataComponentMap.builder().set(type, 1).build());
        assertEquals(1, one.variants().get(type)); assertFalse(one.additional().has(type));
        assertNotEquals(absent.variants(), one.variants());
        assertThrows(IllegalArgumentException.class, () -> ItemKeyComponents.variant(type, 2));
        assertSame(type, ItemKeyComponents.variant(type, 0));
    }

    @Test void unclassifiedTransientDataRemainsAdditionalAndCannotBecomeDurability() {
        var type = DataComponentType.<Integer>builder().networkSynchronized(ByteBufCodecs.VAR_INT).build();
        var builder = DataComponentMap.builder().set(type, 4);
        var parts = ItemKeyComponents.split(builder.build());
        builder.set(type, 7);
        assertEquals(4, parts.additional().get(type)); assertTrue(parts.variants().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> ItemKeyComponents.variant(DataComponents.DAMAGE, 0));
        assertThrows(IllegalArgumentException.class, () -> ItemKeyComponents.variant(DataComponents.POTION_CONTENTS, PotionContents.EMPTY));
    }

    @Test void potionBaseAndCustomFieldsHaveDisjointMatchingRoles() {
        var plain = new PotionContents(Potions.HEALING);
        var custom = new PotionContents(Optional.of(Potions.HEALING), Optional.of(0x123456), List.of(), Optional.of("custom"));
        var base = ItemKeyComponents.split(DataComponentMap.builder().set(DataComponents.POTION_CONTENTS, plain).build());
        var extra = ItemKeyComponents.split(DataComponentMap.builder().set(DataComponents.POTION_CONTENTS, custom).build());
        var speed = ItemKeyComponents.split(DataComponentMap.builder().set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.SWIFTNESS)).build());
        assertEquals(base.variants(), extra.variants()); assertNotEquals(base.variants(), speed.variants());
        assertTrue(base.additional().isEmpty()); assertEquals(base.additional(), speed.additional());
        var payload = extra.additional().get(DataComponents.POTION_CONTENTS);
        assertTrue(payload.potion().isEmpty()); assertEquals(Optional.of(0x123456), payload.customColor());
        assertEquals(Optional.of("custom"), payload.customName());
    }

    @Test void absentFamilyIdentityIsDistinctWhenNoDefaultWasDeclared() {
        var type = ItemKeyComponents.variant(DataComponentType.<String>builder().networkSynchronized(ByteBufCodecs.STRING_UTF8).build(), null);
        var a = ItemKeyComponents.split(DataComponentMap.builder().set(type, "alpha").build());
        var b = ItemKeyComponents.split(DataComponentMap.builder().set(type, "beta").build());
        assertNotEquals(a.variants(), b.variants());
        assertNotEquals(a.variants(), ItemKeyComponents.split(DataComponentMap.EMPTY).variants());
        assertEquals(a.additional(), b.additional());
    }
}
