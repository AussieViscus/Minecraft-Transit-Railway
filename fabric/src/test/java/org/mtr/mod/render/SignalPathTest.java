package org.mtr.mod.render;

import org.junit.jupiter.api.Test;
import org.mtr.core.data.Position;
import org.mtr.core.data.Rail;
import org.mtr.core.data.TransportMode;
import org.mtr.core.tool.Angle;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mod.data.SignalAppearance.State;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SignalPathTest {

	private final Position a = new Position(0, 0, 0);
	private final Position b = new Position(16, 0, 0);
	private final Position c = new Position(32, 0, 0);
	private final Position d = new Position(48, 0, 0);
	private final Map<Position, Map<Position, Rail>> graph = new HashMap<>();
	private final Map<Position, SignalPath.Head> heads = new HashMap<>();

	SignalPathTest() {
		connect(a, b);
		connect(b, c);
		connect(c, d);
	}

	private void connect(Position start, Position end) {
		final Rail rail = Rail.newRail(start, Angle.E, end, Angle.W, Rail.Shape.QUADRATIC, 0, new ObjectArrayList<>(), 80, 80, false, false, true, false, true, TransportMode.TRAIN);
		graph.computeIfAbsent(start, ignored -> new HashMap<>()).put(end, rail);
		graph.computeIfAbsent(end, ignored -> new HashMap<>()).put(start, rail);
	}

	private State display(int localAspect) {
		final int distance = SignalPath.distanceToStop(graph, a, 0, (previous, node, next) -> heads.getOrDefault(node, SignalPath.Head.NONE));
		return State.forDisplay(localAspect, distance);
	}

	@Test
	void clearLocalBlockRespondsToDownstreamChanges() {
		heads.put(b, SignalPath.Head.CLEAR);
		heads.put(c, SignalPath.Head.CLEAR);
		assertEquals(State.PROCEED, display(5));
		heads.put(b, SignalPath.Head.STOP);
		assertEquals(State.NEXT_STOP, display(5));
		heads.put(c, SignalPath.Head.STOP);
		assertEquals(State.NEXT_STOP, display(5));
		assertEquals(State.STOP, display(1));
		heads.put(b, SignalPath.Head.CLEAR);
		assertEquals(State.SECOND_STOP, display(5));
		heads.put(c, SignalPath.Head.CLEAR);
		assertEquals(State.PROCEED, display(5));
	}

	@Test
	void unresolvedPathPreservesLocalCooldown() {
		heads.put(b, SignalPath.Head.UNKNOWN);
		heads.put(c, SignalPath.Head.STOP);
		assertEquals(State.COOLDOWN_2, display(3));
		heads.put(b, SignalPath.Head.STOP);
		connect(b, new Position(32, 0, 16));
		assertEquals(State.COOLDOWN_1, display(2));
	}

	@Test
	void skipsUnsignalledNodesAndOpposingRails() {
		heads.put(c, SignalPath.Head.STOP);
		assertEquals(State.NEXT_STOP, display(5));
		assertEquals(0, SignalPath.distanceToStop(graph, a, 180, (from, node, next) -> SignalPath.Head.STOP));
		final Rail reverseOnly = Rail.newRail(b, Angle.E, c, Angle.W, Rail.Shape.QUADRATIC, 0, new ObjectArrayList<>(), 0, 80, false, false, true, false, true, TransportMode.TRAIN);
		graph.get(b).put(c, reverseOnly);
		assertEquals(0D, reverseOnly.getSpeedLimitMetersPerMillisecond(b));
		assertEquals(State.PROCEED, display(5));
	}
}
