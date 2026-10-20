"""ISS close eight-role one-shot acceptance; no prior suites or product execution."""
import copy
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
from elements import import_roles
from generate import ROOT, digest


def main():
    out = Path(os.environ['TEAM_OUT_DIR'])
    baseline = json.loads((out/'baseline-catalog.json').read_text())
    assert json.loads((ROOT/'ui-catalog/catalog.json').read_text()) == baseline
    table = json.loads((ROOT/'ui-catalog/inputs/iss-confirmed.json').read_text())
    expected = ['I-01.'+x for x in ('open-row-done','open-row-rejected','open-bulk-done','open-bulk-rejected','confirm-close','cancel-close','change-close-reason','change-completion-step')]
    table = dict(table, roles=table['roles'][2:])
    assert [r['rawRole'] for r in table['roles']] == expected
    for r in table['roles']:
        assert r['evidence']['contentSha256'] == 'cae2ae1749d4c421d34490acbc1a567ac74d7086849e48a79de1bdfcf3a9f4ce'
        assert r['screen'] == 'BO-ISS-010' and r['sourceScreen'] == 'I-01'
        assert r['behavior']['productRevision'] == '418a27c385f8a8a50818880da7c3ee40b68f82e6'
    assert baseline['catalogVersion'] == '1.0.0-bo.7' and len(baseline['elements']) == 330
    old_iss = [r for r in json.loads((ROOT/'ui-catalog/inputs/iss-confirmed.json').read_text())['roles'][:2]]
    assert old_iss == [{k:v for k,v in e.items() if k not in ('code','introducedVersion')} for e in baseline['elements'] if e['screen']=='BO-ISS-010']
    version = '1.0.0-bo.8'
    after = import_roles(baseline, table, version)
    assert len(after['elements']) == 338 and after['elements'][:330] == baseline['elements']
    for key in baseline:
        if key not in ('catalogVersion', 'aliases', 'elements'):
            assert after[key] == baseline[key], key
    assert after['aliases'] == [dict(a, version=version) for a in baseline['aliases']]
    print('PASS 1: 원문 hash·ISS8역할 귀속·기존330행/화면/기타 계약 불변')
    duplicate = copy.deepcopy(table)
    duplicate['roles'].append(copy.deepcopy(duplicate['roles'][0]))
    wrong = copy.deepcopy(table)
    wrong['roles'][0]['screen'] = 'BO-ISS-020'
    conflict = copy.deepcopy(table)
    conflict['roles'][0]['locator'] += '; conflict'
    for label, catalog, bad in [('중복', baseline, duplicate), ('화면귀속', baseline, wrong), ('기존역할충돌', after, conflict)]:
        try:
            import_roles(catalog, bad, '1.0.0-bo.8')
        except ValueError:
            print('PASS reject: '+label)
        else:
            raise AssertionError(label)
    (ROOT/'ui-catalog/catalog.json').write_text(json.dumps(after, ensure_ascii=False, indent=2)+'\n')
    run = subprocess.run([sys.executable, str(ROOT/'tools/ui-catalog/generate.py')], capture_output=True, text=True)
    (out/'generation.log').write_text(run.stdout+run.stderr)
    assert run.returncode == 0, 'generator failed'
    generated = ROOT/'generated/ui-catalog'
    runtime = ROOT/'lib/src/main/java/ai/mutuus/common/uicatalog/UiCodeAliases.java'
    runtime.write_bytes((generated/'UiCodeAliases.java').read_bytes())
    assert runtime.read_bytes() == (generated/'UiCodeAliases.java').read_bytes()
    dg = digest(after)
    for path in [generated/'bo-catalog.ts', generated/'UiCodeAliases.java', generated/'catalog.sql', ROOT/'docs/ui-catalog/review.md']:
        content = path.read_text()
        assert version in content and dg in content
    ts = (generated/'bo-catalog.ts').read_text()
    def const(name):
        return json.loads(ts.split('export const '+name+' = ', 1)[1].split(' as const;', 1)[0])
    new = after['elements'][330:]
    keys = [e['code'] for e in new]
    assert keys == ['BO-ISS-010-'+r.split('.',1)[1].upper() for r in expected]
    assert const('ELEMENTS') == sorted(after['elements'], key=lambda e:e['code'])
    assert const('ELEMENT_CODES') == {e['code']:e['code'] for e in after['elements']}
    assert const('SCREEN_ELEMENTS')['BO-ISS-010'] == sorted(keys+['BO-ISS-010-SELECT-STAGE','BO-ISS-010-SELECT-SYSTEM'])
    diag = json.loads((generated/'diagnostics.json').read_text())
    assert not diag['errors'] and diag['catalogVersion'] == version and diag['catalogDigest'] == dg
    hashes = {str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in [ROOT/'ui-catalog/inputs/iss-confirmed.json', ROOT/'ui-catalog/catalog.json', generated/'bo-catalog.ts', generated/'UiCodeAliases.java', generated/'catalog.sql', generated/'diagnostics.json', runtime]}
    result = dict(catalogVersion=version, catalogDigest=dg, elementCodes=keys, previousElements=330, cumulativeElements=338, hashes=hashes, acceptanceCases=5, generatorExecutions=1, previousSuites=0)
    (out/'result.json').write_text(json.dumps(result, ensure_ascii=False, indent=2)+'\n')
    print('PASS 5: version/digest·TS 전체매핑·Java 바이트동일·diagnostics')
    print(json.dumps(result, ensure_ascii=False))


if __name__ == '__main__':
    main()
