# Sprawdzanie kompilacji bez Minecrafta

Gdy nie ma dostępu do Gradle/Fabric (albo chcesz szybko sprawdzić poprawkę przed pushem):

1. `ci/offline/check.sh 26.1` – wersja, która na pewno działa (punkt odniesienia),
2. `ci/offline/check.sh 26.2` – sprawdzana wersja,
3. `cd /tmp/gruz-offline && python3 <repo>/ci/offline/diff.py 26.1 26.2` – nowe błędy.

Jak to działa: `stubgen.py` robi z wydruku javap (`inspect/ALL.txt.gz` z gałęzi `ci-log-<wersja>`) puste klasy
Minecrafta, `lib/` to ręczne stuby Mixin/Fabric/Gson/GLFW, a `extstub.py` dorabia puste typy innych bibliotek.
`javac` kompiluje wszystko razem – liczą się tylko błędy w plikach moda. Kilkanaście błędów (joml, authlib,
GLFWImage) to braki stubów i pojawiają się też w działających wersjach, dlatego porównujemy z 26.1.
Miksy sprawdza osobno `ci/check_mixins.py`. Zrzut ALL.txt.gz jest tylko dla wersji bez obfuskacji i 1.20.1.
