package io.github.makc.aquacompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class NeptuniumFilletBonusManager {
    public static final String DIRECTORY = "neptunium_fillet_bonus";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static volatile Map<ResourceLocation, Definition> definitions = Map.of();

    private NeptuniumFilletBonusManager() {
    }

    public static Optional<Double> getMultiplier(Item item) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        Definition definition = definitions.get(itemId);
        return definition == null ? Optional.empty() : Optional.of(definition.multiplier());
    }

    public static Map<ResourceLocation, Definition> loadDefinitions(ResourceManager resourceManager) {
        Map<ResourceLocation, JsonElement> raw = new LinkedHashMap<>();
        SimpleJsonResourceReloadListener.scanDirectory(resourceManager, DIRECTORY, GSON, raw);

        Map<ResourceLocation, Definition> loaded = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : raw.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }

            JsonObject json = entry.getValue().getAsJsonObject();
            ResourceLocation itemId = ResourceLocation.parse(GsonHelper.getAsString(json, "item"));
            double multiplier = GsonHelper.getAsDouble(json, "multiplier", 1.25D);
            if (multiplier < 1.0D) {
                multiplier = 1.0D;
            }
            loaded.put(itemId, new Definition(itemId, multiplier));
        }
        return Collections.unmodifiableMap(loaded);
    }

    public static void setDefinitions(Map<ResourceLocation, Definition> loadedDefinitions) {
        definitions = loadedDefinitions;
    }

    public static final class ReloadListener extends SimpleJsonResourceReloadListener {
        public ReloadListener() {
            super(GSON, DIRECTORY);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
            setDefinitions(loadDefinitions(resourceManager));
        }
    }

    public record Definition(ResourceLocation itemId, double multiplier) {
    }
}
