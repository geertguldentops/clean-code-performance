# clean-code-performance

## Introduction

A Java 25 / JMH microbenchmark suite that reproduces (as closely as the JVM
allows) the comparisons from the article [*"Clean" Code, Horrible
Performance*](https://www.computerenhance.com/p/clean-code-horrible-performance) (the C++ shape-area example): polymorphism vs switch statements
vs table-driven code, and how the performance gap changes as more properties
are added to the data model.

This project does **not** try to prove or disprove the article's specific
numbers - it gives you a working, reproducible harness to measure the
*same class of effect* on the JVM, with its own caveats clearly called out
below.


## Key Insight

Clean code principles optimize for human comprehension, while data-oriented design optimizes for machine execution. 
When performance is critical, machine-oriented design often wins.

For most Java enterprise systems, maintainability is the bottleneck. 
For performance-critical inner loops, the article contains useful warnings.


## Requirements

- Nothing but a JDK. The project ships a Maven Wrapper (`mvnw`/`mvnw.cmd`),
  so you do not need Maven installed separately.
- Java 25 (the wrapper doesn't provide a JDK - make sure `java -version`
  reports 25 before running).

## Build

```bash
./mvnw clean package
```

This produces a self-contained, runnable jar at `target/benchmarks.jar`
(JMH's `Main` class as entry point).

## Run

**Always run benchmarks from the shaded jar, not `mvn test`/`mvn exec`** -
JMH needs to run as a plain, isolated `java` process (with its own forked
JVMs per the `@Fork` annotations) to produce valid measurements.

```bash
# List all discovered benchmarks
java -jar target/benchmarks.jar -l

# Run everything (this takes a while - see "Runtime expectations" below)
java -jar target/benchmarks.jar

# Run just one class
java -jar target/benchmarks.jar AreaBenchmark
java -jar target/benchmarks.jar CornerAreaBenchmark
java -jar target/benchmarks.jar ColdStartBenchmark

# Run a quick smoke test (few iterations, 1 shape count, 1 fork)
java -jar target/benchmarks.jar AreaBenchmark -p shapeCount=1000 -wi 1 -i 1 -f 1 -w 200ms -r 200ms
```

Unit tests (correctness checks, not benchmarks) run via:

```bash
./mvnw test
```

## What's implemented, and which article listing it maps to

| Representation | Article listings | Class(es) |
|---|---|---|
| Polymorphic hierarchy (`virtual Area()`/`CornerCount()`) | 22, 23, 24, 32 | `shapes.poly.*` |
| Flattened "union" struct + `switch`, array-of-objects | 25, 26, 34, 35 | `shapes.union.*` |
| Struct-of-arrays (parallel primitive arrays) + `switch` | same logic as above, flattened memory | `shapes.soa.ShapeSoAOps.getAreaSwitch/getCornerCountSwitch` |
| Table-driven (struct-of-arrays + coefficient table) | 27, 36 | `shapes.soa.ShapeTables`, `ShapeSoAOps.getAreaTable/getCornerAreaTable` |
| SIMD (Java Vector API) - AVX analog | the article's "lightly optimized AVX version" | `shapes.vector.VectorAreaOps` |

Benchmarks:

- `AreaBenchmark` - total area of N shapes (Listings 22-27), including a
  4-way manually unrolled accumulator variant to remove the loop-carried
  dependency, exactly as the article does.
- `CornerAreaBenchmark` - adds a "corner count" property and computes
  `sum(1/(1+cornerCount) * area)` (Listings 32-36), testing the article's
  claim that the "clean" vs table-driven performance gap *grows* as more
  properties are added. Note that `cornerArea_tableDriven_soa` and
  `cornerArea_vectorApi_soa` reuse the *exact same code* as their
  `AreaBenchmark` counterparts - only the coefficient table differs, which is
  precisely the article's point about table-driven code.
- `ColdStartBenchmark` - best-effort analog of the article's "cold" (first,
  untrained) measurement, see caveats below.

All representations are built from **one shared, seeded random dataset**
(`data.ShapeDataFactory`, default seed `42`), so every benchmark variant
operates on identical shape data. `ShapeRepresentationsAgreeTest` asserts
all representations produce the same numeric result for the same seed, so
a bug in one implementation can't silently produce misleading benchmark
numbers (e.g. from dead-code elimination).

## Interpreting results

JMH reports **ns/op** (nanoseconds per benchmark invocation, i.e. per full
loop over `shapeCount` shapes). To get an approximate per-shape figure
comparable to the article's "cycles per shape":

```
ns_per_shape = ns_per_op / shapeCount
cycles_per_shape ≈ ns_per_shape * (your CPU's clock GHz)
```

This is only an approximation - JMH doesn't have access to a hardware cycle
counter, and modern CPUs run at variable frequency, so treat the resulting
"cycles" number as illustrative, not exact.

## Caveats (please read before drawing conclusions)

- **JIT warm-up vs CPU cold cache.** The article's "cold" measurement
  deliberately flushes L1/L2 caches and an untrained branch predictor, then
  times a single run. The JVM has no equivalent lever. `ColdStartBenchmark`
  approximates this with `SingleShotTime` + zero warmup + a fresh JVM fork
  per measurement, but a "cold" JVM fork is cold with respect to **JIT
  compilation and class loading** - the first invocation runs interpreted
  (or minimally C1-compiled) - which is a fundamentally different kind of
  "cold" than the article's CPU-cache-cold, fully-JIT-optimized-code
  scenario. Treat `ColdStartBenchmark` results as directional only; the
  steady-state benchmarks (`AreaBenchmark`/`CornerAreaBenchmark`) are the
  primary evidence here.
- **Java has no true value-type arrays (pre-Valhalla).** An array of
  `ShapeUnion` objects (the literal 1:1 port of the article's flattened
  struct) is still an array of *references* in Java - it does not get the
  "no indirection, fully contiguous memory" benefit the article measures for
  its C++ struct array. To make the comparison meaningful, this suite adds a
  **struct-of-arrays (SoA)** representation (parallel `int[]`/`float[]`
  arrays) as the idiomatic Java way to get a truly flattened, contiguous
  layout. Compare `totalArea_switch_aos` (reference-indirected, closer to a
  naive port) against `totalArea_switch_soa` (flattened) to see this
  Java-specific effect in isolation.
- **GC and JIT variability.** Results can be affected by GC pauses,
  tiered-compilation timing, and background system load. Run with enough
  forks/iterations for real conclusions (`@Fork(2)`, several warmup and
  measurement iterations are already configured as defaults), and prefer a
  quiet machine.
- **Vector API is still incubating in JDK 25** (`jdk.incubator.vector`).
  Compiling/running code that uses it requires
  `--add-modules jdk.incubator.vector`, which is already wired into
  `pom.xml` (compiler args) and into each benchmark's `@Fork(jvmArgsAppend=...)`,
  so `java -jar target/benchmarks.jar` works out of the box. The API and its
  performance characteristics may still change in future JDK releases.
- **Result magnitude, not absolute numbers, is the point.** Different
  machines, JDK builds, and background load will all shift absolute ns/op
  values. What's interesting is the *relative* ordering and ratio between
  polymorphism / switch / table-driven / vector variants, which is what the
  article's argument is actually about.

## Runtime expectations

The default JMH settings (`@Warmup(3)`, `@Measurement(5)`, `@Fork(2)`) times
3 shape-count parameters (`1,000` / `100,000` / `10,000,000`) times ~7-8
benchmark methods per class add up. Running the *entire* suite
(`java -jar target/benchmarks.jar`) can take significant time (tens of
minutes), dominated by the `10,000,000`-shape parameter. For quick
iteration, filter to one class/method and override `-wi`/`-i`/`-f`/`-w`/`-r`
as shown in the "Run" section above, or restrict to smaller shape counts
with `-p shapeCount=1000,100000`.
