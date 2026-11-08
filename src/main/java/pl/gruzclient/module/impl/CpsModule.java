package pl.gruzclient.module.impl;

import pl.gruzclient.i18n.Lang;
import pl.gruzclient.module.Setting;
import pl.gruzclient.module.TextHudModule;

import java.util.ArrayDeque;
import java.util.Deque;

/** CPS dla lewego i prawego przycisku myszy (licznik kliknięć z ostatniej sekundy). */
public class CpsModule extends TextHudModule {
    private static final Deque<Long> LEFT = new ArrayDeque<>();
    private static final Deque<Long> RIGHT = new ArrayDeque<>();

    public final Setting.Bool showRight = add(new Setting.Bool("showRight", "setting.showRight", true));

    public CpsModule() { super("cps", true, 0.01f, 0.06f); }

    /** Wywoływane przy każdym kliknięciu w grze (0 = lewy, 1 = prawy) */
    public static void click(int button) {
        long now = System.currentTimeMillis();
        if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) LEFT.addLast(now);
        else if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT) RIGHT.addLast(now);
    }

    private static int count(Deque<Long> q) {
        long limit = System.currentTimeMillis() - 1000;
        while (!q.isEmpty() && q.peekFirst() < limit) q.pollFirst();
        return q.size();
    }

    public static int left() { return count(LEFT); }
    public static int right() { return count(RIGHT); }

    @Override
    protected String value(boolean editing) {
        return showRight.value ? left() + " | " + right() : String.valueOf(left());
    }

    @Override protected String label() { return Lang.t("hud.cps"); }
}
