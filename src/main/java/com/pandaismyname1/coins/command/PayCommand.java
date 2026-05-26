package com.pandaismyname1.coins.command;

import com.pandaismyname1.coins.api.CoinsAPIProvider;
import com.pandaismyname1.coins.config.ConfigManager;
import com.pandaismyname1.coins.economy.Wallet;
import com.pandaismyname1.coins.economy.WalletManager;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("deprecation")
public class PayCommand extends AbstractCommand {
    private final RequiredArg playerArg;
    private final RequiredArg amountArg;

    public PayCommand() {
        super("pay", "Pay " + CoinsAPIProvider.getCurrencyName(2).toLowerCase() + " to another player");

        String permission = ConfigManager.getConfig().getPayCommandPermission();
        if (permission == null || permission.isEmpty()) {
            this.setPermissionGroup(GameMode.Adventure);
        } else {
            this.requirePermission(permission);
        }

        this.playerArg = this.withRequiredArg("player", "The player to pay", ArgTypes.PLAYER_REF);
        this.amountArg = this.withRequiredArg("amount", "The amount of " + CoinsAPIProvider.getCurrencyName(1).toLowerCase() + " to pay", ArgTypes.INTEGER);
    }

    @Override
    protected CompletableFuture<Void> execute(@NonNullDecl CommandContext commandContext) {
        if (!commandContext.isPlayer()) {
            commandContext.sendMessage(Message.empty().insert("This command can only be used by players.").color("RED"));
            return CompletableFuture.completedFuture(null);
        }

        Player sender = (Player) commandContext.sender();
        PlayerRef recipientRef = (PlayerRef) commandContext.get(this.playerArg);
        Integer amount = (Integer) commandContext.get(this.amountArg);

        if (recipientRef == null || amount == null) {
            return CompletableFuture.completedFuture(null);
        }

        if (amount <= 0) {
            commandContext.sendMessage(Message.empty().insert("Amount must be greater than zero.").color("RED"));
            return CompletableFuture.completedFuture(null);
        }

        if (sender.getUuid().equals(recipientRef.getUuid())) {
            commandContext.sendMessage(Message.empty().insert("You cannot pay yourself.").color("RED"));
            return CompletableFuture.completedFuture(null);
        }

        Wallet senderWallet = WalletManager.getWallet(sender.getUuid());
        if (senderWallet.remove(amount)) {
            Wallet recipientWallet = WalletManager.getWallet(recipientRef.getUuid());
            recipientWallet.add(amount);

            commandContext.sender().sendMessage(Message.empty()
                    .insert(Message.empty().insert("[Wallet] ").color("GOLD"))
                    .insert(Message.empty().insert("You paid ").color("WHITE"))
                    .insert(Message.empty().insert(CoinsAPIProvider.format(amount) + " ").color("YELLOW"))
                    .insert(Message.empty().insert("to ").color("WHITE"))
                    .insert(Message.empty().insert(recipientRef.getUsername()).color("AQUA"))
                    .insert(Message.empty().insert(".").color("WHITE")));

            // Try to notify recipient if they are online
            recipientRef.sendMessage(Message.empty()
                    .insert(Message.empty().insert("[Wallet] ").color("GOLD"))
                    .insert(Message.empty().insert("You received ").color("WHITE"))
                    .insert(Message.empty().insert(CoinsAPIProvider.format(amount) + " ").color("YELLOW"))
                    .insert(Message.empty().insert("from ").color("WHITE"))
                    .insert(Message.empty().insert(commandContext.sender().getUsername()).color("AQUA"))
                    .insert(Message.empty().insert(".").color("WHITE")));
        } else {
            commandContext.sendMessage(Message.empty().insert("You do not have enough " + CoinsAPIProvider.getCurrencyName(2).toLowerCase() + ".").color("RED"));
        }

        return CompletableFuture.completedFuture(null);
    }
}
