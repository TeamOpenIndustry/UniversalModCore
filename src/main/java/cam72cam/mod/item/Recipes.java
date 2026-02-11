package cam72cam.mod.item;


import cam72cam.mod.event.CommonEvents;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/** Recipe registration */
public class Recipes {
    public static ShapedRecipeBuilder shapedRecipe(CustomItem item, int width, Fuzzy... ingredients) {
        return new ShapedRecipeBuilder(new ItemStack(item, 1), width, ingredients);
    }

    public static ShapedRecipeBuilder shapedRecipe(ItemStack item, int width, Fuzzy... ingredients) {
        return new ShapedRecipeBuilder(item, width, ingredients);
    }

    public static class ShapedRecipeBuilder {
        private final List<Fuzzy> dependencies = new ArrayList<>();
        private final List<Fuzzy> conflicts = new ArrayList<>();

        private ShapedRecipeBuilder(ItemStack item, int width, Fuzzy... ingredients) {
            CommonEvents.Recipe.REGISTER.subscribe(event -> {
                boolean dependencyNotMet = dependencies.stream().anyMatch(f -> f.getTag().getAllElements().isEmpty());
                boolean hasConflict = conflicts.stream().anyMatch(f -> !f.getTag().getAllElements().isEmpty());

                if (dependencyNotMet || hasConflict) {
                    // Don't register recipe
                    return;
                }
                ResourceLocation itemName = item.internal.getItem().getRegistryName();

                int height = ingredients.length / width;
                NonNullList<Ingredient> result = NonNullList.withSize(ingredients.length, Ingredient.EMPTY);
                for (int i = 0; i < ingredients.length; i++) {
                    Fuzzy ingredient = ingredients[i];
                    if (ingredient != null && !ingredient.isEmpty()) {
                        result.set(i, new Ingredient(Stream.of(new Ingredient.TagList(ingredient.getTag()))));
                    }
                }

                ShapedRecipe recipe = new ShapedRecipe(itemName, "", width, height, result, item.internal);
                event.registerCraftingRecipe(recipe, ingredients);
            });
        }

        public ShapedRecipeBuilder require(Fuzzy ...dependencies) {
            this.dependencies.addAll(Arrays.asList(dependencies));
            return this;
        }

        public ShapedRecipeBuilder conflicts(Fuzzy ...conflicts) {
            this.conflicts.addAll(Arrays.asList(conflicts));
            return this;
        }
    }
}