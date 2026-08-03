package org.com.epicawaken_grappling_hook.loot;

import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetNbtFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.com.epicawaken_grappling_hook.Epicawaken_grappling_hook;
import org.com.epicawaken_grappling_hook.enchantment.ModEnchantments;

@Mod.EventBusSubscriber(modid = Epicawaken_grappling_hook.MODID)
public final class LootTableEvents {
    private static final float ROPE_RECOVERY_BOOK_CHANCE = 0.1F;
    private static final float GRAPPLING_SHIELD_DISARM_BOOK_CHANCE = 0.05F;
    private static final Set<ResourceLocation> ENCHANTED_BOOK_LOOT_TABLES = Set.of(
            chest("abandoned_mineshaft"),
            chest("ancient_city"),
            chest("bastion_bridge"),
            chest("bastion_hoglin_stable"),
            chest("bastion_other"),
            chest("bastion_treasure"),
            chest("desert_pyramid"),
            chest("end_city_treasure"),
            chest("jungle_temple"),
            chest("pillager_outpost"),
            chest("ruined_portal"),
            chest("shipwreck_supply"),
            chest("simple_dungeon"),
            chest("stronghold_corridor"),
            chest("stronghold_crossing"),
            chest("stronghold_library"),
            chest("underwater_ruin_big"),
            chest("underwater_ruin_small"),
            chest("woodland_mansion"));

    private LootTableEvents() {
    }

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        if (!ENCHANTED_BOOK_LOOT_TABLES.contains(event.getName())) {
            return;
        }

        event.getTable().addPool(createEnchantedBookPool(
                ROPE_RECOVERY_BOOK_CHANCE,
                ModEnchantments.ROPE_RECOVERY.getId(),
                ModEnchantments.ROPE_RECOVERY.get().getMaxLevel()));
        event.getTable().addPool(createEnchantedBookPool(
                GRAPPLING_SHIELD_DISARM_BOOK_CHANCE,
                ModEnchantments.GRAPPLING_SHIELD_DISARM.getId(),
                1));
    }

    private static LootPool createEnchantedBookPool(float chance, ResourceLocation enchantmentId, int maxLevel) {
        LootPool.Builder pool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .when(LootItemRandomChanceCondition.randomChance(chance));
        for (int level = 1; level <= maxLevel; level++) {
            pool.add(LootItem.lootTableItem(Items.ENCHANTED_BOOK)
                    .apply(SetNbtFunction.setTag(createStoredEnchantmentTag(enchantmentId, level)))
                    .setWeight(1));
        }
        return pool.build();
    }

    private static CompoundTag createStoredEnchantmentTag(ResourceLocation enchantmentId, int level) {
        CompoundTag enchantment = new CompoundTag();
        enchantment.putString("id", enchantmentId.toString());
        enchantment.putShort("lvl", (short) level);

        ListTag enchantments = new ListTag();
        enchantments.add(enchantment);

        CompoundTag tag = new CompoundTag();
        tag.put("StoredEnchantments", enchantments);
        return tag;
    }

    private static ResourceLocation chest(String name) {
        return new ResourceLocation("minecraft", "chests/" + name);
    }
}
