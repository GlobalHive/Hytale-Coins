package com.pandaismyname1.coins.command;

import com.pandaismyname1.coins.ui.WalletPage;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("deprecation")
public class WalletCommand extends AbstractCommand {
    public WalletCommand() {
        super("wallet", "Check your coin balance");
        this.setPermissionGroup(GameMode.Adventure);
    }

    @Override
    protected CompletableFuture<Void> execute(@NonNullDecl CommandContext commandContext) {
        if (!commandContext.isPlayer()) {
            commandContext.sendMessage(Message.empty().insert("This command can only be used by players.").color("RED"));
            return CompletableFuture.completedFuture(null);
        }

        PlayerRef playerRef = commandContext.senderAs(PlayerRef.class);
        Player player = playerRef.getReference().getStore().getComponentConcurrent(playerRef.getReference(), Player.getComponentType());
        if (player == null) {
            commandContext.sendMessage(Message.empty().insert("Could not open wallet right now. Please try again.").color("RED"));
            return CompletableFuture.completedFuture(null);
        }

        // Open the wallet UI
        player.getPageManager().openCustomPage(playerRef.getReference(), playerRef.getReference().getStore(), new WalletPage(playerRef));

        return CompletableFuture.completedFuture(null);
    }
}
