package cam72cam.mod.item;

import cam72cam.mod.event.CommonEvents;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.ShapedOreRecipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Recipes {
    public static void register(ItemBase item, int width, Fuzzy... ingredients) {
        register(new ItemStack(item, 1), width, ingredients);
    }

    public static void register(ItemStack result, int width, Fuzzy... ingredients) {
            CommonEvents.Recipe.REGISTER.subscribe(() -> {
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
}