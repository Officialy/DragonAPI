package reika.dragonapi.interfaces.blockentity;

import javax.annotation.Nullable;

/**
 * A block entity that joins an Applied Energistics 2 grid through a DragonAPI
 * {@link reika.dragonapi.instantiable.modinteract.BasicAEInterface}.
 *
 * <p>1.7.10 machines implemented AE's {@code IGridHost}/{@code IActionHost} directly and had them stripped by
 * {@code @Strippable} when AE was absent. There is no such ASM step any more, and a superinterface that cannot be
 * resolved kills the class at load, so the machine only implements this AE-free marker. {@link
 * reika.dragonapi.modinteract.AEHooks} exposes the returned interface to AE as its in-world grid node host
 * capability.
 */
public interface MEGridHost {

	/** The machine's {@code BasicAEInterface}; typed {@code Object} so AE-less installs never resolve it. Null when AE is absent. */
	@Nullable
	Object getAEInterface();

}
