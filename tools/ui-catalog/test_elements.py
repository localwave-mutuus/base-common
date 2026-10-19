"""Only the new pipeline's synthetic acceptance cases; no product access."""
import copy
import json
import unittest
from elements import import_roles
from generate import ROOT, digest, diagnose, render, canonical

class ElementPipeline(unittest.TestCase):
    def setUp(self):
        self.base=json.loads((ROOT/'ui-catalog/catalog.json').read_text())
        self.table={'schemaVersion':1,'status':'CONFIRMED','roles':[{
            'screen':'BO-TAX-010','role':'SYNTHETIC-PRESS',
            'locator':'[data-synthetic="press"]',
            'evidence':{'repository':'synthetic-only','revision':'0'*40,'path':'fixture.tsx'}}]}

    def convert(self):
        return import_roles(self.base,self.table,'9.0.0-bo.1')

    def test_normal(self):
        before=copy.deepcopy(self.base)
        c=self.convert(); e=c['elements'][0]
        self.assertEqual(e['code'],'BO-TAX-010-SYNTHETIC-PRESS')
        self.assertEqual(e['evidence'],self.table['roles'][0]['evidence'])
        self.assertEqual(e['locator'],self.table['roles'][0]['locator'])
        self.assertEqual(diagnose(c)['errors'],[])
        out=render(c); dg=digest(c)
        self.assertNotEqual(dg,digest(before))
        for text in out.values():
            self.assertIn(dg,text); self.assertIn('9.0.0-bo.1',text)
        self.assertIn('export const ELEMENT_CODES',out['bo-catalog.ts'])
        self.assertIn('export const SCREEN_ELEMENTS',out['bo-catalog.ts'])
        self.assertIn('"BO-TAX-010": [\n    "BO-TAX-010-SYNTHETIC-PRESS"',out['bo-catalog.ts'])
        self.assertIn("INSERT INTO ui_element",out['catalog.sql'])
        for k in ('screens','menus','inventory'): self.assertEqual(c[k],before[k])
        self.assertEqual([(a['value'],a['target'],a['status']) for a in c['aliases']],[(a['value'],a['target'],a['status']) for a in before['aliases']])
        self.assertEqual(canonical(before),canonical(self.base))

    def test_duplicate(self):
        self.table['roles']*=2
        with self.assertRaisesRegex(ValueError,'duplicate'): self.convert()

    def test_conflict(self):
        row=copy.deepcopy(self.table['roles'][0]); row['locator']='[data-synthetic="other"]'
        self.table['roles'].append(row)
        with self.assertRaisesRegex(ValueError,'role conflict'): self.convert()

    def test_missing_screen(self):
        self.table['roles'][0]['screen']='BO-TAX-999'
        with self.assertRaisesRegex(ValueError,'missing screen'): self.convert()

    def test_invalid_role(self):
        self.table['roles'][0]['role']='synthetic_press'
        with self.assertRaisesRegex(ValueError,'invalid role'): self.convert()

    def test_empty(self):
        self.table['roles'][0]['locator']=''
        with self.assertRaisesRegex(ValueError,'empty locator'): self.convert()

    def test_unconfirmed(self):
        self.table['status']='DRAFT'
        with self.assertRaisesRegex(ValueError,'CONFIRMED'): self.convert()

if __name__=='__main__': unittest.main(verbosity=2)
