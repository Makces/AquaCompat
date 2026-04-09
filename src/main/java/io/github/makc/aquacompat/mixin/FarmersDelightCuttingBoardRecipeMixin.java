package io.github.makc.aquacompat.mixin;

import io.github.makc.aquacompat.NeptuniumKnifeBonusHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Pseudo
@Mixin(targets = "vectorwing.farmersdelight.common.crafting.CuttingBoardRecipe", remap = false)
public class FarmersDelightCuttingBoardRecipeMixin {
    @Inject(
            method = "rollResults(Lnet/minecraft/util/RandomSource;I)Ljava/util/List;",
            at = @At("RETURN"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void aquacompat$applyNeptuniumKnifeBonus(RandomSource rand, int fortuneLevel, CallbackInfoReturnable<List<ItemStack>> cir) {
        aquacompat$boostRollResults(rand, cir);
    }

    @Inject(
            method = "rollResults(Lnet/minecraft/util/RandomSource;ILnet/neoforged/neoforge/items/wrapper/RecipeWrapper;)Ljava/util/List;",
            at = @At("RETURN"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void aquacompat$applyLegacyNeptuniumKnifeBonus(RandomSource rand, int fortuneLevel, RecipeWrapper inventory, CallbackInfoReturnable<List<ItemStack>> cir) {
        aquacompat$boostRollResults(rand, cir);
    }

    private static void aquacompat$boostRollResults(RandomSource rand, CallbackInfoReturnable<List<ItemStack>> cir) {
        cir.setReturnValue(NeptuniumKnifeBonusHelper.applyCuttingBoardBonus(cir.getReturnValue(), rand));
    }
}
