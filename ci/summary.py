import os, sys
art, sha, run = sys.argv[1:4]
print(f"# Build {run} ({sha[:8]})\n")
for d in sorted(os.listdir(art)):
    if not d.startswith('out-'): continue
    v = d[4:]
    p = os.path.join(art, d)
    code = open(os.path.join(p, 'exit')).read().strip() if os.path.exists(os.path.join(p, 'exit')) else '?'
    errs = open(os.path.join(p, 'errors.txt'), encoding='utf-8').readline().strip() if os.path.exists(os.path.join(p, 'errors.txt')) else ''
    jar = any(f.endswith('.jar') for f in os.listdir(p))
    smoke = open(os.path.join(p, 'smoke')).read().strip() if os.path.exists(os.path.join(p, 'smoke')) else '-'
    print(f"- {v}: build {'OK' if code == '0' and jar else 'FAIL'} ({errs}), w grze: {smoke}")
