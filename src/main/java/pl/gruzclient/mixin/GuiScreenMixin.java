//#only MC >= 26.2
package pl.gruzclient.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import pl.gruzclient.screen.GruzTitleScreen;

/** Od 26.2 ekrany ustawia mc.gui.setScreen (wcześniej Minecraft.setScreen – patrz MinecraftClientMixin). */
@Mixin(Gui.class)
public abstract class GuiScreenMixin {
    /** Za każdym razem, gdy gra chce pokazać menu główne, pokazujemy menu Gruz. */
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen gruz$replaceTitleScreen(Screen screen) {
        if (screen instanceof TitleScreen) return new GruzTitleScreen();
        return screen;
    }
}
