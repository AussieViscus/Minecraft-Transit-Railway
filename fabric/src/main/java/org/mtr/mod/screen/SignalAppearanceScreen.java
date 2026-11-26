package org.mtr.mod.screen;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.*;
import org.mtr.mod.InitClient;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.data.IGui;
import org.mtr.mod.data.SignalAppearance;
import org.mtr.mod.data.SignalAppearance.State;
import org.mtr.mod.generated.lang.TranslationProvider;
import org.mtr.mod.packet.PacketUpdateSignalAppearance;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.Consumer;

final class SignalAppearanceScreen extends MTRScreenBase implements IGui {

	private final int[] colors = new int[SignalAppearance.COLOR_COUNT];
	private final int[] initialColors;
	private final SignalAppearance originalAppearance;
	private final BlockPos pos;
	private final Consumer<SignalAppearance> save;
	private final State[] states = {State.STOP, State.NEXT_STOP, State.SECOND_STOP, State.PROCEED};
	private static final int[] PRESETS = {SignalAppearance.UNLIT, SignalAppearance.RED, SignalAppearance.YELLOW, SignalAppearance.GREEN};

	SignalAppearanceScreen(SignalColorScreen parent, BlockPos pos, SignalAppearance appearance, Consumer<SignalAppearance> save) {
		super(parent);
		this.pos = pos;
		originalAppearance = appearance;
		this.save = save;
		for (State state : State.values()) {
			colors[state.ordinal() * 2] = appearance.getColor(state, false);
			colors[state.ordinal() * 2 + 1] = appearance.getColor(state, true);
		}
		initialColors = colors.clone();
	}

	@Override
	protected void init2() {
		super.init2();
		final int column = columnWidth();
		for (int i = 0; i < states.length * 2; i++) {
			final int index = states[i / 2].ordinal() * 2 + i % 2;
			final int x = width - (2 - i % 2) * column - TEXT_PADDING;
			final int y = SQUARE_SIZE + (i / 2) * SQUARE_SIZE;
			final ButtonWidgetExtension preset = new ButtonWidgetExtension(0, 0, 0, SQUARE_SIZE, label(colors[index]), button -> {
				final int color = colors[index];
				int next = 0;
				for (int j = 0; j < PRESETS.length; j++) {
					if (color == PRESETS[j]) next = (j + 1) % PRESETS.length;
				}
				colors[index] = PRESETS[next];
				button.setMessage(new Text(label(colors[index]).data));
			});
			place(preset, x, y, column - 28);
			place(new ButtonWidgetExtension(0, 0, 0, SQUARE_SIZE, TextHelper.literal("RGB"), button -> {
				final int initial = colors[index] == SignalAppearance.UNLIT ? RGB_WHITE : colors[index];
				MinecraftClient.getInstance().openScreen(new Screen(new WidgetColorSelector.ColorSelectorScreen(initial, color -> {
					colors[index] = color;
					MinecraftClient.getInstance().openScreen(new Screen(this));
				})));
			}), x + column - 28, y, 26);
		}
		final int y = SQUARE_SIZE * (states.length + 1) + TEXT_PADDING;
		place(new ButtonWidgetExtension(0, 0, 0, SQUARE_SIZE, TranslationProvider.GUI_MTR_COPY_APPEARANCE_TO_BRUSH.getMutableText(), button -> {
			final SignalAppearance preset = Arrays.equals(colors, initialColors) ? originalAppearance : SignalAppearance.fromArray(colors);
			InitClient.REGISTRY_CLIENT.sendPacketToServer(new PacketUpdateSignalAppearance.CopyToBrush(pos, preset));
		}), TEXT_PADDING, y + SQUARE_SIZE, width - TEXT_PADDING * 2);
		place(new ButtonWidgetExtension(0, 0, 0, SQUARE_SIZE, TranslationProvider.GUI_MTR_RESET_SIGNAL_LAMPS.getMutableText(), button -> finish(SignalAppearance.DEFAULT)), TEXT_PADDING, y, width / 2 - TEXT_PADDING * 2);
		place(new ButtonWidgetExtension(0, 0, 0, SQUARE_SIZE, TextHelper.translatable("gui.done"), button -> finish(SignalAppearance.fromArray(colors))), width / 2, y, width / 2 - TEXT_PADDING);
	}

	private void finish(SignalAppearance appearance) {
		save.accept(appearance);
		onClose2();
	}

	private void place(ButtonWidgetExtension button, int x, int y, int w) {
		IDrawing.setPositionAndWidth(button, x, y, w);
		addChild(new ClickableWidget(button));
	}

	private int columnWidth() {
		return Math.min(110, width * 29 / 100);
	}

	private static MutableText label(int color) {
		switch (color) {
			case SignalAppearance.UNLIT: return TranslationProvider.GUI_MTR_LAMP_UNLIT.getMutableText();
			case SignalAppearance.RED: return TextHelper.translatable("color.minecraft.red");
			case SignalAppearance.YELLOW: return TextHelper.translatable("color.minecraft.yellow");
			case SignalAppearance.GREEN: return TextHelper.translatable("color.minecraft.green");
			default: return TextHelper.literal(String.format(Locale.ROOT, "#%06X", color));
		}
	}

	@Override
	public void render(GraphicsHolder graphics, int mouseX, int mouseY, float delta) {
		renderBackground(graphics);
		super.render(graphics, mouseX, mouseY, delta);
		final int column = columnWidth();
		graphics.drawCenteredText(TranslationProvider.GUI_MTR_TOP_LAMP.getMutableText(), width - TEXT_PADDING - 3 * column / 2, TEXT_PADDING, ARGB_WHITE);
		graphics.drawCenteredText(TranslationProvider.GUI_MTR_BOTTOM_LAMP.getMutableText(), width - TEXT_PADDING - column / 2, TEXT_PADDING, ARGB_WHITE);
		for (int i = 0; i < states.length; i++) {
			final State state = states[i];
			final MutableText name = TextHelper.translatable("gui.mtr.signal_" + state.name().toLowerCase(Locale.ROOT));
			graphics.drawText(name, TEXT_PADDING, SQUARE_SIZE * (i + 1) + TEXT_PADDING, ARGB_WHITE, false, GraphicsHolder.getDefaultLight());
		}
	}

	@Override
	public boolean isPauseScreen2() {
		return false;
	}
}
