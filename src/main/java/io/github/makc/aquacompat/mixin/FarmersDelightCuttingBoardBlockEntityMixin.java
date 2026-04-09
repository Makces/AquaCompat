package io.github.makc.aquacompat.mixin;

import io.github.makc.aquacompat.NeptuniumKnifeBonusHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "vectorwing.farmersdelight.common.block.entity.CuttingBoardBlockEntity", remap = false)
public abstract class FarmersDelightCuttingBoardBlockEntityMixin {
    @Shadow(remap = false)
    public abstract ItemStack getStoredItem();

    @Inject(method = "processStoredItemUsingTool", at = @At("HEAD"), remap = false)
    private void aquacompat$beginCuttingBoardContext(ItemStack toolStack, Player player, CallbackInfoReturnable<Boolean> cir) {
        NeptuniumKnifeBonusHelper.beginCuttingBoardContext(toolStack, this.getStoredItem());
    }

    @Inject(method = "processStoredItemUsingTool", at = @At("RETURN"), remap = false)
    private void aquacompat$clearCuttingBoardContext(ItemStack toolStack, Player player, CallbackInfoReturnable<Boolean> cir) {
        NeptuniumKnifeBonusHelper.clearCuttingBoardContext();
    }
}
