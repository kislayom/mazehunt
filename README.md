# Mazehunt — Proactive Agentic AI System

A Java-based agentic AI platform designed to behave like a human assistant rather
than a reactive chatbot. Built for workstations with **48 GB VRAM and 192 GB RAM**.

## Goals

1. **Multi-model orchestration** — local models via [Ollama](https://ollama.ai)
   (Gemma, DeepSeek, Qwen, Llama, etc.) and/or `llama.cpp`, plus optional cloud
   models (OpenAI, Anthropic) when a task requires them.
2. **Multi-modal** — text, voice (Whisper STT + Piper/OpenAI TTS), images
   (vision-capable models).
3. **Industry-leading memory** — keeps the live context window as small as
   possible while transparently fetching relevant facts on demand from a
   layered memory (working → episodic → semantic → procedural).
4. **Planner / Flowchart executor** — decomposes complex goals into a DAG of
   typed steps and executes them, passing structured data between steps.
5. **Proactive** — schedules check-ins, asks clarifying questions, learns about
   the user, runs background tasks while you sleep.
6. **Fact-grounded** — every claim made by the agent is attributed to a source
   (memory id, tool result, citation). Steps that cannot be grounded are
   marked `UNKNOWN` rather than hallucinated.
7. **Pluggable skills** — drop-in `Skill` SPI implementations
   (e.g. `BuffettStockAnalysisSkill`).

## Modules

| Module               | Purpose                                                       |
|----------------------|---------------------------------------------------------------|
| `mazehunt-api`       | Public interfaces & data records (zero deps)                  |
| `mazehunt-core`      | Agent runtime, planner, router, proactive engine              |
| `mazehunt-memory`    | Memory tier as an **independent deployable service** + client |
| `mazehunt-models`    | Adapters: Ollama, OpenAI, Anthropic, llama.cpp                |
| `mazehunt-skills`    | Built-in skills (web, files, stocks, math, …)                 |
| `mazehunt-voice`     | STT (Whisper) + TTS                                           |
| `mazehunt-vision`    | Image ingestion + vision-LM bridge                            |
| `mazehunt-cli`       | Interactive REPL & one-shot CLI                               |

## Quick start

```bash
mvn -q -DskipTests package

# Option A — everything in one JVM (default):
java -jar mazehunt-cli/target/mazehunt-cli.jar

# Option B — run memory as a separate service (recommended for production):
java -jar mazehunt-memory/target/mazehunt-memory-service.jar --port 8765 &
# then set `memory.mode: remote` in config/mazehunt.yaml and launch the CLI
java -jar mazehunt-cli/target/mazehunt-cli.jar
```

See `config/mazehunt.yaml` for routing, model pools and memory tuning.

## Memory as an independent service

The memory tier is a stand-alone HTTP service so the agent, background workers,
CLI and any future UI can share one memory brain — and so you can restart the
agent without losing state.

```
┌────────────────┐    HTTP     ┌──────────────────────┐
│ mazehunt-cli   │ ──────────▶ │ mazehunt-memory      │
│ (agent, tools) │   JSON      │  • LayeredMemory     │
│                │ ◀────────── │  • SQLite + FTS5     │
└────────────────┘             │  • vector index      │
                               │  • consolidation     │
                               └──────────────────────┘
```

Endpoints (`ai.mazehunt.memory.service.MemoryHttpServer`):

| Method | Path                      | Purpose                              |
|--------|---------------------------|--------------------------------------|
| POST   | `/v1/memory/items`        | remember an episodic event           |
| POST   | `/v1/memory/facts`        | learn a semantic fact                |
| POST   | `/v1/memory/recall`       | hybrid BM25 + vector recall          |
| POST   | `/v1/memory/consolidate`  | force episodic → semantic pass       |
| GET    | `/v1/memory/stats`        | per-tier item counts                 |
| GET    | `/v1/healthz`             | liveness probe                       |

The agent calls through `MemoryService` — `MemoryFactory.local(...)` returns an
in-process implementation, `MemoryFactory.remote(url)` returns an HTTP client.
Everything else in the platform is transport-agnostic.

## Memory architecture (TL;DR)

```
 ┌──────────────┐   evict       ┌──────────────┐  summarise   ┌──────────────┐
 │ Working set  │ ───────────▶  │ Episodic log │ ───────────▶ │ Semantic KG  │
 │ (token bdgt) │               │ (sqlite)     │              │ (vec + facts)│
 └──────────────┘               └──────────────┘              └──────────────┘
        ▲                                                            │
        │              retrieve top-k by cosine + recency +          │
        └────────────────── fact-confidence ◀────────────────────────┘
```

The planner only sees the working set. Every other memory tier is queried via
the `MemoryService.recall(query, budget)` API, which returns a *minimal* set of
deduplicated, ranked facts that fit the caller's token budget.

## Hardware budget

The default profile (`config/mazehunt.yaml → hardware.profile: workstation-48gb`)
keeps a primary 70B-class model resident on GPU, an embedding model + small
re-ranker on a second slice, and reserves ~140 GB of RAM for the memory tier
(SQLite page cache + on-heap LRU of recent embeddings).
