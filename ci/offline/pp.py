# Preprocesor jak w gradle/version.gradle: //#if, //#elif, //#else, //#endif, //#only, //#swap, src/swaps.txt
import re, sys, os, pathlib
def verkey(v):
    p=[int(re.sub(r'[^0-9].*$','',x) or 0) for x in v.split('.')]
    while len(p)<3: p.append(0)
    return p[0]*1_000_000+p[1]*1_000+p[2]
def evalc(cond, mc, obf):
    cur=verkey(mc)
    def r(m):
        op,v=m.group(1),m.group(2); k=verkey(v)
        return str({'>=':cur>=k,'<=':cur<=k,'>':cur>k,'<':cur<k,'==':cur==k,'!=':cur!=k}[op]).lower()
    e=re.sub(r'MC\s*(>=|<=|==|!=|>|<)\s*([0-9][0-9.]*)', r, cond)
    e=re.sub(r'\bOBF\b', str(obf).lower(), e)
    e=e.replace('&&',' and ').replace('||',' or ').replace('!',' not ').replace('true','True').replace('false','False')
    return bool(eval(e))
def swappat(f):
    pre='(?<![\\w$])' if re.match(r'[\w$]',f[0]) else ''
    post='(?![\\w$])' if re.match(r'[\w$]',f[-1]) else ''
    return re.compile(pre+re.escape(f)+post)
def run(root, mc, obf, out):
    swaps=[]
    for l in open(os.path.join(root,'src/swaps.txt'),encoding='utf8'):
        l=l.strip()
        if not l or l.startswith('#'): continue
        cond,rule=re.split(r'\s*\|\s*',l,1); fr,to=re.split(r'\s*=>\s*',rule,1)
        if evalc(cond,mc,obf): swaps.append((swappat(fr),to))
    src=pathlib.Path(root,'src/main/java'); skipped=[]
    for f in src.rglob('*.java'):
        rel=f.relative_to(src).as_posix()
        lines=f.read_text(encoding='utf8').split('\n')
        only=next((x for x in lines if x.strip().startswith('//#only ')),None)
        if only and not evalc(only.strip()[8:],mc,obf): skipped.append(rel); continue
        o=[]; st=[]
        act=lambda: not st or st[-1][2]
        for line in lines:
            t=line.strip()
            if t.startswith('//#if '):
                pa=act(); c=pa and evalc(t[6:],mc,obf); st.append([pa,c,c]); o.append('')
            elif t.startswith('//#elif '):
                fr=st[-1]; c=fr[0] and not fr[1] and evalc(t[8:],mc,obf); fr[2]=c
                if c: fr[1]=True
                o.append('')
            elif t=='//#else':
                fr=st[-1]; fr[2]=fr[0] and not fr[1]; fr[1]=True; o.append('')
            elif t=='//#endif': st.pop(); o.append('')
            elif t.startswith('//#only ') or t.startswith('//#swap '): o.append('')
            else: o.append(line if act() else '')
        assert not st, rel
        text='\n'.join(o)
        local=[]
        for l in lines:
            t=l.strip()
            if not t.startswith('//#swap '): continue
            cond,rule=re.split(r'\s*\|\s*',t[8:],1); fr,to=re.split(r'\s*=>\s*',rule,1)
            if evalc(cond,mc,obf): local.append((swappat(fr),to))
        for p,to in local+swaps: text=p.sub(lambda m,to=to: to, text)
        d=pathlib.Path(out,rel); d.parent.mkdir(parents=True,exist_ok=True); d.write_text(text,encoding='utf8')
    return skipped
if __name__=='__main__':
    root,mc,obf,out=sys.argv[1],sys.argv[2],sys.argv[3]=='true',sys.argv[4]
    print('skipped',run(root,mc,obf,out))
