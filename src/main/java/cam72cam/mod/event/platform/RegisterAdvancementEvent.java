package cam72cam.mod.event.platform;

import cam72cam.mod.item.Fuzzy;
import com.google.common.collect.ImmutableMap;
import net.minecraft.advancements.*;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Fired when advancement datapacks are reloaded
 */
public class RegisterAdvancementEvent extends Event implements IModBusEvent {
    private static final ResourceLocation RECIPE_ROOT = ResourceLocation.tryParse("minecraft:recipes/root");
    private final ImmutableMap.Builder<ResourceLocation, AdvancementHolder> map;

    public RegisterAdvancementEvent(ImmutableMap.Builder<ResourceLocation, AdvancementHolder> map) {
        this.map = map;
    }

    @SuppressWarnings("removal")
    public void registerRecipeTrigger(ResourceLocation advancementIdent, ResourceLocation recipe, Fuzzy... trigger) {
        Advancement.Builder builder = Advancement.Builder.advancement().parent(RECIPE_ROOT);
        List<String> stringList = new ArrayList<>();

        Criterion<RecipeUnlockedTrigger.TriggerInstance> alreadyHasRecipe
                = new Criterion<>(CriteriaTriggers.RECIPE_UNLOCKED, new RecipeUnlockedTrigger.TriggerInstance(Optional.empty(), recipe));
        builder.addCriterion("already_has_recipe", alreadyHasRecipe);
        stringList.add("already_has_recipe");

        for (int i = 0; i < trigger.length; i++) {
            Fuzzy ingredient = trigger[i];
            if (ingredient == null || ingredient.getTag() == null) continue;

            Criterion<InventoryChangeTrigger.TriggerInstance> hasItem = InventoryChangeTrigger.TriggerInstance
                    .hasItems(ItemPredicate.Builder.item().of(ingredient.getTag()).build());
            String name = "has" + ingredient + i;
            builder.addCriterion(name, hasItem);
            stringList.add(name);
        }
        //Unlock the recipe when any of the ingredients being acquired
        builder.requirements(AdvancementRequirements.Strategy.OR);
        builder.rewards(AdvancementRewards.Builder.recipe(recipe));

        map.put(advancementIdent, builder.build(advancementIdent));
    }
}