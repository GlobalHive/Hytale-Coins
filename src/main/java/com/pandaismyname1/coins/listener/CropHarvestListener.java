package com.pandaismyname1.coins.listener;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EcsEvent;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Vector3iUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pandaismyname1.coins.config.ConfigManager;
import com.pandaismyname1.coins.economy.Coin;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;

import java.util.Collections;
import java.util.Random;

public class CropHarvestListener extends EntityEventSystem {

    private final Random random = new Random();

    public CropHarvestListener() {
        super(BreakBlockEvent.class);
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk archetypeChunk, @Nonnull Store store, @Nonnull CommandBuffer commandBuffer, @Nonnull EcsEvent event) {
        if (!(event instanceof BreakBlockEvent breakEvent)) return;

        BlockType blockType = breakEvent.getBlockType();
        if (blockType.getFarming() == null) return;

        // Check if it's a mature crop (has harvest drops)
        if (blockType.getGathering() == null || blockType.getGathering().getHarvest() == null) {
            return;
        }

        // Check if the harvester is a player
        // Note: BreakBlockEvent is an EntityEventSystem, so it's invoked on an entity
        // The entity index and archetypeChunk passed to handle() are the ones the event was invoked on.
        // In Hytale, BreakBlockEvent is typically invoked on the player who broke the block.
        Ref<EntityStore> playerRef = archetypeChunk.getReferenceTo(index);
        if (store.getComponent(playerRef, Player.getComponentType()) == null) {
            return;
        }

        // Check if crop drops are enabled
        if (!ConfigManager.getConfig().isEnableCropHarvestDrops()) {
            return;
        }

        // Roll for chance
        float chance = ConfigManager.getConfig().getCropHarvestDropRate();
        if (random.nextFloat() > chance) {
            return;
        }

        // Reward with a copper coin
        ItemStack copperCoin = new ItemStack(Coin.COPPER.getItemId(), 1);
        Vector3d position = Vector3iUtil.toVector3d(breakEvent.getTargetBlock()).add(0.5, 0.5, 0.5);
        Rotation3f rotation = new Rotation3f(0, 0, 0);

        Holder<EntityStore>[] drops = ItemComponent.generateItemDrops(store, Collections.singletonList(copperCoin), position, rotation);
        commandBuffer.addEntities(drops, AddReason.SPAWN);
    }

    @Nullable
    @Override
    public Query getQuery() {
        return Archetype.empty();
    }
}
