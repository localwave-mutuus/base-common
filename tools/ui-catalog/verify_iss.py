"""ISS two-role one-shot acceptance; no prior suites or product execution."""
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
    expected = ['I-01.select-stage', 'I-01.select-system']
    assert [r['rawRole'] for r in table['roles']] == expected
    source = (out/'issue-home-t2-input.md').read_bytes()
    for r in table['roles']:
        assert r['evidence']['contentSha256'] == hashlib.sha256(source).hexdigest()
        assert r['rawRole'] in source.decode()
        assert r['screen'] == 'BO-ISS-010' and r['sourceScreen'] == 'I-01'
        assert r['behavior']['productRevision'] == '7db9239893fc3c005792566cbe4762d2a632f322'
    assert baseline['catalogVersion'] == '1.0.0-bo.6' and len(baseline['elements']) == 328
    version = '1.0.0-bo.7'
    after = import_roles(baseline, table, version)
    assert len(after['elements']) == 330 and after['elements'][:328] == baseline['elements']
    for key in baseline:
        if key not in ('catalogVersion', 'aliases', 'elements'):
            assert after[key] == baseline[key], key
    assert after['aliases'] == [dict(a, version=version) for a in baseline['aliases']]
    print('PASS 1: 원문 hash·ISS2역할 귀속·기존328행/화면/기타 계약 불변')
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
    new = after['elements'][328:]
    keys = [e['code'] for e in new]
    assert keys == ['BO-ISS-010-SELECT-STAGE', 'BO-ISS-010-SELECT-SYSTEM']
    assert const('ELEMENTS') == sorted(after['elements'], key=lambda e:e['code'])
    assert const('ELEMENT_CODES') == {e['code']:e['code'] for e in after['elements']}
    assert const('SCREEN_ELEMENTS')['BO-ISS-010'] == keys
    diag = json.loads((generated/'diagnostics.json').read_text())
    assert not diag['errors'] and diag['catalogVersion'] == version and diag['catalogDigest'] == dg
    hashes = {str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in [generated/'bo-catalog.ts', generated/'UiCodeAliases.java', runtime]}
    result = dict(catalogVersion=version, catalogDigest=dg, elementCodes=keys, previousElements=328, cumulativeElements=330, hashes=hashes, acceptanceCases=5, generatorExecutions=1, previousSuites=0)
    (out/'result.json').write_text(json.dumps(result, ensure_ascii=False, indent=2)+'\n')
    print('PASS 5: version/digest·TS 전체매핑·Java 바이트동일·diagnostics')
    print(json.dumps(result, ensure_ascii=False))


if __name__ == '__main__':
    main()
