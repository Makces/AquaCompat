package io.github.makc.aquacompat.data;

import io.github.makc.aquacompat.AquaCompat;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = AquaCompat.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class AquaCompatDataGenerators {
    private AquaCompatDataGenerators() {
    }

    @SubscribeEvent
    static void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeServer(), new AquacultureCuttingBoardRecipeProvider(event.getGenerator().getPackOutput()));
    }
}
