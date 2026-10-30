# Pomocnik: javap wybranych klas z Minecrafta (z nazwami Mojanga) dla danej wersji.
# ci/inspect.txt: linie "<wersja albo *> <klasa albo LIST>" np. "* net.minecraft.client.gui.GuiGraphics"
import glob, os, subprocess, sys, zipfile
mc, out = sys.argv[1], sys.argv[2]
want = []
for l in open('ci/inspect.txt').read().splitlines():
    l = l.strip()
    if not l or l.startswith('#'): continue
    v, cls = l.split(None, 1)
    if v in ('*', mc): want.append(cls.strip())
if not want: sys.exit(0)
home = os.path.expanduser('~')
cands = [p for p in glob.glob(home + '/.gradle/caches/fabric-loom/**/*.jar', recursive=True) + glob.glob('versions/%s/.gradle/loom-cache/**/*.jar' % mc, recursive=True) + glob.glob(home + '/.gradle/caches/**/*.jar', recursive=True)]
def has(p, name):
    try: return name in zipfile.ZipFile(p).namelist()
    except Exception: return False
probe = 'net/minecraft/client/Minecraft.class'
jars = [p for p in cands if mc in p and has(p, probe)] or [p for p in cands if has(p, probe)]
os.makedirs(out + '/inspect', exist_ok=True)
with open(out + '/inspect/jars.txt', 'w') as f: f.write('\n'.join(jars))
if not jars: sys.exit(0)
jar = sorted(jars, key=lambda p: ('named' not in p and 'merged' not in p, len(p)))[0]
cp = jar + ':' + ':'.join(glob.glob(home + '/.gradle/caches/modules-2/**/*.jar', recursive=True))
for cls in want:
    if cls == 'LIST':
        names = sorted(n[:-6].replace('/', '.') for n in zipfile.ZipFile(jar).namelist() if n.endswith('.class') and '$' not in n)
        open(out + '/inspect/LIST.txt', 'w').write('\n'.join(names))
        continue
    r = subprocess.run(['javap', '-p', '-cp', cp, cls], capture_output=True, text=True)
    open(out + '/inspect/' + cls.replace('$', '_') + '.txt', 'w').write(r.stdout + r.stderr)
