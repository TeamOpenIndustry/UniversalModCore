package cam72cam.mod.item;

import com.google.gson.JsonElement;
import net.minecraft.advancement.criterion.InventoryChangedCriterion;
import net.minecraft.data.server.recipe.ShapedRecipeJsonFactory;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.util.Identifier;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Recipe registration */
public class Recipes {

    public static ShapedRecipeBuilder shapedRecipe(CustomItem item, int width, Fuzzy... ingredients) {
        return new ShapedRecipeBuilder(new ItemStack(item, 1), width, ingredients);
    }

    public static ShapedRecipeBuilder shapedRecipe(ItemStack item, int width, Fuzzy... ingredients) {
        return new ShapedRecipeBuilder(item, width, ingredients);
    }

    private static final List<Consumer<Map<Identifier, JsonElement>>> actions = new ArrayList<>();
    public static void apply(Map<Identifier, JsonElement> map) {
        actions.forEach(c -> c.accept(map));
    }

    public static class ShapedRecipeBuilder {
        private List<Fuzzy> dependencies = new ArrayList<>();
        private List<Fuzzy> conflicts = new ArrayList<>();

        private ShapedRecipeBuilder(ItemStack item, int width, Fuzzy... ingredients) {
            actions.add(map -> {
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

                ShapedRecipeJsonFactory builder = new ShapedRecipeJsonFactory(item.internal.getItem(), item.getCount());

                int height = ingredients.length / width;

                for (int h = 0; h < height; h++) {
                    String line = "";
                    for (int w = 0; w < width; w++) {
                        int idx = h * width + w;
                        Fuzzy ingredient = ingredients[idx];
                        line += ingredient == null ? " " : idx + "";
                        if (ingredient != null) {
                            // TODO tags
                            builder.input((idx + "").charAt(0), ingredient.tag);
                            builder.criterion(
                                    "has" + ingredient.toString() + idx,
                                    InventoryChangedCriterion.Conditions.items(
                                            ItemPredicate.Builder.create().tag(ingredient.tag).build()
                                    )
                            );
                        }
                    }
                    builder.pattern(line);
                }

                Identifier itemName = new Identifier(item.internal.getItem().getTranslationKey());
                Identifier name = new Identifier(itemName.getNamespace(), itemName.getPath() + Arrays.hashCode(ingredients) + dependencies.hashCode() + conflicts.hashCode());

                builder.offerTo(x -> {
                    map.put(name, x.toJson());
                }, name);

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