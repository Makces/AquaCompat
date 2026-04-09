package io.github.makc.aquacompat.event;

import com.teammetallurgy.aquaculture.api.AquacultureAPI;
import io.github.makc.aquacompat.AquaCompat;
import io.github.makc.aquacompat.Config;
import io.github.makc.aquacompat.NeptuniumFilletBonusHelper;
import io.github.makc.aquacompat.NeptuniumFilletBonusManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = AquaCompat.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class AquaCompatClientEvents {
    private static final ResourceLocation NEPTUNIUM_FILLET_KNIFE_ID = ResourceLocation.fromNamespaceAndPath("aquaculture", "neptunium_fillet_knife");

    private AquaCompatClientEvents() {
    }

    @SubscribeEvent
    static void onItemTooltip(ItemTooltipEvent event) {
        if (!Screen.hasShiftDown() || !Screen.hasControlDown()) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }

        if (NeptuniumFilletBonusHelper.isNeptuniumKnife(stack)) {
            addKnifeTechnicalTooltip(event);
            return;
        }

        if (AquacultureAPI.FISH_DATA.hasFilletAmount(stack.getItem())) {
            addFishTechnicalTooltip(event, stack);
        }
    }

    private static void addKnifeTechnicalTooltip(ItemTooltipEvent event) {
        event.getToolTip().add(Component.empty());
        event.getToolTip().add(Component.translatable("tooltip.aquacompat.technical_header").withStyle(ChatFormatting.DARK_AQUA));
        event.getToolTip().add(Component.translatable("tooltip.aquacompat.neptunes_feast.base_effect").withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.translatable(
                "tooltip.aquacompat.neptunes_feast.cutting_board_bonus",
                yesNo(Config.neptuniumKnifeCuttingBoardBonus)
        ).withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.translatable(
                "tooltip.aquacompat.neptunes_feast.multiplier",
                formatMultiplier(Config.neptuniumKnifeCuttingBoardBonusMultiplier)
        ).withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.translatable(
                "tooltip.aquacompat.neptunes_feast.rounding",
                yesNo(Config.neptuniumKnifeCuttingBoardRoundUp)
        ).withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.translatable(
                "tooltip.aquacompat.neptunes_feast.override_source"
        ).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static void addFishTechnicalTooltip(ItemTooltipEvent event, ItemStack fishStack) {
        int baseCount = NeptuniumFilletBonusHelper.getBaseFilletCount(fishStack);
        if (baseCount <= 0) {
            return;
        }

        Item fish = fishStack.getItem();
        NeptuniumFilletBonusHelper.BonusRoll roll = NeptuniumFilletBonusHelper.getBonusRoll(fish, baseCount);
        String fishId = BuiltInRegistries.ITEM.getKey(fish).toString();
        String dataSource = NeptuniumFilletBonusManager.getMultiplier(fish).isPresent()
                ? "tooltip.aquacompat.fish.datapack_override"
                : "tooltip.aquacompat.fish.config_fallback";

        event.getToolTip().add(Component.empty());
        event.getToolTip().add(Component.translatable("tooltip.aquacompat.technical_header").withStyle(ChatFormatting.DARK_AQUA));
        event.getToolTip().add(Component.translatable("tooltip.aquacompat.fish.item_id", fishId).withStyle(ChatFormatting.DARK_GRAY));
        event.getToolTip().add(Component.translatable("tooltip.aquacompat.fish.base_fillets", baseCount).withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.translatable("tooltip.aquacompat.fish.neptunes_feast_guaranteed", roll.guaranteedCount()).withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.translatable(
                "tooltip.aquacompat.fish.neptunes_feast_extra",
                formatPercent(roll.extraChance())
        ).withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.translatable(dataSource).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static Component yesNo(boolean value) {
        return Component.translatable(value ? "tooltip.aquacompat.yes" : "tooltip.aquacompat.no")
                .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private static String formatMultiplier(double value) {
        return String.format(java.util.Locale.ROOT, "x%.2f", value);
    }

    private static String formatPercent(double value) {
        return String.format(java.util.Locale.ROOT, "%.0f%%", value * 100.0D);
    }
}
