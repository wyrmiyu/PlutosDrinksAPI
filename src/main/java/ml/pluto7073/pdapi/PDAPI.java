package ml.pluto7073.pdapi;

import ml.pluto7073.pdapi.addition.DrinkAdditionManager;
import ml.pluto7073.pdapi.addition.action.OnDrinkSerializers;
import ml.pluto7073.pdapi.block.PDBlocks;
import ml.pluto7073.pdapi.client.gui.PDScreens;
import ml.pluto7073.pdapi.command.PDCommands;
import ml.pluto7073.pdapi.component.PDComponents;
import ml.pluto7073.pdapi.entity.PDTrackedData;
import ml.pluto7073.pdapi.entity.effect.PDMobEffects;
import ml.pluto7073.pdapi.gamerule.PDGameRules;
import ml.pluto7073.pdapi.item.PDItems;
import ml.pluto7073.pdapi.networking.PDClientboundPackets;
import ml.pluto7073.pdapi.recipes.PDRecipeTypes;
import ml.pluto7073.pdapi.specialty.SpecialtyDrink;
import ml.pluto7073.pdapi.specialty.SpecialtyDrinkManager;
import ml.pluto7073.pdapi.specialty.SpecialtyDrinkSerializer;
import ml.pluto7073.pdapi.util.DrinkUtil;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PDAPI implements ModInitializer {

    public static final String ID = "pdapi";
    public static final Logger LOGGER = LogManager.getLogger("PDAPI");
    public static final ResourceKey<CreativeModeTab> SPECIALTY_DRINKS_TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, asId("specialty_drinks"));

    @Override
    public void onInitialize() {
        OnDrinkSerializers.init();
        SpecialtyDrinkSerializer.init();
        PDTrackedData.init();
        PDRecipeTypes.init();
        PDBlocks.init();
        PDComponents.init();
        PDItems.init();
        PDMobEffects.init();
        PDGameRules.init();
        PDCommands.init();

        PDClientboundPackets.registerPackets();

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new DrinkAdditionManager());
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SpecialtyDrinkManager());

        // Sync registries to clients when they join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            LOGGER.info("[PlutosDrinksAPI] Syncing drink additions to client: {}", handler.player.getName().getString());
            DrinkAdditionManager.send(handler.player);
            // SpecialtyDrinkManager sync is handled by ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS
        });

        DrinkUtil.registerOldToNewConverter("Coffee/Additions", tag -> {
            if (!(tag instanceof ListTag list)) return tag;
            if (list.isEmpty()) return list;
            for (int i = 0; i < list.size(); i++) {
                ResourceLocation id = new ResourceLocation(list.getString(i));
                boolean ogCoffee = !DrinkAdditionManager.containsId(id) && DrinkAdditionManager.containsId(PDAPI.asId(id.getPath()));
                if (!ogCoffee) continue;
                id = PDAPI.asId(id.getPath());
                list.set(i, DrinkUtil.stringAsNbt(id.toString()));
            }
            return list;
        });

        PDScreens.init();

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SPECIALTY_DRINKS_TAB, FabricItemGroup.builder().icon(() -> new ItemStack(PDItems.MILK_BOTTLE))
                .title(Component.translatable("creative_tab.pdapi.specialty_drinks")).build());
        ItemGroupEvents.modifyEntriesEvent(SPECIALTY_DRINKS_TAB).register(stacks -> {
            for (SpecialtyDrink d : SpecialtyDrinkManager.values()
                    .stream().sorted(DrinkUtil.alphabetizer(SpecialtyDrink::languageKey)).toList()) {
                stacks.accept(d.getAsItem());
            }
        });

        LOGGER.info("Pluto's Drinks API ready!");
    }

    public static ResourceLocation asId(String name) {
        return new ResourceLocation(ID, name);
    }

}
