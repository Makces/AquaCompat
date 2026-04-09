package io.github.makc.aquacompat.integration.jei;

import com.teammetallurgy.aquaculture.api.AquacultureAPI;
import com.teammetallurgy.aquaculture.init.AquaItems;
import io.github.makc.aquacompat.AquaCompat;
import io.github.makc.aquacompat.CuttingRecipeConflictHelper;
import io.github.makc.aquacompat.NeptuniumFilletBonusHelper;
import io.github.makc.aquacompat.NeptuniumFilletBonusManager;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@JeiPlugin
public final class AquaCompatJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_UID = ResourceLocation.fromNamespaceAndPath(AquaCompat.MODID, "jei");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // Native Farmer's Delight cutting-board recipes now carry the Neptunium chance outputs directly.
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void registerCuttingBoardRecipes(IRecipeRegistration registration, Map<ResourceLocation, NeptuniumFilletBonusManager.Definition> definitions) {
        try {
            Class<?> chanceResultClass = Class.forName("vectorwing.farmersdelight.common.crafting.ingredient.ChanceResult");
            Class<?> cuttingBoardRecipeClass = Class.forName("vectorwing.farmersdelight.common.crafting.CuttingBoardRecipe");
            Constructor<?> chanceResultConstructor = chanceResultClass.getConstructor(ItemStack.class, float.class);

            List<RecipeHolder<?>> recipes = new ArrayList<>();
            var recipeManager = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getRecipeManager() : null;
            for (Item fish : AquacultureAPI.FISH_DATA.getFish()) {
                if (!shouldGenerateCuttingBoardPreview(fish, definitions)) {
                    continue;
                }
                if (recipeManager != null && CuttingRecipeConflictHelper.shouldSkipJeiPreview(fish, recipeManager)) {
                    continue;
                }

                int baseCount = AquacultureAPI.FISH_DATA.getFilletAmount(fish, 0);
                NeptuniumFilletBonusHelper.BonusRoll roll = NeptuniumFilletBonusHelper.getBonusRoll(fish, baseCount);
                NonNullList results = NonNullList.create();
                results.add(chanceResultConstructor.newInstance(new ItemStack(AquaItems.FISH_FILLET.get(), roll.guaranteedCount()), 1.0F));
                if (roll.extraChance() > 0.0D) {
                    results.add(chanceResultConstructor.newInstance(new ItemStack(AquaItems.FISH_FILLET.get(), 1), (float) roll.extraChance()));
                }

                ResourceLocation fishId = BuiltInRegistries.ITEM.getKey(fish);
                ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(AquaCompat.MODID, "jei/neptunium_cutting/" + fishId.getPath());
                Object cuttingRecipe = createCuttingBoardRecipe(
                        cuttingBoardRecipeClass,
                        recipeId,
                        Ingredient.of(fish),
                        Ingredient.of(AquaItems.NEPTUNIUM_FILLET_KNIFE.get()),
                        results
                );
                recipes.add(new RecipeHolder<>(recipeId, (Recipe<?>) cuttingRecipe));
            }

            if (!recipes.isEmpty()) {
                registration.addRecipes(resolveCuttingRecipeType(cuttingBoardRecipeClass), (List) recipes);
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static mezz.jei.api.recipe.RecipeType resolveCuttingRecipeType(Class<?> cuttingBoardRecipeClass) throws ReflectiveOperationException {
        try {
            Class<?> fdRecipeTypesClass = Class.forName("vectorwing.farmersdelight.integration.jei.FDRecipeTypes");
            Field cuttingField = fdRecipeTypesClass.getField("CUTTING");
            Object value = cuttingField.get(null);
            if (value instanceof mezz.jei.api.recipe.RecipeType jeiRecipeType) {
                return jeiRecipeType;
            }
        } catch (ClassNotFoundException | NoSuchFieldException ignored) {
            // Fall through to vanilla recipe type conversion.
        }

        Method getTypeMethod = cuttingBoardRecipeClass.getMethod("getType");
        Object recipeType = getTypeMethod.invoke(createCuttingBoardRecipe(
                cuttingBoardRecipeClass,
                ResourceLocation.fromNamespaceAndPath(AquaCompat.MODID, "jei/type_probe"),
                Ingredient.of(AquaItems.FISH_FILLET.get()),
                Ingredient.of(AquaItems.NEPTUNIUM_FILLET_KNIFE.get()),
                NonNullList.create()
        ));
        return mezz.jei.api.recipe.RecipeType.createFromVanilla((net.minecraft.world.item.crafting.RecipeType) recipeType);
    }

    private static Object createCuttingBoardRecipe(Class<?> cuttingBoardRecipeClass, ResourceLocation recipeId, Ingredient input, Ingredient tool, NonNullList<?> results)
            throws ReflectiveOperationException {
        List<Constructor<?>> constructors = Arrays.asList(cuttingBoardRecipeClass.getConstructors());
        for (Constructor<?> constructor : constructors) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            try {
                if (Arrays.equals(parameterTypes, new Class<?>[]{
                        ResourceLocation.class, String.class, Ingredient.class, Ingredient.class, NonNullList.class, String.class
                })) {
                    return constructor.newInstance(recipeId, "", input, tool, results, "");
                }
                if (Arrays.equals(parameterTypes, new Class<?>[]{
                        String.class, Ingredient.class, Ingredient.class, NonNullList.class, Optional.class
                })) {
                    return constructor.newInstance("", input, tool, results, Optional.empty());
                }
            } catch (InvocationTargetException ignored) {
                // Try the next known constructor shape.
            }
        }

        throw new NoSuchMethodException("Unsupported Farmer's Delight CuttingBoardRecipe constructor on class " + cuttingBoardRecipeClass.getName());
    }

    private static boolean shouldGenerateCuttingBoardPreview(Item fish, Map<ResourceLocation, NeptuniumFilletBonusManager.Definition> definitions) {
        int baseCount = AquacultureAPI.FISH_DATA.getFilletAmount(fish, 0);
        if (baseCount <= 0) {
            return false;
        }

        if (fish == net.minecraft.world.item.Items.COD || fish == net.minecraft.world.item.Items.SALMON) {
            return false;
        }

        return definitions.containsKey(BuiltInRegistries.ITEM.getKey(fish));
    }
}
