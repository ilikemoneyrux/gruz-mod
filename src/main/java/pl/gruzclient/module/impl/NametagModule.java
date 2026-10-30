package pl.gruzclient.module.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import pl.gruzclient.cosmetics.Cosmetics;
import pl.gruzclient.module.Module;
import net.minecraft.util.Identifier;
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
    public Text label(Entity entity, Text text) {
        if (!isEnabled() || !(entity instanceof PlayerEntity)) return text;
        Text out = recolor.value ? Text.literal(text.getString()).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color.rgb()))) : text;
        out = pl.gruzclient.cosmetics.NameEffects.apply(out, Cosmetics.of((PlayerEntity) entity), ((PlayerEntity) entity).getGameProfile().getName());
        if (gruzIcon.value && Cosmetics.isGruzUser(entity.getUuid())) return withIcon(out);
        return out;
    }

    private static final Identifier ICON_FONT = Identifier.of("gruzclient", "icons");

    /** Logo Gruz (znak z własnej czcionki) przed nickiem */
    public static Text withIcon(Text name) {
        Text icon = Text.literal("\uE000").setStyle(Style.EMPTY.withFont(ICON_FONT).withColor(TextColor.fromRgb(0xFFFFFF)));
        return Text.empty().append(icon).append(Text.literal(" ")).append(name);
    }

    /** Czy encja jest graczem z Twojej drużyny (scoreboard team) */
    public boolean glow(Entity entity) {
        if (!isEnabled() || !teamGlow.value || mc.player == null) return false;
        return entity instanceof PlayerEntity && entity != mc.player && mc.player.isTeammate(entity) && mc.player.getScoreboardTeam() != null;
    }
}
