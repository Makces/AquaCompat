package io.github.makc.aquacompat.event;

import io.github.makc.aquacompat.AquaCompat;
import io.github.makc.aquacompat.NeptuniumFilletBonusHelper;
import io.github.makc.aquacompat.NeptuniumFilletBonusManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = AquaCompat.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class AquaCompatCommonEvents {
    private AquaCompatCommonEvents() {
    }

    @SubscribeEvent
    static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new NeptuniumFilletBonusManager.ReloadListener());
    }

    @SubscribeEvent
    static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ItemStack crafted = event.getCrafting();
        if (crafted.isEmpty() || !crafted.is(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse("aquaculture:fish_fillet_raw")))) {
            return;
        }

        CraftContext context = CraftContext.from(event.getInventory());
        if (context == null || !NeptuniumFilletBonusHelper.isNeptuniumKnife(context.tool())) {
            return;
        }

        int baseCount = NeptuniumFilletBonusHelper.getBaseFilletCount(context.fish());
        if (baseCount <= 0) {
            return;
        }

        NeptuniumFilletBonusHelper.BonusRoll roll = NeptuniumFilletBonusHelper.getBonusRoll(context.fish().getItem(), baseCount);
        if (roll.extraChance() > 0.0D && serverPlayer.serverLevel().random.nextDouble() < roll.extraChance()) {
            ItemStack extra = crafted.copyWithCount(1);
            if (!serverPlayer.addItem(extra)) {
                serverPlayer.drop(extra, false);
            }
        }
    }

    private record CraftContext(ItemStack fish, ItemStack tool) {
        static CraftContext from(Container container) {
            List<ItemStack> items = new ArrayList<>(container.getContainerSize());
            for (int i = 0; i < container.getContainerSize(); i++) {
                items.add(container.getItem(i).copy());
            }

            ItemStack fish = ItemStack.EMPTY;
            ItemStack tool = ItemStack.EMPTY;
            CraftingInput input = CraftingInput.of(2, 1, items);
            for (int i = 0; i < input.size(); i++) {
                ItemStack stack = input.getItem(i);
                if (stack.isEmpty()) {
                    continue;
                }
                if (com.teammetallurgy.aquaculture.api.AquacultureAPI.FISH_DATA.hasFilletAmount(stack.getItem())) {
                    fish = stack;
                } else if (stack.is(com.teammetallurgy.aquaculture.api.AquacultureAPI.Tags.KNIFE)) {
                    tool = stack;
                }
            }

            return fish.isEmpty() || tool.isEmpty() ? null : new CraftContext(fish, tool);
        }
    }
}
