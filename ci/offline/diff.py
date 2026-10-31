import sys
base, v = sys.argv[1], sys.argv[2]
def load(x):
    out = []
    for l in open(f'errs-{x}.tsv', encoding='utf8'):
        p = l.rstrip('\n').split('\t')
        if len(p) >= 4: out.append(p)
    return out
bk = {(p[0], p[2], p[3], p[4] if len(p) > 4 else '') for p in load(base)}
new = [p for p in load(v) if (p[0], p[2], p[3], p[4] if len(p) > 4 else '') not in bk]
print(f'{v}: {len(new)} new errors vs {base}')
for p in new: print(f'{p[0]}:{p[1]}: {p[2]}  || {p[3][:150]}  {p[4] if len(p)>4 else ""}')
