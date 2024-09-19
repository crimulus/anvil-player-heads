package com.crimulus.anvil_player_heads.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.entity.SkullBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Mixin(SkullBlockEntity.class)
public interface SkullBlockEntityAccessor {
    @Invoker("fetchProfileByName")
    static CompletableFuture<Optional<GameProfile>> aph$fetchProfileByName(String name) {
        throw new UnsupportedOperationException();
    }
}