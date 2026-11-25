package org.mtr.mod.render;

import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.Init;
import org.mtr.mod.block.TwoAspectSignalEntity;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.data.SignalAppearance;

public class RenderSignalLight2Aspect<T extends TwoAspectSignalEntity> extends RenderSignalBase<T> {

	private static final Identifier WHITE = new Identifier(Init.MOD_ID, "textures/block/white.png");
	private final SignalLookAhead lookAhead = new SignalLookAhead();

	public RenderSignalLight2Aspect(Argument dispatcher) {
		super(dispatcher, 12, 2);
	}

	@Override
	protected void render(StoredMatrixTransformations transformations, T entity, float tickDelta, int occupiedAspect, boolean isBackSide) {
		final SignalAppearance appearance = entity.getAppearance();
		final int distance = occupiedAspect == 1 || appearance == SignalAppearance.DEFAULT ? 0 : lookAhead.distanceToStop(entity, isBackSide);
		final SignalAppearance.State state = SignalAppearance.State.forDisplay(occupiedAspect, distance);
		final int top = appearance.getColor(state, false);
		final int bottom = appearance.getColor(state, true);
		if (top == SignalAppearance.UNLIT && bottom == SignalAppearance.UNLIT) return;

		MainRenderer.scheduleRender(WHITE, false, QueuedRenderLayer.LIGHT, (graphics, offset) -> {
			transformations.transform(graphics, offset);
			drawLamp(graphics, 0.4375F, top);
			drawLamp(graphics, 0.0625F, bottom);
			graphics.pop();
		});
	}

	private static void drawLamp(GraphicsHolder graphics, float y, int rgb) {
		if (rgb != SignalAppearance.UNLIT) {
			IDrawing.drawTexture(graphics, -0.125F, y, -0.19375F, 0.125F, y + 0.25F, -0.19375F, Direction.UP, ARGB_BLACK | rgb, GraphicsHolder.getDefaultLight());
		}
	}
}
