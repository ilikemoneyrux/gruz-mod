# versions.json: który jar pasuje do jakiej wersji Minecrafta (czyta mc_dep z versions/<mc>/gradle.properties)
import json, os, sys
rel = sys.argv[1]
out = []
for f in sorted(os.listdir(rel)):
    if not (f.startswith('gruzclient-') and f.endswith('.jar')): continue
    mc = f[len('gruzclient-'):-4]
    props = dict(l.split('=', 1) for l in open(f'versions/{mc}/gradle.properties').read().splitlines() if '=' in l and not l.startswith('#'))
    out.append({'mc': mc, 'range': props['mc_dep'], 'file': f, 'java': int(props['java_version'])})
print(json.dumps({'mod': open('gradle.properties').read().split('mod_version=')[1].split()[0], 'builds': out}, indent=2))
