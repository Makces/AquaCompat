package io.github.makc.aquacompat;

import com.teammetallurgy.aquaculture.item.crafting.FishFilletRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class NeptuniumKnifeBonusHelper {
    private static final ThreadLocal<ItemStack> CUTTING_BOARD_TOOL = new ThreadLocal<>();
    private static final ThreadLocal<ItemStack> CUTTING_BOARD_INPUT = new ThreadLocal<>();

    private NeptuniumKnifeBonusHelper() {
    }

    public static void beginCuttingBoardContext(ItemStack tool, ItemStack input) {
        CUTTING_BOARD_TOOL.set(tool.copy());
        CUTTING_BOARD_INPUT.set(input.copy());
    }

    public static void clearCuttingBoardContext() {
        CUTTING_BOARD_TOOL.remove();
        CUTTING_BOARD_INPUT.remove();
    }

    public static List<ItemStack> applyCuttingBoardBonus(List<ItemStack> results, RandomSource random) {
        ItemStack tool = CUTTING_BOARD_TOOL.get();
        ItemStack input = CUTTING_BOARD_INPUT.get();
        if (tool == null || results.isEmpty() || !Config.neptuniumKnifeCuttingBoardBonus) {
            return results;
        }
        if (!FishFilletRecipe.isKnifeNeptunium(tool.getItem())) {
            return results;
        }
        if (input == null || input.isEmpty()) {
            return results;
        }

        boolean changed = false;
        List<ItemStack> boostedResults = new ArrayList<>(results.size());
        for (ItemStack stack : results) {
            if (stack.isEmpty() || !isEligibleOutput(stack)) {
                boostedResults.add(stack);
                continue;
            }

            ItemStack boostedStack = stack.copy();
            int boostedCount = NeptuniumFilletBonusHelper.rollBonusCount(input.getItem(), boostedStack.getCount(), random);
            if (boostedCount != boostedStack.getCount()) {
                boostedStack.setCount(boostedCount);
                changed = true;
            }
            boostedResults.add(boostedStack);
        }

        return changed ? boostedResults : results;
    }

    private static boolean isEligibleOutput(ItemStack stack) {
        ResourceLocation itemId = stack.getItemHolder().unwrapKey()
                .map(key -> key.location())
                .orElse(null);
        if (itemId != null && itemId.equals(ResourceLocation.parse("aquaculture:fish_fillet_raw"))) {
            return true;
        }
        if (itemId != null && Config.neptuniumKnifeCuttingBoardItemBlacklist.contains(itemId)) {
            return false;
        }
        for (var tag : Config.neptuniumKnifeCuttingBoardTagBlacklist) {
            if (stack.is(tag)) {
                return false;
            }
        }
        if (itemId != null && Config.neptuniumKnifeCuttingBoardItemWhitelist.contains(itemId)) {
            return true;
        }
        for (var tag : Config.neptuniumKnifeCuttingBoardTagWhitelist) {
            if (stack.is(tag)) {
                return true;
            }
        }
        return false;
    }
}
