package org.mtr.mixin.signal;

import org.mtr.core.data.Position;
import org.mtr.core.data.Vehicle;
import org.mtr.core.data.VehiclePosition;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = Vehicle.class, remap = false)
public interface VehicleAccessor {

	@Invoker("checkAndBlockSignal")
	boolean mtr$checkSignal(int pathIndex, ObjectArrayList<Object2ObjectAVLTreeMap<Position, Object2ObjectAVLTreeMap<Position, VehiclePosition>>> positions, boolean reserveRail, boolean secondPass);
}
