package ml.pluto7073.pdapi.component;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import ml.pluto7073.pdapi.addition.DrinkAddition;
import ml.pluto7073.pdapi.addition.DrinkAdditionManager;
import ml.pluto7073.pdapi.addition.action.OnDrinkAction;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

public record DrinkAdditions(List<DrinkAddition> additions) implements TooltipProvider {

    public static final DrinkAdditions EMPTY = new DrinkAdditions(new ArrayList<>());
    public static final Codec<DrinkAdditions> CODEC = DrinkAddition.COMPONENT_CODEC.listOf().xmap(DrinkAdditions::new, DrinkAdditions::additions);
    public static final StreamCodec<RegistryFriendlyByteBuf, DrinkAdditions> STREAM_CODEC =
            StreamCodec.of(ByteBufCodecs.fromCodecWithRegistries(CODEC), ByteBufCodecs.fromCodecWithRegistries(CODEC));

    public static DrinkAdditions or(DrinkAdditions first, DrinkAdditions second) {
        List<DrinkAddition> additions = Lists.newArrayList(first.additions);
        additions.addAll(second.additions);
        return new DrinkAdditions(additions);
    }

    public static DrinkAdditions of(List<ResourceLocation> additions) {
        return new DrinkAdditions(additions.stream()
                .map(DrinkAdditionManager::get)
                .filter(addition -> addition != null)
                .toList());
    }

    public static DrinkAdditions of(ResourceLocation addition) {
        DrinkAddition drinkAddition = DrinkAdditionManager.get(addition);
        if (drinkAddition == null) {
            return EMPTY;
        }
        return new DrinkAdditions(List.of(drinkAddition));
    }

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> tooltip, TooltipFlag config) {
        HashMap<ResourceLocation, Integer> additionCounts = new HashMap<>();
        List<ResourceLocation> order = new ArrayList<>();
        for (DrinkAddition addIn : additions) {
            if (addIn == DrinkAdditionManager.EMPTY) continue;
            ResourceLocation id = DrinkAdditionManager.getId(addIn);
            if (additionCounts.containsKey(id)) {
                int count = additionCounts.get(id);
                additionCounts.put(id, ++count);
            } else {
                additionCounts.put(id, 1);
                order.add(id);
            }
        }
        
        // Show addition names
        order.forEach(id -> {
            DrinkAddition addition = DrinkAdditionManager.get(id);
            tooltip.accept(Component.translatable(addition.getTranslationKey(), additionCounts.get(id)).withStyle(ChatFormatting.GRAY));
            
            // Show effects for each addition (if any)
            if (!addition.actions().isEmpty()) {
                for (OnDrinkAction action : addition.actions()) {
                    // Add indented effect description
                    String effectDescription = getActionDescription(action);
                    if (!effectDescription.isEmpty()) {
                        tooltip.accept(Component.literal("  • " + effectDescription).withStyle(ChatFormatting.DARK_GRAY));
                    }
                }
            }
        });
    }
    
    private String getActionDescription(OnDrinkAction action) {
        if (action instanceof ml.pluto7073.pdapi.addition.action.ApplyStatusEffectAction statusAction) {
            String effectName = Component.translatable(statusAction.getEffect().value().getDescriptionId()).getString();
            int duration = statusAction.getDuration();
            int amplifier = statusAction.getAmplifier();
            
            // Convert ticks to seconds for display
            int seconds = duration / 20;
            String durationText = seconds >= 60 ? String.format("%dm %ds", seconds / 60, seconds % 60) : String.format("%ds", seconds);
            
            if (amplifier > 0) {
                return String.format("%s %s (%s)", effectName, getRomanNumeral(amplifier + 1), durationText);
            } else {
                return String.format("%s (%s)", effectName, durationText);
            }
        } else if (action instanceof ml.pluto7073.pdapi.addition.action.RestoreHungerAction hungerAction) {
            int food = hungerAction.getFood();
            int saturation = hungerAction.getSaturation();
            if (saturation > 0) {
                return String.format("Restores %d hunger + %d saturation", food, saturation);
            } else {
                return String.format("Restores %d hunger", food);
            }
        } else if (action instanceof ml.pluto7073.pdapi.addition.action.DealDamageAction damageAction) {
            float amount = damageAction.getAmount();
            return String.format("Deals %.1f damage", amount);
        } else if (action instanceof ml.pluto7073.pdapi.addition.action.ChorusTeleportAction) {
            return "Random teleportation";
        } else if (action instanceof ml.pluto7073.pdapi.addition.action.ClearHarmfulEffectsAction) {
            return "Clears harmful effects";
        } else if (action instanceof ml.pluto7073.pdapi.addition.action.ApplyEffectRadiusAction) {
            return "Applies area effect";
        } else {
            // Fallback for unknown action types
            String className = action.getClass().getSimpleName();
            return className.replace("Action", "").replaceAll("([A-Z])", " $1").trim();
        }
    }
    
    private String getRomanNumeral(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(number);
        };
    }

    public DrinkAdditions withAddition(DrinkAddition addition) {
        if (addition == null) {
            return this; // Return unchanged if trying to add null
        }
        return new DrinkAdditions(Util.copyAndAdd(additions, addition));
    }
}
