# Architecture & Engineering Guidelines

This project prioritizes **simplicity, readability, and testability** above all else. 
Cleverness is discouraged. Code should read like well-written prose.

---

## 1. Core Principles

* **Simplicity First:** Write the least amount of code needed to satisfy requirements. Avoid premature abstraction, deep inheritance trees, or speculative design for future needs.
* **Functional Core, Imperative Shell:** 
  * Keep core business logic inside **pure functions** (no side effects, deterministic output for given inputs).
  * Push side effects (file I/O, database access, network calls, randomness, system time) to the outer edges of the application.
* **Immutability by Default:** Prefer persistent/immutable data structures. Functions should transform data into new structures rather than mutating inputs.
* **Explicit Over Implicit:** Prefer explicit arguments, explicit return types, and flat control flows over magic decorators, hidden state, or complex middleware loops.

---

## 2. Code Organization & Patterns

### Data Structures
* Use plain, transparent data containers (e.g., Records, Dataclasses, Type aliases, or Interfaces).
* Separate data structure definitions from the functions that operate on them.

### Function Design
* **Single Responsibility:** A function does one thing and does it predictably.
* **Signatures:** Keep parameter lists small (preferably 1–3 parameters). Group related parameters into a single typed data structure if growing beyond 3.
* **Errors:** Treat errors as domain values where possible (e.g., `Result<T, E>` patterns or explicit return tuples) rather than relying on deep, uncaught exception chains for expected failure paths.

### Dependency Handling
* Pass dependencies explicitly as function arguments (Dependency Injection).
* Do not rely on global singletons, shared mutable state, or implicit environment contexts within core domain functions.

---

## 3. Testing Rules

* **Direct Unit Testing:** Core functional logic must be tested directly via standard inputs and expected outputs without requiring mocks or stubs.
* **Mock Boundaries:** Only mock/stub the absolute boundary interfaces (e.g., external API client, filesystem writer) at the edge shell. Never mock pure domain logic.

---

## 4. What to Avoid (Anti-Patterns)

❌ **Do Not:** Create class hierarchies for simple data transformations.  
❌ **Do Not:** Mutate input parameters or global arrays/maps inside functions.  
❌ **Do Not:** Mix file reading/fetching and business logic in the same function.  
❌ **Do Not:** Write abstractions until you have at least 3 distinct instances of repetition.