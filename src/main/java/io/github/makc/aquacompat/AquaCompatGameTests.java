package io.github.makc.aquacompat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Supplier;

@GameTestHolder(AquaCompat.MODID)
@PrefixGameTestTemplate(false)
public final class AquaCompatGameTests {
    private static final ResourceLocation AQUACULTURE_FILLET_ID = ResourceLocation.parse("aquaculture:fish_fillet_raw");
    private static final ResourceLocation AQUACULTURE_FISH_ID = ResourceLocation.parse("aquaculture:atlantic_cod");

    private AquaCompatGameTests() {
    }

    @GameTest(template = "smoke")
    public static void smoke(GameTestHelper helper) throws ReflectiveOperationException {
        if (!BuiltInRegistries.ITEM.containsKey(AQUACULTURE_FILLET_ID)) {
            throw new IllegalStateException("Aquaculture fillet item is missing: " + AQUACULTURE_FILLET_ID);
        }

        if (!BuiltInRegistries.ITEM.containsKey(AQUACULTURE_FISH_ID)) {
            throw new IllegalStateException("Aquaculture fish item is missing: " + AQUACULTURE_FISH_ID);
        }

        RecipeType<?> cuttingRecipeType = resolveCuttingRecipeType();
        if (cuttingRecipeType == null) {
            throw new IllegalStateException("Farmer's Delight cutting recipe type is unavailable");
        }

        RecipeManager recipeManager = helper.getLevel().getRecipeManager();
        @SuppressWarnings({"rawtypes", "unchecked"})
        List<RecipeHolder<?>> cuttingRecipes = ((List) recipeManager.getAllRecipesFor((RecipeType) cuttingRecipeType)).stream()
                .map(recipe -> (RecipeHolder<?>) recipe)
                .toList();
        boolean hasAquaCompatCuttingRecipe = cuttingRecipes.stream().anyMatch(holder -> holder.id().getNamespace().equals(AquaCompat.MODID));
        if (!hasAquaCompatCuttingRecipe) {
            throw new IllegalStateException("AquaCompat cutting recipes did not load");
        }

        Item fish = BuiltInRegistries.ITEM.get(AQUACULTURE_FISH_ID);
        CuttingRecipeConflictHelper.shouldSkipJeiPreview(fish, recipeManager);

        helper.succeed();
    }

    private static RecipeType<?> resolveCuttingRecipeType() throws ReflectiveOperationException {
        Class<?> modRecipeTypesClass = Class.forName("vectorwing.farmersdelight.common.registry.ModRecipeTypes");
        Field cuttingField = modRecipeTypesClass.getField("CUTTING");
        Object supplier = cuttingField.get(null);
        if (!(supplier instanceof Supplier<?> typedSupplier)) {
            return null;
        }

        Object value = typedSupplier.get();
        return value instanceof RecipeType<?> recipeType ? recipeType : null;
    }
}
