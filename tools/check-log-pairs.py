#!/usr/bin/env python3
"""JSON/JSON.gz를 파일 경계에 걸쳐 검사. 본문/식별자 원문은 출력하지 않는다."""
import argparse
import gzip
import json
from collections import Counter, defaultdict
from pathlib import Path

HTTP = ('http.request.in', 'http.response.out')
JOB = ('job.start', 'job.end')

def field(row, name):
    if name in row:
        return row[name]
    value = row
    for part in name.split('.'):
        if not isinstance(value, dict):
            return None
        value = value.get(part)
    return value

def check(paths):
    groups = defaultdict(Counter)
    traces = defaultdict(set)
    malformed = invalid = 0
    for path in dict.fromkeys(Path(p).resolve() for p in paths):
        opener = gzip.open if path.suffix == '.gz' else open
        with opener(path, 'rt', encoding='utf-8') as stream:
            for line in stream:
                if not line.strip():
                    continue
                try:
                    row = json.loads(line)
                except (ValueError, TypeError):
                    malformed += 1
                    continue
                if not isinstance(row, dict):
                    malformed += 1
                    continue
                action = field(row, 'event.action') or row.get('event')
                if not isinstance(action, str) or action not in HTTP + JOB:
                    continue
                kind = 'http' if action in HTTP else 'job'
                identity = field(row, 'requestId' if kind == 'http' else 'jobExecutionId')
                if not isinstance(identity, str) or not identity:
                    invalid += 1
                    continue
                key = (kind, identity)
                groups[key][action] += 1
                trace = field(row, 'trace.id')
                if not isinstance(trace, str) or not trace:
                    invalid += 1
                else:
                    traces[key].add(trace)
                if kind == 'http':
                    for required in ('url.path', 'http.response.status_code', 'event.duration', 'error.code', 'durationMs', 'outcome', 'errorCode'):
                        if field(row, required) is None:
                            invalid += 1
                    if action == HTTP[1] and row.get('outcome') not in ('OK', 'ERROR'):
                        invalid += 1
                else:
                    for required in ('job.name', 'event.duration', 'durationMs', 'error.code', 'errorCode', 'outcome'):
                        if field(row, required) is None:
                            invalid += 1
                    if row.get('outcome') not in ('OK', 'ERROR', 'CANCELLED'):
                        invalid += 1
                    if row.get('requestId') != identity or field(row, 'execution.id') != identity:
                        invalid += 1
    missing = duplicate = 0
    for (kind, identity), count in groups.items():
        expected = HTTP if kind == 'http' else JOB
        missing += sum(count[event] == 0 for event in expected)
        duplicate += sum(max(0, count[event] - 1) for event in expected)
    mismatch = sum(len(values) != 1 for values in traces.values())
    result = dict(pairs=len(groups), missing=missing, duplicate=duplicate, traceMismatch=mismatch,
                  invalidFields=invalid, malformedLines=malformed)
    result['ok'] = not (missing or duplicate or mismatch or invalid or malformed)
    return result

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('files', nargs='+', type=Path)
    parser.add_argument('--min-pairs', type=int, default=1, help='빈 입력이 성공으로 보이지 않게 최소 쌍 지정')
    args = parser.parse_args()
    result = check(args.files)
    result['ok'] = result['ok'] and result['pairs'] >= args.min_pairs
    print(json.dumps(result, ensure_ascii=False))
    raise SystemExit(0 if result['ok'] else 1)
