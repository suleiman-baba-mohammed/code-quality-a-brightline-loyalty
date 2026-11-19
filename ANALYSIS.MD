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


# Decisions

Everything above was written before any code changed. This section was written last.

## 1. The design in one picture

```
Demo ──► LoyaltyConfig (composition root: the only place that says "new Concrete…")
              │ builds
              ▼
        PointsCalculator ──────────────►  MemberRepository ─► InMemoryMemberRepository
              │  (validate, look up, combine)   (Optional<Member>)
              ▼
         PointsRules  (a record that groups the three rule collaborators)
              ├──► BasePointsPolicy ─► SpendBasedBasePointsPolicy   (1 point per whole GHS 10)
              ├──► TierRuleRegistry ─► TierPointsRule ─► MultiplierTierRule  (x1 / x1.5 / x2)
              └──► List<BonusRule>  ─► LargePurchaseBonusRule        (+50 above GHS 500, flat)
```

Packages (dependencies point one way, no cycles): `member` ← `exception` ← `purchase` ← `rules` ← the root package
(`PointsCalculator`, `LoyaltyConfig`, `Demo`).

| Class | The one thing it does |
|---|---|
| `PointsCalculator` | validates the request, finds the member, combines base + tier + bonuses |
| `PointsRules` | groups the base policy, tier rules and bonus rules (so the calculator takes two arguments) |
| `SpendBasedBasePointsPolicy` | turns an amount into base points |
| `TierPointsRule` / `MultiplierTierRule` / `TierRuleRegistry` | one rule per tier, and finding it |
| `BonusRule` / `LargePurchaseBonusRule` | extra points that do not depend on the tier |
| `Purchase`, `Member` | valid-by-construction values |
| `LoyaltyConfig` | wiring, and the *data* of the scheme (named thresholds and multipliers) |
| `LoyaltyException` + 4 subclasses | the ways a request can legitimately fail |

## 2. Key decisions and why

**Constructor injection plus one composition root** (ANALYSIS 2.3). The calculator takes its two collaborators
(the repository and a `PointsRules` record) as parameters and null-checks them. Nothing but `LoyaltyConfig` knows concrete classes. That is what let me test the
orchestration with mocks. Not a DI framework: for one service it would add a dependency to
solve a problem a constructor already solves.

**Strategy per tier, not a `switch`** (2.4). Each tier's rule is an object behind `TierPointsRule`. I made the
tier an `enum` (typos and `null` impossible) and the registry fail loudly with `UnsupportedTierException` for a tier
without a rule, which removes the old silent "unknown tier earns base points" bug.

**Bonus is a separate list of rules, not part of the tier rule.** In the original the bonus is the same for every
tier and is added *after* the multiplier (GOLD 600 → 60 × 2 + 50 = 170, not (60 + 50) × 2). Keeping it outside the
tier rule preserves that, and lets a new bonus campaign be added without touching any tier.

**`BigDecimal` and explicit rounding** (2.6). Both truncations are now named, tested decisions (`RoundingMode.DOWN`),
and the `> 500` test is an exact `compareTo`, so `500`, `500.0` and `500.00` behave the same (a test pins it).

**Unchecked exceptions, one base type** (2.1). `LoyaltyException` is unchecked because the caller cannot repair
any of these at the call site; a shared base lets an HTTP layer map "anything the service rejected" in one
`catch`, while subclasses let it distinguish 404-like (`MemberNotFound`) from 400-like (`InvalidPurchase`) from 5xx-like
(`MemberLookup`, `UnsupportedTier`).

**Where there is a `try/catch`, and where deliberately not.** Exactly two places:
1. around the repository call: a store failure (any `RuntimeException`) is translated to `MemberLookupException`
   with the **cause preserved** (the call is wrapped on its own, in a small method, so nothing else is caught by accident);
2. around the arithmetic: `int` overflow (`ArithmeticException`) becomes `InvalidPurchaseException` with the cause,
   instead of the old silent negative balance.

Nothing catches and returns a default. Nothing logs *and* rethrows (that double-reports); the failure carries its
context in the exception and the outermost caller (here `Demo`) decides what to do with it.

**Validation at the edge, and invalid objects cannot exist.** `Purchase` rejects `null`, zero and negative amounts in its
constructor, `Member` rejects blank ids/names and a `null` tier, and `PointsCalculator` rejects blank ids before
touching the store (a test uses `verifyNoInteractions`). Zero and negative amounts are invalid (business rule: a GHS 0 purchase is not a purchase).

**No logging inside the calculator** (standard 4.4: a method should have no side effects). The first version logged
"Awarded N points" on success; the standard says a method that calculates should not also write output, and failures
already carry their context in the exception. The only output left is in `Demo`, a console runner whose job is to
print, and its output stream is injected so a test can assert exactly what it prints.

## 3. How the design absorbs the coming rule change (a new membership tier)

To add, say, `PLATINUM`:

1. add the constant to `MembershipTier`;
2. write its rule: `new MultiplierTierRule(PLATINUM, new BigDecimal("3"))` if it is a multiplier, or a new class
   implementing `TierPointsRule` if its rules are different;
3. add one line to `LoyaltyConfig.defaultTierRules()`;
4. add rows to the `LoyaltyConfigTest` table.

`PointsCalculator`, `TierRuleRegistry` and the existing rules are **not edited**. Two things make this safe rather than
merely possible:
* **`TierExtensionTest`** plugs in a rule that is *not* a multiplier and shows the calculator, bonus and the other tiers
  behave unchanged.
* **`everyMembershipTierHasARuleInTheDefaultConfiguration`** (`@EnumSource`) fails the build if step 3 is forgotten;
  at runtime the same mistake is an `UnsupportedTierException`, never a silent zero.

*Honest limits.* Step 1 and 3 are one-line edits to existing files; that is data and wiring, not logic, but it is not
literally zero edits. And `TierPointsRule.apply(int basePoints)` sees only the base points. If the new tier needs a rule
based on the purchase itself (say, "double points on purchases over GHS 200"), the interface would need to widen to
receive the `Purchase`. I did not do that speculatively; it would be a small, compiler-guided change.

## 4. What I kept, fixed or rewrote from the AI

| AI artefact | Verdict | Why |
|---|---|---|
| Points formula and tier values (x1 / x1.5 / x2, 1 per GHS 10, +50 above 500) | **Kept**, behaviour identical | These are the business rules; I pinned them with characterization tests *before* refactoring, and the same expected values are in the final suite. |
| `MemberRepository` interface | **Fixed** | Good seam, wrong contract: `null` for absent became `Optional`. |
| `InMemoryMemberRepository` | **Fixed** | Seed data moved out of the constructor; duplicate ids rejected; `sample()` keeps the demo data. |
| `Member` | **Fixed** | Now a validated record with an enum tier instead of a free `String`. |
| `Demo` | **Kept, extended** | Still the quick way to see it run; now also shows a rejection instead of a misleading 0. |
| `PointsCalculator` | **Rewritten** | One method doing six jobs with a swallow-all catch; kept only the name and the public purpose. |
| `awardsPointsToAMember` (`points > 0`) | **Rewritten** | Weak assertion; replaced by exact expected values. |
| `calculatesGoldPoints` (mocks the SUT) | **Deleted** | Tests Mockito, not the code. Nothing to salvage; replaced by the GOLD rows in `LoyaltyConfigTest` that run the real rule. |
| `awardsPointsForAPurchase` (`assertNotNull(int)`) | **Deleted** | A tautology that can never fail. |
| `pom.xml`, JaCoCo, PIT config | **Kept**, PIT given a threshold and bound to `verify` | The tooling was good; it just was not being used as a gate. |

**Deliberate behaviour changes** (everything else is identical): unknown member → `MemberNotFoundException` (was `0`);
negative amount → `InvalidPurchaseException` (was negative points); overflow → `InvalidPurchaseException` (was a
negative or wrapped number); `null`/blank id → `InvalidPurchaseException` (was `0`). Each of these was pinned as a "known bug" in a
characterization test and flipped in the same commit as its fix.

**Assumptions to confirm with the business** (I kept the legacy answer in each case): SILVER rounds *down* (25 × 1.5 = 37, not 38);
the bonus is flat and is not multiplied by the tier (confirmed by the business). A GHS 0 purchase is invalid (confirmed by the business). A valid purchase below GHS 10 earns `0` points.

## 5. How I checked it

* **Characterize, then refactor.** Ten business-rule cases (plus the three bugs) were pinned against the *original* code first, and every
  refactoring commit kept them green.
* **Would the suite catch a bug?** The tests were run against 32 hand-planted bugs (one at a time: `/10` → `/1`, `DOWN` → `HALF_UP`,
  `>` → `>=` on the bonus, bonus 50 → 51, threshold 500 → 499, SILVER 1.5 → 1.6, GOLD ×2 → ×3, `addExact` → `+`, a removed
  `catch` clause, blank-id check removed, base points `+ 1`, zero amount rejected, duplicate-rule check inverted, repository
  always empty, blank-id check in `Member` removed, …). All 32 failed at least one test (the last sweep ran against the final code).
* **Mutation gate.** `mvn verify` now runs PIT with a threshold of 80.

> **PIT score on my machine: 95 %.**

## 6. The one improvement beyond the brief

**An enforceable safety net: a mutation-score gate in the build, plus a guard test for the "tier without a rule" bug class.**

*Why this and not a cosmetic extra.* The defining failure of this code base was tests that were green and proved nothing, and a
line-coverage report would have rated them well (they execute most of the STANDARD and SILVER lines). Mutation testing
measures the thing that actually matters (*would a bug make a test fail?*), and putting a threshold in `mvn verify`
and CI turns "the suite is trustworthy" from a promise into something the build checks on every push, so the next AI-generated
test that asserts nothing cannot slip back in. The guard test closes the specific hole the coming change opens:
a new tier is the likeliest way to reintroduce a silent wrong answer, and it now fails the build instead. The custom exception
hierarchy (section 2) is the supporting piece: the failures are distinct types a caller can act on, not strings.

---
