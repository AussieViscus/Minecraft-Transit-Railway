package org.mtr.mod.item;

import org.mtr.core.data.Rail;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ItemExtension;
import org.mtr.mod.block.BlockNode;
import org.mtr.mod.block.TwoAspectSignalEntity;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.SignalAppearance;
import org.mtr.mod.generated.lang.TranslationProvider;
import org.mtr.mod.packet.PacketUpdateLastRailStyles;

import javax.annotation.Nonnull;

public class ItemBrush extends ItemExtension {

	public ItemBrush(ItemSettings itemSettings) {
		super(itemSettings.maxCount(1));
	}

	/**
	 * Behaviour for shift-clicking on a block can't be defined in the {@link Block#onUse(BlockState, World, BlockPos, PlayerEntity, Hand, BlockHitResult)} method, so that behaviour is defined here instead.
	 */
	@Nonnull
	@Override
	public ActionResult useOnBlock2(ItemUsageContext context) {
		final World world = context.getWorld();
		final PlayerEntity playerEntity = context.getPlayer();
		if (playerEntity != null && playerEntity.isSneaking()) {
			final BlockEntity target = world.getBlockEntity(context.getBlockPos());
			if (target != null && target.data instanceof TwoAspectSignalEntity) {
				if (!world.isClient()) {
					final SignalAppearance preset = SignalAppearance.fromBrush(context.getStack().getTag());
					if (preset == null) {
						playerEntity.sendMessage(TranslationProvider.GUI_MTR_NO_BRUSH_APPEARANCE.getText(), true);
					} else {
						((TwoAspectSignalEntity) target.data).setAppearance(preset);
						playerEntity.sendMessage(TranslationProvider.GUI_MTR_APPLIED_BRUSH_APPEARANCE.getText(), true);
					}
				}
				return ActionResult.SUCCESS;
			}
		}
		if (world.isClient() && playerEntity != null && world.getBlockState(context.getBlockPos()).getBlock().data instanceof BlockNode) {
			final ObjectObjectImmutablePair<Rail, BlockPos> railAndBlockPos = MinecraftClientData.getInstance().getFacingRailAndBlockPos(false);
			if (railAndBlockPos != null) {
				return PacketUpdateLastRailStyles.CLIENT_CACHE.canApplyStylesToRail(playerEntity.getUuid(), railAndBlockPos.left(), true) ? ActionResult.SUCCESS : ActionResult.FAIL;
			}
		}
		return super.useOnBlock2(context);
	}
}
