package io.github.makc.aquacompat.mixin;

import io.github.makc.aquacompat.CuttingRecipeConflictHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Pseudo
@Mixin(targets = "vectorwing.farmersdelight.common.block.entity.CuttingBoardBlockEntity", remap = false)
public abstract class FarmersDelightCuttingBoardRecipeSelectionMixin {
    @Shadow(remap = false)
    public abstract ItemStack getStoredItem();

    @Inject(
            method = "getMatchingRecipe(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)Ljava/util/Optional;",
            at = @At("RETURN"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void aquacompat$preferNonAquacompatRecipes(ItemStack toolStack, Player player, CallbackInfoReturnable<Optional<?>> cir) {
        BlockEntity blockEntity = (BlockEntity) (Object) this;
        if (blockEntity.getLevel() == null) {
            return;
        }

        RecipeManager recipeManager = blockEntity.getLevel().getRecipeManager();
        Optional<RecipeHolder<?>> resolved = CuttingRecipeConflictHelper.resolvePreferredRecipe(
                recipeManager,
                this.getStoredItem(),
                toolStack,
                cir.getReturnValue()
        );
        cir.setReturnValue((Optional<?>) resolved);
    }
}
