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
- `ci/offline/check.sh <wersja>` – sprawdza kompilację offline (bez Gradle i Minecrafta), patrz `ci/offline/README.md`.

## Stan (1.11.2026, mod 0.5.0)
| Wersja | Build | W grze |
|---|---|---|
| 1.20.1 | lokalnie 0 błędów, czeka na CI | – |
| 1.20.4 | OK | OK |
| 1.21.1 | OK | OK |
| 1.21.4 | OK | OK |
| 1.21.11 | OK | OK |
| 26.1 | OK | OK |
| 26.2 | lokalnie 0 błędów, czeka na CI | – |
| 26.3 | lokalnie 0 błędów, czeka na CI | – |
| 1.8.9 | – | osobny projekt (Legacy Fabric) – nie zaczęty |

"Lokalnie" = `ci/offline/check.sh` (kompilacja na stubach z zrzutów javap) + `ci/check_mixins.py` bez błędów.
Pewność daje dopiero CI (build + test uruchomienia gry).

### Co się zmieniło w 0.5.0
- 26.2: ekrany i nakładki są w `mc.gui` (`screen()`, `setScreen`, `overlay()`), HUD to `mc.gui.hud` (klasa `Hud`):
  podmiany w `src/swaps.txt`, miksy HUD (`InGameHudMixin`, `InGameHudInvoker`) celują w `Hud`;
  `hideGui` → `gui.hud.isHidden()`, `Items.WHITE_WOOL` → `Items.WOOL.white()`, ogień: `submitFire`,
  ręka: `submitArmWithItem`.
- 26.3: ręka w 1. osobie to `FirstPersonHandsAndItemsRenderer` (nowy `HandsRendererMixin`), `KeyEvent.keycode()`,
  `InputConstants.Type.KEYBOARD`, `InputConstants.isKeyDown(kod)`, `OptionsScreen(ekran, opcje)`, `Blaze3D.openPath`.
- 1.20.1: `GruzScreen` dokłada `renderBackground(c, x, y, delta)` i `mouseScrolled(x, y, h, v)` jak w 1.20.2+,
  peleryna przez `getCloakTextureLocation`/`getElytraTextureLocation`, `ServerData(nazwa, ip, false)`,
  skin przez `getSkinLocation`, debug przez `options.renderDebug`.

## Wyłączone funkcje na części wersji (do przeniesienia)
- 1.21.2–1.21.10: kosmiczne niebo i kolor mgły (inne API nieba niż 1.21.11+, brak zrzutu klas).
- 1.20.4: dodatki czatu (łączenie powtórzeń, kopiowanie, dłuższa historia, wyszukiwarka).
- ≥ 1.21.2: motion blur (nowy post-processing), item physics (nowe modele przedmiotów), kolor mgły.
- ≥ 1.21.9: block overlay (obrys/wypełnienie bloku), kopiowanie wiadomości z czatu.
- 26.1: własna panorama menu (pokazuje zwykłą), fullbright przez lightmapę. (Kosmiczne niebo od 1.21.11: SkyRendererMixin + FogColorMixin – od 0.5.5.)
- 26.3: poza bloku mieczem z 1.8 i brak opadania ręki po ciosie (nie ma już renderItem/tick w rendererze ręki), motion blur (render() bez parametrów).
- Test w CI sprawdza menu i ekrany moda, ale nie wchodzi do świata – rzeczy w grze trzeba sprawdzić ręcznie.
