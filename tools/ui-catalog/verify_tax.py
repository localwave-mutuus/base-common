"""One-shot TAX acceptance: no legacy suite, generation exactly once."""
import copy
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
from elements import import_roles
from generate import ROOT, digest

out=Path(os.environ['TEAM_OUT_DIR'])
before=json.loads((ROOT/'ui-catalog/catalog.json').read_text())
table=json.loads((ROOT/'ui-catalog/inputs/tax-confirmed.json').read_text())
version='1.0.0-bo.3'
after=import_roles(before,table,version)
assert len(after['elements']) == 28
assert {s:sum(e['screen']==s for e in after['elements']) for s in ['BO-TAX-010','BO-TAX-020','BO-TAX-030','BO-TAX-040','BO-TAX-050']} == dict(zip(['BO-TAX-010','BO-TAX-020','BO-TAX-030','BO-TAX-040','BO-TAX-050'],[4,5,4,9,6]))
for key in before:
    if key not in ('elements','catalogVersion','aliases'): assert before[key] == after[key],key
assert [dict(a,version=version) for a in before['aliases']] == after['aliases']
assert all(e['role']==e['actionToken'].upper() and e['rawRole']=='<'+e['sourceScreen']+'.'+e['actionToken']+'>' and e['code']==e['screen']+'-'+e['role'] for e in after['elements'])
print('PASS actual TAX 28 rows / ownership 4,5,4,9,6 / raw mapping / existing identities')
for label,mutate in [
    ('suffix mismatch',lambda t:t['roles'][0].update(role='DIFFERENT')),
    ('raw mismatch',lambda t:t['roles'][0].update(rawRole='<X-01.other>')),
    ('raw/code conflict',lambda t:t['roles'].append(copy.deepcopy(t['roles'][0]))),
    ('conflicting locator',lambda t:t['roles'].append(dict(t['roles'][0],locator='conflicting-locator'))),
    ('screen prefix mismatch',lambda t:t['roles'][0].update(screen='BO-TAX-020')),
]:
    bad=copy.deepcopy(table); mutate(bad)
    try: import_roles(before,bad,version)
    except ValueError as exc: print('PASS reject '+label+': '+str(exc))
    else: raise AssertionError(label)
(ROOT/'ui-catalog/catalog.json').write_text(json.dumps(after,ensure_ascii=False,indent=2)+'\n')
run=subprocess.run([sys.executable,str(ROOT/'tools/ui-catalog/generate.py')],capture_output=True,text=True)
(out/'generation.log').write_text(run.stdout+run.stderr)
assert run.returncode==0
expected=digest(after)
for path in ['generated/ui-catalog/bo-catalog.ts','generated/ui-catalog/UiCodeAliases.java','generated/ui-catalog/catalog.sql','docs/ui-catalog/review.md']:
    content=(ROOT/path).read_text(); assert expected in content and version in content,path
text=(ROOT/'generated/ui-catalog/bo-catalog.ts').read_text()
elements=json.loads(text.split('export const ELEMENTS = ',1)[1].split(' as const;',1)[0])
assert elements==sorted(after['elements'],key=lambda e:e['code'])
owned=json.loads(text.split('export const SCREEN_ELEMENTS = ',1)[1].split(' as const;',1)[0])
assert owned=={s['code']:sorted(e['code'] for e in elements if e['screen']==s['code']) for s in after['screens']}
diag=json.loads((ROOT/'generated/ui-catalog/diagnostics.json').read_text())
assert not diag['errors'] and diag['catalogDigest']==expected and diag['catalogVersion']==version
print('PASS generated ELEMENTS / SCREEN_ELEMENTS / version / digest TS Java SQL diagnostics')
print('catalogVersion='+version+' catalogDigest='+expected)
print('TS sha256='+hashlib.sha256((ROOT/'generated/ui-catalog/bo-catalog.ts').read_bytes()).hexdigest())
print('Acceptance groups: 7; generator executions: 1; legacy suites: 0')
