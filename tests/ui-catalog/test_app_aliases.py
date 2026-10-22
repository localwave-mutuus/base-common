"""Two bounded APP registration contracts, including the compiled runtime."""
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT/'tools/ui-catalog'))
spec = importlib.util.spec_from_file_location('app_generator', ROOT/'tools/ui-catalog/generate.py')
g = importlib.util.module_from_spec(spec)
spec.loader.exec_module(g)

class AppAliasTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.catalog = json.loads((ROOT/'ui-catalog/catalog.json').read_text())
        cls.work = tempfile.TemporaryDirectory(dir=os.environ.get('TEAM_OUT_DIR', '/tmp'))
        cls.path = Path(cls.work.name)
        generated = g.render(cls.catalog)['UiCodeAliases.java']
        runtime = ROOT/'lib/src/main/java/ai/mutuus/common/uicatalog/UiCodeAliases.java'
        if generated != runtime.read_text():
            raise AssertionError('runtime alias table differs from generator')
        subprocess.run(['javac', '-d', str(cls.path), str(runtime), str(ROOT/'lib/src/main/java/ai/mutuus/common/core/UiCodeResolver.java')], check=True)
        harness = cls.path/'AppAliasCheck.java'
        harness.write_text('''import ai.mutuus.common.core.UiCodeResolver;
import ai.mutuus.common.uicatalog.UiCodeAliases;
public class AppAliasCheck {
 public static void main(String[] args) {
  String v = UiCodeAliases.CATALOG_VERSION;
  if (args[0].equals("resolved")) {
   if (!UiCodeResolver.screen("APP", "HOM-010", v).orElseThrow().equals("APP-HOM-010")) throw new AssertionError();
   System.out.println("PASS HOM-010 -> APP-HOM-010");
  } else {
   if (UiCodeAliases.ALIASES.stream().anyMatch(a -> a.system().equals("APP") && a.aliasValue().equals("AUT-010") && a.aliasKind().equals("LEGACY"))) throw new AssertionError();
   if (UiCodeResolver.resolve("APP", "LEGACY", "AUT-010", v).isPresent()) throw new AssertionError();
   if (!UiCodeResolver.screen("APP", "AUT-010", v).orElseThrow().equals("AUT-010")) throw new AssertionError();
   System.out.println("PASS AUT-010 unregistered; raw AUT-010 retained");
  }
 }
}''')
        subprocess.run(['javac','-cp',str(cls.path),'-d',str(cls.path),str(harness)],check=True)

    @classmethod
    def tearDownClass(cls):
        cls.work.cleanup()

    def test_clear_alias(self):
        self.assertEqual([],g.diagnose(self.catalog)['errors'])
        self.assertEqual('APP-HOM-010',g.resolve(self.catalog,'APP','LEGACY','HOM-010',self.catalog['catalogVersion']))
        subprocess.run(['java','-cp',str(self.path),'AppAliasCheck','resolved'],check=True)

    def test_ambiguous_unregistered(self):
        aliases=[a for a in self.catalog['aliases'] if a['system']=='APP']
        self.assertEqual(598,sum(a['status']=='RESOLVED' for a in aliases))
        self.assertEqual(53,sum(a['status']=='AMBIGUOUS' for a in aliases))
        self.assertIsNone(g.resolve(self.catalog,'APP','LEGACY','AUT-010',self.catalog['catalogVersion']))
        subprocess.run(['java','-cp',str(self.path),'AppAliasCheck','ambiguous'],check=True)

if __name__=='__main__': unittest.main()
