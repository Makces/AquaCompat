package io.github.makc.aquacompat;

import com.teammetallurgy.aquaculture.api.AquacultureAPI;
import com.teammetallurgy.aquaculture.api.fish.FishData;
import com.teammetallurgy.aquaculture.init.AquaDataComponents;
import com.teammetallurgy.aquaculture.item.crafting.FishFilletRecipe;
import com.teammetallurgy.aquaculture.misc.AquaConfig;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class NeptuniumFilletBonusHelper {
    private static final double DEFAULT_MULTIPLIER = 1.25D;

    private NeptuniumFilletBonusHelper() {
    }

    public static boolean isNeptuniumKnife(ItemStack stack) {
        return !stack.isEmpty() && FishFilletRecipe.isKnifeNeptunium(stack.getItem());
    }

    public static int getBaseFilletCount(ItemStack fishStack) {
        int filletAmount = AquacultureAPI.FISH_DATA.getFilletAmount(fishStack.getItem(), 0);
        if (filletAmount <= 0) {
            return 0;
        }

        Float fishWeight = fishStack.get(AquaDataComponents.FISH_WEIGHT.get());
        if (AquaConfig.BASIC_OPTIONS.randomWeight.get() && fishStack.has(AquaDataComponents.FISH_WEIGHT) && fishWeight != null) {
            filletAmount = FishData.getFilletAmountFromWeight(fishWeight);
        }
        return filletAmount;
    }

    public static BonusRoll getBonusRoll(Item fish, int baseCount) {
        if (baseCount <= 0) {
            return new BonusRoll(0, 0.0D);
        }

        double multiplier = NeptuniumFilletBonusManager.getMultiplier(fish)
                .orElse(Config.neptuniumKnifeCuttingBoardBonusMultiplier > 0.0D ? Config.neptuniumKnifeCuttingBoardBonusMultiplier : DEFAULT_MULTIPLIER);
        double exact = baseCount * Math.max(1.0D, multiplier);
        int guaranteed = Math.max(baseCount, (int) Math.floor(exact));
        double extraChance = Math.max(0.0D, Math.min(1.0D, exact - Math.floor(exact)));
        return new BonusRoll(guaranteed, extraChance);
    }

    public static int rollBonusCount(Item fish, int baseCount, RandomSource random) {
        BonusRoll roll = getBonusRoll(fish, baseCount);
        if (roll.guaranteedCount <= 0) {
            return 0;
        }

        int count = roll.guaranteedCount;
        if (roll.extraChance > 0.0D && random.nextDouble() < roll.extraChance) {
            count++;
        }
        return count;
    }

    public record BonusRoll(int guaranteedCount, double extraChance) {
    }
}
