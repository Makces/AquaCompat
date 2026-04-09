package io.github.makc.aquacompat.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.teammetallurgy.aquaculture.api.AquacultureAPI;
import com.teammetallurgy.aquaculture.loot.FishWeightHandler;
import io.github.makc.aquacompat.AquaCompat;
import io.github.makc.aquacompat.NeptuniumFilletBonusHelper;
import io.github.makc.aquacompat.NeptuniumFilletBonusManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class AquacultureCuttingBoardRecipeProvider implements DataProvider {
    private final PackOutput.PathProvider recipePathProvider;
    private final PackOutput.PathProvider bonusPathProvider;

    public AquacultureCuttingBoardRecipeProvider(PackOutput output) {
        this.recipePathProvider = output.createRegistryElementsPathProvider(net.minecraft.core.registries.Registries.RECIPE);
        this.bonusPathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, NeptuniumFilletBonusManager.DIRECTORY);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        ensureFishDataLoaded();

        List<CompletableFuture<?>> writes = new ArrayList<>();
        for (Item fish : AquacultureAPI.FISH_DATA.getFish()) {
            int filletCount = AquacultureAPI.FISH_DATA.getFilletAmount(fish, 0);
            if (filletCount <= 0) {
                continue;
            }

            ResourceLocation fishId = BuiltInRegistries.ITEM.getKey(fish);
            writes.add(DataProvider.saveStable(output, createBonusJson(fishId), this.bonusPathProvider.json(fishId)));

            if (!shouldGenerateRecipe(fish)) {
                continue;
            }

            ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(AquaCompat.MODID, "cutting/" + fishId.getPath());
            writes.add(DataProvider.saveStable(output, createRecipeJson(fish, fishId, filletCount), this.recipePathProvider.json(recipeId)));
        }

        addVanillaRecipe(writes, output, Items.COD, "cod", "farmersdelight:cod_slice", 2);
        addVanillaRecipe(writes, output, Items.SALMON, "salmon", "farmersdelight:salmon_slice", 2);
        addVanillaRecipe(writes, output, Items.COOKED_COD, "cooked_cod", "farmersdelight:cooked_cod_slice", 2);
        addVanillaRecipe(writes, output, Items.COOKED_SALMON, "cooked_salmon", "farmersdelight:cooked_salmon_slice", 2);

        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Aquaculture Farmer's Delight Cutting Recipes";
    }

    private static void ensureFishDataLoaded() {
        if (AquacultureAPI.FISH_DATA.getFish().isEmpty()) {
            FishWeightHandler.registerFishData();
        }
    }

    private static boolean shouldGenerateRecipe(Item fish) {
        int filletCount = AquacultureAPI.FISH_DATA.getFilletAmount(fish, 0);
        if (filletCount <= 0) {
            return false;
        }

        return fish != Items.COD && fish != Items.SALMON;
    }

    private void addVanillaRecipe(List<CompletableFuture<?>> writes, CachedOutput output, Item input, String recipeName, String resultId, int baseCount) {
        ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(AquaCompat.MODID, "cutting/" + recipeName);
        writes.add(DataProvider.saveStable(output, createRecipeJson(input, BuiltInRegistries.ITEM.getKey(input), baseCount, resultId, true), this.recipePathProvider.json(recipeId)));
    }

    private static JsonObject createRecipeJson(Item fish, ResourceLocation fishId, int filletCount) {
        return createRecipeJson(fish, fishId, filletCount, "aquaculture:fish_fillet_raw", false);
    }

    private static JsonObject createRecipeJson(Item fish, ResourceLocation inputId, int baseCount, String resultItemId, boolean addBoneMeal) {
        NeptuniumFilletBonusHelper.BonusRoll roll = NeptuniumFilletBonusHelper.getBonusRoll(fish, baseCount);
        JsonObject json = new JsonObject();
        JsonArray conditions = new JsonArray();
        JsonObject condition = new JsonObject();
        condition.addProperty("type", "neoforge:mod_loaded");
        condition.addProperty("modid", "farmersdelight");
        conditions.add(condition);
        json.add("neoforge:conditions", conditions);
        json.addProperty("type", "farmersdelight:cutting");

        JsonArray ingredients = new JsonArray();
        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("item", inputId.toString());
        ingredients.add(ingredient);
        json.add("ingredients", ingredients);

        JsonArray results = new JsonArray();
        JsonObject resultEntry = new JsonObject();
        JsonObject item = new JsonObject();
        item.addProperty("id", resultItemId);
        item.addProperty("count", roll.guaranteedCount());
        resultEntry.add("item", item);
        results.add(resultEntry);
        if (roll.extraChance() > 0.0D) {
            JsonObject chanceEntry = new JsonObject();
            JsonObject chanceItem = new JsonObject();
            chanceItem.addProperty("id", resultItemId);
            chanceItem.addProperty("count", 1);
            chanceEntry.add("item", chanceItem);
            chanceEntry.addProperty("chance", roll.extraChance());
            results.add(chanceEntry);
        }
        if (addBoneMeal) {
            JsonObject boneMealEntry = new JsonObject();
            JsonObject boneMealItem = new JsonObject();
            boneMealItem.addProperty("id", "minecraft:bone_meal");
            boneMealItem.addProperty("count", 1);
            boneMealEntry.add("item", boneMealItem);
            results.add(boneMealEntry);
        }
        json.add("result", results);

        JsonObject tool = new JsonObject();
        tool.addProperty("item", "aquaculture:neptunium_fillet_knife");
        json.add("tool", tool);
        return json;
    }

    private static JsonObject createBonusJson(ResourceLocation fishId) {
        JsonObject json = new JsonObject();
        json.addProperty("item", fishId.toString());
        json.addProperty("multiplier", 1.25D);
        return json;
    }
}
