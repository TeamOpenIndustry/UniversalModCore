package cam72cam.mod.event.platform;

import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.item.Fuzzy;
import com.google.common.collect.ImmutableMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

import java.util.Map;

/**
 * Fired when recipe datapacks are reloaded
 */
public class RegisterRecipeEvent extends Event implements IModBusEvent {
    Map<RecipeType<?>, ImmutableMap.Builder<ResourceLocation, RecipeHolder<?>>> map;
    ImmutableMap.Builder<ResourceLocation, RecipeHolder<?>> builder;

    public RegisterRecipeEvent(Map<RecipeType<?>, ImmutableMap.Builder<ResourceLocation, RecipeHolder<?>>> map, ImmutableMap.Builder<ResourceLocation, RecipeHolder<?>> builder) {
        this.map = map;
        this.builder = builder;
    }

    public void registerCraftingRecipe(RecipeHolder<ShapedRecipe> recipe, Fuzzy... triggers) {
        //Register corresponding unlocking advancement
        CommonEvents.Recipe.RECIPE_ADVENCEMENTS.subscribe(event -> {
            ResourceLocation advancement = ResourceLocation.tryBuild(recipe.id().getNamespace(), "unlock" + recipe.id().getPath());
            event.registerRecipeTrigger(advancement, recipe.id(), triggers);
        });
        map.computeIfAbsent(RecipeType.CRAFTING, o -> ImmutableMap.builder()).put(recipe.id(), recipe);
        builder.put(recipe.id(), recipe);
    }
}