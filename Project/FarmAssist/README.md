# FarmAssist — An Advanced Algorithm-Based Agricultural Knowledge Search System

DSA-3 (25CS2103E) course project — Team 20
Nimma Lokesh Reddy (2520030366) · Ramagiri Rishik Rao (2520030333) · Manne Yashwanth Manoj (2520030369)

The assistant is called **Sophie**. She is a console chat bot that answers farming
questions about crops, diseases, symptoms and fertilizers. There is no AI and no internet call — **every answer is produced by the
eight algorithms below** running over a local agriculture database.

---

## How to run

**Double-click `FarmAssist.bat`.** It opens its own terminal window, compiles the
project into `out/` and starts Sophie inside it — nothing needs to be open first.
It uses Windows Terminal when that is installed (which gives UTF-8 and 24-bit colour
with no setup) and falls back to a plain console window otherwise.

`run.bat` does the same thing without opening a window, for when you already have a
terminal open in this folder. Or run it by hand:

```
javac -encoding UTF-8 -d out (all files under src)
java -Dstdout.encoding=UTF-8 -cp out ui.ConsoleChat data
```

Requires only a JDK (tested on JDK 25). No libraries, no build tool.

The screen is drawn with box characters and 24-bit colour, so the console has to be in
UTF-8 — both launchers do that for you with `chcp 65001`. If the borders still come out as
question marks, type `ascii on` in the chat for plain `+---+` borders, or `color off`
for plain text. Sophie detects both automatically at start up, so you normally never
need either.

---

## Where each algorithm is used

| # | Algorithm | File | Used for |
|---|-----------|------|----------|
| 1 | **KMP** | `src/algo/KMP.java` | Exact-match a symptom keyword inside a disease record; find the exact position of a term in a document to cut the snippet |
| 2 | **Rabin–Karp** | `src/algo/RabinKarp.java` | Rolling-hash scan of all 227 indexed documents to count how often each term appears → the relevance score |
| 3 | **Aho–Corasick** | `src/algo/AhoCorasick.java` | One single pass over the question detects **all** crops, diseases, fertilizers and symptoms at once (241 patterns); a second automaton rewrites local names (`paddy` → `rice`, `bhindi` → `okra`) |
| 4 | **Edit Distance** | `src/algo/EditDistance.java` | Spelling correction (`tomatoe` → `tomato`); matching typo'd greetings; the "did you mean" fallback when a search returns nothing |
| 5 | **Suffix Array + LCP** | `src/algo/SuffixArrayLCP.java` | "Related articles" — similarity = longest common substring between two article bodies |
| 6 | **0/1 Knapsack** | `src/algo/Knapsack.java` | Best fertilizer basket within the farmer's budget (weight = price, value = benefit) |
| 7 | **Bipartite Matching** | `src/algo/BipartiteMatching.java` | Assign one different fertilizer to each of several crops (Kuhn's augmenting paths) |
| 8 | **Randomized QuickSort** | `src/algo/RandomizedQuickSort.java` | Ranking search results, diseases and related articles |

Nothing is outsourced to the Java library — no `String.contains()`, no `Collections.sort()`.

One small helper sits beside them: `src/engine/CompoundSplitter.java` splits joined words
(`lateblight` → `late blight`) with a word-segmentation DP. It is not one of the eight —
it exists because Edit Distance genuinely cannot repair a missing space.

The chat prints a dim trace line in the left gutter every time one of them runs, so the
algorithm usage is visible during the demo. Turn it off with `trace off`.

---

## The pipeline

```
        farmer's question
              |
   0. Small talk         "hi", "how are you", "thanks"  -> reply and stop
              |
   1. Edit Distance      fix the spelling ("tomatoe" -> "tomato")
      + word splitter    unstick joined words ("lateblight" -> "late blight")
              |
   1b. Aho-Corasick      synonym rewrite: "paddy" / "dhan" / "chawal" -> "rice"
              |
   2. Aho-Corasick       detect every crop / disease / fertilizer / symptom in ONE pass
              |
      intent routing     decide which feature answers
              |
   +----------+-------------------+---------------------+
   |          |                   |                     |
budget?    many crops?        symptoms?             anything else
   |          |                   |                     |
6. Knapsack  7. Bipartite      1. KMP over          2. Rabin-Karp  (score each document)
             Matching          symptom lists        1. KMP         (locate + snippet)
                               8. QuickSort         8. QuickSort   (rank)
                                                    5. Suffix Array+LCP (related articles)
```

---

## Project layout

```
FarmAssist/
├── FarmAssist.bat             double click this - opens a terminal, then runs
├── run.bat                     compile + run inside the terminal you already have
├── data/                       the knowledge base (edit these freely)
│   ├── crops.csv               63 crops (cereals, millets, pulses, oilseeds,
│   │                           vegetables, fruits, spices, plantation crops)
│   │                           each with season, soil, water need, N-P-K,
│   │                           diseases, duration, temperature, rainfall,
│   │                           spacing, varieties and yield
│   ├── diseases.csv            64 diseases with symptoms and treatment
│   ├── fertilizers.csv         38 fertilizers with NPK, price, benefit
│   ├── pests.csv               44 insect pests with damage and control
│   ├── articles.csv            62 searchable documents
│   ├── symptoms.csv            74 symptom keywords for Aho-Corasick
│   ├── synonyms.csv            396 local / alternate names -> our word
│   └── smalltalk.csv           greetings and everyday replies

The search index is 271 documents: the 62 articles plus one generated profile
document for every crop, disease, fertilizer and pest record.
└── src/
    ├── algo/                   THE 8 ALGORITHMS — pure, no project logic
    ├── model/                  Crop, Disease, Fertilizer, Pest, Article
    ├── engine/                 SpellCorrector, EntityExtractor, IntentDetector,
    │                           SearchEngine, Diagnoser, Planner, Recommender,
    │                           DataLoader
    ├── ui/                     ConsoleChat  the chat loop and every screen
    │                           Theme        the grid, palette, panels and tables
    │                           Centering    keeps the page in the middle
    │                           AlgorithmDemo
    └── util/                   Trace
```

All data files use `|` as the separator and `#` for comment lines. Add your own rows and
the program picks them up on the next run — no code change needed.

---

## Demo script (use this order in the viva)

| Type this | Shows |
|-----------|-------|
| `algo demo` | All 8 algorithms on tiny hand-checkable inputs |
| `hi` then `what can you do` | The everyday conversation layer |
| `paddy` | Synonym rewrite (paddy → rice) + the crop record being searchable |
| `my tomatoe has yelow leaves and brown spots` | Edit Distance → Aho-Corasick → KMP → QuickSort |
| `suggest fertilizer for tomato under 3000 rupees` | 0/1 Knapsack |
| `match fertilizers for rice cotton banana groundnut` | Bipartite Matching |
| `how to grow rice` | Rabin-Karp → KMP → QuickSort → Suffix Array + LCP |
| `late blight treatment in potato` | Diagnosis + document search together |
| `my brinjal has a pest` | Lists every disease and pest of the crop |
| `how to control whitefly` | Pest profile with damage and control |
| `compare rice and wheat` | Side by side crop comparison |
| `which crops suit low rainfall` | Crop planner by rainfall and climate |
| `what is dap` | Record lookup |
| `list crops` / `list pests` / `list fertilizers` / `list diseases` / `list articles` | The database |

---

## The terminal UI

Everything is drawn on one 78 column grid owned by `src/ui/Theme.java`, so panels,
tables, headings and cards all line up no matter which screen you are on. Colour is
24-bit with an automatic fall back to the classic 16 colours, and every glyph has an
ASCII twin.

| Type this | Does |
|-----------|------|
| `color on` / `color basic` / `color off` | full colour, 16 colours, or plain text |
| `ascii on` / `ascii off` | `+---+` borders instead of box drawing |
| `width 120` | tell Sophie how wide your window is |
| `center off` / `center on` | stop centring the page, or start again |
| `clear` | wipe the screen and redraw the masthead |
| `trace on` / `trace off` | show or hide the algorithm trace |

---

## Complexity summary

| Algorithm | Time | Space |
|---|---|---|
| KMP | O(n + m) | O(m) |
| Rabin–Karp | O(n + m) average | O(1) |
| Aho–Corasick | O(n + total pattern length + matches) | O(total pattern length) |
| Edit Distance | O(n·m) per word pair | O(n·m) |
| Suffix Array | O(n log²n) build, Kasai LCP O(n) | O(n) |
| 0/1 Knapsack | O(items × budget) | O(items × budget) |
| Bipartite Matching | O(V × E) | O(V + E) |
| Randomized QuickSort | O(n log n) expected | O(log n) |
