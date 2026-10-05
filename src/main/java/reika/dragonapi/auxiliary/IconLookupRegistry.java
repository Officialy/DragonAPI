package reika.dragonapi.auxiliary;

import java.util.HashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;

import reika.dragonapi.base.DragonAPIMod;
import reika.dragonapi.exception.RegistrationException;
import reika.dragonapi.interfaces.IconEnum;

/**
 * V33a {@code IconLookupRegistry}: mods register their icon enums so an icon can be saved by name and found again --
 * the Particle Spawner stores its particle this way. A name {@code forgefluid_<fluid>} resolves to a {@link FluidDelegate}
 * showing that fluid's still texture.
 */
public class IconLookupRegistry {

	public static final IconLookupRegistry instance = new IconLookupRegistry();

	private final HashMap<String, Class> enums = new HashMap();

	private IconLookupRegistry() {

	}

	public <T extends Enum & IconEnum> void registerIcons(DragonAPIMod mod, Class<T> c) {
		this.registerIcons(mod, c.getEnumConstants());
	}

	public void registerIcons(DragonAPIMod mod, IconEnum[] ar) {
		for (IconEnum e : ar) {
			this.registerIcon(mod, e);
		}
	}

	public void registerIcon(DragonAPIMod mod, IconEnum e) {
		if (!(e instanceof Enum))
			throw new RegistrationException(mod, "Invalid icon object "+e+"!");
		enums.put(e.name(), ((Enum)e).getDeclaringClass());
	}

	public IconEnum getIcon(String s) {
		if (s.startsWith("forgefluid_")) {
			Fluid f = FluidDelegate.lookup(s.substring("forgefluid_".length()));
			return f != null ? new FluidDelegate(f) : null;
		}
		Class c = enums.get(s);
		return c != null ? (IconEnum)Enum.valueOf(c, s) : null;
	}

    public record FluidDelegate(Fluid fluid) implements IconEnum {

        /**
         * A registry id, or (as 1.7.10 fluid names were) a bare name matched against the fluid registry's paths.
         */
        private static Fluid lookup(String name) {
            Identifier id = Identifier.tryParse(name);
            if (id != null && BuiltInRegistries.FLUID.containsKey(id))
                return BuiltInRegistries.FLUID.getValue(id);
            for (Fluid f : BuiltInRegistries.FLUID) {
                if (BuiltInRegistries.FLUID.getKey(f).getPath().equals(name))
                    return f;
            }
            return null;
        }

        @Override
        public String name() {
            return "forgefluid_" + BuiltInRegistries.FLUID.getKey(fluid);
        }

        /**
         * Client only: the fluid's still texture, from its baked fluid model.
         */
        @Override
        public Identifier getIcon() {
            return Minecraft.getInstance().getModelManager().getFluidStateModelSet()
                    .get(fluid.defaultFluidState()).stillMaterial().sprite().contents().name();
        }

    }

}
