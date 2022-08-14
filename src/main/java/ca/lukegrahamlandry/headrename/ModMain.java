package ca.lukegrahamlandry.headrename;

import net.fabricmc.api.ModInitializer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.AnvilScreenHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModMain implements ModInitializer {
	@Override
	public void onInitialize() {

	}

	// called by AnvilMixin
	public static boolean handleHeadRename(AnvilScreenHandler anvil, String playerName) {
		ItemStack left = anvil.slots.get(0).getStack();
		ItemStack right = anvil.slots.get(1).getStack();

		if (left.getItem() == Items.PLAYER_HEAD && right.isEmpty()){
			ItemStack output = left.copy();
			NbtCompound nbt = output.getOrCreateNbt();
			nbt.putString("SkullOwner", playerName);
			output.setNbt(nbt);

			anvil.slots.get(2).setStack(output);
			return true;
		}
		return false;
	}
}
