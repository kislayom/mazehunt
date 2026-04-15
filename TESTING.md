# Testing Strategy

The platform is tested at four levels. The higher the level, the more it
proves about real behaviour — and the harder it is to run in CI.

| Level | Depends on | Fast? | Runs in CI? | What it proves |
|-------|------------|-------|-------------|-----------------|
| 1 — unit tests with fakes  | nothing            | yes | always              | plumbing correctness |
| 2 — HTTP contract tests    | loopback network   | yes | always              | wire format, error handling |
| 3 — retrieval benchmark    | deterministic fake | yes | always              | ranking quality, regressions |
| 4 — live model IT          | local Ollama       | no  | only when enabled   | real semantic behaviour |

Total counts (memory module): **55 tests across 9 classes**, of which 54 run
unconditionally and 1 (`LiveOllamaMemoryTest`) activates when `OLLAMA_URL` is set.

## Level 1 — unit tests with fakes

Fast, deterministic, no external dependencies. Drive the code through its
interfaces using two test doubles:

- **`FakeEmbedder`** — bag-of-keywords embedder over a controlled vocabulary.
  Predictable cosine similarity without a real model.
- **`FakeModelClient`** — canned or function-based chat model that records
  every prompt invocation for assertion.

Classes at this level:

- `store/SqliteMemoryStoreTest` (13) — CRUD, FTS5 safety, touch semantics,
  persistence across reopen, embedding blob round-trip.
- `store/InMemoryVectorIndexTest` (8) — topK correctness, ordering, limits,
  add/remove, hydrate.
- `LayeredMemoryServiceTest` (14) — confidence floor, tier filter, required
  tags, token budget, vector similarity preference, MMR near-duplicate
  suppression, consolidation behaviour (success / no-op / blank / failure).
- `api/MemoryDtosTest` (5) — JSON round-trip, epoch-millis wire format,
  nullable fields.

**What these prove**: given correct embedder and model contracts, the code
behaves correctly. They do **not** prove the embedder or model are themselves
useful.

## Level 2 — HTTP contract tests

Spin up the real `MemoryHttpServer` on an ephemeral port and exercise the
wire protocol with both the JDK `HttpClient` (for contract shape) and our
own `MemoryHttpClient` (for symmetry with production callers).

- `MemoryHttpRoundTripTest` (9) — happy paths, 405 on wrong method,
  structured 500 on malformed JSON, `/healthz`, stats shape.
- `client/MemoryHttpClientTest` (4) — unreachable server,
  non-JSON response, non-2xx propagation, trailing slash normalisation.

**What these prove**: the service and the client agree on the contract,
including failure modes.

## Level 3 — retrieval quality benchmark

Deterministic **numerical** regression test that measures `recall@k` on a
curated corpus. The corpus has three query types:

- **lexical** — keyword overlap with the answer (BM25's strength).
- **paraphrase** — zero keyword overlap, same meaning (vector's strength).
- **specific-token** — rare identifiers like tickers or proper nouns where
  embedders average the signal into noise (BM25 wins again).

The hybrid recall used by `LayeredMemoryService` has to clear a bar on all
three categories — that's the whole point of having both retrieval legs.

Current thresholds enforced in CI:

```
recall@1              ≥ 0.70   (actual: 0.90)
recall@3              ≥ 0.90   (actual: 0.90)
paraphrase recall@3   ≥ 0.75   (actual: 0.75)
specific-token @1     ≥ 0.90   (actual: 1.00)
```

Run just this benchmark:

```bash
mvn -q -pl mazehunt-memory -am test \
    -Dtest=MemoryRecallQualityTest \
    -Dsurefire.failIfNoSpecifiedTests=false
```

**What this proves**: a regression in the ranking formula, MMR threshold, or
recall budget will fail the build even without a live LLM.

**What this does not prove**: that real embeddings behave the same way as
`FakeEmbedder`. The vocabulary here is hand-crafted; real text is messier.

## Level 4 — live Ollama integration test

`LiveOllamaMemoryTest` is gated on the `OLLAMA_URL` environment variable and
skipped otherwise. When enabled it talks to a real Ollama server and:

1. Seeds memory with five facts that have deliberately minimal keyword overlap.
2. Queries with a paraphrase that shares *no* tokens with the gold fact.
3. Asserts the gold fact is the top hit — which is only possible if the real
   embeddings place paraphrases near each other.
4. Runs real consolidation through a real chat model and asserts the
   extracted facts echo something the user actually said (anti-hallucination
   spot-check).

Run it:

```bash
export OLLAMA_URL=http://localhost:11434
export OLLAMA_EMBED_MODEL=nomic-embed-text   # optional, defaults to this
export OLLAMA_CHAT_MODEL=gemma2:9b           # optional, defaults to this
mvn -q -pl mazehunt-memory -am test -Dtest=LiveOllamaMemoryTest
```

**What this proves**: the memory system actually works with production
components — not just with our test doubles.

## When to use which

| You changed… | Run |
|--------------|-----|
| Ranking formula, MMR, token budget | Level 1 + 3 |
| HTTP API shape                      | Level 1 + 2 |
| SQL schema or FTS handling          | Level 1 |
| Provider adapter or real embedder   | Level 4 (and Level 1 for the adapter itself) |
| Consolidation prompt                | Level 4 (Level 1 only proves we call the model) |

The pyramid is deliberate: Levels 1–3 run in seconds and catch plumbing
regressions; Level 4 is the only one that catches quality regressions in the
end-to-end system and has to be run by hand against a real model.
