package org.mtr.test;

import org.junit.jupiter.api.Test;
import org.mtr.mapping.holder.CompoundTag;
import org.mtr.mod.data.SignalAppearance;
import org.mtr.mod.data.SignalAppearance.State;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class SignalAppearanceTest {

	@Test
	void independentSignalsAndLampsSurviveStorageRoundTrip() {
		final int[] saved = new int[SignalAppearance.COLOR_COUNT];
		Arrays.fill(saved, SignalAppearance.UNLIT);
		final SignalAppearance a = SignalAppearance.fromArray(saved);
		saved[0] = 0; // Lit black is distinct from unlit.
		saved[1] = 0xDA03BF;
		final SignalAppearance b = SignalAppearance.fromArray(saved);
		assertEquals(-1, a.getColor(State.STOP, false));
		assertEquals(-1, a.getColor(State.STOP, true));
		final CompoundTag tagA = new CompoundTag();
		final CompoundTag tagB = new CompoundTag();
		a.write(tagA);
		b.write(tagB);
		final SignalAppearance loaded = SignalAppearance.read(tagB.copy());
		assertEquals(0, loaded.getColor(State.STOP, false));
		assertEquals(0xDA03BF, loaded.getColor(State.STOP, true));
		SignalAppearance.DEFAULT.write(tagB);
		assertSame(SignalAppearance.DEFAULT, SignalAppearance.read(tagB));
		assertArrayEquals(a.toArray(), SignalAppearance.read(tagA.copy()).toArray());
		saved[1] = SignalAppearance.UNLIT;
		assertEquals(-1, SignalAppearance.fromArray(saved).getColor(State.STOP, true));
		saved[0] = SignalAppearance.UNLIT;
		saved[1] = SignalAppearance.GREEN;
		assertEquals(-1, SignalAppearance.fromArray(saved).getColor(State.STOP, false));
		assertEquals(SignalAppearance.GREEN, SignalAppearance.fromArray(saved).getColor(State.STOP, true));
	}

	@Test
	void brushKeepsAnIndependentReusablePreset() {
		final CompoundTag brush = new CompoundTag();
		brush.putInt("other_brush_data", 42);
		assertNull(SignalAppearance.fromBrush(brush));
		final int[] colors = new int[SignalAppearance.COLOR_COUNT];
		for (int i = 0; i < colors.length; i++) colors[i] = i % 3 == 0 ? SignalAppearance.UNLIT : i * 0x10203;
		SignalAppearance.fromArray(colors).copyToBrush(brush);
		final CompoundTag reloadedBrush = brush.copy();
		for (int i = 0; i < 10; i++) {
			final CompoundTag target = new CompoundTag();
			target.putBoolean("accept_redstone", true);
			SignalAppearance.fromBrush(reloadedBrush).write(target);
			assertArrayEquals(colors, SignalAppearance.read(target).toArray());
			assertTrue(target.getBoolean("accept_redstone"));
		}
		SignalAppearance.DEFAULT.copyToBrush(brush);
		assertSame(SignalAppearance.DEFAULT, SignalAppearance.fromBrush(brush));
		assertEquals(42, brush.getInt("other_brush_data"));
		assertArrayEquals(colors, SignalAppearance.fromBrush(reloadedBrush).toArray());
	}

	@Test
	void missingOrInvalidDataUsesCheckoutDefaults() {
		assertSame(SignalAppearance.DEFAULT, SignalAppearance.fromArray(new int[0]));
		final int[] invalid = new int[SignalAppearance.COLOR_COUNT];
		invalid[2] = 0x1000000;
		assertSame(SignalAppearance.DEFAULT, SignalAppearance.fromArray(invalid));
		final int[] tops = {SignalAppearance.RED, SignalAppearance.RED, SignalAppearance.YELLOW, SignalAppearance.GREEN, SignalAppearance.GREEN};
		for (int i = 0; i < tops.length; i++) {
			final State state = State.forDisplay(i + 1, 0);
			assertEquals(tops[i], SignalAppearance.DEFAULT.getColor(state, false));
			assertEquals(SignalAppearance.RED, SignalAppearance.DEFAULT.getColor(state, true));
		}
	}
}
