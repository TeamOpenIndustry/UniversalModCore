package cam72cam.mod.item;


import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.IFinishedRecipe;
import net.minecraft.data.RecipeProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.common.crafting.ConditionalRecipe;
import net.minecraftforge.common.crafting.conditions.ItemExistsCondition;
import net.minecraftforge.common.crafting.conditions.NotCondition;
import net.minecraftforge.common.crafting.conditions.TagEmptyCondition;

import java.util.ArrayList;
import java.util.Arrays;
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
            registry.add(out -> {
                net.minecraft.data.ShapedRecipeBuilder builder = new net.minecraft.data.ShapedRecipeBuilder(item.internal.getItem(), item.getCount());

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
                                            EntityPredicate.AndPredicate.ANY_AND,
                                            MinMaxBounds.IntBound.UNBOUNDED,
                                            MinMaxBounds.IntBound.UNBOUNDED,
                                            MinMaxBounds.IntBound.UNBOUNDED,
                                            new ItemPredicate[]{ItemPredicate.Builder.create().tag(ingredient.tag).build()}
                                    )
                            );
                        }
                    }
                    builder.patternLine(line);
                }
                ResourceLocation itemName = item.internal.getItem().getRegistryName();
                ResourceLocation name = new ResourceLocation(itemName.getNamespace(), itemName.getPath() + Arrays.hashCode(ingredients) + dependencies.hashCode() + conflicts.hashCode());

                if (!dependencies.isEmpty() || !conflicts.isEmpty()) {
                    ConditionalRecipe.Builder conditions = ConditionalRecipe.builder();
                    for (Fuzzy dependency : dependencies) {
                        conditions = conditions.addCondition(new NotCondition(new TagEmptyCondition(dependency.tag.getName())));
                    }
                    for (Fuzzy conflict : conflicts) {
                        conditions = conditions.addCondition(new TagEmptyCondition(conflict.tag.getName()));
                    }
                    conditions.addRecipe(builder::build).build(out, name);
                } else {
                    builder.build(out, name);
                }
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