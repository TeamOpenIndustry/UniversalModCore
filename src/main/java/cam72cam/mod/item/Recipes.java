package cam72cam.mod.item;

import cam72cam.mod.ModCore;
import cam72cam.mod.event.CommonEvents;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreIngredient;
import net.minecraftforge.oredict.ShapedOreRecipe;

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
        private List<Fuzzy> dependencies = new ArrayList<>();
        private List<Fuzzy> conflicts = new ArrayList<>();

        private ShapedRecipeBuilder(ItemStack item, int width, Fuzzy... ingredients) {
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

                CraftingHelper.ShapedPrimer primer = new CraftingHelper.ShapedPrimer();
                primer.width = width;
                primer.height = ingredients.length / width;
                primer.mirrored = false;
                primer.input = NonNullList.withSize(primer.width * primer.height, Ingredient.EMPTY);

                for (int i = 0; i < ingredients.length; i++) {
                    if (ingredients[i] != null) {
                        primer.input.set(i, new OreIngredient(ingredients[i].toString()));
                    }
                }
                ShapedOreRecipe sor = new ShapedOreRecipe(new ResourceLocation(ModCore.MODID, "recipes"), item.internal, primer);
                sor.setRegistryName(item.internal.getItem().getRegistryName());
                ForgeRegistries.RECIPES.register(sor);
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