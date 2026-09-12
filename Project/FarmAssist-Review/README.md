# FarmAssist — Review 1 build

DSA-3 (25CS2103E) — Team 20
Nimma Lokesh Reddy (2520030366) · Ramagiri Rishik Rao (2520030333) · Manne Yashwanth Manoj (2520030369)

A deliberately small version of the project carrying **four algorithms only**:

| # | Algorithm | Where it runs |
|---|-----------|---------------|
| 1 | **Aho–Corasick** | one pass over the question finds every crop, symptom and disease named in it |
| 2 | **Edit distance** | every word the automaton could *not* match is measured against the keyword list, so `my tomatoe has yelow leves` still reaches the right disease |
| 3 | **KMP** | checks each symptom exactly against every disease record, and locates a term inside an article so the snippet can be cut around it |
| 4 | **Rabin–Karp** | rolling-hash count of how often each term appears in each article — that count is the relevance score |

Every one of the four is **written from scratch** in `src/algo/`. Nothing is
delegated to a library: there is no `String.indexOf`, no `contains`, no
`hashCode`, no regex engine doing the searching. The only library classes used
are containers — `ArrayList`, `HashMap`, and `LinkedList` standing in for a
queue while Aho–Corasick builds its failure links.

Nothing else from the full project is here. No synonyms, no Knapsack, no
matching, no sorting algorithm — the ranking is a plain insertion sort, marked
as such in the code, because it is not one of the four.

---

## Why edit distance was added

Aho–Corasick matches **exactly**. One slipped key and a whole question falls
through it:

```
you > my tomatoe has yelow leves and brown sopts
      Aho-Corasick: 0 hits            ← nothing recognised, no diagnosis
```

Edit distance is the repair. It counts the smallest number of single-character
edits — insert, delete, replace — that turns one word into another, and every
uncovered word in the question is measured against the keyword list. The
nearest keyword within budget replaces it, and the pipeline starts again on the
repaired sentence:

```
| DID YOU MEAN  edit distance against 55 keywords
    tomatoe > tomato   1 edit
    yelow   > yellow   1 edit
    leves   > leaves   1 edit
    sopts   > spots    2 edits
    reading it as: my tomato has yellow leaves and brown spots

| WHAT IT COULD BE
  1. EARLY BLIGHT   ██████████ 5
```

Three rules keep the corrections honest, so it repairs typos instead of
inventing them:

* **The budget grows with the word.** 1 edit up to 4 letters, 2 up to 7, 3
  beyond. One typo in a three-letter word is a far bigger change than one in a
  ten-letter word.
* **The first letter has to agree**, unless the word is a single edit away.
  Without this, `blite` gets "corrected" to `late` — two edits, and completely
  wrong. With it, `late blite in potato` is left alone rather than mangled into
  `late late in potato`.
* **A multi-word keyword also contributes its own words.** `brown spots` is
  stored whole *and* as `brown` and `spots`, so `sopts` can find `spots`;
  putting the single word back rebuilds the phrase the automaton then matches.

Only the previous row of the DP table is ever read, so two `int[]` rows are
kept instead of the whole `n × m` grid — O(min(n, m)) space.

---

## How to run

Double-click `run.bat`, or:

```
javac -encoding UTF-8 -d out (every .java under src)
java -Dstdout.encoding=UTF-8 -cp out app.Main data
```

Only a JDK is needed. Tested on JDK 25.

Try:

```
my tomato has brown spots and yellowing
rice has grey spots on the leaves
late blight in potato
how to grow wheat
my tomatoe has yelow leves      (typos on purpose)
```

Commands:

```
demo             runs all four algorithms on inputs small enough to check
spell <word>     the closest keyword to a word, and how far away it is
dist <a> <b>     the edit distance between any two words
trace on | off   the per algorithm trace line
color on | off   ANSI colour
ascii on | off   plain ASCII instead of box drawing characters
clear            clears the screen
help             the list above
exit             quits
```

The last two also work as command line switches, and `run.bat` passes anything
typed after it straight through:

```
run.bat --no-color      run.bat --ascii
```

---

## The console

Everything the program prints goes through `Ui.java`, which works out once what
the terminal can actually do and then keeps to it:

* **Colour** is used only when the output is a real terminal. Redirect the run
  into a file, or set `NO_COLOR`, and every escape disappears, so a saved
  transcript stays readable. `color off` does the same at any time, which is
  the switch to reach for if a very old console prints the escapes literally
  instead of obeying them.
* **Box drawing characters** are used only when the console is running in
  UTF-8. `run.bat` sets the code page to 65001 and starts Java with
  `-Dstdout.encoding=UTF-8`; if either does not take, `Ui` sees it in
  `stdout.encoding` and falls back to `+`, `-`, `|` and `#` rather than
  printing mojibake.
* **Nothing is measured with `String.length()`.** An escape sequence is several
  characters long and prints as none of them, so wrapping and padding both
  measure with `Ui.visibleLength`, and no line runs past 78 columns whether
  colour is on or off.

What that buys in the output itself:

* the question is echoed back with every Aho-Corasick hit painted in the colour
  of what it is, green crop, yellow symptom, red disease, so the automaton's
  work can be read straight off the sentence
* each result carries a bar showing its score against the best score in that
  list, and each section heading names the algorithm that ranked it
* in the article snippet, the term Rabin-Karp scored highest on, and KMP then
  located, is highlighted where it sits
* the trace lines stay dim and one to a line, so the shape of the pipeline is
  visible without drowning the answer

---

## Layout

```
FarmAssist-Review/
├── run.bat
├── data/
│   ├── keywords.txt      30 patterns for the Aho-Corasick automaton
│   ├── diseases.txt      8 diseases with symptoms and treatment
│   └── articles.txt      12 documents the search runs over
└── src/
    ├── algo/
    │   ├── KMP.java
    │   ├── RabinKarp.java
    │   ├── AhoCorasick.java
    │   └── EditDistance.java
    └── app/
        ├── Data.java     reads the three files, builds the automaton
        ├── Main.java     the question pipeline and what it shows
        └── Ui.java       colour, box drawing, wrapping, width
```

---

## Complexity

| Algorithm | Time | Space |
|---|---|---|
| KMP | O(n + m) | O(m) |
| Rabin–Karp | O(n + m) expected, O(n·m) worst case | O(1) |
| Aho–Corasick | O(n + M + z), M = total pattern length, z = matches | O(M) |
| Edit distance | O(n·m) per pair, O(V·n·m) over a vocabulary of V words | O(min(n, m)) — two rows, not the full table |

Rabin–Karp is only *expected* linear because equal hashes do not prove equal
strings — every hash hit is confirmed with a real character comparison in
`sameAt()`.

---

## When the user gets it wrong

Nothing a user can type ends the program. Every place a mistake can arrive is
wrapped, and each one prints a sentence and returns the prompt:

| What goes wrong | What happens |
|---|---|
| the data folder is missing or misspelled | each file is reported by name, and the program says how to pass the right folder |
| a data line has too few columns | that line is skipped with its line number, the rest of the file still loads |
| a data file is unreadable mid-run | `IOException` is caught in `readRows`, the other two files still load |
| `spell` with no word, `dist` with one word or three | a `usage` line with a worked example |
| a word longer than the 256-character DP limit | `EditDistance` throws `IllegalArgumentException`, `Main` catches it and prints the limit |
| a pasted question over 500 characters | refused before any algorithm runs |
| an algorithm throws on some question | caught around the whole pipeline — the answer is abandoned, the session is not |
| Ctrl+Z / Ctrl+D, or the console disappears | `NoSuchElementException` is caught and the loop exits cleanly |
| anything at all escaping the above | a `Throwable` catch in `main` prints one line instead of a stack trace |

The algorithms themselves reject bad arguments at the door rather than failing
somewhere deep inside a loop: `KMP` and `EditDistance` throw
`IllegalArgumentException` on a null, and `RabinKarp` and `AhoCorasick` return
an empty result list.

---

## Why the data files are `.txt` and not `.csv`

Both were considered. `.txt` won, for three reasons:

**1. The data already contains commas.** A symptom column reads
`brown spots, yellowing, stunted growth`. In a real CSV that field would have
to be wrapped in quotes, and the parser would then have to understand quoting,
escaped quotes and quoted newlines. With a `|` separator the parser stays one
line — `line.split("\\|")` — and the commas inside a field keep their natural
meaning as a list separator.

**2. Excel silently corrupts this data.** Double-clicking a `.csv` opens it in
Excel, and Excel reformats what it thinks it recognises. An NPK value like
`10-26-26` and a range like `400-600` are read as dates and written back
destroyed. Saving once is enough to ruin the file, and the damage is easy to
miss. A `.txt` file opens in Notepad and comes back exactly as it went in.

**3. The extension should not lie.** CSV means comma-separated. A file full of
`|` separators named `.csv` misleads anyone who opens it, and any tool that
tries to parse it properly will get it wrong.

Use `.csv` only if the data has to go into Excel or pandas *and* no field
contains a comma — neither is true here.

> Worth noting: the main project's `data/` folder has this exact mismatch —
> the files are named `.csv` but are pipe-separated. Renaming them to `.txt`
> would cost nothing and would stop Excel mangling the NPK and rainfall
> columns.

---

## Checked

The search algorithms were cross-checked against Java's own `String.indexOf`
over 4,000 randomly generated cases on a small alphabet, where overlapping
matches are common — `KMP.searchAll`, `KMP.search`, `RabinKarp.search` and
`RabinKarp.count` agree with it on every case. Edge cases (empty pattern,
pattern longer than text, pattern equal to text, overlapping `aaa` in `aaaa`)
and the Aho–Corasick word-boundary rule (`rice` must not match inside `price`,
`spot` must not match inside `spots`) were checked separately.

`EditDistance` keeps only two rows of the DP table, so it was cross-checked
against a full `n x m` table over 20,000 random pairs on a four letter
alphabet, together with the symmetry rule `distance(a, b) == distance(b, a)` —
they agree on every case. The textbook values come out right as well:
`kitten`/`sitting` = 3, `flaw`/`lawn` = 2, a word against itself = 0, a word
against the empty string = its own length. `demo` prints the first of those so
it can be checked by eye during the review.
