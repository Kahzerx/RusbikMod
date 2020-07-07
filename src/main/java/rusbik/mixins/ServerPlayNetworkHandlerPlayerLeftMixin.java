package rusbik.mixins;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rusbik.discord.DiscordListener;
import rusbik.perms.Perms;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerPlayerLeftMixin {
    @Shadow
    public ServerPlayerEntity player;
    @Inject(method = "onDisconnected", at = @At("RETURN"))
    private void onPlayerLeft(Text reason, CallbackInfo ci){
        if (DiscordListener.chatBridge){
            DiscordListener.sendMessage(":arrow_left: **" + player.getName().getString() + " left the game!**");
        }
        Perms.removeFromArray(player.getName().getString());
    }
}
