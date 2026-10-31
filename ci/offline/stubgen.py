# Generuje stuby .java z wydruku javap (ALL.txt) – tylko do sprawdzania kompilacji moda.
import re, sys, os, collections, pathlib
src, out = sys.argv[1], sys.argv[2]
text = open(src, encoding='utf8', errors='replace').read()
blocks = re.split(r'^Compiled from "[^"]*"\n', text, flags=re.M)
HDR = re.compile(r'^((?:(?:public|protected|private|abstract|final|static|sealed|non-sealed|strictfp)\s+)*)(class|interface)\s+([\w.$]+)(<.*?>)?(?:\s+extends\s+(.+?))?(?:\s+implements\s+(.+?))?(?:\s+permits\s+.+?)?\s*\{$')
classes = {}
for b in blocks:
    lines = b.split('\n')
    i = 0
    while i < len(lines):
        m = HDR.match(lines[i])
        if not m: i += 1; continue
        mods, kind, name, tps, ext, impl = m.groups()
        members = []
        i += 1
        while i < len(lines) and lines[i] != '}':
            l = lines[i].strip()
            if l and not l.startswith('descriptor:') and not l.startswith('Signature') and l.endswith(';'):
                members.append(l)
            i += 1
        classes[name] = dict(mods=mods.split(), kind=kind, tps=tps or '', ext=ext, impl=impl, members=members)
        i += 1
print('classes', len(classes))

def split_top(s, sep=','):
    out, depth, cur = [], 0, ''
    for ch in s:
        if ch == '<': depth += 1
        elif ch == '>': depth -= 1
        if ch == sep and depth == 0: out.append(cur.strip()); cur = ''
        else: cur += ch
    if cur.strip(): out.append(cur.strip())
    return out

def jt(t):
    # typ z javap -> źródło: Outer$Inner -> Outer.Inner
    return re.sub(r'([\w.])\$([A-Za-z_])', r'\1.\2', re.sub(r'([\w.])\$([A-Za-z_])', r'\1.\2', t))

# drzewo zagnieżdżeń
tops = collections.defaultdict(list)
for n in classes:
    if re.search(r'\$\d', n): continue
    if '$' in n:
        tops[n.rsplit('$', 1)[0]].append(n)
skip = {n for n in classes if re.search(r'\$\d', n)}

MEM = re.compile(r'^((?:(?:public|protected|private|abstract|final|static|synchronized|native|transient|volatile|default|strictfp)\s+)*)(<.+?>\s+)?(.+?)\s+([\w$]+)\((.*)\)(?:\s+throws\s+(.+))?;$')
CTOR = re.compile(r'^((?:(?:public|protected|private)\s+)*)([\w.$]+)\((.*)\)(?:\s+throws\s+(.+))?;$')
FIELD = re.compile(r'^((?:(?:public|protected|private|final|static|transient|volatile)\s+)*)(.+?)\s+([\w$]+);$')
PRIM = {'int':'0','long':'0L','short':'(short)0','byte':'(byte)0','char':"'\\0'",'float':'0f','double':'0d','boolean':'false'}

def gen(name, nested=False):
    c = classes[name]
    simple = name.rsplit('.', 1)[-1].rsplit('$', 1)[-1]
    mods = [m for m in c['mods'] if m in ('public', 'protected', 'abstract', 'final')]
    if 'private' in c['mods']: mods = [m for m in mods if m not in ('public','protected')]
    is_enum = c['ext'] and c['ext'].startswith('java.lang.Enum<')
    is_record = c['ext'] == 'java.lang.Record'
    is_annot = c['kind'] == 'interface' and (c['ext'] or '').strip() == 'java.lang.annotation.Annotation'
    head = ' '.join(mods + (['static'] if nested else []))
    body = []
    if is_annot:
        head = ' '.join([m for m in mods if m != 'abstract'] + (['static'] if nested else []))
        decl = f'{head} @interface {simple} {{'
        for mm in c['members']:
            m = MEM.match(mm)
            if m and '(' in mm and m.group(5) == '': body.append(f'  {jt(m.group(3))} {m.group(4)}();')
        return decl + '\n' + '\n'.join(body) + '\n' + ''.join(gen(n, True) for n in tops.get(name, []) if n not in skip) + '}\n'
    if is_enum:
        head = ' '.join([m for m in mods if m not in ('abstract', 'final')] + (['static'] if nested else []))
        decl = f'{head} enum {simple}'
        consts = []
        for mm in c['members']:
            f = FIELD.match(mm)
            if f and 'static' in f.group(1) and 'final' in f.group(1) and jt(f.group(2)) == jt(name) and 'public' in f.group(1):
                consts.append(f.group(3))
        body.append('  ' + (', '.join(consts)) + ';')
    else:
        kind = c['kind']
        if kind == 'interface': mods = [m for m in mods if m != 'abstract']
        decl = f'{" ".join(mods + (["static"] if nested else []))} {kind} {simple}{jt(c["tps"])}'
        ext = c['ext']
        if ext and not is_record and ext != 'java.lang.Object':
            decl += ' extends ' + jt(ext)
    if c['impl'] and not is_annot:
        decl += (' extends ' if c['kind'] == 'interface' and not c['ext'] else ' implements ') + jt(c['impl'])
    seen = set()
    for mm in c['members']:
        if mm.startswith('static {}') or mm.startswith('{}'): continue
        if re.match(r'^(private\s)', mm): continue
        cm = CTOR.match(mm)
        if cm and jt(cm.group(2)) == jt(name):
            if is_enum or c['kind'] == 'interface': continue
            params = split_top(cm.group(3))
            ps = ', '.join(f'{jt(p)} a{i}' for i, p in enumerate(params))
            th = f' throws {jt(cm.group(4))}' if cm.group(4) else ''
            body.append(f'  {cm.group(1)}{simple}({ps}){th} {{ throw new RuntimeException(); }}')
            continue
        m = MEM.match(mm)
        if m and '(' in mm:
            mods2, tp, ret, mname, params, throws = m.groups()
            if '$' in mname or mname in ('values', 'valueOf') and is_enum: continue
            params = split_top(params)
            key = (mname, len(params), tuple(re.sub(r'<.*>', '', p) for p in params))
            if key in seen: continue
            seen.add(key)
            ml = mods2.split()
            ps = ', '.join(f'{jt(p)} a{i}' for i, p in enumerate(params))
            th = f' throws {jt(throws)}' if throws else ''
            abstract = 'abstract' in ml and not is_enum
            ml = [x for x in ml if x not in ('native', 'synchronized', 'transient', 'volatile', 'strictfp')]
            if is_enum: ml = [x for x in ml if x != 'abstract']
            if c['kind'] == 'interface':
                ml = [x for x in ml if x not in ('public', 'abstract')]
                if 'static' in ml or 'default' in ml or 'private' in ml:
                    b = '{ throw new RuntimeException(); }'
                else: b = ';'
            else:
                b = ';' if abstract else '{ throw new RuntimeException(); }'
            body.append(f'  {" ".join(ml)} {jt(tp or "")}{jt(ret)} {mname}({ps}){th} {b}')
            continue
        f = FIELD.match(mm)
        if f:
            fm, ft, fn = f.groups()
            if '$' in fn: continue
            fl = fm.split()
            if is_enum and jt(ft) == jt(name) and 'static' in fl: continue
            init = ''
            if 'final' in fl:
                if 'static' in fl: init = ' = ' + PRIM.get(ft.strip(), 'null')
                else: fl.remove('final')
            if c['kind'] == 'interface': fl = [x for x in fl if x not in ('public', 'static', 'final')]; init = ' = ' + PRIM.get(ft.strip(), 'null')
            body.append(f'  {" ".join(fl)} {jt(ft)} {fn}{init};')
    inner = ''.join(gen(n, True) for n in tops.get(name, []) if n not in skip)
    return decl + ' {\n' + '\n'.join(body) + '\n' + inner + '}\n'

n = 0
for name in classes:
    if '$' in name: continue
    pkg, simple = name.rsplit('.', 1)
    p = pathlib.Path(out, *pkg.split('.'), simple + '.java')
    p.parent.mkdir(parents=True, exist_ok=True)
    try:
        p.write_text(f'package {pkg};\n' + gen(name), encoding='utf8'); n += 1
    except Exception as e:
        print('ERR', name, e)
print('files', n)
