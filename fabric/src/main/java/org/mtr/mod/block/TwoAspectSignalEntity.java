package org.mtr.mod.block;

import org.mtr.core.data.Position;
import org.mtr.libraries.it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import org.mtr.mapping.holder.*;
import org.mtr.mod.Init;
import org.mtr.mod.data.SignalAppearance;
import org.mtr.mod.data.SignalNetwork;

public abstract class TwoAspectSignalEntity extends BlockSignalBase.BlockEntityBase {

	private SignalAppearance appearance = SignalAppearance.DEFAULT;
	// Two bits per face: local Stop, one ahead, two ahead, clear.
	private int distances;
	private boolean samplePending;
	private static final String KEY_DISTANCES = "signal_distances";

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
	public int getActualAspect(boolean occupied, boolean isBackSide) {
		return 1 + ((distances >> (isBackSide ? 2 : 0)) & 3);
	}

	@Override
	public void blockEntityTick() {
		final World world = getWorld2();
		if (world == null || world.isClient() || samplePending) return;
		final BlockPos pos = getPos2();
		if ((world.getTime() + Long.hashCode(pos.asLong())) % 10 != 0) return;
		final SignalNetwork network = Init.getSignalNetwork(world);
		final MinecraftServer server = world.getServer();
		if (network == null || server == null) return;

		final float angle = BlockSignalBase.getAngle(world.getBlockState(pos)) + 90;
		final Position front = node(world, pos, angle);
		final Position back = isDoubleSided ? node(world, pos, angle + 180) : null;
		final IntAVLTreeSet frontFilter = new IntAVLTreeSet(getSignalColors(false));
		final IntAVLTreeSet backFilter = new IntAVLTreeSet(getSignalColors(true));
		samplePending = true;
		network.run(() -> {
			int result = 0;
			try {
				if (front != null) result = network.distanceToStop(front, angle, frontFilter);
				if (back != null) result |= network.distanceToStop(back, angle + 180, backFilter) << 2;
			} finally {
				final int sample = result;
				server.execute(() -> {
					samplePending = false;
					if (!isRemoved2() && distances != sample) {
						distances = sample;
						markDirty2();
					}
				});
			}
		});
	}

	private static Position node(World world, BlockPos pos, float angle) {
		final BlockPos node = BlockSignalBase.getNodePos(world, pos, Direction.fromRotation(angle));
		return node == null ? null : Init.blockPosToPosition(node);
	}

	@Override
	public void readCompoundTag(CompoundTag tag) {
		super.readCompoundTag(tag);
		appearance = SignalAppearance.read(tag);
		distances = tag.getInt(KEY_DISTANCES) & 15;
	}

	@Override
	public void writeCompoundTag(CompoundTag tag) {
		super.writeCompoundTag(tag);
		appearance.write(tag);
		tag.putInt(KEY_DISTANCES, distances);
	}
}
