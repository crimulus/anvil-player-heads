package com.crimulus.anvil_player_heads;

import com.crimulus.anvil_player_heads.mixin.AnvilScreenHandlerAccessor;
import com.crimulus.anvil_player_heads.mixin.SkullBlockEntityAccessor;
import net.fabricmc.api.ModInitializer;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AnvilPlayerHeads implements ModInitializer {

	public static final String MOD_ID = "anvil-player-heads";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final Map<Integer, String> ENTITY_ID_TO_LOOKUP_NAME = new ConcurrentHashMap<>();

	@Override
	public void onInitialize() {
	}

	public static boolean applyRenaming(PlayerEntity player, DefaultedList<Slot> slots, final String playerName) {

		Item leftItem = slots.get(0).getStack().getItem();
		int middleCount = slots.get(1).getStack().getCount();

		if (
				leftItem == Items.PLAYER_HEAD &&
				middleCount == 0 &&
				player instanceof final ServerPlayerEntity serverPlayer &&
				!playerName.isEmpty()
		) {

			ENTITY_ID_TO_LOOKUP_NAME.put(player.getId(), playerName);

			SkullBlockEntityAccessor.aph$fetchProfileByName(playerName).thenAcceptAsync(profile -> {

				if (
						profile.isPresent() &&
						!profile.get().getProperties().isEmpty() &&
						serverPlayer.currentScreenHandler instanceof AnvilScreenHandler anvilHandler
				) {

					if (
							!ENTITY_ID_TO_LOOKUP_NAME.containsKey(player.getId()) ||
							!ENTITY_ID_TO_LOOKUP_NAME.get(player.getId()).equals(playerName)
					) return;

					serverPlayer.server.executeSync(() -> {
						((AnvilScreenHandlerAccessor) anvilHandler).aph$getLevelCost().set(1);

						ItemStack newItem = anvilHandler.slots.get(0).getStack().copy();
						newItem.set(
								DataComponentTypes.PROFILE,
								new ProfileComponent(profile.get())
						);
						anvilHandler.slots.get(2).setStack(newItem);
						anvilHandler.updateResult();
					});
				}
			});
		}
		else {
			slots.get(2).setStack(slots.get(0).getStack().copy());
		}
		return true;
	}

}