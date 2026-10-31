#!/usr/bin/env python3
"""
Sprawdza miksy Gruz Client względem prawdziwych klas Minecrafta danej wersji (zrzuty javap z ci/inspect).
Wykrywa to, co wywala grę dopiero przy starcie: brakujące metody/pola (@Shadow, @Accessor, @Invoker)
i złe parametry w @Inject. Użycie: check_mixins.py <katalog-zrzutów> <wersja> [<wersja>...]
"""
import os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'src/main/java')


def vkey(v):
    p = [int(re.sub(r'[^0-9].*$', '', x) or 0) for x in v.split('.')]
    while len(p) < 3: p.append(0)
    return p[0] * 1_000_000 + p[1] * 1000 + p[2]


def cond(expr, mc):
    cur = vkey(mc)
    def rep(m):
        op, v = m.group(1), vkey(m.group(2))
        return str({'>=': cur >= v, '<=': cur <= v, '>': cur > v, '<': cur < v, '==': cur == v, '!=': cur != v}[op])
    e = re.sub(r'MC\s*(>=|<=|==|!=|>|<)\s*([0-9][0-9.]*)', rep, expr)
    e = re.sub(r'\bOBF\b', str(vkey(mc) < vkey('26.1')), e)
    e = e.replace('&&', ' and ').replace('||', ' or ').replace('!', ' not ')
    return eval(e)


def swaps(mc):
    out = []
    for l in open(os.path.join(ROOT, 'src/swaps.txt'), encoding='utf-8'):
        l = l.strip()
        if not l or l.startswith('#'): continue
        c, rule = [x.strip() for x in l.split('|', 1)]
        a, b = [x.strip() for x in rule.split('=>', 1)]
        if cond(c, mc): out.append((re.compile(r'(?<![\w$])' + re.escape(a) + r'(?![\w$])'), b))
    return out


def preprocess(text, mc, sw):
    lines = text.split('\n')
    only = next((l.strip() for l in lines if l.strip().startswith('//#only ')), None)
    if only and not cond(only[8:], mc): return None
    out, st = [], []
    act = lambda: not st or st[-1][2]
    for l in lines:
        t = l.strip()
        if t.startswith('//#if '):
            pa = act(); c = pa and cond(t[6:], mc); st.append([pa, c, c]); out.append('')
        elif t.startswith('//#elif '):
            f = st[-1]; c = f[0] and not f[1] and cond(t[8:], mc); f[2] = c; f[1] = f[1] or c; out.append('')
        elif t == '//#else':
            f = st[-1]; f[2] = f[0] and not f[1]; f[1] = True; out.append('')
        elif t == '//#endif':
            st.pop(); out.append('')
        elif t.startswith('//#only '):
            out.append('')
        else:
            out.append(l if act() else '')
    s = '\n'.join(out)
    for p, b in sw: s = p.sub(b, s)
    return s


# ---------------- zrzuty javap ----------------
class Dump:
    def __init__(self, path):
        self.methods = {}  # name -> [list of param type lists]
        self.fields = set()
        self.supers = []
        txt = open(path, encoding='utf-8', errors='replace').read()
        self.ok = 'class not found' not in txt
        m = re.search(r'(?:class|interface) ([\w.$]+)(?:<[^{]*?>)?(?: extends ([\w.$]+))?(?:<[^{]*?>)?(?: implements ([^{]+))?\s*\{', txt)
        if m:
            self.name = m.group(1)
            if m.group(2): self.supers.append(m.group(2))
            if m.group(3): self.supers += [re.sub(r'<.*', '', x.strip()) for x in split_top(m.group(3))]
        last = None
        for l in txt.splitlines():
            l = l.strip()
            if l.startswith('descriptor:'):
                d = l.split(':', 1)[1].strip()
                if last and d.startswith('('):
                    self.methods[last][-1] = desc_params(d)
                last = None
                continue
            last = None
            if not l.endswith(';') or l.startswith('Compiled'): continue
            l = re.sub(r' throws [^;]+', '', l)
            mm = re.match(r'(.*?)([\w$<>]+)\((.*)\);$', l)
            if mm:
                name = mm.group(2)
                if self.ok and m and name == m.group(1).split('.')[-1]: name = '<init>'
                if '.' in mm.group(1).split(' ')[-1] and mm.group(1).strip().split(' ')[-1] == '': pass
                params = [simple(x) for x in split_top(mm.group(3))] if mm.group(3).strip() else []
                full = mm.group(1).strip()
                # konstruktor: "public net.minecraft.X(" -> nazwa = ostatni człon klasy
                if name == (m.group(1).split('.')[-1] if m else None) or full.endswith(m.group(1) if m else '#'):
                    name = '<init>'
                self.methods.setdefault(name, []).append(params)
                last = name
            else:
                fm = re.match(r'.*?([\w$]+);$', l)
                if fm: self.fields.add(fm.group(1))


def split_top(s):
    out, depth, cur = [], 0, ''
    for ch in s:
        if ch == '<': depth += 1
        elif ch == '>': depth -= 1
        if ch == ',' and depth == 0:
            out.append(cur.strip()); cur = ''
        else:
            cur += ch
    if cur.strip(): out.append(cur.strip())
    return out


def simple(t):
    t = re.sub(r'<.*>', '', t.strip())
    t = re.sub(r'^(final|@\w+(\([^)]*\))?)\s+', '', t)
    t = t.replace('...', '[]')
    arr = t.count('[]')
    base = t.replace('[]', '').strip().split(' ')[0]
    base = re.split(r'[.$]', base)[-1]
    return base + '[]' * arr


class Dumps:
    def __init__(self, d, mc):
        self.dir = os.path.join(d, mc, 'inspect')
        self.cache = {}

    def get(self, fqn):
        if fqn in self.cache: return self.cache[fqn]
        p = os.path.join(self.dir, fqn.replace('$', '_') + '.txt')
        dmp = Dump(p) if os.path.exists(p) else None
        if dmp is not None and not dmp.ok: dmp = None
        self.cache[fqn] = dmp
        return dmp

    def lookup(self, fqn, attr, name, depth=0):
        """Szuka metody/pola w klasie i nadklasach. Zwraca (znaleziono, wynik) albo (None, None) gdy brak zrzutu."""
        dmp = self.get(fqn)
        if dmp is None: return None, None
        if attr == 'field' and name in dmp.fields: return True, None
        if attr == 'method' and name in dmp.methods: return True, dmp.methods[name]
        unknown = False
        if depth < 6:
            for s in dmp.supers:
                f, r = self.lookup(s, attr, name, depth + 1)
                if f: return True, r
                if f is None: unknown = True
        return (None if unknown else False), None


def desc_params(desc):
    out = []
    i = 0
    s = desc[desc.index('(') + 1:desc.index(')')]
    while i < len(s):
        arr = 0
        while s[i] == '[': arr += 1; i += 1
        c = s[i]
        if c == 'L':
            j = s.index(';', i); name = re.split(r'[/$]', s[i + 1:j])[-1]; i = j + 1
        else:
            name = {'Z': 'boolean', 'B': 'byte', 'C': 'char', 'S': 'short', 'I': 'int', 'J': 'long', 'F': 'float', 'D': 'double'}[c]; i += 1
        out.append(name + '[]' * arr)
    return out


ANN = r'@(Inject|Redirect|ModifyVariable|ModifyArgs|ModifyArg|ModifyConstant|ModifyExpressionValue|WrapOperation|WrapWithCondition)\s*\('


def check(d, mc):
    sw = swaps(mc)
    dumps = Dumps(d, mc)
    problems = []
    mdir = os.path.join(SRC, 'pl/gruzclient/mixin')
    for f in sorted(os.listdir(mdir)):
        s = preprocess(open(os.path.join(mdir, f), encoding='utf-8').read(), mc, sw)
        if s is None: continue
        imports = {m.group(1).split('.')[-1]: m.group(1) for m in re.finditer(r'import ([\w.]+);', s)}
        tm = re.search(r'@Mixin\((?:value\s*=\s*)?\{?\s*([\w.]+)\.class', s)
        if not tm: continue
        t = tm.group(1)
        head = t.split('.')[0]
        fqn = imports.get(head, t if '.' in t and t[0].islower() else 'net.minecraft.' + t)
        if '.' in t and head in imports: fqn = imports[head] + '$' + t.split('.', 1)[1].replace('.', '$')
        if dumps.get(fqn) is None:
            problems.append(f'?  {f}: brak zrzutu klasy {fqn}')
            continue
        # @Shadow
        for m in re.finditer(r'@Shadow[^;{(]*?\s([\w$]+)\s*(\(|;)', s):
            kind = 'method' if m.group(2) == '(' else 'field'
            found, _ = dumps.lookup(fqn, kind, m.group(1))
            if found is False: problems.append(f'X  {f}: @Shadow {kind} {m.group(1)} nie istnieje w {fqn}')
        for m in re.finditer(r'@(Accessor|Invoker)\("([\w$]+)"\)', s):
            kind = 'field' if m.group(1) == 'Accessor' else 'method'
            found, _ = dumps.lookup(fqn, kind, m.group(2))
            if found is False: problems.append(f'X  {f}: @{m.group(1)} {m.group(2)} nie istnieje w {fqn}')
        # wstrzyknięcia
        for m in re.finditer(ANN, s):
            kind = m.group(1)
            # treść adnotacji do pasującego nawiasu
            i, depth = m.end(), 1
            while depth and i < len(s):
                depth += {'(': 1, ')': -1}.get(s[i], 0); i += 1
            ann = s[m.end():i - 1]
            req0 = re.search(r'require\s*=\s*0', ann) is not None
            mspec = re.search(r'method\s*=\s*(\{[^}]*\}|"[^"]*")', ann)
            specs = re.findall(r'"([^"]+)"', mspec.group(1)) if mspec else []
            # sygnatura handlera
            hm = re.search(r'(?:private|public|protected)[^;{=]*?\s([\w$]+)\s*\(', s[i:])
            hparams = []
            if hm:
                j, depth = i + hm.end(), 1
                k = j
                while depth and k < len(s):
                    depth += {'(': 1, ')': -1}.get(s[k], 0); k += 1
                raw = re.sub(r'\s+', ' ', s[j:k - 1])
                hparams = [simple(' '.join(p.split(' ')[:-1])) for p in split_top(raw)] if raw.strip() else []
            for spec in specs:
                name = spec.split('(')[0]
                found, overloads = dumps.lookup(fqn, 'method', name)
                if found is None: continue
                if not found:
                    problems.append(('~  ' if req0 else 'X  ') + f'{f}: {kind} -> metoda {name} nie istnieje w {fqn}' + (' (require=0: moduł po cichu nie działa)' if req0 else ''))
                    continue
                if '(' in spec:
                    want = desc_params(spec)
                    if want not in overloads:
                        problems.append(('~  ' if req0 else 'X  ') + f'{f}: {kind} -> {spec} – brak takiej wersji; są: {overloads}')
                        continue
                    cand = [want]
                else:
                    cand = overloads
                if kind == 'Inject':
                    hp = [p for p in hparams if p not in ('CallbackInfo', 'CallbackInfoReturnable')]
                    if hp and not any(hp == c[:len(hp)] and len(hp) == len(c) for c in cand):
                        problems.append(f'X  {f}: @Inject {name}: parametry {hp} nie pasują do {cand}')
        # cele @At(INVOKE)
        for m in re.finditer(r'target\s*=\s*"L([\w/$]+);([\w<>$]+)(\([^"]*\)[^"]*)"', s):
            owner = m.group(1).replace('/', '.')
            found, overloads = dumps.lookup(owner, 'method', m.group(2))
            if found is False:
                problems.append(f'~  {f}: @At target {owner}.{m.group(2)} nie istnieje')
            elif found and desc_params(m.group(3)) not in overloads:
                problems.append(f'~  {f}: @At target {owner}.{m.group(2)}{m.group(3)} – inne parametry: {overloads}')
    return problems


if __name__ == '__main__':
    d = sys.argv[1]
    bad = False
    for mc in sys.argv[2:]:
        ps = check(d, mc)
        print(f'=== {mc}: {sum(p.startswith("X") for p in ps)} błędów, {sum(p.startswith("~") for p in ps)} ostrzeżeń')
        for p in ps: print('  ' + p)
        bad |= any(p.startswith('X') for p in ps)
    sys.exit(1 if bad else 0)
