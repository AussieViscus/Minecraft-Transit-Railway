package org.mtr.mod.data;

import org.mtr.mapping.holder.CompoundTag;

import javax.annotation.Nullable;

/** Lamp values are 24-bit RGB, with a separate sentinel for no draw call. */
public final class SignalAppearance {

	public static final int UNLIT = -1;
	public static final int RED = 0xFF0000;
	public static final int YELLOW = 0xFFAA00;
	public static final int GREEN = 0x00FF00;

	// Row order is part of the saved format. Each row contains top, then bottom.
	public enum State {
		STOP(RED, RED), NEXT_STOP(RED, YELLOW), SECOND_STOP(YELLOW, GREEN),
		COOLDOWN_1(RED, RED), COOLDOWN_2(YELLOW, RED), COOLDOWN_3(GREEN, RED), PROCEED(GREEN, RED);

		final int top;
		final int bottom;

		State(int top, int bottom) {
			this.top = top;
			this.bottom = bottom;
		}

		public static State forDisplay(int localAspect, int distanceToStop) {
			if (localAspect == 1) return STOP;
			if (distanceToStop == 1) return NEXT_STOP;
			if (distanceToStop == 2) return SECOND_STOP;
			switch (localAspect) {
				case 2: return COOLDOWN_1;
				case 3: return COOLDOWN_2;
				case 4: return COOLDOWN_3;
				default: return PROCEED;
			}
		}
	}

	public static final int COLOR_COUNT = State.values().length * 2;
	public static final SignalAppearance DEFAULT = new SignalAppearance(new int[0]);
	private final int[] colors;
	private static final String KEY_LAMPS = "aspect_lamps";
	private static final String BRUSH_PRESET = "mtr_two_aspect_lamps";

	private SignalAppearance(int[] colors) {
		this.colors = colors;
	}

	public static SignalAppearance fromArray(int[] colors) {
		if (colors.length != COLOR_COUNT) return DEFAULT;
		for (int color : colors) {
			if (color < UNLIT || color > 0xFFFFFF) return DEFAULT;
		}
		return new SignalAppearance(colors.clone());
	}

	public int getColor(State state, boolean bottom) {
		if (colors.length == 0) return bottom ? state.bottom : state.top;
		return colors[state.ordinal() * 2 + (bottom ? 1 : 0)];
	}

	public int[] toArray() {
		return colors.clone();
	}

	public static SignalAppearance read(CompoundTag tag) {
		return fromArray(tag.getIntArray(KEY_LAMPS));
	}

	public void copyToBrush(CompoundTag tag) {
		// An empty array explicitly copies the original, unconfigured signal behaviour.
		tag.putIntArray(BRUSH_PRESET, toArray());
	}

	@Nullable
	public static SignalAppearance fromBrush(@Nullable CompoundTag tag) {
		if (tag == null || !tag.contains(BRUSH_PRESET, 11)) return null; // NBT int array
		final int[] colors = tag.getIntArray(BRUSH_PRESET);
		if (colors.length == 0) return DEFAULT;
		final SignalAppearance preset = fromArray(colors);
		return preset == DEFAULT ? null : preset;
	}

	public void write(CompoundTag tag) {
		if (this == DEFAULT) {
			tag.remove(KEY_LAMPS);
		} else {
			tag.putIntArray(KEY_LAMPS, toArray());
		}
	}
}
