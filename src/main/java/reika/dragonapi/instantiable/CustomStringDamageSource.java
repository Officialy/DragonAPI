package reika.dragonapi.instantiable;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class CustomStringDamageSource extends DamageSource {

    private final String message;

    /** The type must be a registered datapack {@link DamageType} (resolve via {@link #resolve});
     * a direct/unregistered holder cannot be network-encoded for the damage-event packet. */
    public CustomStringDamageSource(Holder<DamageType> type, String msg) {
        super(type);
        message = msg;
    }

    public static Holder<DamageType> resolve(Level level, ResourceKey<DamageType> key) {
        return level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key);
    }

    @Override
    public Component getLocalizedDeathMessage(LivingEntity e) {
        return Component.literal(e.getName().getString() + " " + message);
    }

}
