-- Proposed PostgreSQL contract, isolated snapshot for B3 adaptation; NOT a migration.
-- Single code PKs preserve identity; codes must never be reused.
-- Version lifecycle is recorded by introduced_version / retired_version.
-- Environment activation is separate and deliberately has no rows here.
CREATE TABLE ui_catalog_version (
 catalog_version text PRIMARY KEY, source_revision text NOT NULL,
 catalog_digest text NOT NULL, applied_at timestamptz
);
CREATE TABLE ui_catalog_activation (
 environment text PRIMARY KEY,
 catalog_version text NOT NULL REFERENCES ui_catalog_version(catalog_version)
);
CREATE TABLE ui_system (
 system_code text PRIMARY KEY, name text NOT NULL, repository text NOT NULL,
 status text NOT NULL CHECK (status IN ('ACTIVE','RESERVED'))
);
CREATE TABLE ui_domain (
 system_code text NOT NULL REFERENCES ui_system(system_code), domain_code text NOT NULL,
 name text NOT NULL, PRIMARY KEY (system_code,domain_code)
);
CREATE TABLE ui_screen (
 screen_code text PRIMARY KEY,
 system_code text NOT NULL, domain_code text NOT NULL, name text NOT NULL,
 kind text NOT NULL CHECK (kind IN ('SCREEN','JOB','SHELL')), route_template text,
 business_number text, implementation text, owner_team text NOT NULL,
 status text NOT NULL CHECK (status IN ('IMPLEMENTED','PLANNED','RETIRED')),
 replacement_code text, introduced_version text NOT NULL REFERENCES ui_catalog_version(catalog_version),
 retired_version text REFERENCES ui_catalog_version(catalog_version),
 UNIQUE (screen_code,system_code),
 FOREIGN KEY (system_code,domain_code) REFERENCES ui_domain(system_code,domain_code),
 FOREIGN KEY (replacement_code) REFERENCES ui_screen(screen_code) DEFERRABLE INITIALLY DEFERRED
);
CREATE INDEX ui_screen_lookup ON ui_screen(system_code,domain_code,status);
CREATE TABLE ui_menu (
 menu_code text PRIMARY KEY,
 system_code text NOT NULL, domain_code text NOT NULL, parent_code text,
 name text NOT NULL, sort_order integer NOT NULL, target_screen_code text NOT NULL,
 status text NOT NULL CHECK (status IN ('IMPLEMENTED','PLANNED','RETIRED')),
 introduced_version text NOT NULL REFERENCES ui_catalog_version(catalog_version),
 retired_version text REFERENCES ui_catalog_version(catalog_version),
 FOREIGN KEY(system_code,domain_code) REFERENCES ui_domain(system_code,domain_code),
 FOREIGN KEY(target_screen_code,system_code) REFERENCES ui_screen(screen_code,system_code),
 FOREIGN KEY(parent_code) REFERENCES ui_menu(menu_code) DEFERRABLE INITIALLY DEFERRED
);
CREATE TABLE ui_element (
 element_code text PRIMARY KEY,
 screen_code text NOT NULL, name text NOT NULL, element_kind text NOT NULL,
 status text NOT NULL, replacement_code text,
 locator text NOT NULL, evidence_json jsonb NOT NULL,
 introduced_version text NOT NULL REFERENCES ui_catalog_version(catalog_version),
 retired_version text REFERENCES ui_catalog_version(catalog_version),
 FOREIGN KEY(screen_code) REFERENCES ui_screen(screen_code),
 FOREIGN KEY(replacement_code) REFERENCES ui_element(element_code) DEFERRABLE INITIALLY DEFERRED
);
CREATE TABLE ui_code_alias (
 system_code text NOT NULL REFERENCES ui_system(system_code),
 alias_kind text NOT NULL CHECK(alias_kind IN ('INTERNAL_ID','BO_ID','LEGACY','ROUTE')),
 alias_value text NOT NULL, catalog_version text NOT NULL REFERENCES ui_catalog_version(catalog_version),
 screen_code text, status text NOT NULL CHECK(status IN ('RESOLVED','AMBIGUOUS')),
 candidates_json jsonb NOT NULL, reason text,
 UNIQUE(system_code,alias_kind,alias_value,catalog_version),
 FOREIGN KEY(screen_code,system_code) REFERENCES ui_screen(screen_code,system_code),
 CHECK ((status='RESOLVED' AND screen_code IS NOT NULL) OR
        (status='AMBIGUOUS' AND screen_code IS NULL AND reason IS NOT NULL))
);
CREATE INDEX ui_alias_lookup ON ui_code_alias(system_code,alias_kind,alias_value,catalog_version);
-- No ui_element rows or action records are inferred. A future action column must
-- CHECK(action IN ('view','press','submit','change','error')).
-- B3 must grant application accounts SELECT only, using actual reviewed roles.
