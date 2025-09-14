package de.tomalbrc.avatarrenderer.mixin;

import de.tomalbrc.avatarrenderer.AvatarRendererMod;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Inject(method = "placeNewPlayer", at = @At("HEAD"))
    private void ar$onPlacePlayer(Connection connection, ServerPlayer serverPlayer, CommonListenerCookie commonListenerCookie, CallbackInfo ci) {
        for (int i = 0; i < 256; i++) {
            // hm we technically dont need to cache every offset
            AvatarRendererMod.get(serverPlayer.getScoreboardName(), i, false, AvatarRendererMod.NOOP);
            AvatarRendererMod.get(serverPlayer.getScoreboardName(), i, true, AvatarRendererMod.NOOP);
        }
    }
}
