package org.mtr.mixin.signal;

import org.mtr.core.data.Position;
import org.mtr.core.data.VehiclePosition;
import org.mtr.core.simulation.Simulator;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Simulator.class, remap = false)
public interface SimulatorAccessor {

	@Accessor("vehiclePositions")
	ObjectImmutableList<ObjectArrayList<Object2ObjectAVLTreeMap<Position, Object2ObjectAVLTreeMap<Position, VehiclePosition>>>> mtr$getVehiclePositions();
}
