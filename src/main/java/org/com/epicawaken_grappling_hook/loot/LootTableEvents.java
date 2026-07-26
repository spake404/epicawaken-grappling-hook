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

        LootPool.Builder pool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .when(LootItemRandomChanceCondition.randomChance(ROPE_RECOVERY_BOOK_CHANCE));
        for (int level = 1; level <= ModEnchantments.ROPE_RECOVERY.get().getMaxLevel(); level++) {
            pool.add(LootItem.lootTableItem(Items.ENCHANTED_BOOK)
                    .apply(SetNbtFunction.setTag(createStoredEnchantmentTag(level)))
                    .setWeight(1));
        }
        event.getTable().addPool(pool.build());
    }

    private static CompoundTag createStoredEnchantmentTag(int level) {
        CompoundTag enchantment = new CompoundTag();
        enchantment.putString("id", ModEnchantments.ROPE_RECOVERY.getId().toString());
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
