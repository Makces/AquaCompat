package io.github.makc.aquacompat;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class CuttingRecipeConflictHelper {
    private static final TagKey<Item> COOKED_FISH_TAG = TagKey.create(Registries.ITEM, ResourceLocation.parse("c:foods/cooked_fish"));
    private static final ResourceLocation NEPTUNIUM_FILLET_KNIFE_ID = ResourceLocation.parse("aquaculture:neptunium_fillet_knife");

    private CuttingRecipeConflictHelper() {
    }

    public static boolean shouldHideBaseJeiRecipe(RecipeHolder<?> holder, List<? extends RecipeHolder<?>> allRecipes) {
        if (!isAquacompatCuttingRecipe(holder)) {
            return false;
        }

        ItemStack input = getPrimaryInputStack(holder);
        if (input.isEmpty()) {
            return false;
        }

        if (shouldDisableAquacompatRecipe(input)) {
            return true;
        }

        if (isNeptuniumSpecificRecipe(holder)) {
            return false;
        }

        return Config.neptuniumKnifeCuttingBoardLessPriority
                && findCompetingNonAquacompatRecipe(allRecipes, input, ItemStack.EMPTY, holder).isPresent();
    }

    public static boolean shouldSkipJeiPreview(Item fish, RecipeManager recipeManager) {
        if (!Config.neptuniumKnifeCuttingBoardLessPriority) {
            return false;
        }

        ItemStack input = new ItemStack(fish);
        return findCompetingNonAquacompatRecipe(getAllCuttingRecipes(recipeManager), input, ItemStack.EMPTY, null).isPresent();
    }

    public static Optional<RecipeHolder<?>> resolvePreferredRecipe(RecipeManager recipeManager, ItemStack input, ItemStack tool, Optional<?> currentRecipe) {
        if (!Config.neptuniumKnifeCuttingBoardLessPriority || input.isEmpty()) {
            return castRecipeHolder(currentRecipe);
        }

        Optional<RecipeHolder<?>> currentHolder = castRecipeHolder(currentRecipe);
        if (currentHolder.isEmpty() || !isAquacompatCuttingRecipe(currentHolder.get())) {
            return currentHolder;
        }

        if (shouldDisableAquacompatRecipe(input)) {
            return findCompetingNonAquacompatRecipe(getAllCuttingRecipes(recipeManager), input, tool, currentHolder.get());
        }

        if (isNeptuniumSpecificRecipe(currentHolder.get())) {
            return currentHolder;
        }

        return findCompetingNonAquacompatRecipe(getAllCuttingRecipes(recipeManager), input, tool, currentHolder.get())
                .or(() -> currentHolder);
    }

    public static List<RecipeHolder<?>> filterBaseJeiRecipes(List<?> recipes) {
        List<RecipeHolder<?>> allHolders = castRecipeHolderList(recipes);
        List<RecipeHolder<?>> holders = new ArrayList<>();
        for (Object recipe : recipes) {
            if (recipe instanceof RecipeHolder<?> holder
                    && !shouldHideBaseJeiRecipe(holder, allHolders)) {
                holders.add(rewriteGenericJeiRecipeForNeptunium(holder, allHolders));
            }
        }
        return holders;
    }

    private static List<RecipeHolder<?>> castRecipeHolderList(List<?> recipes) {
        List<RecipeHolder<?>> holders = new ArrayList<>();
        for (Object recipe : recipes) {
            if (recipe instanceof RecipeHolder<?> holder) {
                holders.add(holder);
            }
        }
        return holders;
    }

    private static Optional<RecipeHolder<?>> castRecipeHolder(Optional<?> recipe) {
        return recipe.filter(RecipeHolder.class::isInstance).map(RecipeHolder.class::cast);
    }

    private static List<RecipeHolder<?>> getAllCuttingRecipes(RecipeManager recipeManager) {
        RecipeType<?> cuttingType = getCuttingRecipeType();
        if (cuttingType == null) {
            return List.of();
        }
        @SuppressWarnings({"rawtypes", "unchecked"})
        List<RecipeHolder<?>> recipes = ((List) recipeManager.getAllRecipesFor((RecipeType) cuttingType)).stream()
                .map(recipe -> (RecipeHolder<?>) recipe)
                .toList();
        return recipes;
    }

    private static RecipeType<?> getCuttingRecipeType() {
        try {
            Class<?> modRecipeTypesClass = Class.forName("vectorwing.farmersdelight.common.registry.ModRecipeTypes");
            Field cuttingField = modRecipeTypesClass.getField("CUTTING");
            Object supplier = cuttingField.get(null);
            if (supplier instanceof java.util.function.Supplier<?> typedSupplier) {
                Object value = typedSupplier.get();
                if (value instanceof RecipeType<?> recipeType) {
                    return recipeType;
                }
            }
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException ignored) {
        }
        return null;
    }

    private static Optional<RecipeHolder<?>> findCompetingNonAquacompatRecipe(List<? extends RecipeHolder<?>> allRecipes, ItemStack input, ItemStack tool, RecipeHolder<?> self) {
        Predicate<RecipeHolder<?>> candidate = holder -> !holder.id().getNamespace().equals(AquaCompat.MODID)
                && (self == null || !holder.id().equals(self.id()))
                && matchesInput(holder, input)
                && matchesTool(holder, tool);

        for (RecipeHolder<?> holder : allRecipes) {
            if (candidate.test(holder)) {
                return Optional.of(holder);
            }
        }
        return Optional.empty();
    }

    private static boolean isAquacompatCuttingRecipe(RecipeHolder<?> holder) {
        return holder.id().getNamespace().equals(AquaCompat.MODID)
                && (holder.id().getPath().startsWith("cutting/") || holder.id().getPath().startsWith("recipe/cutting/"));
    }

    private static boolean shouldDisableAquacompatRecipe(ItemStack input) {
        return !Config.neptuniumKnifeCuttingBoardMultiplyCookedFish && input.is(COOKED_FISH_TAG);
    }

    private static RecipeHolder<?> rewriteGenericJeiRecipeForNeptunium(RecipeHolder<?> holder, List<? extends RecipeHolder<?>> allRecipes) {
        if (isAquacompatCuttingRecipe(holder)) {
            return holder;
        }

        ItemStack input = getPrimaryInputStack(holder);
        if (input.isEmpty()) {
            return holder;
        }

        boolean hasNeptuniumVariant = allRecipes.stream()
                .anyMatch(candidate -> isAquacompatCuttingRecipe(candidate)
                        && isNeptuniumSpecificRecipe(candidate)
                        && matchesInput(candidate, input));
        if (!hasNeptuniumVariant) {
            return holder;
        }

        try {
            Method getToolMethod = holder.value().getClass().getMethod("getTool");
            Object toolIngredient = getToolMethod.invoke(holder.value());
            if (!(toolIngredient instanceof Ingredient ingredient)) {
                return holder;
            }

            ItemStack[] filteredTools = java.util.Arrays.stream(ingredient.getItems())
                    .filter(stack -> stack.getItemHolder().unwrapKey().map(key -> !key.location().equals(NEPTUNIUM_FILLET_KNIFE_ID)).orElse(true))
                    .toArray(ItemStack[]::new);
            if (filteredTools.length == ingredient.getItems().length || filteredTools.length == 0) {
                return holder;
            }

            Ingredient rewrittenTool = Ingredient.of(filteredTools);
            Object clonedRecipe = cloneRecipeWithTool(holder.value(), rewrittenTool);
            if (!(clonedRecipe instanceof Recipe<?> recipe)) {
                return holder;
            }
            return new RecipeHolder<>(holder.id(), recipe);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            return holder;
        }
    }

    private static boolean isNeptuniumSpecificRecipe(RecipeHolder<?> holder) {
        try {
            Method getToolMethod = holder.value().getClass().getMethod("getTool");
            Object toolIngredient = getToolMethod.invoke(holder.value());
            if (!(toolIngredient instanceof Ingredient ingredient)) {
                return false;
            }

            ItemStack[] toolItems = ingredient.getItems();
            return toolItems.length == 1
                    && toolItems[0].getItemHolder().unwrapKey().map(key -> key.location().equals(NEPTUNIUM_FILLET_KNIFE_ID)).orElse(false);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            return false;
        }
    }

    private static Object cloneRecipeWithTool(Recipe<?> recipe, Ingredient rewrittenTool) {
        try {
            Class<?> recipeClass = recipe.getClass();
            Method getGroupMethod = recipeClass.getMethod("getGroup");
            Method getIngredientsMethod = recipeClass.getMethod("getIngredients");
            Method getRollableResultsMethod = recipeClass.getMethod("getRollableResults");
            Method getSoundEventMethod = recipeClass.getMethod("getSoundEvent");

            String group = (String) getGroupMethod.invoke(recipe);
            Object ingredients = getIngredientsMethod.invoke(recipe);
            Ingredient input = ingredients instanceof List<?> list && !list.isEmpty() && list.getFirst() instanceof Ingredient ingredient
                    ? ingredient
                    : Ingredient.EMPTY;
            Object results = getRollableResultsMethod.invoke(recipe);
            Object soundEvent = getSoundEventMethod.invoke(recipe);

            Constructor<?> constructor = recipeClass.getConstructor(String.class, Ingredient.class, Ingredient.class, net.minecraft.core.NonNullList.class, Optional.class);
            return constructor.newInstance(group, input, rewrittenTool, results, soundEvent);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException ignored) {
            return recipe;
        }
    }

    private static ItemStack getPrimaryInputStack(RecipeHolder<?> holder) {
        Recipe<?> recipe = holder.value();
        if (recipe.getIngredients().isEmpty()) {
            return ItemStack.EMPTY;
        }
        Ingredient ingredient = recipe.getIngredients().get(0);
        ItemStack[] items = ingredient.getItems();
        return items.length == 0 ? ItemStack.EMPTY : items[0].copy();
    }

    private static boolean matchesInput(RecipeHolder<?> holder, ItemStack input) {
        if (input.isEmpty()) {
            return false;
        }
        Recipe<?> recipe = holder.value();
        if (recipe.getIngredients().isEmpty()) {
            return false;
        }
        return recipe.getIngredients().get(0).test(input);
    }

    private static boolean matchesTool(RecipeHolder<?> holder, ItemStack tool) {
        if (tool.isEmpty()) {
            return true;
        }

        try {
            Method getToolMethod = holder.value().getClass().getMethod("getTool");
            Object toolIngredient = getToolMethod.invoke(holder.value());
            return toolIngredient instanceof Ingredient ingredient && ingredient.test(tool);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            return true;
        }
    }
}
