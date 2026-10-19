#!/usr/bin/env python3
"""Validate and convert confirmed role tables offline; never generates products."""
import argparse
import json
from pathlib import Path
from elements import import_roles
from generate import ROOT, diagnose


def main():
    p=argparse.ArgumentParser()
    p.add_argument('--catalog',type=Path,default=ROOT/'ui-catalog/catalog.json')
    p.add_argument('--roles',type=Path,required=True)
    p.add_argument('--version',required=True)
    p.add_argument('--output',type=Path,required=True)
    a=p.parse_args()
    try:
        c=import_roles(json.loads(a.catalog.read_text()),json.loads(a.roles.read_text()),a.version)
        errors=diagnose(c)['errors']
        if errors: raise ValueError('; '.join(errors))
    except (ValueError,KeyError,TypeError) as exc:
        p.exit(1,'Rejected: '+str(exc)+'\n')
    a.output.write_text(json.dumps(c,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

if __name__=='__main__': main()
