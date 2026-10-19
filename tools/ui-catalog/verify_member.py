"""First acceptance of confirmed member input over bo.5; never run old suites."""
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
    assert before == json.loads((out/'baseline-catalog.json').read_text())
    assert before['catalogVersion'] == '1.0.0-bo.5' and len(before['elements']) == 176
    table = json.loads((ROOT/'ui-catalog/inputs/member-confirmed.json').read_text())
    coverage = json.loads((ROOT/'ui-catalog/inputs/member-coverage.json').read_text())
    source = (out/'member-source.md').read_bytes()
    rows = [[v.strip() for v in line.strip('|').split('|')] for line in source.decode().splitlines()
            if re.match(r'^\| M-\d+ \|', line)]
    assert coverage['source']['revision'] == 'bab24fe0f7270738e7871c2f94d7e2dded31b630'
    assert coverage['source']['contentSha256'] == hashlib.sha256(source).hexdigest()
    assert coverage['productRevision'] == 'aa0d4f9c65fb4306d09f58ad08e159683d4b40ca'
    assert len(rows) == len(coverage['roles']) == 292
    registered = []
    for cols, audit in zip(rows, coverage['roles']):
        assert cols == audit['sourceColumns']
        assert [audit[k] for k in ('sourceScreen', 'rawRole', 'locatorEvidence', 'label', 'kind', 'sharedScope', 'elementSupply')] == [cols[i] for i in (0, 2, 3, 4, 5, 6, 7)]
        assert audit['screen'] == re.search(r'BO-MBR-\d{3}', cols[1]).group()
        if audit['registered']:
            assert ' / ' in audit['locatorEvidence'] and not audit['locatorEvidence'].startswith('components/')
            registered.append(audit)
        else:
            assert audit['code'] is None and audit['status'].startswith('미등록:')
    assert len(registered) == len(table['roles']) == 152
    for audit, row in zip(registered, table['roles']):
        assert all(audit[k] == row[k] for k in ('rawRole', 'screen', 'sourceScreen', 'actionToken', 'role'))
        assert row['locator'] == audit['locatorEvidence'] and row['evidence'] == coverage['source']
        assert row['behavior']['label'] == row['behavior']['trigger'] == audit['label']
        assert row['behavior']['kind'] == audit['kind']
        assert row['behavior']['gate'] == coverage['gatePolicy']
        assert row['behavior']['notes'] == audit['sharedScope']+'; '+coverage['repeatPolicy']
        assert row['behavior']['productRevision'] == coverage['productRevision']
    expected = {'M-01': (37,14), 'M-02': (34,15), 'M-10': (30,16), 'M-11': (16,10),
                'M-20': (29,16), 'M-30': (48,15), 'M-40': (52,33), 'M-50': (46,33)}
    for screen, (total, count) in expected.items():
        assert sum(r['sourceScreen'] == screen for r in coverage['roles']) == total
        assert sum(r['sourceScreen'] == screen for r in registered) == count
    print('PASS 1: 회원 원문292 1:1·등록152/제외140·화면별 분모·근거 revision/hash·단계/정책')
    version = '1.0.0-bo.6'
    after = import_roles(before, table, version)
    new = after['elements'][176:]
    assert len(after['elements']) == 328 and after['elements'][:176] == before['elements']
    assert [e['code'] for e in new] == [r['code'] for r in registered]
    assert {e['rawRole'] for e in new}.isdisjoint({r['rawRole'] for r in coverage['roles'] if not r['registered']})
    for key in before:
        if key not in ('elements', 'catalogVersion', 'aliases'): assert before[key] == after[key], key
    assert [dict(a, version=version) for a in before['aliases']] == after['aliases']
    print('PASS 2: SYS130/TAX28/DSH18 기존176행 바이트 의미·코드·화면 owner·기타 계약 불변')
    dup = copy.deepcopy(table); dup['roles'].append(copy.deepcopy(dup['roles'][0]))
    conflict = copy.deepcopy(table); other = copy.deepcopy(conflict['roles'][0]); other['behavior']['label'] = 'different-action'; conflict['roles'].append(other)
    wrong = copy.deepcopy(table); wrong['roles'][0]['screen'] = 'BO-MBR-080'
    cases = [('같은 행동 중복', before, dup), ('같은 역할 다른 동작 충돌', before, conflict),
             ('화면 귀속 충돌', before, wrong), ('기존 회원 역할 재등록', after, table)]
    for label, catalog, bad in cases:
        try: import_roles(catalog, bad, '1.0.0-bo.7')
        except ValueError: print('PASS reject: '+label)
        else: raise AssertionError('accepted: '+label)
    pair = {e['rawRole']:e for e in new if e['rawRole'] in ('M-50.open-place', 'M-50.confirm-place')}
    a, b = pair['M-50.open-place'], pair['M-50.confirm-place']
    assert a['locator'].split(' / ')[1] == b['locator'].split(' / ')[1] == 'm50-confirm-{bn}'
    assert a['code'] != b['code'] and a['behavior']['label'] != b['behavior']['label']
    assert all('{' not in e['code'] and '}' not in e['code'] for e in new)
    print('PASS 7: 같은 testId의 상세 열기/확인 실행 분리·대상ID 코드 혼입 없음')
    (ROOT/'ui-catalog/catalog.json').write_text(json.dumps(after, ensure_ascii=False, indent=2)+'\n')
    run = subprocess.run([sys.executable, str(ROOT/'tools/ui-catalog/generate.py')], capture_output=True, text=True)
    (out/'generation.log').write_text(run.stdout+run.stderr)
    assert run.returncode == 0, 'generator failed; see generation.log'
    dg = digest(after)
    for path in ['generated/ui-catalog/bo-catalog.ts', 'generated/ui-catalog/UiCodeAliases.java',
                 'generated/ui-catalog/catalog.sql', 'docs/ui-catalog/review.md']:
        content = (ROOT/path).read_text(); assert dg in content and version in content, path
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
    result = dict(catalogVersion=version, catalogDigest=dg, hashes=hashes, acceptanceCases=8,
                  registered=152, excluded=140, generatorExecutions=1, previousSuites=0)
    (out/'result.json').write_text(json.dumps(result, indent=2)+'\n')
    print('PASS 8: 신규 version/digest·TS 매핑·SQL 근거·diagnostics 정합; 생성1회')
    print(json.dumps(result))

if __name__ == '__main__': main()
