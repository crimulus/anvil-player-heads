package ca.lukegrahamlandry.headrename.mixin;

import ca.lukegrahamlandry.headrename.ModMain;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.Property;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreenHandler.class)
public class AnvilMixin {
	@Shadow @Final private Property levelCost;

	@Shadow private String newItemName;

	@Inject(at = @At("HEAD"), method = "updateResult", cancellable = true)
	private void init(CallbackInfo info) {
		if (ModMain.handleHeadRename((AnvilScreenHandler) (Object) this, newItemName)){
			levelCost.set(1);
			info.cancel();
		}
	}
}
