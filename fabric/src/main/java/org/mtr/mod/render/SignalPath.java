package org.mtr.mod.render;

import org.mtr.core.data.Position;
import org.mtr.core.data.Rail;
import org.mtr.core.tool.Angle;
import org.mtr.core.tool.Utilities;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class SignalPath {

	enum Head { NONE, CLEAR, STOP, UNKNOWN }

	interface HeadLookup {
		Head at(Position previous, Position node, Position next);
	}

	static boolean facesRail(Position start, Position end, float angle) {
		final long bearing = Math.round(Math.toDegrees(Math.atan2(end.getZ() - start.getZ(), end.getX() - start.getX())));
		return Math.abs(Utilities.circularDifference(bearing, Math.round(angle), 360)) < 90;
	}

	static int distanceToStop(Map<Position, ? extends Map<Position, Rail>> graph, Position start, float angle, HeadLookup heads) {
		Position node = start;
		Position previous = null;
		Angle heading = null;
		int signalsPassed = 0;
		final Set<Position> visited = new HashSet<>();
		// Bound client work on long stretches of unsignalled track and loops.
		while (visited.size() < 16 && visited.add(node)) {
			final Map<Position, Rail> rails = graph.get(node);
			if (rails == null) break;
			Position next = null;
			for (Map.Entry<Position, Rail> edge : rails.entrySet()) {
				final Rail rail = edge.getValue();
				if (rail.getSpeedLimitMetersPerMillisecond(node) <= 0) continue;
				if (heading == null) {
					if (!facesRail(node, edge.getKey(), angle)) continue;
				} else if (!rail.canTurnBack() && rail.getStartAngle(node) != heading) {
					continue;
				}
				if (next != null || rail.canTurnBack()) return 0;
				next = edge.getKey();
			}
			if (next == null || visited.contains(next)) break;
			if (previous != null) {
				switch (heads.at(previous, node, next)) {
					case UNKNOWN: return 0;
					case STOP: return signalsPassed + 1;
					case CLEAR:
						if (++signalsPassed == 2) return 0;
						break;
				}
			}
			heading = rails.get(next).getStartAngle(next).getOpposite();
			previous = node;
			node = next;
		}
		return 0;
	}
}
