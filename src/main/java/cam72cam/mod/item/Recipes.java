package cam72cam.mod.item;


import cam72cam.mod.event.CommonEvents;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
                for (Fuzzy dependency : dependencies) {
                    if (dependency.enumerate().isEmpty()) {
                        // Don't register recipe
                        return;
                    }
                }
                for (Fuzzy conflict : conflicts) {
                    if (!conflict.enumerate().isEmpty()) {
                        // Don't register recipe
                        return;
                    }
                }

                ResourceLocation itemName = ForgeRegistries.ITEMS.getKey(item.internal().getItem());

                int height = ingredients.length / width;
                NonNullList<Ingredient> input = NonNullList.withSize(ingredients.length, Ingredient.EMPTY);
                for (int i = 0; i < ingredients.length; i++) {
                    Fuzzy ingredient = ingredients[i];
                    if (ingredient != null) {
                        //ForgeHooks#L1171: If oc isn't loaded radio card's recipe will refuse to show, that's normal
                        //TODO Mixing in ClientRecipeBook#L53 incomplete?
                        input.set(i, Ingredient.of(ingredient.getTag()));
                    }
                }

                ShapedRecipe recipe = new ShapedRecipe(itemName, "", CraftingBookCategory.MISC, width, height, input, item.internal());
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