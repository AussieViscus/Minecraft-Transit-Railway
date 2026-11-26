package org.mtr.mod.data;

import org.mtr.core.data.PathData;
import org.mtr.core.data.Position;
import org.mtr.core.data.Rail;
import org.mtr.core.tool.Angle;
import org.mtr.libraries.it.unimi.dsi.fastutil.ints.IntAVLTreeSet;

import java.util.*;
import java.util.function.IntPredicate;

/** A plain rail is one block. A coloured block continues while rails share an entry colour, as in Vehicle.checkAndBlockSignal. */
final class SignalBlocks {

	static final int CLEAR = 3;

	interface Occupancy {
		boolean occupied(Position start, Position end, Rail rail);
	}

	static int onRoute(List<PathData> path, int index, IntPredicate occupied, IntPredicate entryBlocked) {
		for (int distance = 0; distance < CLEAR && index < path.size(); distance++) {
			final IntAVLTreeSet colors = path.get(index).getSignalColors();
			if (!colors.isEmpty() && entryBlocked.test(index)) return distance;
			do {
				if (occupied.test(index++)) return distance;
			} while (index < path.size() && !Collections.disjoint(colors, path.get(index).getSignalColors()));
		}
		return CLEAR;
	}

	/** With no approaching train, consider each possible exit and use whole-rail occupancy for an unsized train. */
	static int withoutTrain(Map<Position, ? extends Map<Position, Rail>> graph, Position start, Position end, Occupancy occupancy) {
		return section(graph, start, end, occupancy, 0, 0);
	}

	private static int section(Map<Position, ? extends Map<Position, Rail>> graph, Position start, Position end, Occupancy occupancy, int distance, int occupiedDistance) {
		final IntAVLTreeSet entryColors = graph.get(start).get(end).getSignalColors();
		final Set<Map.Entry<Position, Position>> visited = new HashSet<>();
		final Deque<Map.Entry<Position, Position>> pending = new ArrayDeque<>();
		pending.add(new AbstractMap.SimpleImmutableEntry<>(start, end));
		int result = CLEAR;
		while (!pending.isEmpty()) {
			final Map.Entry<Position, Position> edge = pending.removeFirst();
			if (!visited.add(edge)) continue;
			final Position from = edge.getKey();
			final Position to = edge.getValue();
			final Rail rail = graph.get(from).get(to);
			if (occupancy.occupied(from, to, rail)) return occupiedDistance;
			final Map<Position, Rail> outgoing = graph.get(to);
			if (outgoing == null || distance == CLEAR) continue;
			final Angle heading = rail.getStartAngle(to).getOpposite();
			for (Map.Entry<Position, Rail> next : outgoing.entrySet()) {
				final Rail nextRail = next.getValue();
				if (next.getKey().equals(from) || nextRail.getStartAngle(to) != heading || nextRail.getSpeedLimitMetersPerMillisecond(to) <= 0) continue;
				if (!Collections.disjoint(entryColors, nextRail.getSignalColors())) {
					pending.add(new AbstractMap.SimpleImmutableEntry<>(to, next.getKey()));
				} else if (distance + 1 < CLEAR || !entryColors.isEmpty()) {
					final int atExit = entryColors.isEmpty() ? distance + 1 : distance;
					result = Math.min(result, section(graph, to, next.getKey(), occupancy, distance + 1, atExit));
				}
			}
			if (result == occupiedDistance) return result;
		}
		return result;
	}
}
