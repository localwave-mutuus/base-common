"""One-shot SYS real-input acceptance; execute only from the TAX bo.3 catalog."""
import copy
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
from elements import import_roles
from generate import ROOT, digest

out=Path(os.environ['TEAM_OUT_DIR'])
before=json.loads((ROOT/'ui-catalog/catalog.json').read_text())
original=json.loads((out/'catalog.before.json').read_text())
table=json.loads((ROOT/'ui-catalog/inputs/sys-confirmed.json').read_text())
source=(out/'sys-source.md').read_bytes()
assert before['catalogVersion']=='1.0.0-bo.3'
source_rows=[[v.strip() for v in line.strip('|').split('|')] for line in source.decode().splitlines() if re.match(r'^\| Y-\d\d\.',line)]
assert len(source_rows)==len(table['roles'])==130
for cols,row in zip(source_rows,table['roles']):
    assert [row['rawRole'],row['screen'],row['locator'],*[row['behavior'][k] for k in ('label','trigger','kind','gate')],'없음',row['behavior']['notes']]==cols
    assert row['evidence']['contentSha256']==hashlib.sha256(source).hexdigest()
    assert row['evidence']['revision']=='d9051c7219816cc3065376b1b3366b7827656757'
    assert row['evidence']['revisionKind']=='GIT_COMMIT'
    assert row['behavior']['productRevision']=='ef89e424231bc36ce58fda10825573ed1d177ed3'
    assert row['rawRole']==row['sourceScreen']+'.'+row['actionToken']
    assert row['role']==row['actionToken'].upper()
print('PASS SYS source 130/130 exact row correspondence, provenance hash, product revision, gates')
version='1.0.0-bo.4'
after=import_roles(before,table,version)
sys_rows=[e for e in after['elements'] if e['screen'].startswith('BO-SYS-')]
counts={s:sum(e['screen']==s for e in sys_rows) for s in sorted({e['screen'] for e in sys_rows})}
assert list(counts.values())==[1,15,4,10,27,30,22,21], counts
assert len(after['elements'])==158 and not any(e['screen']=='BO-SYS-040' for e in sys_rows)
assert after['elements'][:28]==before['elements']
for old,new in zip(original['elements'],after['elements'][:28]):
    assert {k:v for k,v in old.items() if k!='evidence'}=={k:v for k,v in new.items() if k!='evidence'}
assert len(original['elements'])==28
for key in before:
    if key not in ('elements','catalogVersion','aliases'): assert before[key]==after[key],key
assert [dict(a,version=version) for a in before['aliases']]==after['aliases']
print('PASS registration SYS 130/130, excluded in denominator 0, 8-screen ownership '+str(counts))
print('PASS TAX 28 codes/locators/introduction unchanged; screens/menus/ambiguous aliases preserved')
bad=copy.deepcopy(table); bad['roles'].append(copy.deepcopy(bad['roles'][0]))
try: import_roles(before,bad,version)
except ValueError as exc: print('PASS reject duplicate SYS row: '+str(exc))
else: raise AssertionError('duplicate accepted')
(ROOT/'ui-catalog/catalog.json').write_text(json.dumps(after,ensure_ascii=False,indent=2)+'\n')
run=subprocess.run([sys.executable,str(ROOT/'tools/ui-catalog/generate.py')],capture_output=True,text=True)
(out/'generation.log').write_text(run.stdout+run.stderr)
assert run.returncode==0,run.stdout+run.stderr
expected=digest(after)
for path in ['generated/ui-catalog/bo-catalog.ts','generated/ui-catalog/UiCodeAliases.java','generated/ui-catalog/catalog.sql','docs/ui-catalog/review.md']:
    content=(ROOT/path).read_text(); assert expected in content and version in content,path
text=(ROOT/'generated/ui-catalog/bo-catalog.ts').read_text()
def ts(name): return json.loads(text.split('export const '+name+' = ',1)[1].split(' as const;',1)[0])
assert ts('ELEMENTS')==sorted(after['elements'],key=lambda e:e['code'])
assert ts('ELEMENT_CODES')=={e['code']:e['code'] for e in after['elements']}
assert ts('SCREEN_ELEMENTS')=={s['code']:sorted(e['code'] for e in after['elements'] if e['screen']==s['code']) for s in after['screens']}
diag=json.loads((ROOT/'generated/ui-catalog/diagnostics.json').read_text())
assert not diag['errors'] and diag['catalogDigest']==expected and diag['catalogVersion']==version
# SQL must retain behavioral evidence too; no DB is touched.
from generate import sql
sql_text=(ROOT/'generated/ui-catalog/catalog.sql').read_text()
for e in sys_rows:
    payload=dict(e['evidence'],**{k:e[k] for k in ('rawRole','sourceScreen','actionToken','behavior')})
    assert sql(json.dumps(payload,ensure_ascii=False,sort_keys=True)) in sql_text
print('PASS TS ELEMENTS/ELEMENT_CODES/SCREEN_ELEMENTS, SQL behavioral evidence, TS/Java/SQL/diagnostics version/digest')
print('catalogVersion='+version+' catalogDigest='+expected)
for name in ['bo-catalog.ts','UiCodeAliases.java','catalog.sql']:
    print(name+' sha256='+hashlib.sha256((ROOT/'generated/ui-catalog'/name).read_bytes()).hexdigest())
print('actual acceptance cases: 5; generator executions: 1; previous TAX/synthetic/common/web/SQL suites: 0')
