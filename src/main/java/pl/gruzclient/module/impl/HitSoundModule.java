package pl.gruzclient.module.impl;

import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import pl.gruzclient.module.Module;
import pl.gruzclient.module.Setting;

/** Własny dźwięk przy trafieniu przeciwnika (słyszysz tylko Ty). */
public class HitSoundModule extends Module {
    public final Setting.Choice sound = add(new Setting.Choice("sound", "setting.sound", 0,
            "sound.crystal", "sound.ding", "sound.pling", "sound.bell", "sound.arrow"));
    public final Setting.Number volume = add(new Setting.Number("volume", "setting.volume", 70f, 10f, 100f, 5f, "%"));
    public final Setting.Number pitch = add(new Setting.Number("pitch", "setting.pitch", 1.0f, 0.5f, 2.0f, 0.1f, ""));

    public HitSoundModule() { super("hitsound", Category.PVP, false); }

    /** SoundEvents mają w 1.21 dwa typy (SoundEvent i RegistryEntry) – obsługujemy oba */
    private static SoundEvent of(Object o) {
        if (o instanceof RegistryEntry<?> r) return (SoundEvent) r.value();
        return (SoundEvent) o;
    }

    private SoundEvent event() {
        return switch (sound.index) {
            case 1 -> of(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP);
            case 2 -> of(SoundEvents.BLOCK_NOTE_BLOCK_PLING);
            case 3 -> of(SoundEvents.BLOCK_NOTE_BLOCK_BELL);
            case 4 -> of(SoundEvents.ENTITY_ARROW_HIT_PLAYER);
            default -> of(SoundEvents.BLOCK_AMETHYST_BLOCK_HIT);
        };
    }

    public void play() {
        if (!isEnabled()) return;
        mc.getSoundManager().play(PositionedSoundInstance.master(event(), pitch.value, volume.value / 100f));
    }
}
