package org.mtr.mod.data;

import org.mtr.core.data.*;
import org.mtr.core.simulation.Simulator;
import org.mtr.core.tool.Utilities;
import org.mtr.libraries.it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mixin.signal.SidingAccessor;
import org.mtr.mixin.signal.SimulatorAccessor;
import org.mtr.mixin.signal.VehicleAccessor;
import org.mtr.mixin.signal.VehicleSchemaAccessor;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Read only on the owning simulator thread. Signal posts in a dimension share the approach index. */
public final class SignalNetwork {

	private final Simulator simulator;
	private final Map<Position, Map<Position, Approach>> approaches = new HashMap<>();
	private long lastIndexMillis = Long.MIN_VALUE;

	public SignalNetwork(Simulator simulator) {
		this.simulator = simulator;
	}

	public void run(Runnable task) {
		simulator.run(task);
	}

	public int distanceToStop(Position node, float angle, IntAVLTreeSet filter) {
		final Map<Position, Rail> rails = simulator.positionsToRail.get(node);
		if (rails == null) return SignalBlocks.CLEAR;
		refreshApproaches();
		final Map<Position, Approach> atNode = approaches.getOrDefault(node, Collections.emptyMap());
		Approach nearest = null;
		for (Map.Entry<Position, Approach> edge : atNode.entrySet()) {
			final Rail rail = rails.get(edge.getKey());
			if (rail != null && matches(rail, node, angle, filter) && edge.getValue().closerThan(nearest)) nearest = edge.getValue();
		}
		if (nearest != null) return routedDistance(nearest);

		int distance = SignalBlocks.CLEAR;
		for (Map.Entry<Position, Rail> edge : rails.entrySet()) {
			if (matches(edge.getValue(), node, angle, filter)) {
				distance = Math.min(distance, SignalBlocks.withoutTrain(simulator.positionsToRail, node, edge.getKey(), (from, to, rail) -> {
					final boolean[] reserved = {false};
					rail.iterateCurrentlyBlockedSignalColors(color -> reserved[0] = true);
					rail.iteratePreBlockedSignalColors(color -> reserved[0] = true);
					return reserved[0] || occupied(from, to, rail);
				}));
			}
		}
		return distance;
	}

	private static boolean matches(Rail rail, Position node, float angle, IntAVLTreeSet filter) {
		return rail.getSpeedLimitMetersPerMillisecond(node) > 0 &&
				Math.abs(Utilities.circularDifference(Math.round(rail.getStartAngle(node).angleDegrees), Math.round(angle), 360)) < 90 &&
				(filter.isEmpty() || !Collections.disjoint(filter, rail.getSignalColors()));
	}

	private int routedDistance(Approach approach) {
		final Vehicle vehicle = approach.vehicle;
		final List<PathData> path = vehicle.vehicleExtraData.immutablePath;
		final ObjectArrayList<Object2ObjectAVLTreeMap<Position, Object2ObjectAVLTreeMap<Position, VehiclePosition>>> positions = positions(vehicle.getTransportMode());
		return SignalBlocks.onRoute(path, approach.index, index -> {
			final PathData part = path.get(index);
			return occupied(part.getOrderedPosition1(), part.getOrderedPosition2(), part.getRail()) ||
					part.isSignalBlocked(vehicle.getId(), Rail.BlockReservation.DO_NOT_RESERVE);
		}, index -> ((VehicleAccessor) vehicle).mtr$checkSignal(index, positions, false, false));
	}

	private boolean occupied(Position from, Position to, Rail rail) {
		final boolean reverse = from.compareTo(to) > 0;
		for (Object2ObjectAVLTreeMap<Position, Object2ObjectAVLTreeMap<Position, VehiclePosition>> frame : positions(rail.getTransportMode())) {
			final VehiclePosition spans = Data.tryGet(frame, reverse ? to : from, reverse ? from : to);
			if (spans != null && spans.getClosestOverlap(0, rail.railMath.getLength(), false, Long.MIN_VALUE) >= 0) return true;
		}
		return false;
	}

	private ObjectArrayList<Object2ObjectAVLTreeMap<Position, Object2ObjectAVLTreeMap<Position, VehiclePosition>>> positions(TransportMode mode) {
		return ((SimulatorAccessor) simulator).mtr$getVehiclePositions().get(mode.ordinal());
	}

	private void refreshApproaches() {
		final long now = simulator.getCurrentMillis();
		if (lastIndexMillis != Long.MIN_VALUE && now >= lastIndexMillis && now - lastIndexMillis < 250) return;
		lastIndexMillis = now;
		approaches.clear();
		for (Siding siding : simulator.sidings) {
			for (Vehicle vehicle : ((SidingAccessor) (Object) siding).mtr$getVehicles()) {
				if (!vehicle.getIsOnRoute()) continue;
				final double progress = ((VehicleSchemaAccessor) vehicle).mtr$getRailProgress();
				final List<PathData> path = vehicle.vehicleExtraData.immutablePath;
				final int current = Math.max(0, Utilities.getIndexFromConditionalList(vehicle.vehicleExtraData.immutablePath, progress));
				for (int index = current; index < path.size(); index++) {
					final PathData part = path.get(index);
					final double distance = part.getStartDistance() - progress;
					if (distance >= 0) {
						final Position from = part.reversePositions ? part.getOrderedPosition2() : part.getOrderedPosition1();
						final Position to = part.reversePositions ? part.getOrderedPosition1() : part.getOrderedPosition2();
						final Map<Position, Approach> atNode = approaches.computeIfAbsent(from, ignored -> new HashMap<>());
						final Approach approach = new Approach(vehicle, index, distance);
						if (approach.closerThan(atNode.get(to))) atNode.put(to, approach);
					}
					if (index > current && part.getDwellTime() > 0) break;
				}
			}
		}
	}

	private static final class Approach {
		final Vehicle vehicle;
		final int index;
		final double distance;

		Approach(Vehicle vehicle, int index, double distance) {
			this.vehicle = vehicle;
			this.index = index;
			this.distance = distance;
		}

		boolean closerThan(Approach other) {
			return other == null || distance < other.distance || distance == other.distance && vehicle.getId() < other.vehicle.getId();
		}
	}
}
