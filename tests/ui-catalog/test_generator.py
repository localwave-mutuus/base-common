"""Synthetic fixtures only: never load or regenerate the real catalog."""
import copy
import importlib.util
from pathlib import Path
import unittest

path=Path(__file__).resolve().parents[2]/'tools/ui-catalog/generate.py'
spec=importlib.util.spec_from_file_location('catalog_generator',path)
g=importlib.util.module_from_spec(spec); spec.loader.exec_module(g)

def fixture():
    return dict(catalogVersion='test.1',sourceRevision='abc',systemMapping={'BACKOFFICE':'BO'},
        systems=[dict(code='BO',name='Test',repository='fixture',status='ACTIVE')],
        domains=[dict(system='BO',code='ISS',name='Issues',legacyMenuKey='I')],
        screens=[dict(code='BO-ISS-010',system='BO',domain='ISS',name="quote'\"\\\n\t한글",kind='SCREEN',route='/i-01',businessNumber='I-01',implementation='fixture.tsx',owner='A02',status='IMPLEMENTED',replacement=None,introducedVersion='test.1',retiredVersion=None)],
        menus=[dict(code='BO-MENU-ISS-STATUS',system='BO',domain='ISS',parent=None,name='Status',order=10,target='BO-ISS-010',status='IMPLEMENTED')],
        aliases=[dict(system='BO',kind=k,value=v,version='test.1',target='BO-ISS-010',status='RESOLVED',candidates=[]) for k,v in [('BO_ID','I-01'),('ROUTE','/i-01')]],
        inventory=dict(registryIds=['I-01'],menuIds=['I-01'],issueCatalogIds=['I-01'],shellIds=[],expected={},notes=[]))

class GeneratorTest(unittest.TestCase):
    def test_fixture_coverage_and_three_outputs(self):
        c=fixture(); d=g.diagnose(c)
        self.assertEqual([],d['errors']); self.assertEqual(2,d['coverage']['preserved'])
        outputs=g.render(c)
        for name in ['bo-catalog.ts','UiCodeAliases.java','catalog.sql']:
            self.assertIn(g.digest(c),outputs[name]); self.assertIn('test.1',outputs[name]); self.assertIn('DO NOT EDIT',outputs[name]); self.assertIn('BO-ISS-010',outputs[name])
    def test_duplicates_all_keys(self):
        for table in ['systems','domains','screens','menus','aliases']:
            with self.subTest(table=table):
                c=fixture(); c[table].append(copy.deepcopy(c[table][0]))
                self.assertEqual(1,g.diagnose(c)['duplicates'])
    def test_ambiguity_never_resolves_or_disappears_from_denominator(self):
        c=fixture(); a=c['aliases'][0]; a.update(status='AMBIGUOUS',target=None,reason='uncertain',candidates=['BO-ISS-010'])
        self.assertIsNone(g.resolve(c,'BO','BO_ID','I-01','test.1'))
        d=g.diagnose(c); self.assertEqual(2,d['coverage']['denominator']); self.assertEqual(2,d['coverage']['preserved']); self.assertEqual(1,d['coverage']['uniqueResolved']); self.assertEqual(1,len(d['ambiguous']))
        a['target']='BO-ISS-010'; self.assertTrue(any('ambiguous alias' in e for e in g.diagnose(c)['errors']))
    def test_reference_and_missing_alias(self):
        mutations=[lambda c:c['menus'][0].update(target='BO-ISS-999'),lambda c:c['aliases'][0].update(target='BO-ISS-999'),lambda c:c['screens'][0].update(replacement='BO-ISS-999'),lambda c:c['screens'][0].update(domain='XXX'),lambda c:c['aliases'].pop()]
        for mutate in mutations:
            c=fixture(); mutate(c); self.assertTrue(g.diagnose(c)['errors'])
    def test_scope_and_version(self):
        c=fixture(); c['aliases'][0]['version']='wrong'
        self.assertTrue(any('version mismatch' in e for e in g.diagnose(c)['errors']))
        self.assertIsNone(g.resolve(c,'APP','BO_ID','I-01','test.1'))
        self.assertIsNone(g.resolve(c,'BO','LEGACY','I-01','test.1'))
    def test_escaping(self):
        value="a'\"\\\n\t한글"
        self.assertEqual('"a\'\\"\\\\\\n\\t한글"',g.java(value))
        self.assertEqual("E'a''\"\\\\\\n\\t한글'",g.sql(value))
        import json
        self.assertEqual(value,json.loads(g.js(value)))
        self.assertEqual('null',g.java(None)); self.assertEqual('NULL',g.sql(None))
    def test_deterministic_sort(self):
        c=fixture(); other=copy.deepcopy(c); other['aliases'].reverse()
        self.assertEqual(g.render(c),g.render(other))
        self.assertEqual(g.render(c),g.render(copy.deepcopy(c)))
    def test_duplicate_resolution_fails_closed(self):
        c=fixture(); c['aliases'].append(copy.deepcopy(c['aliases'][0]))
        self.assertIsNone(g.resolve(c,'BO','BO_ID','I-01','test.1'))

if __name__=='__main__': unittest.main()
