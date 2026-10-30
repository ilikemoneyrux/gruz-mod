# Tabela yarn -> Mojang dla danej wersji (do poprawiania miksów).
import io, json, sys, urllib.request, zipfile
mc, yarn = sys.argv[1], sys.argv[2]
get = lambda u: urllib.request.urlopen(urllib.request.Request(u, headers={'User-Agent': 'gruz-ci'})).read()
man = json.loads(get('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'))
ver = json.loads(get(next(v['url'] for v in man['versions'] if v['id'] == mc)))
moj = get(ver['downloads']['client_mappings']['url']).decode()
tiny = zipfile.ZipFile(io.BytesIO(get(f'https://maven.fabricmc.net/net/fabricmc/yarn/{yarn}/yarn-{yarn}-mergedv2.jar'))).read('mappings/mappings.tiny').decode()

# Mojang (proguard): moj -> obf
cls_m2o, cls_o2m, mem = {}, {}, {}
cur = None
prim = {'void': 'V', 'boolean': 'Z', 'byte': 'B', 'char': 'C', 'short': 'S', 'int': 'I', 'long': 'J', 'float': 'F', 'double': 'D'}
for line in moj.splitlines():
    if line.startswith('#') or not line.strip(): continue
    if not line.startswith(' '):
        a, b = line.rstrip(':').split(' -> ')
        cur = b; cls_m2o[a] = b; cls_o2m[b] = a; mem[b] = []
    else:
        a, obf = line.strip().split(' -> ')
        mem[cur].append((a, obf))
def jdesc(t):
    arr = 0
    while t.endswith('[]'): arr += 1; t = t[:-2]
    d = prim.get(t) or 'L' + cls_m2o.get(t, t).replace('.', '/') + ';'
    return '[' * arr + d
o_methods, o_fields = {}, {}
for o, lst in mem.items():
    for a, obf in lst:
        if '(' in a:
            a = a.split(':')[-1]
            ret, rest = a.split(' ', 1)
            name, args = rest.split('(', 1)
            args = args.rstrip(')')
            d = '(' + ''.join(jdesc(x) for x in args.split(',') if x) + ')' + jdesc(ret)
            o_methods[(o, obf, d)] = name
        else:
            t, name = a.split(' ')
            o_fields[(o, obf)] = name
# tiny v2: official intermediary named
out = []
cur = None
for line in tiny.splitlines()[1:]:
    p = line.split('\t')
    if p[0] == 'c':
        off, named = p[1], p[3]
        cur = (off, named)
        out.append(f"C {named.replace('/', '.')} -> {cls_o2m.get(off.replace('/', '.'), '?')}")
    elif len(p) > 1 and p[0] == '' and p[1] in ('m', 'f'):
        kind, desc, off, inter, named = p[1], p[2], p[3], p[4], p[5] if len(p) > 5 else p[4]
        owner = cur[0].replace('/', '.')
        if kind == 'm':
            mn = o_methods.get((owner, off, desc), '?')
            out.append(f"M {cur[1].replace('/', '.')}.{named}{desc} -> {mn}")
        else:
            out.append(f"F {cur[1].replace('/', '.')}.{named} -> {o_fields.get((owner, off), '?')}")
print('\n'.join(out))
