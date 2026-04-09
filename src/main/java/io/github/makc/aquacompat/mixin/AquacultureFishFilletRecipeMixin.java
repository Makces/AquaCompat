package io.github.makc.aquacompat.mixin;

import com.teammetallurgy.aquaculture.api.AquacultureAPI;
import com.teammetallurgy.aquaculture.item.crafting.FishFilletRecipe;
import io.github.makc.aquacompat.NeptuniumFilletBonusHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishFilletRecipe.class)
public class AquacultureFishFilletRecipeMixin {
    @Inject(method = "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"), cancellable = true)
    private void aquacompat$useStochasticNeptuniumBonus(CraftingInput craftingInput, HolderLookup.Provider registries, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack result = cir.getReturnValue();
        if (result.isEmpty() || !result.is(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("aquaculture:fish_fillet_raw")))) {
            return;
        }

        ItemStack fish = ItemStack.EMPTY;
        ItemStack tool = ItemStack.EMPTY;
        for (int i = 0; i < craftingInput.size(); i++) {
            ItemStack stack = craftingInput.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (AquacultureAPI.FISH_DATA.hasFilletAmount(stack.getItem())) {
                fish = stack;
            } else if (stack.is(AquacultureAPI.Tags.KNIFE)) {
                tool = stack;
            }
        }

        if (fish.isEmpty() || !NeptuniumFilletBonusHelper.isNeptuniumKnife(tool)) {
            return;
        }

        int baseCount = NeptuniumFilletBonusHelper.getBaseFilletCount(fish);
        NeptuniumFilletBonusHelper.BonusRoll roll = NeptuniumFilletBonusHelper.getBonusRoll(fish.getItem(), baseCount);
        if (roll.guaranteedCount() <= 0) {
            return;
        }

        ItemStack adjusted = result.copy();
        adjusted.setCount(roll.guaranteedCount());
        cir.setReturnValue(adjusted);
    }
}
