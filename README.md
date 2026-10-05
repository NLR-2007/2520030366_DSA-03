<div align="center">

# 📚 Data Structures & Algorithms (DSA-3) Coursework & Project Repository

**Course:** 25CS2103E - Data Structures and Algorithms III  
**Student:** Nimma Lokesh Reddy | **Register ID:** `2520030366` | **Section:** S-06 | **Batch:** 20  
**Institution:** KL University

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Algorithms](https://img.shields.io/badge/Core%20Algorithms-13%2B-green)](#-major-project-kisaan-krushi-ai--farmassist)
[![Course](https://img.shields.io/badge/Course-25CS2103E-blue)](#-student--course-details)

</div>

---

## 📌 Repository Overview

This repository contains the complete academic work for the **DSA-3 (25CS2103E)** course, including:
1. **Major Course Project — Kisaan Krushi AI (`FarmAssist`)**: A console-based intelligent agricultural search engine powered by 13 custom-implemented algorithms (no external ML frameworks).
2. **Practical Experiments (`Practical/`)**: Lab exercises focusing on text processing, string searching, and rolling-hash algorithms (Experiments 1–4).
3. **Skill & Practice (`Skill/`, `Programs/`)**: Practice problem solutions, frequency search, and utility scripts.

---

## 📁 Repository Structure

```text
2520030366_DSA-03/
├── 📂 Project/
│   └── 📂 FarmAssist/         # 🌾 Major Course Project: Kisaan Krushi AI (Terminal Assistant)
│       ├── 📂 src/             # Pure Java implementation (algo, engine, model, ui, util)
│       ├── 📂 data/            # Knowledge base (crops, diseases, pests, fertilizers, articles)
│       ├── 📂 lib/             # Minimal dependencies (JLine for terminal UI)
│       ├── 📜 FarmAssist.bat   # Double-click launcher (Windows)
│       ├── 📜 run.bat          # Command-line launcher
│       └── 📜 README.md        # Detailed FarmAssist documentation
├── 📂 Practical/               # 🧪 Lab Exercises & Practical Solutions
│   ├── 📜 CorpusLoader.java    # Expt 1: Data ingestion & storage
│   ├── 📜 QueryProcessor.java   # Expt 2: Keyword search across corpus
│   ├── 📜 PatternSearch.java    # Expt 3: Naïve & KMP pattern matching
│   ├── 📜 RabinKarpSearch.java  # Expt 4: Rolling-hash pattern matching
│   └── 📜 README.md            # Lab documentation & setup
├── 📂 Programs/                # 💻 Algorithmic practice scripts & implementations
└── 📂 Skill/                   # 🎯 Skill building tasks & exercises
```

---

## 🌾 Major Project: Kisaan Krushi AI (`FarmAssist`)

**Kisaan Krushi AI** is an interactive, algorithm-driven terminal assistant designed to help farmers answer agricultural queries, diagnose plant diseases, plan crop cultivation based on climate parameters, optimize fertilizer purchases within budgets, and fetch live weather advice.

> **Key Highlight:** Every response is calculated using **13 core classic algorithms** implemented entirely from scratch in pure Java without third-party ML libraries.

### ⚡ Quick Start (Running FarmAssist)

1. **Prerequisites:** JDK 21 or newer installed.
2. **Windows (Recommended):** Double-click `Project/FarmAssist/FarmAssist.bat` or run:
   ```cmd
   cd Project\FarmAssist
   FarmAssist.bat
   ```
3. **Terminal / Command Line:**
   ```cmd
   cd Project\FarmAssist
   run.bat
   ```

### 🧠 The 13 Core Algorithms Implemented

| # | Algorithm | Target Implementation File | Practical Application in Project |
|---|---|---|---|
| **1** | **KMP (Knuth-Morris-Pratt)** | [`KMP.java`](./Project/FarmAssist/src/algo/KMP.java) | Exact string matching inside disease symptoms & snippet extraction |
| **2** | **Rabin–Karp** | [`RabinKarp.java`](./Project/FarmAssist/src/algo/RabinKarp.java) | Rolling-hash multi-document scan to compute relevance scores |
| **3** | **Aho–Corasick** | [`AhoCorasick.java`](./Project/FarmAssist/src/algo/AhoCorasick.java) | Single-pass multi-pattern matching for entity extraction & local synonym mapping |
| **4** | **Edit Distance (Levenshtein)** | [`EditDistance.java`](./Project/FarmAssist/src/algo/EditDistance.java) | Typo correction & fuzzy term suggestion ("did you mean?") |
| **5** | **Suffix Array + LCP** | [`SuffixArrayLCP.java`](./Project/FarmAssist/src/algo/SuffixArrayLCP.java) | Longest common substring detection for related article discovery |
| **6** | **0/1 Knapsack (DP)** | [`Knapsack.java`](./Project/FarmAssist/src/algo/Knapsack.java) | Optimal fertilizer basket selection under strict price budget constraints |
| **7** | **Bipartite Matching (Kuhn's)** | [`BipartiteMatching.java`](./Project/FarmAssist/src/algo/BipartiteMatching.java) | Unique one-to-one allocation of fertilizers to distinct crops |
| **8** | **Randomized QuickSort** | [`RandomizedQuickSort.java`](./Project/FarmAssist/src/algo/RandomizedQuickSort.java) | Ranking search results, articles, and crop recommendations |
| **9** | **Bitmask DP** | [`BitmaskAssignment.java`](./Project/FarmAssist/src/algo/BitmaskAssignment.java) | Highest-benefit fertilizer assignment on crop subsets |
| **10** | **Max Flow + Min Cut (Edmonds-Karp)** | [`MaxFlow.java`](./Project/FarmAssist/src/algo/MaxFlow.java) | Multi-field stock distribution network flow & bottleneck identification |
| **11** | **Greedy Set Cover** | [`SetCoverGreedy.java`](./Project/FarmAssist/src/algo/SetCoverGreedy.java) | Minimal set of fertilizers covering all farm crop requirements |
| **12** | **Greedy Knapsack (½-Approx)** | [`KnapsackGreedy.java`](./Project/FarmAssist/src/algo/KnapsackGreedy.java) | Fast approximation comparison against exact 0/1 Knapsack DP |
| **13** | **Parallel Map & Reduce** | [`ParallelPrimitives.java`](./Project/FarmAssist/src/algo/ParallelPrimitives.java) | Multi-threaded table scoring and data filtering for crop planning |

---

## 🧪 Practical Experiments Summary

| Experiment | Title | Description | Code Link |
| :--- | :--- | :--- | :--- |
| **Experiment 1** | Corpus Loader | Reads articles from raw corpus & populates dataset structures | [`CorpusLoader.java`](./Practical/CorpusLoader.java) |
| **Experiment 2** | Query Processor | Keyword search engine executing multi-document queries | [`QueryProcessor.java`](./Practical/QueryProcessor.java) |
| **Experiment 3** | Pattern Search | Substring matching comparisons (Naïve search vs. KMP) | [`PatternSearch.java`](./Practical/PatternSearch.java) |
| **Experiment 4** | Rabin-Karp Search | Rolling-hash pattern matching algorithm implementation | [`RabinKarpSearch.java`](./Practical/RabinKarpSearch.java) |

---

## 👤 Student & Course Details

- **Student Name:** Nimma Lokesh Reddy
- **Register ID:** `2520030366`
- **Course Code:** 25CS2103E (DSA-3)
- **Section:** S-06
- **Batch:** 20
- **GitHub Repository:** [https://github.com/NLR-2007/2520030366_DSA-03.git](https://github.com/NLR-2007/2520030366_DSA-03.git)

---

## 📜 Maintenance Note

> Binary files (PDF handouts, DOCX reports, PPTX presentations, temporary Office files `~$*`, and local cache directories `.hit/`) have been removed from Git tracking and added to `.gitignore` to maintain a clean, lightweight repository structure.
