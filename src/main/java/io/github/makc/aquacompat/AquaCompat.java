package io.github.makc.aquacompat;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(AquaCompat.MODID)
public class AquaCompat {
    public static final String MODID = "aquacompat";

    public AquaCompat(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
