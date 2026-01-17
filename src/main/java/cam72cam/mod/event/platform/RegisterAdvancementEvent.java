package cam72cam.mod.event.platform;

import cam72cam.mod.item.Fuzzy;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;

import java.util.Map;

public class RegisterAdvancementEvent extends Event {
    private static final ResourceLocation RECIPE = new ResourceLocation("minecraft:recipes/root");
    private final Map<ResourceLocation, Advancement.Builder> map;

    public RegisterAdvancementEvent(Map<ResourceLocation, Advancement.Builder> map) {
        this.map = map;
    }

    public void registerRecipeTrigger(ResourceLocation ident, ResourceLocation recipe, Fuzzy... trigger) {
        Advancement.Builder builder = Advancement.Builder.builder().withParentId(RECIPE);

//        Criterion hasRecipe = new Criterion(new RecipeUnlockedTrigger.Instance(recipe));
//        builder.withCriterion("has_recipe", hasRecipe);
        for (int i = 0; i < trigger.length; i++) {
            Fuzzy ingredient = trigger[i];
            if (ingredient == null || ingredient.getTag() == null) continue;

            Criterion hasItem = new Criterion(InventoryChangeTrigger.Instance.forItems(
                    ItemPredicate.Builder.create().tag(ingredient.getTag()).build()));
            builder.withCriterion("has" + ingredient + i, hasItem);
        }
        builder.withRewards(AdvancementRewards.Builder.recipe(recipe));

        map.put(ident, builder);
    }
}
