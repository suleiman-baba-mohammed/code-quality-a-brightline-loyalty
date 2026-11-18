# Analysis: Brightline Loyalty points service


> **How the evidence was gathered.** I inspected the files, then ran the unchanged
> `PointsCalculator` to confirm each claim below
> that is about behaviour. Outputs are quoted where I use them. Claims about what a test "would
> let through" come from reading the test against the code.


Standards cited are of the AmaliTech Coding Standards: Java Edition. Java source-file formatting follows the Google Java Style Guide, which
that standard adopts.

---

## 1. What the code does today

`PointsCalculator.calculatePoints(memberId, amount)` looks the member up, takes `floor(amount / 10)`
as base points, multiplies by a tier factor (STANDARD ×1, SILVER ×1.5, GOLD ×2), adds a flat 50 when
`amount > 500`, prints a line and returns the result. Any exception returns `0`.

Observed on the unchanged code:

| Call | Result | Comment |
|---|---|---|
| `m1`, 100 (STANDARD) | 10 | correct |
| `m2`, 250 (GOLD) | 50 | correct |
| `m3`, 200 (SILVER) | 30 | correct |
| `m3`, 250 (SILVER) | 37 | base 25 × 1.5 = 37.5, silently truncated; no rule says so |
| `m1`, 500 / 500.01 | 50 / 100 | bonus is strictly `> 500` |
| **`unknown`, 100** | **0** | member not found, reported as "earned nothing" |
| **`null` id, 100** | **0** | same |
| **`m1`, −100** | **−10** | a malformed purchase *deducts* points |
| **`m1`, NaN** | **0** | garbage in, silent zero out |
| **`m1`, 1e12** | **−2147483599** | `int` overflow: a huge purchase yields a hugely negative balance |

## 2. Problems in the code

Ranked, most serious first.

### 2.1 The catch-all that returns 0 swallows every failure — **the most serious problem**
`catch (Exception e) { return 0; }` wraps the whole method body.
*Breaks: Error handling & observability ("no empty catch blocks, swallowed exceptions"); Correctness & robustness.*

* An unknown member throws a `NullPointerException` (the repository returns `null`), which is caught and turned into `0`.
  **`0` is also a legitimate answer** (a STANDARD member spending GHS 9.99). The caller cannot tell "earned nothing" from "we could not work out the answer". The demo's third line shows this.
* It hides *every other* bug too: a `null` tier, a repository outage, a future arithmetic error all become `0`. Nothing is logged, nothing is rethrown, and no one finds out.
* It is money-adjacent: members are silently short-changed, and support has nothing to investigate.
* It is the reason the AI's tests could never fail (see §3): the production code was built so that nothing ever fails.

This is the most serious problem because it is both a wrong-result bug and a *detection* bug: it turns errors into plausible-looking data, so no test, log or alert can catch it afterwards.

### 2.2 No input validation: negative, `NaN`, overflow and null inputs produce wrong points
*Breaks: Security & data protection ("inputs validated"); Correctness & robustness.*
Verified above: −100 gives −10, `NaN` gives 0, 1e12 overflows to a negative number. These are the "malformed purchase" the brief mentions, and nothing checks for them. Negative points on a bad input is a data-integrity bug, not a style issue.

### 2.3 Hard-wired dependency: `private final MemberRepository repository = new InMemoryMemberRepository();`
*Breaks: Structure & architecture; Dependency Inversion (SOLID-D); makes Test hygiene impossible.*
The class depends on a concrete implementation it creates itself, although an interface (`MemberRepository`) already exists. Callers and tests cannot supply another repository, cannot simulate "member not found" or "database down", and are tied to the seeded ids `m1`/`m2`/`m3`. The comment in the code admits it ("Convenient, but …"). This is what makes a meaningful unit test impossible.

### 2.4 The tier rules are an `if / else if` chain on strings: closed to the coming change
*Breaks: Open/Closed (SOLID-O); Readability & naming (magic strings); Correctness & robustness.*
* A new membership tier means editing `calculatePoints` again, which is exactly the "rewrite what is there" the brief wants to avoid.
* The chain has **no final `else`**: an unrecognised tier (a new one nobody wired up, or a typo) silently earns base points with no multiplier and no error. The next tier would go wrong *quietly*.
* `member.getTier().equals(...)` throws `NullPointerException` for a `null` tier (and 2.1 then hides it).
* Tier is a free `String` in a comment-documented set (`"STANDARD", "SILVER", "GOLD"`): stringly-typed, so the compiler cannot help.

### 2.5 One method, many jobs: Single Responsibility violation (god method)
*Breaks: Structure & architecture ("no god-classes … separation of concerns"); SOLID-S.*
`calculatePoints` does member lookup, base-points arithmetic, tier rule selection, bonus rule, logging, *and* the error policy. Each has a different reason to change, and none can be tested alone.

### 2.6 Money as `double`, with unnamed rounding
*Breaks: Correctness & robustness.* `double` is the wrong type for currency (binary fractions, `NaN`/`Infinity` allowed). Points are produced by two silent truncations (`(int)(amount / 10)`, then `(int)(points * 1.5)`) that no one decided on or documented. The threshold comparison `amount > 500` is a floating-point comparison.

### 2.7 Debug output in business logic, containing personal data
*Breaks: Error handling & observability ("no System.out debugging"); Security & data protection ("no sensitive data in logs").*
`System.out.println("Awarded … to " + member.getName())`: console output instead of a logger, and the member's *name* (personal data) rather than an id. Calling the class now has a side effect on stdout.

### 2.8 Magic values and dead arithmetic
*Breaks: Configuration & dependency hygiene ("no … magic values").* `10`, `1.5`, `2`, `500`, `50` and the tier strings are unnamed literals scattered through the method. `points = points * 1` is a no-op written out for symmetry.

### 2.9 `MemberRepository.findById` returns `null` for "absent"
*Breaks: Correctness & robustness; API & database hygiene (sound data access contract).* A `null` return is an undeclared failure mode that the caller must remember to check, and this caller did not. `Optional` makes absence part of the signature.

### 2.10 `Member` and the repository are loose
`Member` accepts `null`/blank id, name and tier, so an invalid member can exist and blow up later. `InMemoryMemberRepository` seeds its own data inside the constructor, so it cannot be built with other fixtures. (*Correctness & robustness; Structure & architecture.*)

### 2.11 Formatting and documentation
*Breaks: Readability & naming; Documentation & completeness.* Source uses 4-space indentation where Google Java Style requires 2; no Javadoc on public types; `PointsCalculatorTest` uses wildcard static imports (Google Style 3.3.1). The README is a generic lab note with no description of the rules, the design, or the failure behaviour. There is no CI, so "green" is only ever "green on one machine".
*(Build, delivery & version control.)*

---

## 3. The AI-generated tests: which prove nothing

`PointsCalculatorTest` has three tests and all three are hollow. Ranked, worst first.

### T2 `calculatesGoldPoints`: tests a mock of the class under test
```java
PointsCalculator calculator = mock(PointsCalculator.class);
when(calculator.calculatePoints("m2", 250.0)).thenReturn(50);
assertEquals(50, calculator.calculatePoints("m2", 250.0));
```
It asserts that Mockito returns what Mockito was told to return. **No production code runs.** It would stay green if `calculatePoints` were deleted or returned `-1`. Its name claims the GOLD multiplier is checked, and it is not. The worst kind of test, because it manufactures confidence. *(Test hygiene: tests must be "meaningful"; the standard's own phrase for this is an assertion-free/non-asserting test.)*

### T3 `awardsPointsForAPurchase`: `assertNotNull(int)`
`calculatePoints` returns a primitive `int`; autoboxing means `assertNotNull` can **never** fail. It is a tautology. It also leaves SILVER's result (30) unchecked and passes if the answer is `0`, `-5` or `999999`.

### T1 `awardsPointsToAMember`: `assertTrue(points > 0)`
A weak assertion: any positive number passes, so this accepts 10 (correct) and also 11, 100 or 1000. *(Predicted)* PIT would let these mutants survive: `/ 10` → `* 10`, `* 1` → `* 2`, `> 500` → `>= 500`, removing the bonus, changing `+ 50` to `+ 51`. It also silently depends on the seeded data (`m1`) of a repository the test did not choose.

### What the suite does not test at all
* Both bonus boundaries (500 / 500.01), the bonus amount, GOLD, truncation of SILVER;
* the not-found path, a `null` member, malformed amounts (negative, zero, `NaN`);
* any failure at all (the catch block is never reached);
* any isolation: every test uses the real, seeded repository.

Because T1 and T3 *do* execute the STANDARD and SILVER lines, a coverage report would look healthy while GOLD, the bonus and the catch are never exercised. High line coverage and zero verification at the same time: a tidy illustration of why coverage alone proves nothing.

**Ranking across code and tests.** The swallowed exception (§2.1) is the most serious defect in the *code*; the mock-of-the-SUT test (T2) is the most serious defect in the *tests*. They compound each other: the code can never report an error, and the tests can never detect one. Fixing only one would leave the other hiding it.

---

## 4. Plan (what I will change)

1. Characterise the legacy behaviour with tests (a safety net, including the known bugs, pinned and labelled).
2. Inject the repository through the constructor, with a single composition root.
3. Replace the string `if` chain with a `MembershipTier` enum and one rule object per tier.
4. Extract base-points and bonus rules, use `BigDecimal` for money.
5. Custom exceptions, `Optional` repository, validation, logging; remove the swallow. This is a deliberate behaviour change.
6. Replace the three hollow tests with a real JUnit 5 + Mockito suite.
7. Improvement: enforce the safety net in the build (mutation threshold, style check, CI).
8. Write the Decisions section.

---
