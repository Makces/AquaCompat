package io.github.makc.aquacompat.mixin;

import io.github.makc.aquacompat.CuttingRecipeConflictHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Pseudo
@Mixin(targets = "vectorwing.farmersdelight.integration.jei.FDRecipes", remap = false)
public class FarmersDelightJeiRecipesMixin {
    @Inject(method = "getCuttingBoardRecipes", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void aquacompat$filterBaseCuttingRecipes(CallbackInfoReturnable<List<?>> cir) {
        cir.setReturnValue(CuttingRecipeConflictHelper.filterBaseJeiRecipes(cir.getReturnValue()));
    }
}
