package com.crimulus.anvil_player_heads;

import com.crimulus.anvil_player_heads.mixin.AnvilScreenHandlerAccessor;
import com.mojang.authlib.GameProfile;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.text.html.Option;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class AnvilPlayerHeads implements ModInitializer {
    public static final String MOD_ID = "anvil-player-heads";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Map<Integer, String> ENTITY_ID_TO_LOOKUP_NAME = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
    }

    public static boolean applyRenaming(final Player player, @NotNull NonNullList<Slot> slots, final String newItemName) {
        Item leftItem = slots.get(0).getItem().getItem();
        int middleCount = slots.get(1).getItem().getCount();

        if (
                leftItem != Items.PLAYER_HEAD ||
                middleCount != 0 ||
                !(player instanceof final ServerPlayer serverPlayer) ||
                !(serverPlayer.containerMenu instanceof AnvilMenu anvilHandler)
        ) {
            return false;
        }

        ENTITY_ID_TO_LOOKUP_NAME.put(player.getId(), newItemName);

        if (newItemName.isEmpty()) {
            // If player removes custom name, gives back a default PlayerHead without a custom name
            perform_rename(serverPlayer, anvilHandler, null, null);
            return true;
        }

        CompletableFuture.supplyAsync(
                () -> fetch_player_profile(serverPlayer, anvilHandler, newItemName)
        );
        return true;
    }

    static Optional<String> fetch_player_profile(final ServerPlayer serverPlayer, final AnvilMenu anvilHandler, final String newItemName) {
        try {
            Thread.sleep(500); // Don't call the API at every keystroke. Wait a tiny bit and check if the Player stopped typing
        } catch (InterruptedException e) {
            // Interrupted
        }
        if (is_rename_outdated(serverPlayer, newItemName)) {
            return Optional.empty(); // Player changed the head name, return early
        }

        ItemStack current_item = anvilHandler.slots.getFirst().getItem();
        ResolvableProfile previous_profile_component = current_item.getItem().components().get(DataComponents.PROFILE);
        GameProfile previous_profile = previous_profile_component != null ? previous_profile_component.partialProfile() : null;
        Component custom_name = current_item.getCustomName();
        String custom_name_to_assign = custom_name != null ? custom_name.getString() : null;

        custom_name_to_assign = newItemName;

        MinecraftServer minecraftServer = serverPlayer.level().getServer();
        ProfileResolver resolver = minecraftServer.services().profileResolver();

        Optional<GameProfile> profile = resolver.fetchByName(newItemName);

        GameProfile profile_to_assign = previous_profile;


        if (profile.isPresent() && !profile.get().properties().isEmpty()) {
            profile_to_assign = profile.get();
            custom_name_to_assign = null; // We don't want the head renamed if there is a new profile assigned to it
        }

        perform_rename(serverPlayer, anvilHandler, custom_name_to_assign, profile_to_assign);

        return Optional.empty();
    }

    static boolean is_rename_outdated(@NotNull ServerPlayer serverPlayer, String newItemName) {
        return !ENTITY_ID_TO_LOOKUP_NAME.containsKey(serverPlayer.getId()) || !ENTITY_ID_TO_LOOKUP_NAME.get(serverPlayer.getId()).equals(newItemName);
    }

    static void perform_rename(@NotNull ServerPlayer serverPlayer, AnvilMenu anvilHandler, String new_name, GameProfile new_profile) {
        MinecraftServer minecraftServer = serverPlayer.level().getServer();
        minecraftServer.executeIfPossible(() -> {
            ((AnvilScreenHandlerAccessor) anvilHandler).aph$getLevelCost().set(1);
            ItemStack newItem = anvilHandler.slots.getFirst().getItem().copy();

            if (new_name != null) {
                newItem.set(DataComponents.CUSTOM_NAME, Component.literal(new_name));
            } else {
                newItem.remove(DataComponents.CUSTOM_NAME);
            }
            if (new_profile != null) {
                newItem.set(DataComponents.PROFILE, ResolvableProfile.createResolved(new_profile));
            } else {
                newItem.remove(DataComponents.PROFILE);
            }

            anvilHandler.slots.get(2).setByPlayer(newItem);
            anvilHandler.broadcastChanges();
        });
    }
}