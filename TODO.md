# Gruz Client – mod na wiele wersji

## Jak to działa
- Jeden kod w `src/main/java`, różnice między wersjami w komentarzach `//#if MC >= 1.21.2` … `//#endif`,
  `//#only` (cały plik) i `//#swap` (podmiana nazw w jednym pliku). Globalne podmiany nazw: `src/swaps.txt`.
- Różnice API zebrane w `pl.gruzclient.compat` (`Compat`, `Pose2D`, `GruzScreen`).
- GitHub Actions (`.github/workflows/build.yml`) buduje każdą wersję z `versions/`, uruchamia grę pod xvfb
  (test: menu → mody → edytor HUD → menu, log `GRUZ_SMOKE_OK`) i publikuje tylko przetestowane jary
  w wydaniu `mod` z `versions.json`. Launcher pobiera stamtąd jar pasujący do wersji gry.
- Logi buildów: gałęzie `ci-log-<wersja>` i `ci-results`. Zrzuty klas Minecrafta (javap): `ci/inspect.txt`.
- `ci/check_mixins.py <zrzuty> <wersje>` – sprawdza miksy offline względem zrzutów klas.

## Stan (31.10.2026)
| Wersja | Build | W grze |
|---|---|---|
| 1.20.4 | OK | OK |
| 1.21.1 | OK | OK |
| 1.21.4 | OK | OK |
| 1.21.11 | OK | OK |
| 26.1 | OK | OK |
| 26.2 | 51 błędów | – (Minecraft.setScreen/screen → mc.gui; HUD to klasa `Hud`) |
| 26.3 | 112 błędów | – (jak 26.2 + więcej) |
| 1.20.1 | 26 błędów | – (PlayerSkin, renderBackground/mouseScrolled, ServerData) |
| 1.8.9 | – | osobny projekt (Legacy Fabric) |

## Wyłączone funkcje na części wersji (do przeniesienia)
- 1.20.4: dodatki czatu (łączenie powtórzeń, kopiowanie, dłuższa historia, wyszukiwarka).
- ≥ 1.21.2: motion blur (nowy post-processing), item physics (nowe modele przedmiotów), kolor mgły.
- ≥ 1.21.9: block overlay (obrys/wypełnienie bloku), kopiowanie wiadomości z czatu.
- 26.1: własna panorama menu (pokazuje zwykłą), fullbright przez lightmapę, kolor nieba.
- Test w CI sprawdza menu i ekrany moda, ale nie wchodzi do świata – rzeczy w grze trzeba sprawdzić ręcznie.
