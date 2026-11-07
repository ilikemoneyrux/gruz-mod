#!/bin/bash
# Sprawdzenie kompilacji moda BEZ Minecrafta i Gradle: stuby klas z zrzutów javap (gałęzie ci-log-<wersja>).
#   ci/offline/check.sh 26.2          -> błędy kompilacji w errs-26.2.tsv
#   python3 ci/offline/diff.py 26.1 26.2   -> tylko błędy, których nie ma w działającej wersji (reszta to braki stubów bibliotek)
# Wymaga: python3, javac 21+, git (pobiera inspect/ALL.txt.gz z gałęzi ci-log-<wersja>).
set -e
v=$1; ROOT=$(cd "$(dirname "$0")/../.." && pwd); W=${GRUZ_OFFLINE_DIR:-/tmp/gruz-offline}; H=$ROOT/ci/offline
mkdir -p $W && cd $W
obf=$(grep -oP 'obf=\K\w+' $ROOT/versions/$v/gradle.properties)
if [ ! -f dump-$v.txt ]; then
  git -C $ROOT fetch --depth=1 origin ci-log-$v:refs/remotes/origin/ci-log-$v >/dev/null 2>&1 || true
  git -C $ROOT show origin/ci-log-$v:inspect/ALL.txt.gz | gunzip > dump-$v.txt
fi
rm -rf pp-$v out-$v; python3 -W ignore $H/pp.py $ROOT $v $obf pp-$v > /dev/null
[ -d stubs-$v ] || python3 $H/stubgen.py dump-$v.txt stubs-$v > /dev/null
rm -rf ext-$v; python3 $H/extstub.py stubs-$v $H/lib ext-$v $(find pp-$v -name "*.java") > /dev/null
find stubs-$v $H/lib ext-$v pp-$v -name "*.java" > src-$v.txt
if command -v cygpath >/dev/null; then sed -i 's#^/\([a-z]\)/#\1:/#' src-$v.txt; fi  # Git Bash na Windows: javac potrzebuje ścieżek C:/
mkdir -p out-$v
javac -J-Xmx6g --release 21 -proc:none -nowarn -Xmaxerrs 1000000 -encoding UTF-8 -d out-$v @src-$v.txt 2> javac-$v.log || true
python3 - "$v" <<'PY'
import re, sys
v = sys.argv[1]
log = open(f'javac-{v}.log', encoding='utf8', errors='replace').read().split('\n')
errs = []
for i, l in enumerate(log):
    m = re.match(rf'^pp-{re.escape(v)}/(.+?\.java):(\d+): error: (.*)$', l)
    if not m: continue
    code = log[i + 1].strip() if i + 1 < len(log) else ''
    sym = ''
    for j in range(i + 2, min(i + 6, len(log))):
        if 'symbol:' in log[j]: sym = log[j].strip(); break
    errs.append((m.group(1), m.group(2), m.group(3), code, sym))
open(f'errs-{v}.tsv', 'w').write('\n'.join('\t'.join(e) for e in errs))
print(v, 'błędy w kodzie moda:', len(errs))
PY
