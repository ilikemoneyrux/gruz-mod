package pl.gruzclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.UUID;
import net.minecraft.client.gui.components.BossHealthOverlay;

@Mixin(BossHealthOverlay.class)
public interface BossBarHudAccessor {
    @Accessor("events")
    Map<UUID, ?> gruz$getBossBars();
}
