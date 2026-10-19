"""Strict confirmed role-table contract; no role inference or normalization."""
import re


MAPPING_FIELDS = {'rawRole', 'sourceScreen', 'actionToken'}
BEHAVIOR_FIELDS = {'label', 'trigger', 'kind', 'gate', 'notes', 'productRevision'}

def validate_behavior(value):
    if not isinstance(value, dict) or set(value) != BEHAVIOR_FIELDS:
        raise ValueError('invalid behavior fields')
    if any(not isinstance(v, str) or not v.strip() or v != v.strip() for v in value.values()):
        raise ValueError('empty behavior field')
    if not re.fullmatch(r'[0-9a-f]{40}', value['productRevision']):
        raise ValueError('invalid product revision')



def validate_mapping(row):
    for key in MAPPING_FIELDS:
        if not isinstance(row[key], str) or not row[key]:
            raise ValueError('invalid mapping field: '+key)
    if not re.fullmatch(r'[A-Z][A-Z0-9]*-[0-9]{2}', row['sourceScreen']):
        raise ValueError('invalid source screen prefix')
    if not re.fullmatch(r'[a-z][a-z0-9]*(?:-[a-z][a-z0-9]*)*', row['actionToken']):
        raise ValueError('ambiguous action token; explicit lowercase hyphen token required')
    if row['rawRole'] not in (row['sourceScreen']+'.'+row['actionToken'], '<'+row['sourceScreen']+'.'+row['actionToken']+'>'):
        raise ValueError('raw role mapping mismatch')
    if row['role'] != row['actionToken'].upper():
        raise ValueError('explicit action token/code suffix mismatch')


def validate(elements, screens):
    if not isinstance(elements, list):
        raise ValueError('elements must be an array')
    codes, roles, raw_roles, prefixes = set(), {}, set(), {}
    for e in elements:
        if not isinstance(e, dict) or set(e) not in ({'code', 'screen', 'role', 'locator', 'evidence', 'introducedVersion'}, {'code', 'screen', 'role', 'locator', 'evidence', 'introducedVersion'} | MAPPING_FIELDS, {'code', 'screen', 'role', 'locator', 'evidence', 'introducedVersion', 'behavior'} | MAPPING_FIELDS):
            raise ValueError('invalid element fields')
        if 'behavior' in e:
            validate_behavior(e['behavior'])
        if MAPPING_FIELDS <= set(e):
            validate_mapping(e)
        for key in ('code', 'screen', 'role', 'locator', 'introducedVersion'):
            if not isinstance(e[key], str) or not e[key].strip() or e[key] != e[key].strip():
                raise ValueError('invalid or empty '+key)
        if e['screen'] not in screens:
            raise ValueError('missing screen: '+e['screen'])
        if not re.fullmatch(r'[A-Z][A-Z0-9]*(?:-[A-Z][A-Z0-9]*)*', e['role']):
            raise ValueError('invalid role; normalization forbidden')
        if e['code'] != e['screen']+'-'+e['role'] or len(e['code']) > 64:
            raise ValueError('invalid element code')
        if MAPPING_FIELDS <= set(e):
            if e['rawRole'] in raw_roles:
                raise ValueError('raw role conflict')
            raw_roles.add(e['rawRole'])
            if e['sourceScreen'] in prefixes and prefixes[e['sourceScreen']] != e['screen']:
                raise ValueError('source screen mapping conflict')
            prefixes[e['sourceScreen']]=e['screen']
        identity=(e['screen'], e['role'])
        if identity in roles and roles[identity] != (e['locator'], e['evidence']):
            raise ValueError('role conflict: different locator/evidence for same screen role')
        if e['code'] in codes:
            raise ValueError('duplicate element code: '+e['code'])
        if (e['screen'], e['role']) in roles:
            raise ValueError('role conflict')
        evidence=e['evidence']
        if not isinstance(evidence, dict) or set(evidence) not in ({'repository','revision','path'}, {'repository','revision','path','contentSha256','revisionKind'}):
            raise ValueError('invalid evidence fields')
        if 'contentSha256' in evidence and (not re.fullmatch(r'[0-9a-f]{64}', evidence['contentSha256']) or evidence['revisionKind'] not in ('WORKTREE_BASE', 'GIT_COMMIT')):
            raise ValueError('invalid worktree snapshot evidence')
        if any(not isinstance(v,str) or not v.strip() or v != v.strip() for v in evidence.values()):
            raise ValueError('empty evidence')
        if not re.fullmatch(r'[0-9a-f]{40}', evidence['revision']):
            raise ValueError('evidence revision must be full Git SHA')
        if evidence['path'].startswith('/') or '..' in evidence['path'].split('/'):
            raise ValueError('evidence path must be repository relative')
        codes.add(e['code']); roles[identity]=(e['locator'], e['evidence'])


def import_roles(catalog, table, version):
    import copy
    if not isinstance(table,dict) or set(table) != {'schemaVersion','status','roles'} or table['schemaVersion'] not in (1, 2, 3) or table['status'] != 'CONFIRMED' or not isinstance(table['roles'],list) or not table['roles']:
        raise ValueError('nonempty CONFIRMED role table schemaVersion=1/2/3 required')
    if not isinstance(version,str) or not re.fullmatch(r'[0-9]+\.[0-9]+\.[0-9]+-bo\.[0-9]+',version) or version == catalog['catalogVersion']:
        raise ValueError('explicit new catalog version required')
    result=copy.deepcopy(catalog)
    rows=[]
    for row in table['roles']:
        fields={'screen','role','locator','evidence'} | (MAPPING_FIELDS if table['schemaVersion'] >= 2 else set()) | ({'behavior'} if table['schemaVersion'] == 3 else set())
        if not isinstance(row,dict) or set(row) != fields:
            raise ValueError('invalid role row fields; codes and target IDs are not inputs')
        if not isinstance(row['screen'],str) or not isinstance(row['role'],str):
            raise ValueError('screen and role must be strings')
        if table['schemaVersion'] >= 2:
            validate_mapping(row)
            if not any(s['code'] == row['screen'] and s['businessNumber'] == row['sourceScreen'] for s in result['screens']):
                raise ValueError('source screen/catalog screen mapping mismatch')
        rows.append(dict(row,code=row['screen']+'-'+row['role'],introducedVersion=version))
    existing=result.get('elements',[])
    for row in rows:
        for old in existing:
            if (old['screen'],old['role']) == (row['screen'],row['role']):
                raise ValueError('role conflict with existing element; explicit review required')
    result['elements']=existing+rows
    validate(result['elements'],{s['code'] for s in result['screens']})
    result['catalogVersion']=version
    for a in result['aliases']: a['version']=version
    return result
