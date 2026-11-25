package org.mtr.mod.block;

import org.mtr.mapping.holder.*;
import org.mtr.mod.data.SignalAppearance;

public abstract class TwoAspectSignalEntity extends BlockSignalBase.BlockEntityBase {

	private SignalAppearance appearance = SignalAppearance.DEFAULT;

	protected TwoAspectSignalEntity(BlockEntityType<?> type, boolean doubleSided, BlockPos pos, BlockState state) {
		super(type, doubleSided, pos, state);
	}

	public SignalAppearance getAppearance() {
		return appearance;
	}

	public void setAppearance(SignalAppearance appearance) {
		this.appearance = appearance;
		markDirty2();
	}

	@Override
	public void readCompoundTag(CompoundTag tag) {
		super.readCompoundTag(tag);
		appearance = SignalAppearance.read(tag);
	}

	@Override
	public void writeCompoundTag(CompoundTag tag) {
		super.writeCompoundTag(tag);
		appearance.write(tag);
	}
}
