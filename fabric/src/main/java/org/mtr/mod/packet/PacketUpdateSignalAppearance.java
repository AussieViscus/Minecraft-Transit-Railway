package org.mtr.mod.packet;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;
import org.mtr.mod.Items;
import org.mtr.mod.block.TwoAspectSignalEntity;
import org.mtr.mod.data.SignalAppearance;

public final class PacketUpdateSignalAppearance extends PacketHandler {

	private final BlockPos pos;
	private final SignalAppearance appearance;

	public PacketUpdateSignalAppearance(BlockPos pos, SignalAppearance appearance) {
		this.pos = pos;
		this.appearance = appearance;
	}

	public PacketUpdateSignalAppearance(PacketBufferReceiver buffer) {
		pos = BlockPos.fromLong(buffer.readLong());
		if (buffer.readBoolean()) {
			final int[] colors = new int[SignalAppearance.COLOR_COUNT];
			for (int i = 0; i < colors.length; i++) colors[i] = buffer.readInt();
			appearance = SignalAppearance.fromArray(colors);
		} else {
			appearance = SignalAppearance.DEFAULT;
		}
	}

	@Override
	public void write(PacketBufferSender buffer) {
		buffer.writeLong(pos.asLong());
		buffer.writeBoolean(appearance != SignalAppearance.DEFAULT);
		for (int color : appearance.toArray()) buffer.writeInt(color);
	}

	@Override
	public void runServer(MinecraftServer server, ServerPlayerEntity player) {
		final World world = player.getEntityWorld();
		if (!world.isRegionLoaded(pos, pos) || !player.isHolding(Items.BRUSH.get()) || player.getBlockPos().getManhattanDistance(new Vector3i(pos.data)) > 12) return;
		final BlockEntity entity = world.getBlockEntity(pos);
		if (entity != null && entity.data instanceof TwoAspectSignalEntity) {
			((TwoAspectSignalEntity) entity.data).setAppearance(appearance);
		}
	}
}
