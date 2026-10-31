//#swap MC >= 26.3 | e.scancode() => e.keycode()
package pl.gruzclient.compat;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
//#if MC >= 1.21.9
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
//#endif

/**
 * Baza ekranów Gruz. Od 1.21.9 Minecraft przekazuje zdarzenia myszy i klawiatury jako obiekty
 * (MouseButtonEvent, KeyEvent) – tu tłumaczymy je na klasyczne metody (x, y, przycisk),
 * więc ekrany moda wyglądają tak samo na każdej wersji.
 */
public abstract class GruzScreen extends Screen {
    protected GruzScreen(Component title) { super(title); }

    //#if MC >= 1.21.9
    private int lastModifiers;

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        lastModifiers = e.modifiers();
        return mouseClicked(e.x(), e.y(), e.button());
    }

    public boolean mouseClicked(double mx, double my, int button) {
        return super.mouseClicked(new MouseButtonEvent(mx, my, new MouseButtonInfo(button, lastModifiers)), false);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        lastModifiers = e.modifiers();
        return mouseReleased(e.x(), e.y(), e.button());
    }

    public boolean mouseReleased(double mx, double my, int button) {
        return super.mouseReleased(new MouseButtonEvent(mx, my, new MouseButtonInfo(button, lastModifiers)));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        lastModifiers = e.modifiers();
        return mouseDragged(e.x(), e.y(), e.button(), dx, dy);
    }

    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        return super.mouseDragged(new MouseButtonEvent(mx, my, new MouseButtonInfo(button, lastModifiers)), dx, dy);
    }

    @Override
    public boolean keyPressed(KeyEvent e) { return keyPressed(e.key(), e.scancode(), e.modifiers()); }

    public boolean keyPressed(int key, int scancode, int modifiers) { return super.keyPressed(new KeyEvent(key, scancode, modifiers)); }

    @Override
    public boolean keyReleased(KeyEvent e) { return keyReleased(e.key(), e.scancode(), e.modifiers()); }

    public boolean keyReleased(int key, int scancode, int modifiers) { return super.keyReleased(new KeyEvent(key, scancode, modifiers)); }

    @Override
    public boolean charTyped(CharacterEvent e) {
        int cp = e.codepoint();
        //#if MC >= 26.1
        return Character.isBmpCodePoint(cp) && charTyped((char) cp, 0);
        //#else
        return Character.isBmpCodePoint(cp) && charTyped((char) cp, e.modifiers());
        //#endif
    }

    public boolean charTyped(char chr, int modifiers) {
        //#if MC >= 26.1
        return super.charTyped(new CharacterEvent(chr));
        //#else
        return super.charTyped(new CharacterEvent(chr, modifiers));
        //#endif
    }
    //#endif

    //#if MC < 1.20.2
    // 1.20.1: Screen.render nie rysuje tła (ekran wołał renderBackground(GuiGraphics) sam), a kółko myszy ma 3 parametry.
    // Dokładamy metody z 1.20.2+, więc ekrany moda nadpisują renderBackground(c, x, y, delta) i mouseScrolled(x, y, h, v) jak wszędzie.
    @Override
    public void render(net.minecraft.client.gui.GuiGraphics c, int mouseX, int mouseY, float delta) {
        renderBackground(c, mouseX, mouseY, delta);
        super.render(c, mouseX, mouseY, delta);
    }

    public void renderBackground(net.minecraft.client.gui.GuiGraphics c, int mouseX, int mouseY, float delta) { super.renderBackground(c); }

    @Override
    public boolean mouseScrolled(double mx, double my, double v) { return mouseScrolled(mx, my, 0, v); }

    public boolean mouseScrolled(double mx, double my, double h, double v) { return super.mouseScrolled(mx, my, v); }
    //#endif

    //#if MC >= 26.1
    // 26.1: render -> extractRenderState itd. Ekrany moda dalej nadpisują render/renderBackground.
    @Override
    public void extractRenderState(net.minecraft.client.gui.GuiGraphics c, int mouseX, int mouseY, float delta) { render(c, mouseX, mouseY, delta); }

    public void render(net.minecraft.client.gui.GuiGraphics c, int mouseX, int mouseY, float delta) { super.extractRenderState(c, mouseX, mouseY, delta); }

    @Override
    public void extractBackground(net.minecraft.client.gui.GuiGraphics c, int mouseX, int mouseY, float delta) { renderBackground(c, mouseX, mouseY, delta); }

    public void renderBackground(net.minecraft.client.gui.GuiGraphics c, int mouseX, int mouseY, float delta) { super.extractBackground(c, mouseX, mouseY, delta); }

    protected void renderPanorama(net.minecraft.client.gui.GuiGraphics c, float delta) { super.extractPanorama(c, delta); }
    //#endif

    // ---------------- modyfikatory ----------------
    public static boolean ctrl() { return key(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL) || key(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL); }
    public static boolean shift() { return key(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) || key(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT); }
    public static boolean alt() { return key(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT) || key(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_ALT); }

    private static boolean key(int code) {
        return org.lwjgl.glfw.GLFW.glfwGetKey(Compat.windowHandle(), code) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
    }
}
