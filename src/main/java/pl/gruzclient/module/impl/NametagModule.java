package pl.gruzclient.module.impl;

import pl.gruzclient.compat.Compat;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import pl.gruzclient.cosmetics.Cosmetics;
import pl.gruzclient.module.Module;
import pl.gruzclient.module.Setting;

/** Własny kolor nicków nad głowami i podświetlenie członków Twojej drużyny (obrys widoczny przez ściany). */
public class NametagModule extends Module {
    public final Setting.Bool recolor = add(new Setting.Bool("recolor", "setting.recolorNames", false));
    public final Setting.Color color = add(new Setting.Color("color", "setting.color", 2));
    public final Setting.Bool teamGlow = add(new Setting.Bool("teamGlow", "setting.teamGlow", true));
    public final Setting.Bool showOwn = add(new Setting.Bool("own", "setting.showOwnName", false));
    public final Setting.Bool gruzIcon = add(new Setting.Bool("gruzIcon", "setting.gruzIcon", true));

    public NametagModule() { super("nametags", Category.VISUAL, true); }

    /** Nowy tekst nicku nad graczem (kolor nadpisywany tylko dla graczy) */
    public Component label(Entity entity, Component text) {
        if (!isEnabled() || !(entity instanceof Player)) return text;
        Component out = recolor.value ? Component.literal(text.getString()).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color.rgb()))) : text;
        out = pl.gruzclient.cosmetics.NameEffects.apply(out, Cosmetics.of((Player) entity), Compat.profileName(((Player) entity).getGameProfile()));
        if (gruzIcon.value && Cosmetics.isGruzUser(entity.getUUID())) return withIcon(out);
        return out;
    }

    private static final ResourceLocation ICON_FONT = Compat.id("gruzclient", "icons");

    /** Logo Gruz (znak z własnej czcionki) przed nickiem */
    public static Component withIcon(Component name) {
        Component icon = Component.literal("\uE000").setStyle(Compat.font(ICON_FONT).withColor(TextColor.fromRgb(0xFFFFFF)));
        return Component.empty().append(icon).append(Component.literal(" ")).append(name);
    }

    /** Czy encja jest graczem z Twojej drużyny (scoreboard team) */
    public boolean glow(Entity entity) {
        if (!isEnabled() || !teamGlow.value || mc.player == null) return false;
        return entity instanceof Player && entity != mc.player && mc.player.isAlliedTo(entity) && mc.player.getTeam() != null;
    }
}
