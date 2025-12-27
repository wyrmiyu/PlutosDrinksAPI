package ml.pluto7073.pdapi.addition.action;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ApplyStatusEffectAction implements OnDrinkAction {

    private final Holder<MobEffect> effect;
    private final int duration, amplifier;

    public ApplyStatusEffectAction(Holder<MobEffect> effect, int duration, int amplifier) {
        this.effect = effect;
        this.duration = duration;
        this.amplifier = amplifier;
    }

    @Override
    public void onDrink(ItemStack stack, Level level, LivingEntity user) {
        user.addEffect(new MobEffectInstance(effect, duration, amplifier));
    }

    @Override
    public OnDrinkSerializer<?> serializer() {
        return OnDrinkSerializers.APPLY_STATUS_EFFECT;
    }

    public Holder<MobEffect> getEffect() {
        return effect;
    }

    public int getDuration() {
        return duration;
    }

    public int getAmplifier() {
        return amplifier;
    }

    public static class Serializer implements OnDrinkSerializer<ApplyStatusEffectAction> {

        public static final MapCodec<ApplyStatusEffectAction> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(action -> action.effect),
                        Codec.INT.fieldOf("duration").forGetter(action -> action.duration),
                        Codec.INT.fieldOf("amplifier").forGetter(action -> action.amplifier))
                        .apply(instance, ApplyStatusEffectAction::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ApplyStatusEffectAction> STREAM_CODEC =
                StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

        @Override
        public MapCodec<ApplyStatusEffectAction> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ApplyStatusEffectAction> streamCodec() {
            return STREAM_CODEC;
        }

        public static ApplyStatusEffectAction fromNetwork(FriendlyByteBuf buf) {
            ResourceLocation id = buf.readResourceLocation();
            Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.getHolder(id).orElseThrow();
            int duration = buf.readInt();
            int amplifier = buf.readInt();
            return new ApplyStatusEffectAction(effect, duration, amplifier);
        }

        public static void toNetwork(FriendlyByteBuf buf, ApplyStatusEffectAction action) {
            ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(action.effect.value());
            if (id == null) throw new IllegalStateException();
            buf.writeResourceLocation(id);
            buf.writeInt(action.duration);
            buf.writeInt(action.amplifier);
        }
    }

}
