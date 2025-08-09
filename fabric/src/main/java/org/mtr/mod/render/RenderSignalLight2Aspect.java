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
    // Define constant positions for top and bottom lights for clarity
    final float yTop = 0.4375F;
    final float yBottom = 0.0625F;

    // Determine the color for the top light
    // If occupied, RED. If clear, GREEN (proceedColor).
    final int topColor = occupiedAspect == 1 ? 0xFFFF0000 : proceedColor;
    
    // The bottom light in your new logic is always RED.
    final int bottomColor = 0xFFFF0000;

    MainRenderer.scheduleRender(new Identifier(Init.MOD_ID, "textures/block/white.png"), false, QueuedRenderLayer.LIGHT, (graphicsHolder, offset) -> {
        storedMatrixTransformations.transform(graphicsHolder, offset);
        
        // MODIFICATION: Draw the top light with its calculated color
        IDrawing.drawTexture(graphicsHolder, -0.125F, yTop, -0.19375F, 0.125F, yTop + 0.25F, -0.19375F, Direction.UP, topColor, GraphicsHolder.getDefaultLight());
        
        // MODIFICATION: Always draw the bottom light as RED
        IDrawing.drawTexture(graphicsHolder, -0.125F, yBottom, -0.19375F, 0.125F, yBottom + 0.25F, -0.19375F, Direction.UP, bottomColor, GraphicsHolder.getDefaultLight());
        
        graphicsHolder.pop();
    });
}
}
