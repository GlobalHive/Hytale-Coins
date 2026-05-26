package com.pandaismyname1.coins.listener;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.AllLegacyLivingEntityTypesQuery;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pandaismyname1.coins.config.ConfigManager;
import com.pandaismyname1.coins.economy.Coin;

import javax.annotation.Nonnull;

import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

public class MobDeathListener extends DeathSystems.OnDeathSystem {

    @Nonnull
    @Override
    public Query getQuery() {
        return AllLegacyLivingEntityTypesQuery.INSTANCE;
    }

    @Override
    public void onComponentAdded(@Nonnull Ref ref, @Nonnull DeathComponent component, @Nonnull Store store, @Nonnull CommandBuffer commandBuffer) {
        // Check if coin drops are enabled
        if (!ConfigManager.getConfig().isEnableMobDeathDrops()) {
            return;
        }

        // We only care about non-player mobs
        if (store.getComponent(ref, Player.getComponentType()) != null) {
            return;
        }

        Damage deathInfo = component.getDeathInfo();
        if (deathInfo == null) return;

        Damage.Source source = deathInfo.getSource();
        if (!(source instanceof Damage.EntitySource)) return;

        Ref<EntityStore> attackerRef = ((Damage.EntitySource) source).getRef();
        if (!attackerRef.isValid()) return;

        // Check if the killer is a player
        if (store.getComponent(attackerRef, Player.getComponentType()) == null) {
            return;
        }

        // Killer is a player, calculate coins based on mob health
        EntityStatMap statMap = (EntityStatMap) store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap == null) return;

        long totalValue = 0;

        // Try to get specific drop value from config
        ModelComponent modelComponent = (ModelComponent) store.getComponent(ref, ModelComponent.getComponentType());
        if (modelComponent != null && modelComponent.getModel() != null) {
            String modelAssetId = modelComponent.getModel().getModelAssetId();
            if (modelAssetId != null) {
                // Hytale modelAssetIds often look like "hytale:fox", so we'll check both full and short name
                totalValue = ConfigManager.getConfig().getMobDeathDrops().getOrDefault(modelAssetId, 0L);
                if (totalValue == 0) {
                    String shortName = modelAssetId.contains(":") ? modelAssetId.substring(modelAssetId.indexOf(":") + 1) : modelAssetId;
                    totalValue = ConfigManager.getConfig().getMobDeathDrops().getOrDefault(shortName, 0L);
                }
            }
        }

        if (totalValue <= 0) {
            float maxHealth = statMap.get(DefaultEntityStatTypes.getHealth()).getMax();
            if (maxHealth <= 0) return;

            // Drop coins based on max health and configured drop rate
            float dropRate = ConfigManager.getConfig().getMobDeathDropRate();
            totalValue = (long) (maxHealth * dropRate);
            if (totalValue <= 0) totalValue = 1; // Minimum 1 coin
        }

        List<ItemStack> coinsToDrop = calculateCoinStacks(totalValue);

        // Get position to drop
        TransformComponent transform = (TransformComponent) store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d position = transform.getPosition().add(0, 1, 0);
        
        HeadRotation headRotationComponent = (HeadRotation) store.getComponent(ref, HeadRotation.getComponentType());
        Rotation3f rotation = (headRotationComponent != null) ? headRotationComponent.getRotation() : new Rotation3f(0, 0, 0);

        Holder<EntityStore>[] drops = ItemComponent.generateItemDrops(store, coinsToDrop, position, rotation);
        commandBuffer.addEntities(drops, AddReason.SPAWN);
    }

    private List<ItemStack> calculateCoinStacks(long value) {
        List<ItemStack> stacks = new ArrayList<>();
        Coin gold = Coin.GOLD;
        long goldValue = gold.getValue();
        if (goldValue <= 0) {
            return stacks;
        }

        // Convert total value to only GOLD coins, rounding up to avoid zero drops for low values.
        long goldCount = Math.max(1L, (value + goldValue - 1) / goldValue);
        while (goldCount > 0) {
            int stackCount = (int) Math.min(goldCount, Integer.MAX_VALUE);
            stacks.add(new ItemStack(gold.getItemId(), stackCount));
            goldCount -= stackCount;
        }

        return stacks;
    }
}
