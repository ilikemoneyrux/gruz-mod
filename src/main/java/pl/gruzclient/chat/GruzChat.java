package pl.gruzclient.chat;

import net.minecraft.util.FormattedCharSequence;

/** Dodatkowe metody, które mixin dokłada do ChatHud. */
public interface GruzChat {
    /** Przebudowuje widoczne linie (np. po zmianie wyszukiwania) */
    void gruz$rebuild();

    /** Pełny tekst wiadomości pod kursorem (albo null) */
    String gruz$messageAt(double mouseX, double mouseY);

    static String plain(FormattedCharSequence text) {
        StringBuilder sb = new StringBuilder();
        text.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });
        return sb.toString();
    }
}
