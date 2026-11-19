# Brightline Loyalty: points service

Works out how many loyalty points a member earns on a purchase.

## The rules

| Step | Rule |
|---|---|
| Valid purchase | the amount must be **greater than zero**; zero and negative amounts are rejected |
| Base points | 1 point per whole GHS 10 spent; the remainder earns nothing |
| Tier | STANDARD x1, SILVER x1.5, GOLD x2, rounded **down** to whole points |
| Bonus | +50 points when the purchase is **strictly more than** GHS 500; flat, added after the tier rule |

Example: a GOLD member spends GHS 600 → 60 base × 2 = 120, + 50 bonus = **170**.

## Failures are explicit, never a silent zero

`PointsCalculator.calculatePoints(memberId, purchase)` returns the points, or throws a `LoyaltyException`:

| Exception | Meaning |
|---|---|
| `MemberNotFoundException` | no member with that id |
| `InvalidPurchaseException` | missing/blank member id, missing, zero or negative amount, or an amount too large to turn into points |
| `MemberLookupException` | the member store failed (original error kept as the cause) |
| `UnsupportedTierException` | the member's tier has no registered rule (a configuration error) |

A purchase that earns nothing (for example GHS 9.99) returns `0`, and `0` now only ever means "earned nothing".

## Running it

JDK 21 and Maven.

```bash
mvn compile
mvn exec:java -Dexec.mainClass=com.brightline.loyalty.Demo   # see it run, including a rejection
mvn test                                                      # unit tests
mvn spotless:apply                                            # auto-format to Google Java Style
mvn verify                                                    # style check + tests + JaCoCo 100% gate (target/site/jacoco) + PIT mutation gate (target/pit-reports)

```

`mvn verify` fails below 100% line and branch coverage (standard section 9) and if the mutation score falls below
the threshold in `pom.xml`; CI runs the same command.

## Adding a membership tier

1. Add the constant to `MembershipTier`.
2. Write its rule (`new MultiplierTierRule(tier, multiplier)`, or a new `TierPointsRule` if its logic differs).
3. Register it in `LoyaltyConfig.defaultTierRules()`.
4. Add rows to the `LoyaltyConfigTest` table.

`PointsCalculator` and the existing rules are not edited. If step 3 is forgotten, `LoyaltyConfigTest` fails.

## Layout

```
com.brightline.loyalty            PointsCalculator, LoyaltyConfig, Demo
com.brightline.loyalty.member     Member, MembershipTier, MemberRepository, InMemoryMemberRepository
com.brightline.loyalty.exception  LoyaltyException and its four subclasses
com.brightline.loyalty.purchase   Purchase
com.brightline.loyalty.rules      base policy, tier rules and registry, bonus rules, PointsRules
```

Dependencies point one way: `member` ← `exception` ← `purchase` ← `rules` ← the root package.

See **ANALYSIS.md**: what was wrong with the inherited code and tests, the design, the decisions, the standard
section by section, and the review notes.

