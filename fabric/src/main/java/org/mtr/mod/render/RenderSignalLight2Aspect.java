package org.mtr.mod.render;

import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.Init;
import org.mtr.mod.block.BlockSignalBase;
import org.mtr.mod.client.IDrawing;

public class RenderSignalLight2Aspect<T extends BlockSignalBase.BlockEntityBase> extends RenderSignalBase<T> {

	private final boolean redOnTop;
	private final int proceedColor;

	public RenderSignalLight2Aspect(Argument dispatcher, boolean redOnTop, int proceedColor) {
		super(dispatcher, 12, 2);
		this.redOnTop = redOnTop;
		this.proceedColor = proceedColor;
	}

	@Override
	protected void render(StoredMatrixTransformations storedMatrixTransformations, T entity, float tickDelta, int occupiedAspect, boolean isBackSide) {
    // Define positions and colors for clarity
    final float yTop = 0.4375F;
    final float yBottom = 0.0625F;
    final int COLOR_RED = 0xFFFF0000;
    final int COLOR_YELLOW = 0xFFFFAA00;
    final int COLOR_GREEN = 0xFF00FF00;

    int topColor;
    int bottomColor;

    // Determine the color for top and bottom lights based on the aspect
    switch (occupiedAspect) {
        case 1: // Occupied
            topColor = COLOR_RED;
            bottomColor = COLOR_RED;
            break;
        case 2: // Immediately after occupied
            topColor = COLOR_RED;
            bottomColor = COLOR_YELLOW;
            break;
        case 3: // After first cooldown
            topColor = COLOR_YELLOW;
            bottomColor = COLOR_GREEN;
            break;
        case 4: // After second cooldown
            break;
        case 5: // After third cooldown (Final state)
        default: // Failsafe to the final clear state
            topColor = COLOR_GREEN;
            bottomColor = COLOR_RED;
            break;
    }

    // Schedule both lights to be rendered with their determined colors
    MainRenderer.scheduleRender(new Identifier(Init.MOD_ID, "textures/block/white.png"), false, QueuedRenderLayer.LIGHT, (graphicsHolder, offset) -> {
        storedMatrixTransformations.transform(graphicsHolder, offset);

        // Draw the top light
        IDrawing.drawTexture(graphicsHolder, -0.125F, yTop, -0.19375F, 0.125F, yTop + 0.25F, -0.19375F, Direction.UP, topColor, GraphicsHolder.getDefaultLight());

        // Draw the bottom light
        IDrawing.drawTexture(graphicsHolder, -0.125F, yBottom, -0.19375F, 0.125F, yBottom + 0.25F, -0.19375F, Direction.UP, bottomColor, GraphicsHolder.getDefaultLight());

        graphicsHolder.pop();
    });
}
}
