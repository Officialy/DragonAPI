package reika.dragonapi.instantiable;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class CustomStringDamageSource extends DamageSource {

    private final String message;

    /** Takes one arg - the rest of the message after the player's name.
     * For example, supplying "was sucked into a jet engine" turns into
     * "[Player] was sucked into a jet engine". */
    public CustomStringDamageSource(String msg) {
        super("custom");
        message = msg;
    }

    @Override
    public Component getLocalizedDeathMessage(LivingEntity pLivingEntity) {
        return Component.literal(I18n.get(pLivingEntity.getName()+" "+message));

    }

    @Override
    public boolean isBypassArmor() {
        return super.isBypassArmor();
    }

    @Override
    public DamageSource bypassInvul() {
        return super.bypassInvul();
    }

}
