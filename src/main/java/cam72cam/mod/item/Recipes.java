package cam72cam.mod.item;


import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.ConditionalRecipe;
import net.minecraftforge.common.crafting.conditions.ItemExistsCondition;
import net.minecraftforge.common.crafting.conditions.NotCondition;
import net.minecraftforge.common.crafting.conditions.TagEmptyCondition;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/** Recipe registration */
public class Recipes extends RecipeProvider {
    private static final List<Consumer<Consumer<FinishedRecipe>>> registry = new ArrayList<>();

    public Recipes(PackOutput generatorIn) {
        super(generatorIn);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> consumer) {
        // TODO 1.18.2+ registry.forEach(fn -> fn.accept(consumer));
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
                net.minecraft.data.recipes.ShapedRecipeBuilder builder = new net.minecraft.data.recipes.ShapedRecipeBuilder(RecipeCategory.MISC, item.internal.getItem(), item.getCount());

                int height = ingredients.length / width;

                for (int h = 0; h < height; h++) {
                    String line = "";
                    for (int w = 0; w < width; w++) {
                        int idx = h * width + w;
                        Fuzzy ingredient = ingredients[idx];
                        line += ingredient == null ? " " : idx + "";
                        if (ingredient != null) {
                            // TODO tags
                            builder.define((idx + "").charAt(0), ingredient.tag);
                            builder.unlockedBy(
                                    "has" + ingredient.toString() + idx,
                                    new InventoryChangeTrigger.TriggerInstance(
                                            EntityPredicate.Composite.ANY,
                                            MinMaxBounds.Ints.ANY,
                                            MinMaxBounds.Ints.ANY,
                                            MinMaxBounds.Ints.ANY,
                                            new ItemPredicate[]{ItemPredicate.Builder.item().of(ingredient.tag).build()}
                                    )
                            );
                        }
                    }
                    builder.pattern(line);
                }
                ResourceLocation itemName = ForgeRegistries.ITEMS.getKey(item.internal.getItem());
                ResourceLocation name = new ResourceLocation(itemName.getNamespace(), itemName.getPath() + Arrays.hashCode(ingredients) + dependencies.hashCode() + conflicts.hashCode());

                if (!dependencies.isEmpty() || !conflicts.isEmpty()) {
                    ConditionalRecipe.Builder conditions = ConditionalRecipe.builder();
                    for (Fuzzy dependency : dependencies) {
                        conditions = conditions.addCondition(new NotCondition(new TagEmptyCondition(dependency.tag.location())));
                    }
                    for (Fuzzy conflict : conflicts) {
                        conditions = conditions.addCondition(new TagEmptyCondition(conflict.tag.location()));
                    }
                    conditions.addRecipe(builder::save).build(out, name);
                } else {
                    builder.save(out, name);
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