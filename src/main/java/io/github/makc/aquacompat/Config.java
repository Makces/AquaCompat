package io.github.makc.aquacompat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = AquaCompat.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue NEPTUNIUM_KNIFE_CUTTING_BOARD_BONUS = BUILDER
            .comment("Whether Neptune's Feast should increase fish slice outputs from cutting-board style recipes.")
            .define("neptuniumKnifeCuttingBoardBonus", true);
    private static final ModConfigSpec.BooleanValue NEPTUNIUM_KNIFE_CUTTING_BOARD_LESS_PRIORITY = BUILDER
            .comment("If true, AquaCompat cutting-board recipes defer to non-AquaCompat recipes for the same input and are hidden from JEI when replaced.")
            .define("neptuniumKnifeCuttingBoardLessPriority", true);
    private static final ModConfigSpec.BooleanValue NEPTUNIUM_KNIFE_CUTTING_BOARD_MULTIPLY_COOKED_FISH = BUILDER
            .comment("If true, Neptunium knife cutting-board recipes also multiply outputs for items in c:foods/cooked_fish.")
            .define("neptuniumKnifeCuttingBoardMultiplyCookedFish", false);
    private static final ModConfigSpec.DoubleValue NEPTUNIUM_KNIFE_CUTTING_BOARD_BONUS_MULTIPLIER = BUILDER
            .comment("Output multiplier applied to eligible fish slice items when using a Neptunium Fillet Knife.")
            .defineInRange("neptuniumKnifeCuttingBoardBonusMultiplier", 1.25D, 1.0D, 64.0D);
    private static final ModConfigSpec.BooleanValue NEPTUNIUM_KNIFE_CUTTING_BOARD_ROUND_UP = BUILDER
            .comment("Rounds boosted cutting board outputs up so 25% bonuses still affect 2-slice recipes.")
            .define("neptuniumKnifeCuttingBoardRoundUp", true);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> NEPTUNIUM_KNIFE_CUTTING_BOARD_TAG_WHITELIST = BUILDER
            .comment("Item tags eligible for the Neptunium cutting board bonus.")
            .defineListAllowEmpty(
                    "neptuniumKnifeCuttingBoardTagWhitelist",
                    List.of("forge:raw_fishes", "forge:cooked_fishes", "c:foods/raw_fish", "c:foods/cooked_fish", "minecraft:fishes"),
                    Config::validateResourceLocation
            );
    private static final ModConfigSpec.ConfigValue<List<? extends String>> NEPTUNIUM_KNIFE_CUTTING_BOARD_ITEM_WHITELIST = BUILDER
            .comment("Item ids eligible for the Neptunium cutting board bonus in addition to the tag whitelist.")
            .defineListAllowEmpty("neptuniumKnifeCuttingBoardItemWhitelist", List.of(), Config::validateResourceLocation);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> NEPTUNIUM_KNIFE_CUTTING_BOARD_TAG_BLACKLIST = BUILDER
            .comment("Item tags excluded from the Neptunium cutting board bonus.")
            .defineListAllowEmpty("neptuniumKnifeCuttingBoardTagBlacklist", List.of(), Config::validateResourceLocation);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> NEPTUNIUM_KNIFE_CUTTING_BOARD_ITEM_BLACKLIST = BUILDER
            .comment("Item ids excluded from the Neptunium cutting board bonus.")
            .defineListAllowEmpty("neptuniumKnifeCuttingBoardItemBlacklist", List.of(), Config::validateResourceLocation);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean neptuniumKnifeCuttingBoardBonus;
    public static boolean neptuniumKnifeCuttingBoardLessPriority;
    public static boolean neptuniumKnifeCuttingBoardMultiplyCookedFish;
    public static double neptuniumKnifeCuttingBoardBonusMultiplier;
    public static boolean neptuniumKnifeCuttingBoardRoundUp;
    public static Set<ResourceLocation> neptuniumKnifeCuttingBoardItemWhitelist;
    public static Set<ResourceLocation> neptuniumKnifeCuttingBoardItemBlacklist;
    public static List<TagKey<Item>> neptuniumKnifeCuttingBoardTagWhitelist;
    public static List<TagKey<Item>> neptuniumKnifeCuttingBoardTagBlacklist;

    private static boolean validateResourceLocation(final Object obj) {
        if (!(obj instanceof String id)) {
            return false;
        }
        return ResourceLocation.tryParse(id) != null;
    }

    private static Set<ResourceLocation> parseResourceLocations(List<? extends String> ids) {
        return ids.stream()
                .map(ResourceLocation::parse)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static List<TagKey<Item>> parseItemTags(List<? extends String> ids) {
        return ids.stream()
                .map(ResourceLocation::parse)
                .map(id -> TagKey.create(Registries.ITEM, id))
                .toList();
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        neptuniumKnifeCuttingBoardBonus = NEPTUNIUM_KNIFE_CUTTING_BOARD_BONUS.get();
        neptuniumKnifeCuttingBoardLessPriority = NEPTUNIUM_KNIFE_CUTTING_BOARD_LESS_PRIORITY.get();
        neptuniumKnifeCuttingBoardMultiplyCookedFish = NEPTUNIUM_KNIFE_CUTTING_BOARD_MULTIPLY_COOKED_FISH.get();
        neptuniumKnifeCuttingBoardBonusMultiplier = NEPTUNIUM_KNIFE_CUTTING_BOARD_BONUS_MULTIPLIER.get();
        neptuniumKnifeCuttingBoardRoundUp = NEPTUNIUM_KNIFE_CUTTING_BOARD_ROUND_UP.get();
        neptuniumKnifeCuttingBoardItemWhitelist = parseResourceLocations(NEPTUNIUM_KNIFE_CUTTING_BOARD_ITEM_WHITELIST.get());
        neptuniumKnifeCuttingBoardItemBlacklist = parseResourceLocations(NEPTUNIUM_KNIFE_CUTTING_BOARD_ITEM_BLACKLIST.get());
        neptuniumKnifeCuttingBoardTagWhitelist = parseItemTags(NEPTUNIUM_KNIFE_CUTTING_BOARD_TAG_WHITELIST.get());
        neptuniumKnifeCuttingBoardTagBlacklist = parseItemTags(NEPTUNIUM_KNIFE_CUTTING_BOARD_TAG_BLACKLIST.get());
    }
}
