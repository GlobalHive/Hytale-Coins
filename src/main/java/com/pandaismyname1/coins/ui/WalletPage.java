package com.pandaismyname1.coins.ui;

import com.pandaismyname1.coins.api.CoinsAPIProvider;
import com.pandaismyname1.coins.config.ConfigManager;
import com.pandaismyname1.coins.economy.Coin;
import com.pandaismyname1.coins.economy.Wallet;
import com.pandaismyname1.coins.economy.WalletManager;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class WalletPage extends InteractiveCustomUIPage<WalletPage.WalletEventData> {
    private List<Coin> displayedCoins = new ArrayList<>();

    public WalletPage(PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, WalletEventData.CODEC);
    }

    @Override
    public void build(@Nonnull Ref ref, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, @Nonnull Store store) {
        Wallet wallet = WalletManager.getWallet(playerRef.getUuid());
        long balance = wallet.getBalance();

        commandBuilder.append("Pages/WalletPage.ui");
        commandBuilder.set("#Balance.Text", "Total Balance: " + CoinsAPIProvider.format(balance));

        displayedCoins.clear();
        commandBuilder.clear("#CoinList");

        // Display coins in descending order of value
        Coin[] coins = Coin.values();
        int index = 0;
        for (int i = coins.length - 1; i >= 0; i--) {
            Coin coin = coins[i];
            long maxWithdrawable = balance / coin.getValue();
            
            if (maxWithdrawable > 0) {
                String selector = "#CoinList[" + index + "]";
                commandBuilder.append("#CoinList", "Pages/WalletCoinRow.ui");
                
                commandBuilder.set(selector + " #Name.Text", coin.name().charAt(0) + coin.name().substring(1).toLowerCase() + " " + CoinsAPIProvider.getCurrencyName(1));
                commandBuilder.set(selector + " #Description.Text", "Value: " + CoinsAPIProvider.format(coin.getValue()) + " | Max: " + maxWithdrawable);
                
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, selector + " #Button", 
                        new EventData().append("Index", String.valueOf(index)), false);
                
                commandBuilder.set(selector + " #Icon.ItemId", coin.getItemId());
                
                displayedCoins.add(coin);
                index++;
            }
        }

        if (displayedCoins.isEmpty()) {
            commandBuilder.set("#EmptyMessage.Visible", true);
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref ref, @Nonnull Store store, @Nonnull WalletEventData data) {
        if (data.getIndex() == null) return;
        int slotIndex = Integer.parseInt(data.getIndex());

        if (slotIndex >= 0 && slotIndex < displayedCoins.size()) {
            Coin coin = displayedCoins.get(slotIndex);
            Wallet wallet = WalletManager.getWallet(playerRef.getUuid());
            long balance = wallet.getBalance();

            // Withdraw 1 coin by default, 100 if shift is held (or max possible)
            long maxWithdrawable = balance / coin.getValue();
            if (maxWithdrawable > 0) {
                int toWithdraw = data.isShiftHeld() ? (int) Math.min(maxWithdrawable, 100) : 1;
                long totalValue = (long) toWithdraw * coin.getValue();

                if (wallet.remove(totalValue)) {
                    Player playerComponent = (Player) store.getComponent(ref, Player.getComponentType());
                    if (playerComponent != null) {
                        ItemStack toAdd = new ItemStack(coin.getItemId(), toWithdraw);
                        ItemStackTransaction transaction = playerComponent.getInventory().getCombinedHotbarFirst().addItemStack(toAdd);
                        
                        ItemStack remainder = transaction.getRemainder();
                        if (remainder != null && !remainder.isEmpty()) {
                            // Part of the stack (or all of it) couldn't fit
                            int addedCount = toAdd.getQuantity() - remainder.getQuantity();
                            if (addedCount > 0) {
                                playerComponent.notifyPickupItem(ref, toAdd.withQuantity(addedCount), null, store);
                            }
                            ItemUtils.dropItem(ref, remainder, store);
                        } else {
                            // Everything was added successfully
                            playerComponent.notifyPickupItem(ref, toAdd, null, store);
                        }
                    }

                    rebuild();
                }
            }
        }
    }

    public static class WalletEventData {
        public static final BuilderCodec<WalletEventData> CODEC = BuilderCodec.builder(WalletEventData.class, WalletEventData::new)
                .append(new KeyedCodec<>("Index", Codec.STRING), (d, s) -> d.index = s, d -> d.index).add()
                .append(new KeyedCodec<>("ShiftHeld", Codec.BOOLEAN), (d, b) -> {
                    if (b != null) {
                        d.shiftHeld = b;
                    }
                }, d -> d.shiftHeld).add()
                .build();

        private String index;
        private boolean shiftHeld;

        public String getIndex() {
            return index;
        }

        public boolean isShiftHeld() {
            return shiftHeld;
        }
    }
}
