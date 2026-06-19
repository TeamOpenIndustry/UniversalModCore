package cam72cam.mod.event.platform;

import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.item.Fuzzy;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

import java.util.SortedMap;

/**
 * Fired when recipe datapacks are reloaded
 */
public class RegisterRecipeEvent extends Event implements IModBusEvent {
    private final SortedMap<ResourceLocation, Recipe<?>> map;

    public RegisterRecipeEvent(SortedMap<ResourceLocation, Recipe<?>> map) {
        this.map = map;
    }

    public void registerCraftingRecipe(RecipeHolder<ShapedRecipe> recipe, Fuzzy... triggers) {
        //Register corresponding unlocking advancement
        CommonEvents.Recipe.RECIPE_ADVENCEMENTS.subscribe(event -> {
            ResourceLocation advancement = ResourceLocation.tryBuild(recipe.id().location().getNamespace(), "unlock" + recipe.id().location().getPath());
            event.registerRecipeTrigger(advancement, recipe.id().location(), triggers);
        });
        map.put(recipe.id().location(), recipe.value());
    }
}