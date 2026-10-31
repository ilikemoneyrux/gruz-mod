# Puste stuby dla bibliotek (fastutil, guava, DFU, brigadier, netty, joml…), do których odwołują się stuby Minecrafta
import re, sys, pathlib, collections
stubdir, libdir, out = sys.argv[1], sys.argv[2], sys.argv[3]
have = set()
for d in (stubdir, libdir):
    for p in pathlib.Path(d).rglob('*.java'):
        have.add('.'.join(p.relative_to(d).with_suffix('').parts))
JDK = ('java.', 'javax.', 'jdk.', 'sun.')
FQN = re.compile(r'\b((?:com|org|it|io|net|oshi|joptsimple)\.(?:[a-z][a-z0-9_]*\.)+)([A-Z]\w*)((?:\.[A-Z]\w*)*)(<)?')
arity = collections.defaultdict(int); nested = collections.defaultdict(set); classlike = set()
def count_args(s, i):
    depth, n, j = 0, 1, i
    while j < len(s):
        ch = s[j]
        if ch == '<': depth += 1
        elif ch == '>':
            depth -= 1
            if depth == 0: return n
        elif ch == ',' and depth == 1: n += 1
        j += 1
    return 0
srcs = list(pathlib.Path(stubdir).rglob('*.java')) + [pathlib.Path(x) for x in sys.argv[4:]]
for p in srcs:
    s = p.read_text(encoding='utf8', errors='replace')
    for m in FQN.finditer(s):
        top = m.group(1) + m.group(2)
        if top in have or top.startswith('net.minecraft') or top.startswith('pl.gruzclient'): continue
        chain = [x for x in m.group(3).split('.') if x]
        full = top + ''.join('.' + x for x in chain)
        for k in range(len(chain)):
            nested[top + ''.join('.' + x for x in chain[:k])].add(chain[k])
        if m.group(4):
            a = count_args(s, m.end() - 1)
            arity[full] = max(arity[full], a)
        else: arity.setdefault(full, 0)
        pre = s[max(0, m.start() - 9):m.start()]
        if 'extends ' in pre and ' class ' in s[max(0, m.start() - 300):m.start()].split('\n')[-1]: classlike.add(full)
def tps(n): return '' if not arity.get(n) else '<' + ', '.join(f'T{i}' for i in range(arity[n])) + '>'
def body(n, nested_flag):
    kind = 'abstract class' if n in classlike else 'interface'
    simple = n.rsplit('.', 1)[-1]
    inner = ''.join(body(n + '.' + c, True) for c in sorted(nested.get(n, ())))
    return f'public {"static " if nested_flag and kind != "interface" else ""}{kind} {simple}{tps(n)} {{\n{inner}}}\n'
tops = {n for n in arity if not re.search(r'\.[A-Z]\w*\.[A-Z]', n) or n in nested}
written = 0
for n in sorted(set(arity) | set(nested)):
    parts = n.split('.')
    idx = next(i for i, x in enumerate(parts) if x[0].isupper())
    if idx != len(parts) - 1: continue  # zagnieżdżone – w pliku zewnętrznej
    pkg = '.'.join(parts[:idx])
    if n in have: continue
    f = pathlib.Path(out, *parts[:idx], parts[idx] + '.java'); f.parent.mkdir(parents=True, exist_ok=True)
    f.write_text(f'package {pkg};\n' + body(n, False), encoding='utf8'); written += 1
print('ext stubs', written)
