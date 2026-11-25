package org.mtr.mod.render;

import org.mtr.core.data.Position;
import org.mtr.mapping.holder.*;
import org.mtr.mod.Init;
import org.mtr.mod.block.BlockNode;
import org.mtr.mod.block.BlockSignalBase;
import org.mtr.mod.client.MinecraftClientData;

import java.util.Map;
import java.util.WeakHashMap;

/** Read-only client lookup. A signal without a unique onward path uses its own aspect. */
final class SignalLookAhead {

	private final Map<BlockSignalBase.BlockEntityBase, Sample[]> samples = new WeakHashMap<>();

	int distanceToStop(BlockSignalBase.BlockEntityBase signal, boolean back) {
		final ClientWorld world = MinecraftClient.getInstance().getWorldMapped();
		if (world == null) return 0;
		final Sample[] sides = samples.computeIfAbsent(signal, unused -> new Sample[2]);
		final int side = back ? 1 : 0;
		final long tick = world.getTime();
		if (sides[side] == null || tick < sides[side].tick || tick - sides[side].tick >= 10) {
			final BlockPos pos = signal.getPos2();
			final float angle = BlockSignalBase.getAngle(world.getBlockState(pos)) + (back ? 270 : 90);
			sides[side] = new Sample(tick, trace(world, pos, angle));
		}
		return sides[side].distance;
	}

	private static int trace(ClientWorld world, BlockPos pos, float angle) {
		if (!loadedAround(world, pos, 4)) return 0;
		final BlockPos start = RenderSignalBase.getNodePos(world, pos, Direction.fromRotation(angle));
		if (start == null) return 0;
		return SignalPath.distanceToStop(MinecraftClientData.getInstance().positionsToRail, Init.blockPosToPosition(start), angle,
				(previous, node, next) -> findHead(world, previous, node, next));
	}

	private static boolean loadedAround(ClientWorld world, BlockPos pos, int radius) {
		return world.isRegionLoaded(pos.add(-radius, 0, -radius), pos.add(radius, 0, radius));
	}

	private static SignalPath.Head findHead(ClientWorld world, Position previous, Position node, Position next) {
		final BlockPos nodePos = Init.positionToBlockPos(node);
		// Every nearby head and its own nearest-node search must be loaded.
		if (!loadedAround(world, nodePos, 8) || !(world.getBlockState(nodePos).getBlock().data instanceof BlockNode)) return SignalPath.Head.UNKNOWN;
		SignalPath.Head found = SignalPath.Head.NONE;
		for (int x = -4; x <= 4; x++) {
			for (int z = -4; z <= 4; z++) {
				for (int y = -5; y <= 5; y++) {
					final BlockPos pos = nodePos.add(x, y, z);
					final BlockState state = world.getBlockState(pos);
					if (!(state.getBlock().data instanceof BlockSignalBase)) continue;
					final BlockEntity entity = world.getBlockEntity(pos);
					if (entity == null || !(entity.data instanceof BlockSignalBase.BlockEntityBase)) return SignalPath.Head.UNKNOWN;
					final BlockSignalBase.BlockEntityBase signal = (BlockSignalBase.BlockEntityBase) entity.data;
					for (int side = 0; side < (signal.isDoubleSided ? 2 : 1); side++) {
						final float angle = BlockSignalBase.getAngle(state) + side * 180 + 90;
						if (!SignalPath.facesRail(node, next, angle)) continue;
						if (!nodePos.equals(RenderSignalBase.getNodePos(world, pos, Direction.fromRotation(angle)))) continue;
						if (found != SignalPath.Head.NONE || SignalPath.facesRail(node, previous, angle)) return SignalPath.Head.UNKNOWN;
						found = RenderSignalBase.getAspectAtNode(nodePos, angle).isOccupied(signal.getSignalColors(side == 1)) ? SignalPath.Head.STOP : SignalPath.Head.CLEAR;
					}
				}
			}
		}
		return found;
	}


	private static final class Sample {
		final long tick;
		final int distance;

		Sample(long tick, int distance) {
			this.tick = tick;
			this.distance = distance;
		}
	}
}
