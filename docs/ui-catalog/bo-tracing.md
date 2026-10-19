# common-platform 0.5.0 UI tracing

Registry `log-fields.v2.json` now has registry version **4** (log schema remains 2).
Generated bo.6 Java is copied byte-for-byte into the runtime source path:
`lib/src/main/java/ai/mutuus/common/uicatalog/UiCodeAliases.java`.
Catalog version: `1.0.0-bo.6`; catalog digest:
`ff91f5f3af1b57f7484f2c9028d95c832eed7ea0b76025c2b005e192a90d77d4`.

The bo.6 catalog has 328 registered entries; this Java artifact contains 150
alias rows (149 RESOLVED and one AMBIGUOUS), not 328 alias rows. Runtime
acceptance is limited to the RESOLVED targets and exact aliases present in
this artifact; catalog registration alone does not make a value resolvable.
Java SHA-256: `696f8d980b0e4737512555cfa15d5f85de622f3971634e79cc323102457761ac`.

Send `X-Screen-Catalog-Version: 1.0.0-bo.6` with `X-Screen-Id`.
`golmok.screen.id` contains the standard screen code, e.g. `BO-ISS-010`.
Known BO_ID, ROUTE and INTERNAL_ID aliases are resolved at the shared
`ScreenMetadata` / `UiCodeResolver` boundary. Explicit alias callers use
`resolve(system, kind, value, version)` with all four exact keys. No trimming,
case folding, query stripping, URL parsing or system inference is performed.
Only unique RESOLVED entries are accepted. Standard BO codes must occur as
RESOLVED targets in this version. Unknown or mismatched screen/version drops
UI metadata, without rejecting the HTTP request.

| Header | Canonical field | Validation |
| --- | --- | --- |
| X-Screen-Id | golmok.screen.id | Known standard code or unique alias |
| X-Screen-Catalog-Version | golmok.screen.catalog_version | Exact bo.6 version |
| X-UI-Element-Id | golmok.ui.element.id | Standard screen code + `-` + uppercase alphanumeric tokens separated by `-`, at most 64 chars |
| X-UI-Action | golmok.ui.action | view, press, submit, change, error |
| X-Event-Id | golmok.ui.event.id | Nonzero 16 lowercase hex root span ID |
| X-Business-Screen-No | golmok.screen.business_no | Existing uppercase 3 letters + hyphen + 3 digits |

Element validation checks syntax and screen ownership prefix; The generated Java has no element
inventory, so it does not claim catalog membership. Example element codes in
unit tests are synthetic, not additions to the catalog. UI event ID identifies
the browser action root; W3C traceparent carries the request CLIENT span ID.
They need not match. The server does not invent an event ID without the header.
`golmok.screen.internal_id` is retired and `X-Internal-Screen-Id` is ignored.
BO consumers must not duplicate alias conversion.

Route aliases are resolved before the generic safe-hint parser: `/i-01` maps
without permitting arbitrary slashes/URLs in logs. BO `/home`, `/i-10` and
ambiguous legacy `I-10` remain unmapped. APP/MEMBER/BAT catalog targets are
reserved and unconfirmed; no target is invented for them.

Do not attach input values, searches, passwords or URL query strings to UI
codes. The access facade omits query arguments. Identity, authentication,
masking and rolling policies remain their existing contracts. UI hints are
untrusted metadata and never authorize requests.

Offline targeted verification (reactor includes changed lib, no installed jar):

```sh
mvn -o -pl samples/sample-api -am -Dtest=UiCodeResolverTest#runtimeCopyIsByteIdenticalToBo6+exactKeysAndResolvedStatusAreRequired -Dsurefire.failIfNoSpecifiedTests=false test
```

Integration and rollback are P00 patches. No original checkout commit, remote
publish, service restart, global install or database changes are part of this
patch. A05 must consume artifacts built from the integrated revision, not an
older installed 0.4.1 jar.
