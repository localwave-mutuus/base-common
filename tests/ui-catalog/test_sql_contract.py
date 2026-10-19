"""SQL-only lifecycle contract tests; synthetic fixture, no catalog regeneration."""
import unittest
from test_generator import g, fixture

class SqlContractTest(unittest.TestCase):
    def test_single_identity_keys_and_lifecycle_foreign_keys(self):
        c = fixture()
        q = g.render_sql(c)
        schema = (g.ROOT / 'tools/ui-catalog/schema.sql').read_text()
        self.assertIn(schema, q)
        for table, code in [('ui_screen', 'screen_code'), ('ui_menu', 'menu_code'), ('ui_element', 'element_code')]:
            body = schema.split('CREATE TABLE ' + table + ' (', 1)[1].split('\n);', 1)[0]
            self.assertIn(code + ' text PRIMARY KEY', body)
            self.assertNotIn('catalog_version text', body)
            for col in ['introduced_version', 'retired_version']:
                self.assertIn(col + ' text', body)
                self.assertIn('REFERENCES ui_catalog_version(catalog_version)', body)
        for fk in [
            'FOREIGN KEY (replacement_code) REFERENCES ui_screen(screen_code)',
            'FOREIGN KEY(target_screen_code,system_code) REFERENCES ui_screen(screen_code,system_code)',
            'FOREIGN KEY(parent_code) REFERENCES ui_menu(menu_code)',
            'FOREIGN KEY(screen_code) REFERENCES ui_screen(screen_code)',
            'FOREIGN KEY(replacement_code) REFERENCES ui_element(element_code)',
            'FOREIGN KEY(screen_code,system_code) REFERENCES ui_screen(screen_code,system_code)',
        ]:
            self.assertIn(fk, schema)
        self.assertIn('UNIQUE (screen_code,system_code)', schema)
        self.assertIn('UNIQUE(system_code,alias_kind,alias_value,catalog_version)', schema)
        self.assertIn('CREATE TABLE ui_catalog_activation', schema)
        self.assertNotIn('INSERT INTO ui_catalog_activation', q)
        for table in ['ui_screen', 'ui_menu']:
            cols = q.split('INSERT INTO ' + table + ' (', 1)[1].split(')', 1)[0]
            self.assertNotIn('catalog_version', cols)
            self.assertIn('introduced_version, retired_version', cols)
        self.assertIn("E'IMPLEMENTED', NULL, E'test.1', NULL)", q)
        self.assertIn("E'IMPLEMENTED', E'test.1', NULL)", q)
        for token in [g.digest(c), 'test.1', 'DO NOT EDIT']:
            self.assertIn(token, q)
