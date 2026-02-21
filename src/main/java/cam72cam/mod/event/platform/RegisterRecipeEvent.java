package cam72cam.mod.event.platform;

import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.item.Fuzzy;
import com.google.common.collect.ImmutableMap;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;

import java.util.Map;

/**
 * Fired when recipe datapacks are reloaded
 */
public class RegisterRecipeEvent extends Event {
    Map<IRecipeType<?>, ImmutableMap.Builder<ResourceLocation, IRecipe<?>>> map;

    public RegisterRecipeEvent(Map<IRecipeType<?>, ImmutableMap.Builder<ResourceLocation, IRecipe<?>>> map) {
        this.map = map;
    }

    public void registerCraftingRecipe(ShapedRecipe recipe, Fuzzy... triggers) {
        //Register corresponding unlocking advancement
        CommonEvents.Recipe.RECIPE_TRIGGERS.subscribe(event -> {
            ResourceLocation advancement = new ResourceLocation(recipe.getId().getNamespace(), "unlock" + recipe.getId().getPath());
            event.registerRecipeTrigger(advancement, recipe.getId(), triggers);
        });
        map.computeIfAbsent(IRecipeType.CRAFTING, o -> ImmutableMap.builder()).put(recipe.getId(), recipe);
    }
}