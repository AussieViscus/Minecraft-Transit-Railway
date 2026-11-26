package org.mtr.mod.data;

import org.junit.jupiter.api.Test;
import org.mtr.core.data.*;
import org.mtr.core.tool.Angle;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.IntPredicate;

import static org.junit.jupiter.api.Assertions.*;

class SignalBlocksTest {

	private final Map<Position, Map<Position, Rail>> graph = new HashMap<>();
	private final ObjectArrayList<PathData> path = new ObjectArrayList<>();
	private final Set<Rail> occupied = new HashSet<>();

	private void add(int length, int... colors) {
		final int x = path.isEmpty() ? 0 : (int) path.get(path.size() - 1).getEndDistance();
		final Position from = new Position(x, 0, 0);
		final Position to = new Position(x + length, 0, 0);
		final Rail rail = Rail.newRail(from, Angle.E, to, Angle.W, Rail.Shape.QUADRATIC, 0, new ObjectArrayList<>(), 80, 80, false, false, true, false, true, TransportMode.TRAIN);
		final SignalModification modification = new SignalModification(from, to, false);
		for (int color : colors) modification.putColorToAdd(color);
		rail.applyModification(modification);
		graph.computeIfAbsent(from, unused -> new HashMap<>()).put(to, rail);
		graph.computeIfAbsent(to, unused -> new HashMap<>()).put(from, rail);
		path.add(new PathData(rail, 0, 0, 0, x, x + length, from, Angle.E, to, Angle.W));
	}

	private int passive(int start) {
		final PathData section = path.get(start);
		return SignalBlocks.withoutTrain(graph, section.getOrderedPosition1(), section.getOrderedPosition2(), (from, to, rail) -> occupied.contains(rail));
	}

	@Test
	void departingTrainClearsEveryPostWithoutAClockOrDownstreamHeads() {
		for (int i = 0; i < 6; i++) add(16);
		for (int train = 0; train < path.size(); train++) {
			occupied.clear();
			occupied.add(path.get(train).getRail());
			for (int post = 0; post < path.size(); post++) {
				final int expected = train < post ? SignalBlocks.CLEAR : Math.min(train - post, SignalBlocks.CLEAR);
				assertEquals(expected, passive(post));
				assertEquals(expected, SignalBlocks.onRoute(path, post, i -> occupied.contains(path.get(i).getRail()), i -> fail("Plain rail has no entry reservation")));
			}
		}
	}

	@Test
	void colouredSectionsProtectTheirOwnRailsAndTheExit() {
		add(12, 1);
		add(12, 1);
		add(12, 2);
		add(12, 3);
		occupied.add(path.get(2).getRail());
		assertEquals(0, passive(0));
		assertEquals(0, passive(1));
		occupied.clear();
		occupied.add(path.get(3).getRail());
		assertEquals(1, passive(0));
		assertEquals(0, passive(2));
		occupied.clear();
		assertEquals(SignalBlocks.CLEAR, passive(0));
	}

	@Test
	void passiveJunctionShowsTheRestrictiveBranchAndRoutedTrainUsesItsOwnBranch() {
		add(12, 1);
		add(12, 2);
		add(12);
		final Position fork = path.get(0).getOrderedPosition2();
		final Position otherEnd = new Position(28, 0, 8);
		final Rail branch = Rail.newRail(fork, Angle.E, otherEnd, Angle.W, Rail.Shape.QUADRATIC, 0, new ObjectArrayList<>(), 80, 0, false, false, true, false, true, TransportMode.TRAIN);
		graph.get(fork).put(otherEnd, branch);
		graph.computeIfAbsent(otherEnd, unused -> new HashMap<>()).put(fork, branch);
		occupied.add(branch);
		assertEquals(0, passive(0));
		assertEquals(SignalBlocks.CLEAR, SignalBlocks.onRoute(path, 0, i -> occupied.contains(path.get(i).getRail()), i -> false));
		final Position end = path.get(2).getOrderedPosition2();
		final Position previous = path.get(2).getOrderedPosition1();
		assertEquals(SignalBlocks.CLEAR, SignalBlocks.withoutTrain(graph, end, previous, (from, to, rail) -> rail == branch));
	}

	@Test
	void selectedRouteUsesTheSimulatorsTrainLengthAndClearanceAcrossPlainRails() throws Exception {
		add(16, 1);
		add(16, 1);
		add(16, 2);
		add(4);
		add(40);
		// Exercise the same pinned Core method that the Mixin invoker calls in the running game.
		final Method gate = Vehicle.class.getDeclaredMethod("checkAndBlockSignal", int.class, ObjectArrayList.class, boolean.class, boolean.class);
		gate.setAccessible(true);
		final ObjectArrayList<VehicleCar> cars = new ObjectArrayList<>();
		cars.add(new VehicleCar("test", 10, 2, -3, 3, 0, 0));
		final VehicleExtraData data = VehicleExtraData.create(1, 2, 16, cars, new ObjectArrayList<>(path.subList(0, 1)),
				new ObjectArrayList<>(path.subList(1, 4)), new ObjectArrayList<>(path.subList(4, 5)), path.get(0), false, 1, 1, false, 80, 0);
		final Vehicle train = new Vehicle(data, null, TransportMode.TRAIN, new ClientData());
		assertEquals(10, data.getTotalVehicleLength());
		final int required = 10 + TransportMode.TRAIN.stoppingSpace;
		final PathData exit = path.get(4);
		for (int available : new int[]{required - 1, required, required + 1}) {
			final ObjectArrayList<Object2ObjectAVLTreeMap<Position, Object2ObjectAVLTreeMap<Position, VehiclePosition>>> frames = new ObjectArrayList<>();
			frames.add(new Object2ObjectAVLTreeMap<>());
			frames.add(new Object2ObjectAVLTreeMap<>());
			final VehiclePosition spans = new VehiclePosition();
			spans.addSegment(available - 4, 35, train.getId() + 1);
			frames.get(1).computeIfAbsent(exit.getOrderedPosition1(), unused -> new Object2ObjectAVLTreeMap<>()).put(exit.getOrderedPosition2(), spans);
			final IntPredicate blocked = index -> {
				try {
					return (boolean) gate.invoke(train, index, frames, false, false);
				} catch (ReflectiveOperationException e) {
					throw new AssertionError(e);
				}
			};
			assertEquals(available < required ? 0 : 2, SignalBlocks.onRoute(path, 2, index -> index == 4, blocked));
			assertEquals(available < required ? 1 : SignalBlocks.CLEAR, SignalBlocks.onRoute(path, 0, index -> index == 4, blocked));
			assertFalse(path.get(2).isSignalBlocked(train.getId() + 1, Rail.BlockReservation.DO_NOT_RESERVE));
		}
	}
}
