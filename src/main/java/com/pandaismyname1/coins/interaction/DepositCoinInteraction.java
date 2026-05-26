package com.pandaismyname1.coins.interaction;

import com.pandaismyname1.coins.api.CoinsAPIProvider;
import com.pandaismyname1.coins.config.ConfigManager;
import com.pandaismyname1.coins.economy.Coin;
import com.pandaismyname1.coins.economy.Wallet;
import com.pandaismyname1.coins.economy.WalletManager;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.Message;

import javax.annotation.Nonnull;
import java.util.UUID;

public class DepositCoinInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec CODEC = BuilderCodec.builder(DepositCoinInteraction.class, DepositCoinInteraction::new, SimpleInstantInteraction.CODEC).build();

    public DepositCoinInteraction() {
        super("DepositCoin");
    }

    @Override
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        ItemStack heldItem = context.getHeldItem();
        if (heldItem == null || heldItem.isEmpty()) return;

        Coin coin = Coin.fromItemId(heldItem.getItemId());
        if (coin != null) {
            Ref<EntityStore> ref = context.getEntity();
            UUIDComponent uuidComponent = (UUIDComponent) context.getCommandBuffer().getComponent(ref, UUIDComponent.getComponentType());
            if (uuidComponent == null) return;
            UUID uuid = uuidComponent.getUuid();

            long quantity = heldItem.getQuantity();
            long value = coin.getValue() * quantity;

            Wallet wallet = WalletManager.getWallet(uuid);
            wallet.add(value);

            // Inform the user
            PlayerRef player = (PlayerRef) context.getCommandBuffer().getComponent(ref, PlayerRef.getComponentType());
            if (player != null) {
                player.sendMessage(Message.empty()
                        .insert(Message.empty().insert("[Wallet] ").color("GOLD"))
                        .insert(Message.empty().insert("Deposited ").color("WHITE"))
                        .insert(Message.empty().insert(CoinsAPIProvider.format(value)).color("YELLOW"))
                        .insert(Message.empty().insert(". New balance: ").color("WHITE"))
                        .insert(Message.empty().insert(CoinsAPIProvider.format(wallet.getBalance())).color("YELLOW"))
                        .insert(Message.empty().insert(".").color("WHITE")));
            }

            // Remove items from inventory
            ItemContainer container = context.getHeldItemContainer();
            if (container != null) {
                container.removeItemStackFromSlot(context.getHeldItemSlot(), (int) quantity);
            }
        }
    }
}
