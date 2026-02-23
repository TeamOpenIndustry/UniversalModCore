package cam72cam.mod.event.platform;

import cam72cam.mod.item.Fuzzy;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.IRequirementsStrategy;
import net.minecraft.advancements.criterion.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.lifecycle.IModBusEvent;

import java.util.Map;

/**
 * Fired when advancement datapacks are reloaded
 */
public class RegisterAdvancementEvent extends Event implements IModBusEvent {
    private static final ResourceLocation RECIPE_ROOT = new ResourceLocation("minecraft:recipes/root");
    private final Map<ResourceLocation, Advancement.Builder> map;

    public RegisterAdvancementEvent(Map<ResourceLocation, Advancement.Builder> map) {
        this.map = map;
    }

    public void registerRecipeTrigger(ResourceLocation advancementIdent, ResourceLocation recipe, Fuzzy... trigger) {
        Advancement.Builder builder = Advancement.Builder.advancement().parent(RECIPE_ROOT);

        Criterion alreadyHasRecipe = new Criterion(new RecipeUnlockedTrigger.Instance(EntityPredicate.AndPredicate.ANY, recipe));
        builder.addCriterion("already_has_recipe", alreadyHasRecipe);
        for (int i = 0; i < trigger.length; i++) {
            Fuzzy ingredient = trigger[i];
            if (ingredient == null || ingredient.getTag() == null) continue;

            Criterion hasItem = new Criterion(InventoryChangeTrigger.Instance.hasItems(
                    ItemPredicate.Builder.item().of(ingredient.getTag()).build()));
            builder.addCriterion("has" + ingredient + i, hasItem);
        }
        //Unlock the recipe when any of the ingredients being acquired
        builder.requirements(IRequirementsStrategy.OR);
        builder.rewards(AdvancementRewards.Builder.recipe(recipe));

        map.put(advancementIdent, builder);
    }
}