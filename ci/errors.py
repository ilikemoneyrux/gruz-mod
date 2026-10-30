# Wyciąga z logu Gradle to, co ważne: błędy kompilacji, "What went wrong" i koniec logu.
import re, sys
log = open(sys.argv[1], encoding='utf-8', errors='replace').read().splitlines()
out, seen = [], set()
for i, l in enumerate(log):
    m = re.search(r'(pp/java/|src/main/java/)(\S+\.java):(\d+): (error|warning): (.*)', l)
    if m and m.group(4) == 'error':
        key = (m.group(2), m.group(3), m.group(5))
        if key in seen: continue
        seen.add(key)
        out.append(f"{m.group(2)}:{m.group(3)}: {m.group(5)}")
        for k in range(1, 4):
            if i + k < len(log) and not re.search(r'\.java:\d+: ', log[i + k]):
                out.append('    ' + log[i + k].rstrip())
for i, l in enumerate(log):
    if 'What went wrong' in l:
        out.append('--- What went wrong ---')
        out.extend(log[i + 1:i + 25])
        break
print(f"errors: {len(seen)}")
print('\n'.join(out[:3000]))
