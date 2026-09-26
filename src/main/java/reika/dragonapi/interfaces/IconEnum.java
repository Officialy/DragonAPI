package reika.dragonapi.interfaces;

import net.minecraft.resources.Identifier;

/**
 * V33a {@code IconEnum} (with its {@code IconProvider} parent): a named, registrable icon. In 1.7.10 {@code getIcon()}
 * returned the stitched {@code IIcon}; in 26.2 it is the sprite's id in the block atlas, which client code resolves
 * through the atlas when drawing.
 */
public interface IconEnum {

	String name();

	Identifier getIcon();

}
