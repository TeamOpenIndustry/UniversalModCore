package cam72cam.mod.item;

import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.ShapedOreRecipe;

import java.util.ArrayList;
import java.util.List;

public class Recipes {
    private static List<Runnable> registrations = new ArrayList<>();

    public static void registerRecipes() {
        registrations.forEach(Runnable::run);
    }

    public static void register(ItemBase item, int width, Fuzzy... ingredients) {
        register(new ItemStack(item, 1), width, ingredients);
    }

    public static void register(ItemStack result, int width, Fuzzy... ingredients) {
        String offsets = "123456789";
        registrations.add(() -> {
            Object[] data = new Object[ingredients.length * 2];
            for (int i = 0; i < ingredients.length/width; i++) {
                data[i] = offsets.substring(i * width,  i * width + width);
            }
            for (int i = 0; i < ingredients.length; i++) {
                data[ingredients.length/width + i * 2] = offsets.charAt(i);
                data[ingredients.length/width + i * 2 + 1] = ingredients[i];
            }
            GameRegistry.addRecipe(new ShapedOreRecipe(result.internal, data));
        });
    }
}