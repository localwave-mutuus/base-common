#!/usr/bin/env python3
"""Standalone Python 3 compiler for program-owned runtime aliases (no dependencies)."""
import argparse
import json
from pathlib import Path
import re


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('input', type=Path)
    parser.add_argument('resources', type=Path, help='Program src/main/resources directory')
    args = parser.parse_args()
    source = json.loads(args.input.read_text(encoding='utf-8'))
    rows = source['aliases'] if isinstance(source, dict) else source
    result = {}
    for row in rows:
        if row.get('status', 'RESOLVED') != 'RESOLVED':
            continue
        alias = {field: row[field] for field in ('system', 'kind', 'value', 'target')}
        if (not all(isinstance(v, str) for v in alias.values())
                or alias['system'] not in ('APP', 'MEM', 'BO')
                or not 0 < len(alias['value']) <= 128 or not alias['value'].strip()
                or not all(re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.:-]{0,127}', alias[f]) for f in ('kind', 'target'))):
            raise ValueError('Invalid alias fields')
        key = tuple(alias[f] for f in ('system', 'kind', 'value'))
        if key in result and result[key] != alias:
            raise ValueError('Conflicting alias targets; select one program catalog version')
        result[key] = alias
    output = args.resources / 'META-INF/ui-catalog/aliases.json'
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps([result[k] for k in sorted(result)], ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
