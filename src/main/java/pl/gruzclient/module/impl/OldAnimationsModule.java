package pl.gruzclient.module.impl;

import pl.gruzclient.compat.Compat;
import pl.gruzclient.module.Module;
import pl.gruzclient.module.Setting;

/**
 * Animacje jak w 1.7/1.8: ręka nie opada po ciosie (brak animacji cooldownu),
 * wolniejsze lub szybsze machanie i brak "przeładowania" przy zmianie na ten sam przedmiot.
 */
public class OldAnimationsModule extends Module {
    public final Setting.Bool noCooldown = add(new Setting.Bool("noCooldown", "setting.noCooldownDip", true));
    public final Setting.Number swing = add(new Setting.Number("swing", "setting.swingSpeed", 6f, 3f, 14f, 1f, ""));

    public final Setting.Bool swordBlock = add(new Setting.Bool("swordBlock", "setting.swordBlock", true));

    public OldAnimationsModule() { super("oldanim", Category.VISUAL, false); }

    public boolean noCooldown() { return isEnabled() && noCooldown.value; }
    /** Blokowanie mieczem jak w 1.8: miecz w ręce + tarcza w drugiej, tarcza w użyciu */
    public boolean swordBlocking(net.minecraft.world.entity.player.Player p) {
        if (!isEnabled() || !swordBlock.value || p == null || !p.isUsingItem()) return false;
        var hand = p.getUsedItemHand();
        var other = hand == net.minecraft.world.InteractionHand.MAIN_HAND ? net.minecraft.world.InteractionHand.OFF_HAND : net.minecraft.world.InteractionHand.MAIN_HAND;
        return p.getItemInHand(hand).is(net.minecraft.world.item.Items.SHIELD)
                && Compat.isSword(p.getItemInHand(other));
    }

    public int swingDuration(int vanilla) { return isEnabled() ? Math.round(swing.value) + (vanilla - 6) : vanilla; }
}
