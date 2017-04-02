Karballo Chess Engine
=====================

Karballo is a Kotlin version of the Carballo Chess Engine, a Java chess engine (http://github.com/albertoruibal/carballo).

Karballo has only a command line UCI interface that can be used in chess interfaces like SCID or Arena.

The source code is hosted at https://github.com/albertoruibal/karballo with a MIT license, you are free to use,
distribute or modify the code.

Features
========

* Based on bitboards with a magic bitboard move generator, it also includes code for magic number generation
* Move iterator sorting moves with four killer move slots, Static Exchange Evaluation (SEE), Most Valuable Victim/Least Valuable Aggressor (MVV/LVA) and history heuristic
* PVS searcher
* Aspiration window, moves only one border of the window if it falls out
* Transposition Table (TT) with zobrist keys (it uses two zobrist keys per board to avoid collisions) and multiprobe
* Quiescent Search (QS) with only good or equal captures (according to SEE) and limited check generation
* Internal Iterative Deepening to improve sorting
* Extensions: Check (only with positive SEE), pawn push, mate threat and singular move
* Reductions: Late Move Reductions (LMR)
* Pruning: Null move pruning, static null move pruning, futility pruning and history pruning
* Pluggable evaluator function, distinct functions provided: the Simplified Evaluator Function, other Complete and other Experimental
* Selectable ELO level with an UCI parameter
* Supports Chess960
* Polyglot opening book support; in the code it includes Fruit's Small Book
* FEN notation import/export support, also EPD support for testing
* JUnit used for testing, multiple test suites provided (Perft, BS2830, BT2630, LCTII, WinAtChess, etc.)

Building
========

Carballo uses the Gradle build system with a gradle wrapper.

Build the UCI interface creating a karballo.jar in the project root:
```
./gradlew clean proguard
```

Running
=======

```
java -jar karballo.jar
```

Authors
=======

* Alberto Alonso Ruibal: http://www.alonsoruibal.com
