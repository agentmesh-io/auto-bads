# Changelog

All notable changes to **Auto-BADS** are documented in this file.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and the project adheres to [Semantic Versioning](https://semver.org/).

## [1.0.0] — 2026-04-26 — General Availability

**Sprint:** M13.2 (AgentMesh workspace) · **Tag:** `v1.0.0`

### Highlights
- First **GA** release of Auto-BADS, dropping the `-RC1` suffix.
- Full agent suite (Ingestion, Market, Product, Analytical/Financial,
  SolutionSynthesis) operational against shared infra.
- Spring Modulith event flow stable; per-module ports & CRUD endpoints
  documented in `docs/API_ENDPOINTS.md`.

### Changed
- `pom.xml` version: `1.0.0-RC1` → `1.0.0`.
- `IdeaIngestionService.ingestIdea` GA contract clarified: LLM failures are
  swallowed and the idea is persisted with a fallback "pending LLM analysis"
  structured-problem statement (graceful degradation, no exception escapes).
  See Risk Register R5 in `docs/PLAN_M13.2.md`.

### Fixed
- `ErrorRecoveryTest` (4 cases): `testLlmServiceFailureRecovery`,
  `testTransientLlmFailureRecovery`, `testRepeatedFailureResilience`,
  `testDatabaseTransactionRollback` — updated to assert the
  graceful-degradation contract instead of the obsolete
  exception-propagation contract.
- `RedisCacheIntegrationTest` — gated with JUnit5 `@EnabledIf` so the class
  is cleanly skipped on hosts where TestContainers cannot reach a Docker
  daemon (e.g. minimal CI runners). Tests run unchanged when Docker is
  available.

### Test status
- Surefire: **128 tests, 0 failures, 0 errors** (Redis IT auto-skips
  off-Docker; runs on CI lanes with Docker).

### Known limitations
- `testSpecialCharactersHandling` remains `@Disabled` pending the
  `event_publication` schema VARCHAR(255) widening (tracked separately,
  no functional impact — SQL-injection prevention is verified by other
  cases).

## [0.9.0] — 2025-12 — Feature freeze
- Full feature implementation, file reorganisation, docs cleanup.
  (Tag `v0.9.0`.)

