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
- ≥ 1.21.2: motion blur (nowy post-processing – w menu „NIEDOSTĘPNY NA TEJ WERSJI”, Module.available()).
- 1.21.2–1.21.8 (np. 1.21.4): item physics (od 1.21.9 działa przez ItemEntitySubmitMixin).
- 26.1: własna panorama menu (pokazuje zwykłą).
- 26.3: poza bloku mieczem z 1.8 i brak opadania ręki po ciosie (nie ma już renderItem/tick w rendererze ręki), motion blur (render() bez parametrów).
- Test w CI sprawdza menu i ekrany moda, ale nie wchodzi do świata. Lokalnie (Windows, z GPU) jest test w świecie:
  `GRUZ_MC=26.1 GRUZ_FT=1 ./gradlew :26.1:runClient` – status/FeatureTest.java wczytuje run/saves/GruzTest, sprawdza minimapę, block overlay,
  item physics, freecam (log pozycji i obrotu kamery), kosmiczne niebo, fullbright w ciemnym pokoju, kopiowanie z czatu i robi screeny
  run/screenshots/ft-*.png. Mody gracza można dorzucić do run/mods (z Fabric API).

## Naprawione w 0.5.7 (test w świecie 26.1, także z Sodium/BadOptimizations/EntityCulling/ImmediatelyFast)
- Minimapa od 1.21.6: enableScissor liczy się w układzie modułu HUD – wcześniej mapa była wycinana (widać było tylko strzałkę).
- Freecam: widok bierze yHeadRot (LivingEntity.getViewYRot), a turn() zmienia yRot – obraz się nie obracał, ruch szedł „w bok”.
- Block overlay od 1.21.9: BlockOutlineMixin (kolor/grubość renderHitOutline / submitHitOutline + wypełnienie QUADS).
- Fullbright od 26.1: LightmapStateMixin na Lightmap.render (BadOptimizations pomija extract).
- Item physics od 1.21.9: ItemEntitySubmitMixin. Kopiowanie wiadomości z czatu od 1.21.9: własne liczenie wiersza pod kursorem.
- HUD Gruz znikał przy włączonych Hitboxach (od 1.21.9 showDebugScreen() = true przy każdym wpisie debug) – Compat.debugScreenOpen.
