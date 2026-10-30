package pl.gruzclient.chat;

import net.minecraft.text.OrderedText;

/** Dodatkowe metody, które mixin dokłada do ChatHud. */
public interface GruzChat {
    /** Przebudowuje widoczne linie (np. po zmianie wyszukiwania) */
    void gruz$rebuild();

    /** Pełny tekst wiadomości pod kursorem (albo null) */
    String gruz$messageAt(double mouseX, double mouseY);

    static String plain(OrderedText text) {
        StringBuilder sb = new StringBuilder();
        text.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });
        return sb.toString();
    }
}
