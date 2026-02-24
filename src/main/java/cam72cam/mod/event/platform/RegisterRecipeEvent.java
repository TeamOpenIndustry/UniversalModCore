package cam72cam.mod.event.platform;

import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.item.Fuzzy;
import com.google.common.collect.ImmutableMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;

import java.util.Map;

/**
 * Fired when recipe datapacks are reloaded
 */
public class RegisterRecipeEvent extends Event implements IModBusEvent {
    Map<RecipeType<?>, ImmutableMap.Builder<ResourceLocation, Recipe<?>>> map;
    ImmutableMap.Builder<ResourceLocation, Recipe<?>> builder;

    public RegisterRecipeEvent(Map<RecipeType<?>, ImmutableMap.Builder<ResourceLocation, Recipe<?>>> map, ImmutableMap.Builder<ResourceLocation, Recipe<?>> builder) {
        this.map = map;
        this.builder = builder;
    }

    public void registerCraftingRecipe(ShapedRecipe recipe, Fuzzy... triggers) {
        //Register corresponding unlocking advancement
        CommonEvents.Recipe.RECIPE_ADVENCEMENTS.subscribe(event -> {
            ResourceLocation advancement = ResourceLocation.fromNamespaceAndPath(recipe.getId().getNamespace(), "unlock" + recipe.getId().getPath());
            event.registerRecipeTrigger(advancement, recipe.getId(), triggers);
        });
        map.computeIfAbsent(RecipeType.CRAFTING, o -> ImmutableMap.builder()).put(recipe.getId(), recipe);
        builder.put(recipe.getId(), recipe);
    }
}