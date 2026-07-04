package cam72cam.mod.event.platform;

import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.item.Fuzzy;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;

/**
 * Fired when recipe datapacks are reloaded
 */
public class RegisterRecipeEvent extends Event implements IModBusEvent {
    private final ImmutableMultimap.Builder<RecipeType<?>, RecipeHolder<?>> byType;
    private final ImmutableMap.Builder<ResourceLocation, RecipeHolder<?>> byName;

    public RegisterRecipeEvent(ImmutableMultimap.Builder<RecipeType<?>, RecipeHolder<?>> byType, ImmutableMap.Builder<ResourceLocation, RecipeHolder<?>> byName) {
        this.byType = byType;
        this.byName = byName;
    }

    public void registerCraftingRecipe(RecipeHolder<ShapedRecipe> recipe, Fuzzy... triggers) {
        //Register corresponding unlocking advancement
        CommonEvents.Recipe.RECIPE_ADVENCEMENTS.subscribe(event -> {
            ResourceLocation advancement = ResourceLocation.tryBuild(recipe.id().getNamespace(), "unlock" + recipe.id().getPath());
            event.registerRecipeTrigger(advancement, recipe.id(), triggers);
        });
        byType.put(RecipeType.CRAFTING, recipe);
        byName.put(recipe.id(), recipe);
    }
}