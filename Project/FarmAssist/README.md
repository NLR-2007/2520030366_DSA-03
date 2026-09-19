<div align="center">

# Kisaan Krushi AI

**An algorithm-driven agricultural knowledge search system for the terminal**

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Platform](https://img.shields.io/badge/platform-Windows%20Terminal-blue)
![Algorithms](https://img.shields.io/badge/core%20algorithms-8-green)
![Course](https://img.shields.io/badge/DSA--3-25CS2103E-lightgrey)

*Course project · DSA-3 (25CS2103E) · Team 20 · KL University*

</div>

---

## Overview

Kisaan Krushi AI is a console chat assistant that answers a farmer's questions about
crops, diseases, pests and fertilizers, plans crops for a climate, builds a fertilizer
basket within a budget, and reports live weather with field advice.

There is no machine-learning model behind it. **Every answer is produced by eight classic
string, graph and dynamic-programming algorithms** implemented from scratch and run over a
local agriculture knowledge base. The project's goal is to show those algorithms doing
real, visible work end-to-end.

```
 you  › my tomatoe has yelow leaves and brown spots

 ◆ Kisaan Krushi AI  │ I adjusted your words: tomatoe → tomato, yelow → yellow
                     │ Found 3 diseases matching 2 symptoms in tomato ...
```

---

## Features

| Area | What it does | Powered by |
|------|--------------|-----------|
| **Search** | Ranked answers with snippets and related reading for any farming question | Rabin–Karp, KMP, QuickSort, Suffix Array + LCP |
| **Diagnosis** | "my tomato has yellow leaves" → likely diseases, ranked, with treatment | Aho–Corasick, KMP, QuickSort |
| **Spelling & language** | Fixes typos, splits joined words, maps local names (`paddy`, `bhindi`, `dhan`) | Edit Distance, word-segmentation DP, Aho–Corasick |
| **Fertilizer basket** | Best set of fertilizers for a crop under a rupee budget | 0/1 Knapsack |
| **Field allocation** | One distinct fertilizer per crop across several fields | Bipartite Matching (Kuhn) |
| **Crop planner** | Crops suited to a rainfall, temperature or water profile | Record scoring + QuickSort |
| **Live search box** | Suggestions drop down under the prompt as you type, like a video site's search bar | Trie |
| **Live weather** | Current reading, five-day outlook and farming advice for any town | OpenWeatherMap API |
| **Algorithm trace** | Optional gutter showing which algorithm ran and what it found | — |

---

## Quick start

**Requirements:** JDK 21 or newer. Nothing else — no build tool, no package manager.

**Windows (recommended):** double-click **`FarmAssist.bat`**. It opens a terminal window
(Windows Terminal when installed), compiles into `out/` and starts the chat.

**From an open terminal:**

```bat
run.bat
```

**By hand, on any OS:**

```bash
javac -encoding UTF-8 -cp "lib/*" -d out $(find src -name "*.java")
java  -Dstdout.encoding=UTF-8 -cp "out:lib/*" ui.ConsoleChat data      # use ; instead of : on Windows
```

The single jar in `lib/` (JLine) is used only to read the keyboard one key at a time for
the live suggestion box. Everything else is plain JDK.

> The screen uses box-drawing characters and 24-bit colour. Both launchers switch the
> console to UTF-8 (`chcp 65001`). If borders still look wrong, type `ascii on` or
> `color off` inside the chat.

---

## Using the chat

Type a question in plain English (or mixed Hindi/Telugu words — see `data/synonyms.txt`).
A drop-down of completions appears as you type: **↑/↓** pick, **Tab** fills, **Enter** asks,
**Esc** hides.

### Example questions

| Type this | Shows |
|-----------|-------|
| `how to grow rice` | Full crop profile |
| `compare rice and wheat` | Side-by-side facts |
| `my tomato has yellow leaves` | Disease diagnosis |
| `my brinjal has a pest` | Every disease and pest of the crop |
| `how to control whitefly` | Pest profile with damage and control |
| `which crops suit low rainfall` | Crop planner |
| `suggest fertilizer for tomato under 3000` | Best basket within budget (Knapsack) |
| `match fertilizers for rice cotton banana` | One bag per crop (Bipartite Matching) |
| `what is dap` | Fertilizer profile |
| `weather in guntur` · `will it rain tomorrow` | Live weather, outlook and field advice |

### Commands

| Command | Does |
|---------|------|
| `help` | Examples and commands |
| `list crops` · `list diseases` · `list pests` · `list fertilizers` · `list articles` | Browse the knowledge base |
| `algo demo` | Run all eight algorithms on tiny, hand-checkable inputs |
| `trace on` / `trace off` | Show or hide the algorithm trace in the gutter |
| `city guntur` | Set the default town for weather questions |
| `color on` / `color basic` / `color off` · `ascii on` / `ascii off` · `width 120` · `center off` | Terminal appearance |
| `clear` · `exit` | Housekeeping |

### Suggested demo order

`algo demo` → `hi` → `paddy` → `my tomatoe has yelow leaves and brown spots` →
`suggest fertilizer for tomato under 3000 rupees` → `match fertilizers for rice cotton banana groundnut` →
`how to grow rice` → `compare rice and wheat` → `which crops suit low rainfall` → `weather in guntur`

---

## How a question is answered

```
        farmer's question
              │
   0. Small talk          "hi", "thanks", "what can you do"        → reply and stop
   0. Weather intent      "weather in …", "will it rain"           → OpenWeatherMap
              │
   1. Edit Distance       fix spelling      "tomatoe"  → "tomato"
      + word splitter     unstick words     "lateblight" → "late blight"
   1b. Aho–Corasick       synonym rewrite   "paddy" / "dhan" / "chawal" → "rice"
              │
   2. Aho–Corasick        detect every crop / disease / fertilizer / symptom in ONE pass
              │
   3. Intent routing
              │
   ┌──────────┼──────────────────┬─────────────────────┐
budget?   several crops?      symptoms?           anything else
   │          │                  │                     │
6. Knapsack  7. Bipartite     1. KMP over          2. Rabin–Karp   score documents
             Matching         symptom lists        1. KMP          locate + snippet
                              8. QuickSort         8. QuickSort    rank
                                                   5. Suffix Array + LCP  related articles
```

---

## The eight algorithms

All live in `src/algo/`, are pure (no project logic inside), and are used for real work —
no `String.contains()`, no `Collections.sort()`.

| # | Algorithm | File | Used for | Time | Space |
|---|-----------|------|----------|------|-------|
| 1 | **KMP** | `KMP.java` | Exact match of a symptom inside a disease record; locate a term to cut the snippet | O(n + m) | O(m) |
| 2 | **Rabin–Karp** | `RabinKarp.java` | Rolling-hash scan of all indexed documents to count term hits → relevance score | O(n + m) avg | O(1) |
| 3 | **Aho–Corasick** | `AhoCorasick.java` | One pass detects every crop, disease, fertilizer and symptom (285 patterns); a second automaton rewrites local names | O(n + Σ‖p‖ + matches) | O(Σ‖p‖) |
| 4 | **Edit Distance** | `EditDistance.java` | Spelling correction, typo-tolerant greetings, "did you mean" | O(n·m) | O(n·m) |
| 5 | **Suffix Array + LCP** | `SuffixArrayLCP.java` | Related articles by longest common substring | O(n log² n) build, O(n) LCP | O(n) |
| 6 | **0/1 Knapsack** | `Knapsack.java` | Fertilizer basket within budget (weight = price, value = benefit) | O(items × budget) | O(items × budget) |
| 7 | **Bipartite Matching** | `BipartiteMatching.java` | One distinct fertilizer per crop (Kuhn's augmenting paths) | O(V · E) | O(V + E) |
| 8 | **Randomized QuickSort** | `RandomizedQuickSort.java` | Ranking results, diseases, related articles | O(n log n) expected | O(log n) |

Two supporting structures sit beside them: a **Trie** (`Trie.java`) drives the live
search-box completions in O(L) per keystroke, and a word-segmentation DP
(`engine/CompoundSplitter.java`) repairs missing spaces, which Edit Distance cannot do.

---

## Live weather

`weather in guntur`, `will it rain tomorrow`, `forecast for this week` — Kisaan Krushi AI
fetches the current reading and a five-day outlook from OpenWeatherMap and turns the
numbers into decisions:

- rain or strong wind coming → hold irrigation and spraying
- hot and dry → irrigate in the evening
- warm and humid → inspect for blight and mildew
- heat or frost warnings for seedlings and transplanting
- which crops in the knowledge base suit today's temperature

**Setup (one step):** copy `data/weather.example.txt` to `data/weather.txt` and paste a
free key from [openweathermap.org](https://openweathermap.org/api). The real file is
git-ignored so the key never reaches the repository. Questions that name no town use the
default town from that file; change it for the session with `city <town>`. Without the
file, no internet, an unknown town or a bad key, the chat says so in one line and carries on.

---

## Knowledge base

Plain text, `|`-separated, `#` for comments. Add a row and it is live on the next run.

| File | Rows | Contents |
|------|------|----------|
| `crops.txt` | 63 | Season, soil, water need, N-P-K, diseases, duration, temperature, rainfall, spacing, varieties, yield |
| `diseases.txt` | 64 | Symptoms and treatment |
| `pests.txt` | 44 | Damage and control |
| `fertilizers.txt` | 38 | NPK, price, benefit |
| `articles.txt` | 62 | Searchable field notes |
| `symptoms.txt` | 74 | Symptom keywords for Aho–Corasick |
| `synonyms.txt` | 396 | Local / alternate names → canonical word |
| `smalltalk.txt` | — | Greetings and everyday replies |
| `weather.example.txt` | — | Template for `weather.txt` (OpenWeatherMap key and default town) |

The search index holds 271 documents: the articles plus a generated profile for every
crop, disease, fertilizer and pest.

---

## Project structure

```
FarmAssist/
├── FarmAssist.bat          double-click launcher (opens a terminal, compiles, runs)
├── run.bat                 compile + run in the current terminal
├── lib/jline-3.27.1.jar    raw keyboard input for the live search box
├── data/                   the knowledge base (see above)
└── src/
    ├── algo/               the eight algorithms + Trie — pure, reusable
    ├── model/              Crop, Disease, Fertilizer, Pest, Article
    ├── engine/             SpellCorrector · SynonymMapper · EntityExtractor
    │                       IntentDetector · SearchEngine · Diagnoser · Planner
    │                       Recommender · AutocompleteEngine · WeatherService
    │                       CompoundSplitter · SmallTalk · DataLoader
    ├── ui/                 ConsoleChat  the chat loop and every screen
    │                       LiveInput    the search box with live suggestions
    │                       Theme        78-column grid, palette, panels, tables
    │                       Centering    keeps the page centred in the window
    │                       AlgorithmDemo
    └── util/               Trace (algorithm gutter) · Json (minimal JSON reader)
```

---

## Team

| Name | ID |
|------|----|
| Nimma Lokesh Reddy | 2520030366 |
| Ramagiri Rishik Rao | 2520030333 |
| Manne Yashwanth Manoj | 2520030369 |

Section S-06 · Batch 20 · DSA-3 (25CS2103E)

---

## Acknowledgements

- Weather data by [OpenWeatherMap](https://openweathermap.org/)
- Terminal raw-mode input by [JLine](https://github.com/jline/jline3)
