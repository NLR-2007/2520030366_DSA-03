# 🧪 DSA Practical Experiments (TextHack System)

> **Student Details:**  
> **Name:** Nimma Lokesh Reddy | **ID:** 2520030366 | **Section:** S-06 | **Batch:** 20

This folder contains the complete Java implementations and solution files for the **TextHack** Article Processing and Keyword Retrieval System (Practical Experiments 1 to 4).

---

## 📋 Table of Experiments

| Experiment | Title | Core Algorithm / Concept | Java File | Solution File |
| :--- | :--- | :--- | :--- | :--- |
| **Experiment 1** | Corpus Loader | File I/O, Data Structs, Repository Storage | [`CorpusLoader.java`](./CorpusLoader.java) | [`DSA Expt-1 Solution.txt`](./DSA%20Expt-1%20Solution.txt) |
| **Experiment 2** | Query Retrieval System | Keyword Searching, Text Processing | [`QueryProcessor.java`](./QueryProcessor.java) | [`DSA Expt-2 Solution.txt`](./DSA%20Expt-2%20Solution.txt) |
| **Experiment 3** | Pattern Search | Naïve Pattern Matching & KMP Algorithm | [`PatternSearch.java`](./PatternSearch.java) | [`DSA Expt-3 Solution.txt`](./DSA%20Expt-3%20Solution.txt) |
| **Experiment 4** | Rabin-Karp Search | Rabin-Karp Hashing Algorithm | [`RabinKarpSearch.java`](./RabinKarpSearch.java) | [`DSA Expt-4 Solution.txt`](./DSA%20Expt-4%20Solution.txt) |

---

## 🚀 Experiment Details & Overview

### 🔹 Experiment 1: Corpus Loader
- **Objective:** Design and implement a Corpus Loader that reads multiple text documents (`a1.txt`, `a2.txt`, etc.) from disk and populates an in-memory `Article Repository` containing Article ID, Title, Content, and Word Count.
- **Key Concepts:** Object-Oriented Design (`Article` class), File I/O (`BufferedReader`), String tokenization.
- **Source Files:** [`CorpusLoader.java`](./CorpusLoader.java) \| [`DSA Expt-1 Solution.txt`](./DSA%20Expt-1%20Solution.txt)

---

### 🔹 Experiment 2: Query Retrieval System
- **Objective:** Implement a Query Processor that searches articles in the repository based on a user-provided keyword and displays matching articles.
- **Key Concepts:** Substring searching, Case-insensitive match, Repositories.
- **Source Files:** [`QueryProcessor.java`](./QueryProcessor.java) \| [`DSA Expt-2 Solution.txt`](./DSA%20Expt-2%20Solution.txt)

---

### 🔹 Experiment 3: Naïve Pattern Matching & KMP Algorithm
- **Objective:** Implement and compare Naïve Pattern Matching and the Knuth-Morris-Pratt (KMP) pattern searching algorithm for string retrieval.
- **Key Concepts:** 
  - **Naïve Algorithm:** Sliding window comparison $O((N-M+1) \times M)$.
  - **KMP Algorithm:** Prefix function / Longest Prefix Suffix (LPS) table computation for $O(N + M)$ search efficiency.
- **Source Files:** [`PatternSearch.java`](./PatternSearch.java) \| [`DSA Expt-3 Solution.txt`](./DSA%20Expt-3%20Solution.txt)

---

### 🔹 Experiment 4: Rabin-Karp Search Algorithm
- **Objective:** Implement the Rabin-Karp algorithm using rolling hash matching to identify pattern occurrences across documents efficiently.
- **Key Concepts:** Hashing, Rolling Hash ($O(1)$ state updates), Modular Arithmetic, Collision resolution.
- **Source Files:** [`RabinKarpSearch.java`](./RabinKarpSearch.java) \| [`DSA Expt-4 Solution.txt`](./DSA%20Expt-4%20Solution.txt)

---

## 💻 How to Run the Experiments

### Prerequisites
Ensure Java JDK (version 8 or higher) is installed.

### Compilation
Compile all Java experiment files:
```bash
javac CorpusLoader.java QueryProcessor.java PatternSearch.java RabinKarpSearch.java
```

### Execution
Run any experiment:
```bash
# Experiment 1
java CorpusLoader

# Experiment 2
java QueryProcessor

# Experiment 3
java PatternSearch

# Experiment 4
java RabinKarpSearch
```
