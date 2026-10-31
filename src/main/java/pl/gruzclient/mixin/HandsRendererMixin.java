//#only MC >= 26.3
package pl.gruzclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gruzclient.module.Modules;

/**
 * 26.3+: ręka i przedmioty w pierwszej osobie rysuje FirstPersonHandsAndItemsRenderer (stan renderowania zamiast gracza).
 * Niżej trzymana tarcza i ukrywanie tarczy przy bloku mieczem; poza bloku z 1.8 i brak opadania ręki są tu wyłączone
 * (nie ma już renderItem/tick, w które wpinały się te funkcje).
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class HandsRendererMixin {
    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true, require = 0)
    private void gruz$shieldDown(PlayerRenderState state, FirstPersonHandsAndItemsRenderState hands, float tickDelta, float pitch,
                                 InteractionHand hand, float swingProgress, ItemStack item, float equipProgress, PoseStack matrices,
                                 SubmitNodeCollector collector, int light, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && item.is(Items.SHIELD) && Modules.OLD_ANIM.swordBlocking(player)) { ci.cancel(); return; }
        matrices.pushPose();
        if (player != null && item.is(Items.SHIELD) && !(player.isUsingItem() && player.getUsedItemHand() == hand)) {
            matrices.translate(0f, -Modules.LOW_FIRE.shieldOffset(), 0f);
        }
    }

    @Inject(method = "submitArmWithItem", at = @At("RETURN"), require = 0)
    private void gruz$shieldRestore(PlayerRenderState state, FirstPersonHandsAndItemsRenderState hands, float tickDelta, float pitch,
                                    InteractionHand hand, float swingProgress, ItemStack item, float equipProgress, PoseStack matrices,
                                    SubmitNodeCollector collector, int light, CallbackInfo ci) {
        matrices.popPose();
    }
}
