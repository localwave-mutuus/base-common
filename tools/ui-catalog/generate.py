#!/usr/bin/env python3
"""Dependency-free, offline catalog compiler. No product or database access."""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import re
from elements import validate as validate_elements

ROOT = Path(__file__).resolve().parents[2]

def canonical(c):
    return json.dumps(c, ensure_ascii=False, sort_keys=True, separators=(',', ':')).encode('utf-8')

def digest(c):
    return hashlib.sha256(canonical(ordered(c))).hexdigest()

def alias_key(a):
    return (a['system'], a['kind'], a['value'], a['version'])

def resolve(c, system, kind, value, version):
    matches = [a for a in c['aliases'] if alias_key(a) == (system, kind, value, version)]
    if len(matches) != 1 or matches[0]['status'] != 'RESOLVED':
        return None
    return matches[0]['target']

def diagnose(c):
    errors = []
    try:
        validate_elements(c.get("elements", []), {s["code"] for s in c["screens"]})
    except ValueError as exc:
        errors.append(str(exc))
    def unique(rows, key, label):
        for value, count in sorted(Counter(key(r) for r in rows).items()):
            if count > 1:
                errors.append(f'duplicate {label}: {value}')
    for table in ('systems', 'screens', 'menus'):
        unique(c[table], lambda r:r['code'], table)
    unique(c['domains'], lambda r:(r['system'],r['code']), 'domains')
    unique(c['aliases'], alias_key, 'aliases')
    systems={s['code'] for s in c['systems']}
    domains={(d['system'],d['code']) for d in c['domains']}
    screens={s['code']:s for s in c['screens']}
    menus={m['code']:m for m in c['menus']}
    for d in c['domains']:
        if d['system'] not in systems: errors.append('missing domain system: '+d['code'])
    for s in c['screens']:
        if not re.fullmatch(r'[A-Z]+-[A-Z]{3}-[0-9]{3}', s['code']): errors.append('invalid screen code: '+s['code'])
        if (s['system'],s['domain']) not in domains: errors.append('missing screen domain: '+s['code'])
        if s['replacement'] and s['replacement'] not in screens: errors.append('missing replacement: '+s['code'])
        if s['status'] not in ('IMPLEMENTED','PLANNED','RETIRED'): errors.append('invalid screen status: '+s['code'])
        if s['system'] not in ('BO', 'APP'): errors.append('reserved system has active screen: '+s['code'])
    for m in c['menus']:
        if m['target'] not in screens: errors.append('missing menu target: '+m['code'])
        elif screens[m['target']]['system'] != m['system']: errors.append('cross-system menu: '+m['code'])
        if (m['system'],m['domain']) not in domains: errors.append('missing menu domain: '+m['code'])
        if m['parent'] and m['parent'] not in menus: errors.append('missing menu parent: '+m['code'])
    ambiguous=[]
    for a in c['aliases']:
        if a['version'] != c['catalogVersion']: errors.append('alias version mismatch: '+a['value'])
        if a['system'] not in systems: errors.append('missing alias system: '+a['value'])
        if a['kind'] not in ('BO_ID','ROUTE','LEGACY','INTERNAL_ID'): errors.append('invalid alias kind: '+a['value'])
        if a['status']=='AMBIGUOUS':
            ambiguous.append(a)
            if a['target'] is not None or not a.get('reason'): errors.append('ambiguous alias must have null target and reason: '+a['value'])
        elif a['status'] != 'RESOLVED' or a['target'] not in screens:
            errors.append('missing alias target or invalid status: '+a['value'])
        elif screens[a['target']]['system'] != a['system']: errors.append('cross-system alias: '+a['value'])
        for target in a['candidates']:
            if target not in screens: errors.append('missing candidate: '+target)
    if '\x00' in canonical(c).decode('utf-8').replace('\\u0000','\x00'):
        errors.append('NUL is not supported by PostgreSQL text')
    inv=c['inventory']; version=c['catalogVersion']
    source_ids=set(inv['registryIds'])|set(inv['menuIds'])|set(inv['issueCatalogIds'])
    required={('BO','LEGACY' if i=='I-10' else 'BO_ID',i,version) for i in source_ids}
    required |= {('BO','ROUTE','/'+i.lower(),version) for i in inv['menuIds']}
    required |= {('BO','INTERNAL_ID',i,version) for i in inv['shellIds']}
    required |= {('BO','ROUTE',s['route'],version) for s in c['screens'] if s['kind']=='SHELL' and s['route']}
    app = inv.get('app', {})
    for mapping in app.get('screenMappings', []):
        if mapping['code'] not in screens:
            errors.append('missing APP inventory screen: '+mapping['code'])
        required.add(('APP', 'INTERNAL_ID', mapping['internal_id'], version))
    for binding in app.get('routeBindings', []):
        required.add(('APP', 'ROUTE', binding, version))
    available={alias_key(a) for a in c['aliases']}
    missing=sorted(required-available)
    errors += ['missing source alias: '+str(k) for k in missing]
    for i in inv['menuIds']:
        target=resolve(c,'BO','BO_ID',i,version)
        if target is None: errors.append('menu BO_ID not uniquely resolved: '+i)
        elif not any(m['target']==target for m in c['menus']): errors.append('source menu missing: '+i)
    covered=len(required & available)
    resolved=sum(resolve(c,*k) is not None for k in required)
    return dict(errors=sorted(errors),duplicates=sum(e.startswith('duplicate ') for e in errors),
        coverage=dict(denominator=len(required),preserved=covered,uniqueResolved=resolved,
                      preservedPercent=round(100*covered/len(required),4) if required else 100),
        planned=sum(s['status']=='PLANNED' for s in c['screens']),ambiguous=sorted(ambiguous,key=alias_key),
        measured=dict(registry=len(inv['registryIds']),menuAreas=len({d['legacyMenuKey'] for d in c['domains'] if d['legacyMenuKey']}),
                      menuItems=len(inv['menuIds']),menuWithGuide=len(c['menus']),sourceUnionWithShell=len(set(inv['registryIds'])|set(inv['menuIds']))+len(inv['shellIds']),standardScreens=len(c['screens']),appScreens=sum(s['system']=='APP' for s in c['screens']),appRoutes=len(app.get('routeBindings', [])),appMenus=sum(m['system']=='APP' for m in c['menus']),appRegisteredAliases=sum(a['system']=='APP' and a['status']=='RESOLVED' for a in c['aliases']),appExcludedAliases=sum(a['system']=='APP' and a['status']=='AMBIGUOUS' for a in c['aliases'])),
        expected=inv['expected'],unconfirmed=['I-10 unique target','menu semantic item identifiers require user review','environment active version and operational grants deferred to B3'])

def js(value):
    return json.dumps(value,ensure_ascii=False,sort_keys=True,indent=2).replace('\u2028','\\u2028').replace('\u2029','\\u2029')

def java(value):
    if value is None: return 'null'
    escapes={'"':'\\"','\\':'\\\\','\n':'\\n','\r':'\\r','\t':'\\t','\b':'\\b','\f':'\\f'}
    return '"'+''.join(escapes.get(x,('\\%03o'%ord(x) if ord(x)<32 else x)) for x in value)+'"'

def sql(value):
    if value is None: return 'NULL'
    if isinstance(value,int): return str(value)
    return "E'"+str(value).replace('\\','\\\\').replace("'","''").replace('\n','\\n').replace('\r','\\r').replace('\t','\\t')+"'"

def ordered(c):
    result=dict(c)
    for table in ('systems','domains','screens','menus','aliases'):
        key=alias_key if table=='aliases' else (lambda r:(r['system'],r['code'])) if table=='domains' else (lambda r:r['code'])
        result[table]=sorted(c[table],key=key)
    if 'elements' in c: result['elements']=sorted(c['elements'],key=lambda e:e['code'])
    return result

def render_sql(c):
    c=ordered(c); dg=digest(c); version=c['catalogVersion']
    header=f'GENERATED — DO NOT EDIT. catalogVersion={version}; catalogDigest=sha256:{dg}'
    q='-- '+header+'\n-- B3 review input only. No activation, GRANT, database execution or issue backfill.\n'
    q+=(ROOT/'tools/ui-catalog/schema.sql').read_text()+'\n'
    def insert(table,cols,rows):
        nonlocal q
        if rows: q+='INSERT INTO '+table+' ('+', '.join(cols)+') VALUES\n'+',\n'.join('('+', '.join(sql(v) for v in row)+')' for row in rows)+';\n'
    insert('ui_catalog_version',['catalog_version','source_revision','catalog_digest'],[(version,c['sourceRevision'],dg)])
    insert('ui_system',['system_code','name','repository','status'],[(s['code'],s['name'],s['repository'],s['status']) for s in c['systems']])
    insert('ui_domain',['system_code','domain_code','name'],[(d['system'],d['code'],d['name']) for d in c['domains']])
    insert('ui_screen',['screen_code','system_code','domain_code','name','kind','route_template','business_number','implementation','owner_team','status','replacement_code','introduced_version','retired_version'],[(s['code'],*[s[k] for k in ['system','domain','name','kind','route','businessNumber','implementation','owner','status','replacement','introducedVersion','retiredVersion']]) for s in c['screens']])
    insert('ui_menu',['menu_code','system_code','domain_code','parent_code','name','sort_order','target_screen_code','status','introduced_version','retired_version'],[(m['code'],*[m[k] for k in ['system','domain','parent','name','order','target','status']],version,None) for m in c['menus']])
    insert('ui_code_alias',['system_code','alias_kind','alias_value','catalog_version','screen_code','status','candidates_json','reason'],[(a['system'],a['kind'],a['value'],a['version'],a['target'],a['status'],json.dumps(sorted(a['candidates']),ensure_ascii=False),a.get('reason')) for a in c['aliases']])
    insert('ui_element',['element_code','screen_code','name','element_kind','status','introduced_version','locator','evidence_json'],[(e['code'],e['screen'],e['role'],'ACTION_ROLE','CONFIRMED',e['introducedVersion'],e['locator'],json.dumps(dict(e['evidence'], **{k:e[k] for k in ('rawRole','sourceScreen','actionToken','behavior') if k in e}),ensure_ascii=False,sort_keys=True)) for e in c.get('elements',[])])
    return q

def render(c):
    c=ordered(c); dg=digest(c)
    # Digest represents the canonical catalog, independent of collection ordering.
    version=c['catalogVersion']; header=f'GENERATED — DO NOT EDIT. catalogVersion={version}; catalogDigest=sha256:{dg}'
    ts='// '+header+'\n'
    ts+='export const CATALOG_VERSION = '+js(version)+' as const;\nexport const CATALOG_DIGEST = '+js(dg)+' as const;\n'
    for table,name in [('screens','SCREENS'),('menus','MENUS'),('aliases','ALIASES'),('elements','ELEMENTS')]:
        ts+='export const '+name+' = '+js(c.get(table,[]))+' as const;\n'
    ts+='export const SCREEN_CODES = '+js({s['code']:s['code'] for s in c['screens']})+' as const;\n'
    ts+='export const MENU_CODES = '+js({m['code']:m['code'] for m in c['menus']})+' as const;\n'
    ts+='export type ScreenCode = typeof SCREENS[number]["code"];\nexport type MenuCode = typeof MENUS[number]["code"];\nexport type Alias = typeof ALIASES[number];\n'
    ts+='export const ELEMENT_CODES = '+js({e['code']:e['code'] for e in c.get('elements',[])})+' as const;\n'
    ts+='export const SCREEN_ELEMENTS = '+js({s['code']:[e['code'] for e in c.get('elements',[]) if e['screen']==s['code']] for s in c['screens']})+' as const;\nexport type ElementCode = typeof ELEMENTS[number]["code"];\n' if c.get('elements') else 'export const SCREEN_ELEMENTS = '+js({s['code']:[] for s in c['screens']})+' as const;\nexport type ElementCode = never;\n'
    j='// '+header+'\npackage ai.mutuus.common.uicatalog;\n\nimport java.util.List;\nimport java.util.Map;\nimport java.util.Optional;\n\npublic final class UiCodeAliases {\n  private UiCodeAliases() {}\n'
    j+='  public static final String CATALOG_VERSION = '+java(version)+';\n  public static final String CATALOG_DIGEST = '+java(dg)+';\n'
    j+='  public static final Map<String,String> SYSTEM_MAPPING = Map.ofEntries('+', '.join('Map.entry('+java(k)+','+java(v)+')' for k,v in sorted(c['systemMapping'].items()))+');\n'
    j+='  public record Alias(String system, String aliasKind, String aliasValue, String version, String target, String status, List<String> candidates) {}\n  public static final List<Alias> ALIASES = List.of(\n'
    j+=',\n'.join('    new Alias('+', '.join(java(a[k]) for k in ['system','kind','value','version','target','status'])+', List.of('+', '.join(map(java,sorted(a['candidates'])))+'))' for a in c['aliases'] if a['system'] != 'APP' or a['status'] == 'RESOLVED')+');\n'
    j+='  public static Optional<String> resolve(String system, String kind, String value, String version) {\n    var matches = ALIASES.stream().filter(a -> a.system().equals(system) && a.aliasKind().equals(kind) && a.aliasValue().equals(value) && a.version().equals(version)).toList();\n    return matches.size() == 1 && matches.get(0).status().equals("RESOLVED") ? Optional.ofNullable(matches.get(0).target()) : Optional.empty();\n  }\n}\n'
    q=render_sql(c)
    diag=diagnose(c)
    md='# BO·APP 코드 검토 목록\n\n'+header+'\n\n사용자 승인 전 검토안. 메뉴 영문 항목은 설계의 STATUS 패턴을 따른 신규 식별자다.\n\n| 표준 코드 | 이름 | 기존 코드 / 주소 | 상태 | 소유 | 메뉴 |\n|---|---|---|---|---|---|\n'
    def cell(v): return str(v or '—').replace('|','\\|').replace('\n','<br>')
    for s in c['screens']:
        md+='| '+' | '.join(map(cell,[s['code'],s['name'],str(s['businessNumber'] or '—')+' / '+str(s['route'] or '주소 없음'),s['status'],s['owner'],', '.join(m['code'] for m in c['menus'] if m['target']==s['code'])]))+' |\n'
    md+='\n## 진단\n\n```json\n'+js(diag)+'\n```\n\n'+ '\n'.join('- '+note for note in c['inventory']['notes'])+'\n'
    candidates = '# APP 모호 별칭 후보 (런타임 미등록)\n\n```json\n'+js([a for a in c['aliases'] if a['system']=='APP' and a['status']=='AMBIGUOUS'])+'\n```\n'
    return {'app-alias-candidates.md':candidates,'bo-catalog.ts':ts,'UiCodeAliases.java':j,'catalog.sql':q,'review.md':md,'diagnostics.json':js(dict(catalogVersion=version,catalogDigest=dg,**diag))+'\n'}

def main():
    parser=argparse.ArgumentParser(); parser.add_argument('--catalog',type=Path,default=ROOT/'ui-catalog/catalog.json'); parser.add_argument('--sql-only',action='store_true'); args=parser.parse_args()
    c=json.loads(args.catalog.read_text())
    d=diagnose(c)
    if d['errors']:
        print(js(d)); return 1
    if args.sql_only:
        (ROOT/'generated/ui-catalog/catalog.sql').write_text(render_sql(c),encoding='utf-8')
        return 0
    outputs=render(c)
    for name,content in outputs.items():
        dest=ROOT/('docs/ui-catalog' if name in ('review.md', 'app-alias-candidates.md') else 'generated/ui-catalog')/name
        dest.write_text(content,encoding='utf-8')
    print(outputs['diagnostics.json'],end=''); return 0

if __name__=='__main__':
    raise SystemExit(main())
