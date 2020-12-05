package cam72cam.mod.item;

import cam72cam.mod.event.CommonEvents;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.ShapedOreRecipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Recipe registration */
public class Recipes {

    public static ShapedRecipeBuilder shapedRecipe(CustomItem item, int width, Fuzzy... ingredients) {
        return new ShapedRecipeBuilder(new ItemStack(item, 1), width, ingredients);
    }

    public static ShapedRecipeBuilder shapedRecipe(ItemStack item, int width, Fuzzy... ingredients) {
        return new ShapedRecipeBuilder(item, width, ingredients);
    }

    public static class ShapedRecipeBuilder {
        private List<Fuzzy> dependencies = new ArrayList<>();
        private List<Fuzzy> conflicts = new ArrayList<>();

        private ShapedRecipeBuilder(ItemStack result, int width, Fuzzy... ingredients) {
            CommonEvents.Recipe.REGISTER.subscribe(() -> {
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

                int rows = ingredients.length/width;
                List<Fuzzy> ingredientSet = new ArrayList(Arrays.stream(ingredients).filter(Objects::nonNull).collect(Collectors.toSet()));

                Object[] data = new Object[rows + ingredientSet.size() * 2];
                for (int i = 0; i < rows; i++) {
                    data[i] = "";
                    for (int j = 0; j < width; j++) {
                        int idx = i * width + j;
                        if (ingredients[idx] != null) {
                            data[i] += "" + ingredientSet.indexOf(ingredients[idx]);
                        } else {
                            data[i] += " ";
                        }
                    }
                }
                for (int i = 0; i < ingredientSet.size(); i++) {
                    data[rows + i * 2] = (""+i).charAt(0);
                    data[rows + i * 2 + 1] = ingredientSet.get(i).toString();
                }
                GameRegistry.addRecipe(new ShapedOreRecipe(result.internal, data));
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