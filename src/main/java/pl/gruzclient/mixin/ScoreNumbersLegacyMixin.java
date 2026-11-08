//#only MC < 1.20.3
package pl.gruzclient.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pl.gruzclient.module.Modules;

/**
 * 1.20.1: ukrywanie czerwonych liczb na tablicy wyników (NumberFormat jest dopiero od 1.20.3).
 * W displayScoreboardSidebar tekst (String) rysują i mierzą tylko liczby – nicki i tytuł to Component.
 */
@Mixin(Gui.class)
public abstract class ScoreNumbersLegacyMixin {
    @Redirect(method = "displayScoreboardSidebar", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I"))
    private int gruz$drawNumber(GuiGraphics c, Font font, String text, int x, int y, int color, boolean shadow) {
        if (Modules.SCOREBOARD.hideNumbers()) return 0;
        return c.drawString(font, text, x, y, color, shadow);
    }

    @Redirect(method = "displayScoreboardSidebar", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;width(Ljava/lang/String;)I"))
    private int gruz$numberWidth(Font font, String text) {
        if (Modules.SCOREBOARD.hideNumbers()) return 0;
        return font.width(text);
    }
}
