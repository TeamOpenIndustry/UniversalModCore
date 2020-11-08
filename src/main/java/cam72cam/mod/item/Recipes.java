package cam72cam.mod.item;


import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.IFinishedRecipe;
import net.minecraft.data.RecipeProvider;
import net.minecraft.data.ShapedRecipeBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.common.crafting.ConditionalRecipe;
import net.minecraftforge.common.crafting.conditions.ItemExistsCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Recipe registration */
public class Recipes extends RecipeProvider {
    private static final List<Consumer<Consumer<IFinishedRecipe>>> registry = new ArrayList<>();

    public Recipes(DataGenerator generatorIn) {
        super(generatorIn);
    }

    @Override
    protected void registerRecipes(Consumer<IFinishedRecipe> consumer) {
        registry.forEach(fn -> fn.accept(consumer));
    }

    public static void register(CustomItem item, int width, Fuzzy... ingredients) {
        register(new ItemStack(item, 1), width, ingredients);
    }

    public static void register(ItemStack result, int width, Fuzzy... ingredients) {
        ShapedRecipeBuilder builder = new ShapedRecipeBuilder(result.internal.getItem(), result.getCount());

        int height = ingredients.length / width;

        for (int h = 0; h < height; h++) {
            String line = "";
            for (int w = 0; w < width; w++) {
                int idx = h * width + w;
                Fuzzy ingredient = ingredients[idx];
                line += ingredient == null ? " " : idx + "";
                if (ingredient != null) {
                    // TODO tags
                    builder.key((idx + "").charAt(0), ingredient.tag);
                    builder.addCriterion(
                            "has" + ingredient.toString() + idx,
                            new InventoryChangeTrigger.Instance(
                                    MinMaxBounds.IntBound.UNBOUNDED,
                                    MinMaxBounds.IntBound.UNBOUNDED,
                                    MinMaxBounds.IntBound.UNBOUNDED,
                                    new ItemPredicate[] {ItemPredicate.Builder.create().tag(ingredient.tag).build()}
                            )
                    );
                }
            }
            builder.patternLine(line);
        }
        System.out.println("ADDED BUILDER!");
        registry.add(o -> ConditionalRecipe.builder().addCondition(new ItemExistsCondition("forge:ingots/steel")).addRecipe(builder::build).build(o, Registry.ITEM.getKey(result.internal.getItem())));
    }
}