package ml.pluto7073.pdapi.addition.action;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DealDamageAction implements OnDrinkAction {

    private final float amount;
    private final ResourceKey<DamageType> source;

    public DealDamageAction(float amount, ResourceKey<DamageType> source) {
        this.amount = amount;
        this.source = source;
    }

    @Override
    public void onDrink(ItemStack stack, Level level, LivingEntity user) {
        user.hurt(user.damageSources().source(source), amount);
    }

    @Override
    public OnDrinkSerializer<?> serializer() {
        return OnDrinkSerializers.DEAL_DAMAGE;
    }

    public float getAmount() {
        return amount;
    }

    public ResourceKey<DamageType> getSource() {
        return source;
    }

    public static class Serializer implements OnDrinkSerializer<DealDamageAction> {

        public static final MapCodec<DealDamageAction> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(Codec.FLOAT.fieldOf("amount").forGetter(action -> action.amount),
                        ResourceKey.codec(Registries.DAMAGE_TYPE).fieldOf("source").forGetter(action -> action.source))
                        .apply(instance, DealDamageAction::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, DealDamageAction> STREAM_CODEC =
                StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

        @Override
        public MapCodec<DealDamageAction> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DealDamageAction> streamCodec() {
            return STREAM_CODEC;
        }

        public static DealDamageAction fromNetwork(FriendlyByteBuf buf) {
            float amount = buf.readFloat();
            ResourceKey<DamageType> source = buf.readResourceKey(Registries.DAMAGE_TYPE);
            return new DealDamageAction(amount, source);
        }

        public static void toNetwork(FriendlyByteBuf buf, DealDamageAction action) {
            buf.writeFloat(action.amount);
            buf.writeResourceKey(action.source);
        }
    }

}
