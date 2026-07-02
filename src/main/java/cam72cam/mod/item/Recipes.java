package cam72cam.mod.item;


import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.util.RegistryUtil;
import net.minecraft.core.HolderSet;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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

                ResourceLocation itemName = BuiltInRegistries.ITEM.getKey(item.internal().getItem());

                int height = ingredients.length / width;
                List<Optional<Ingredient>> input = NonNullList.withSize(ingredients.length, Optional.empty());
                for (int i = 0; i < ingredients.length; i++) {
                    Fuzzy ingredient = ingredients[i];
                    if (ingredient != null) {
                        //ForgeHooks#L1171: If oc isn't loaded radio card's recipe will refuse to show, that's normal
                        HolderSet<Item> tag = RegistryUtil.getRecipeItemTags(ingredient);
                        try {
                            input.set(i, Optional.of(Ingredient.of(tag)));
                        } catch (UnsupportedOperationException e) {
                            //Cannot resolve
                            return;
                        }
                    }
                }

                ShapedRecipe recipe = new ShapedRecipe("", CraftingBookCategory.MISC, new ShapedRecipePattern(width, height, input, Optional.empty()), item.internal());
                event.registerCraftingRecipe(new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, itemName), recipe), ingredients);
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