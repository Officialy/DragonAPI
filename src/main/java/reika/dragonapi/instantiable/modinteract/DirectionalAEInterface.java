package reika.dragonapi.instantiable.modinteract;

import java.util.EnumSet;
import java.util.Set;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** 1.7.10 {@code DirectionalAEInterface}: a {@link BasicAEInterface} that only connects on chosen sides. */
public class DirectionalAEInterface extends BasicAEInterface {

	private EnumSet<Direction> sideSet;

	public DirectionalAEInterface(BlockEntity te, ItemStack is) {
		this(te, is, EnumSet.noneOf(Direction.class));
	}

	private DirectionalAEInterface(BlockEntity te, ItemStack is, EnumSet<Direction> sides) {
		super(te, is);
		sideSet = sides;
		this.updateSides();
	}

	public DirectionalAEInterface connect(Direction dir) {
		sideSet.add(dir);
		this.updateSides();
		return this;
	}

	public DirectionalAEInterface disconnect(Direction dir) {
		sideSet.remove(dir);
		this.updateSides();
		return this;
	}

	public DirectionalAEInterface disconnectAll() {
		sideSet.clear();
		this.updateSides();
		return this;
	}

	@Override
	protected Set<Direction> getConnectableSides() {
		//called from the super constructor before sideSet is assigned
		return sideSet != null ? sideSet : EnumSet.noneOf(Direction.class);
	}

	public static DirectionalAEInterface omni(BlockEntity te, ItemStack is) {
		return new DirectionalAEInterface(te, is, EnumSet.allOf(Direction.class));
	}

}
