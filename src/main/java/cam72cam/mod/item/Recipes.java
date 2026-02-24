package cam72cam.mod.item;


import cam72cam.mod.event.CommonEvents;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.NonNullList;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraftforge.common.crafting.ConditionalRecipe;
import net.minecraftforge.common.crafting.conditions.ItemExistsCondition;
import net.minecraftforge.common.crafting.conditions.NotCondition;
import net.minecraftforge.common.crafting.conditions.TagEmptyCondition;

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

                ResourceLocation itemName = item.internal.getItem().getRegistryName();

                int height = ingredients.length / width;
                NonNullList<Ingredient> input = NonNullList.withSize(ingredients.length, Ingredient.EMPTY);
                for (int i = 0; i < ingredients.length; i++) {
                    Fuzzy ingredient = ingredients[i];
                    if (ingredient != null && !ingredient.isEmpty()) {
                        input.set(i, Ingredient.of(ingredient.getTag()));
                    }
                }

                ShapedRecipe recipe = new ShapedRecipe(itemName, "", width, height, input, item.internal);
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