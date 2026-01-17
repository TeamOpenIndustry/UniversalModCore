package cam72cam.mod.event.platform;

import cam72cam.mod.ModCore;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;
import com.google.common.collect.ImmutableMap;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

//Only support full height recipe for now
public class RegisterCraftingRecipeEvent extends Event {
    Map<IRecipeType<?>, ImmutableMap.Builder<ResourceLocation, IRecipe<?>>> map;

    public RegisterCraftingRecipeEvent(Map<IRecipeType<?>, ImmutableMap.Builder<ResourceLocation, IRecipe<?>>> map) {
        this.map = map;
    }

    public void register(ItemStack target, int width, List<Fuzzy> ingredients, List<Fuzzy> dependencies, List<Fuzzy> conflicts) {
        ResourceLocation itemName = target.internal.getItem().getRegistryName();
        ResourceLocation name = new ResourceLocation(itemName.getNamespace(), itemName.getPath()
                + ingredients.hashCode() + dependencies.hashCode() + conflicts.hashCode());
        boolean dependencyNotMet = dependencies.stream().anyMatch(f -> f.getTag().getAllElements().isEmpty());
        boolean hasConflict = conflicts.stream().anyMatch(f -> !f.getTag().getAllElements().isEmpty());

        if (dependencyNotMet || hasConflict) {
            ModCore.info("Requirements not met, skipping UMC recipe %s", name.toString());
            return;
        }

        List<Ingredient> n = new ArrayList<>();
        for (Fuzzy ingredient : ingredients) {
            if (ingredient == null || ingredient.isEmpty()) {
                n.add(new Ingredient(Stream.of(new Ingredient.SingleItemList(net.minecraft.item.ItemStack.EMPTY))));
            } else {
                n.add(new Ingredient(Stream.of(new Ingredient.TagList(ingredient.getTag()))));
            }
        }
        NonNullList<Ingredient> ingredient = NonNullList.create();
        ingredient.addAll(n);

        ShapedRecipe recipe = new ShapedRecipe(name, "", width, 3, ingredient, target.internal);

        CommonEvents.Recipe.RECIPE_LISTENER.get().add(event -> {
            ResourceLocation ad = new ResourceLocation(name.getNamespace(), "unlock" + name.getPath());
            event.registerRecipeTrigger(ad, name, ingredients.toArray(new Fuzzy[0]));
        });
        map.computeIfAbsent(IRecipeType.CRAFTING, o -> ImmutableMap.builder()).put(name, recipe);
    }
}
