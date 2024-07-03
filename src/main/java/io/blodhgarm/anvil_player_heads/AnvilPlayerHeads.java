package io.blodhgarm.anvil_player_heads;

import com.mojang.authlib.GameProfile;
import io.blodhgarm.anvil_player_heads.mixin.AnvilScreenHandlerAccessor;
import io.blodhgarm.anvil_player_heads.mixin.SkullBlockEntityAccessor;
import net.fabricmc.api.ModInitializer;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class AnvilPlayerHeads implements ModInitializer {

    public static final String MODID = "anvil_player_heads";

    public void onInitialize() {}

    private static final Map<Integer, String> ENTITY_ID_TO_LOOKUP_NAME = new ConcurrentHashMap<>();

    public static boolean canHandleRenaming(PlayerEntity player, DefaultedList<Slot> slots, final String playerName) {
        var left = slots.get(0).getStack();

        if (left.getItem() != Items.PLAYER_HEAD || !slots.get(1).getStack().isEmpty() || !(player instanceof final ServerPlayerEntity serverPlayer) || playerName.isEmpty()) return false;

        final var playerId = player.getId();

        ENTITY_ID_TO_LOOKUP_NAME.put(playerId, playerName);

        SkullBlockEntityAccessor.aph$fetchProfile(playerName).thenAcceptAsync(gameProfile -> {
            if(!ENTITY_ID_TO_LOOKUP_NAME.containsKey(playerId) || !ENTITY_ID_TO_LOOKUP_NAME.get(playerId).equals(playerName)) return;

            serverPlayer.server.executeSync(() -> {
                var handler = serverPlayer.currentScreenHandler;

                if(handler instanceof AnvilScreenHandler anvilHandler && runScreenUpdate(anvilHandler, gameProfile)) {
                    anvilHandler.updateResult();
                }
            });
        });

        return true;
    }

    private static boolean runScreenUpdate(AnvilScreenHandler anvilScreenHandler, Optional<GameProfile> profile) {
        var slots = anvilScreenHandler.slots;

        var left = slots.get(0).getStack();

        if (left.getItem() != Items.PLAYER_HEAD || !slots.get(1).getStack().isEmpty()) return true;

        var output = left.copy();

        var bl = profile.isPresent();
        var nbt = output.getOrCreateNbt();

        if (bl) {
            nbt.putString("SkullOwner", profile.get().getName());

            ((AnvilScreenHandlerAccessor) anvilScreenHandler).aph$getLevelCost().set(1);
        }

        output.setNbt(nbt);

        slots.get(2).setStack(output);

        return !bl;
    }
}
