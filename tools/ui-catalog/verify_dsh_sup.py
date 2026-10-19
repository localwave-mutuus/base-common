"""One-shot DSH/SUP acceptance from cumulative bo.4; no prior suites."""
import copy
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
from elements import import_roles
from generate import ROOT, digest, sql


def main():
    out = Path(os.environ['TEAM_OUT_DIR'])
    before = json.loads((ROOT/'ui-catalog/catalog.json').read_text())
    baseline = json.loads((out/'baseline-catalog.json').read_text())
    table = json.loads((ROOT/'ui-catalog/inputs/dsh-sup-confirmed.json').read_text())
    coverage = json.loads((ROOT/'ui-catalog/inputs/dsh-sup-coverage.json').read_text())
    source = (out/'dsh-sup-source.md').read_bytes()
    assert before == baseline and before['catalogVersion'] == '1.0.0-bo.4'
    source_rows = [[v.strip() for v in line.strip('|').split('|')]
                   for line in source.decode().splitlines()
                   if re.match(r'^\| (?:D-01|S-01|S-10) \| `', line)]
    assert len(source_rows) == len(coverage['roles']) == 36
    assert coverage['source']['revision'] == 'f7c3c019f10ea2de27b7ce1138b267fe8f1b8eb7'
    assert coverage['source']['contentSha256'] == hashlib.sha256(source).hexdigest()
    registered = []
    for cols, row in zip(source_rows, coverage['roles']):
        assert [row['sourceScreen'], '`'+row['rawRole']+'`', row['label'], row['locatorEvidence'], row['status']] == cols
        assert row['registered'] == ('미확정' not in cols[4])
        if row['registered']: registered.append(row)
        else: assert row['code'] is None and '미확정' in row['status']
    assert len(registered) == len(table['roles']) == 18
    for audit, row in zip(registered, table['roles']):
        for key in ('rawRole', 'screen', 'sourceScreen', 'actionToken', 'role'):
            assert row[key] == audit[key]
        assert row['locator'] == audit['locatorEvidence']
        assert row['evidence'] == coverage['source']
        assert row['behavior']['label'] == row['behavior']['trigger'] == audit['label']
        assert row['behavior']['productRevision'] == coverage['productRevision']
        assert coverage['repeatPolicy'] in row['behavior']['notes']
        if row['actionToken'] in ('save-draft', 'publish-content', 'archive-content'):
            assert row['behavior']['gate'] in coverage['statePolicy']
    print('PASS 1: 원문36 1:1·revision/hash·등록18/미등록18 사유·gate/반복 정책')
    version = '1.0.0-bo.5'
    after = import_roles(before, table, version)
    new = after['elements'][len(before['elements']):]
    assert len(before['elements']) == 158 and len(after['elements']) == 176
    assert after['elements'][:158] == before['elements']
    assert {e['rawRole'] for e in new}.isdisjoint({r['rawRole'] for r in coverage['roles'] if not r['registered']})
    expected = {'D-01': ('BO-DSH-010', 3, 1), 'S-01': ('BO-CNT-010', 18, 10), 'S-10': ('BO-CNT-020', 15, 7)}
    for source_screen, (screen, total, count) in expected.items():
        assert sum(r['sourceScreen'] == source_screen for r in coverage['roles']) == total
        assert sum(e['screen'] == screen for e in new) == count
        assert all(e['screen'] == screen for e in new if e['sourceScreen'] == source_screen)
    for key in before:
        if key not in ('elements', 'catalogVersion', 'aliases'): assert before[key] == after[key], key
    assert [dict(a, version=version) for a in before['aliases']] == after['aliases']
    print('PASS 2: 등록·제외 분모·기존 화면 귀속·기존158행 및 나머지 계약 불변')
    duplicate = copy.deepcopy(table)
    duplicate['roles'].append(copy.deepcopy(duplicate['roles'][0]))
    wrong_screen = copy.deepcopy(table)
    wrong_screen['roles'][0]['screen'] = 'BO-CNT-010'
    target_id = copy.deepcopy(table)
    target_id['roles'][0]['targetId'] = 'not-a-role'
    cases = [('중복 역할', before, duplicate), ('화면 귀속 충돌', before, wrong_screen),
             ('대상ID 필드 혼입', before, target_id), ('기존 역할 재등록', after, table)]
    for label, catalog, bad in cases:
        try: import_roles(catalog, bad, '1.0.0-bo.6')
        except ValueError: print('PASS reject: '+label)
        else: raise AssertionError('accepted: '+label)
    (ROOT/'ui-catalog/catalog.json').write_text(json.dumps(after, ensure_ascii=False, indent=2)+'\n')
    run = subprocess.run([sys.executable, str(ROOT/'tools/ui-catalog/generate.py')], capture_output=True, text=True)
    (out/'generation.log').write_text(run.stdout+run.stderr)
    assert run.returncode == 0, 'generator failed; see generation.log'
    dg = digest(after)
    for path in ['generated/ui-catalog/bo-catalog.ts', 'generated/ui-catalog/UiCodeAliases.java',
                 'generated/ui-catalog/catalog.sql', 'docs/ui-catalog/review.md']:
        content = (ROOT/path).read_text()
        assert dg in content and version in content, path
    ts_text = (ROOT/'generated/ui-catalog/bo-catalog.ts').read_text()
    def ts(name): return json.loads(ts_text.split('export const '+name+' = ', 1)[1].split(' as const;', 1)[0])
    assert ts('ELEMENTS') == sorted(after['elements'], key=lambda e:e['code'])
    assert ts('ELEMENT_CODES') == {e['code']:e['code'] for e in after['elements']}
    assert ts('SCREEN_ELEMENTS') == {s['code']:sorted(e['code'] for e in after['elements'] if e['screen'] == s['code']) for s in after['screens']}
    sql_text = (ROOT/'generated/ui-catalog/catalog.sql').read_text()
    for e in new:
        payload = dict(e['evidence'], **{k:e[k] for k in ('rawRole', 'sourceScreen', 'actionToken', 'behavior')})
        assert sql(json.dumps(payload, ensure_ascii=False, sort_keys=True)) in sql_text
    diag = json.loads((ROOT/'generated/ui-catalog/diagnostics.json').read_text())
    assert not diag['errors'] and diag['catalogDigest'] == dg and diag['catalogVersion'] == version
    hashes = {name:hashlib.sha256((ROOT/'generated/ui-catalog'/name).read_bytes()).hexdigest()
              for name in ['bo-catalog.ts', 'UiCodeAliases.java', 'catalog.sql']}
    result = dict(catalogVersion=version, catalogDigest=dg, hashes=hashes, acceptanceCases=7,
                  generatorExecutions=1, previousSuites=0)
    (out/'result.json').write_text(json.dumps(result, indent=2)+'\n')
    print('PASS 7: TS/Java/SQL/diagnostics version/digest 및 TS 매핑·SQL 행동 근거')
    print(json.dumps(result))

if __name__ == '__main__': main()
