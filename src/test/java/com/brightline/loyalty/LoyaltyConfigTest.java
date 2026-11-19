package com.brightline.loyalty;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.brightline.loyalty.exception.InvalidPurchaseException;
import com.brightline.loyalty.exception.MemberNotFoundException;
import com.brightline.loyalty.member.MemberRepository;
import com.brightline.loyalty.member.MembershipTier;
import com.brightline.loyalty.purchase.Purchase;
import com.brightline.loyalty.rules.TierRuleRegistry;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * The real rules wired together exactly as production wires them, against the sample members: m1 is
 * STANDARD, m2 is GOLD, m3 is SILVER. These are the business rules of the scheme, and the expected
 * numbers are worked out by hand in the comments, not copied from the code.
 */
class LoyaltyConfigTest {

  private final LoyaltyConfig config = new LoyaltyConfig();
  private final PointsCalculator calculator = config.defaultCalculator();

  private int points(String memberId, String amount) {
    return calculator.calculatePoints(memberId, new Purchase(new BigDecimal(amount)));
  }

  @ParameterizedTest
  @CsvSource({
    // STANDARD (x1): one point per whole GHS 10
    "m1, 0.01, 0",
    "m1, 9.99, 0",
    "m1, 10, 1",
    "m1, 19.99, 1",
    "m1, 100, 10",
    "m1, 499.99, 49",
    // bonus: +50 only when strictly above GHS 500
    "m1, 500, 50", // 50 base, no bonus at the threshold
    "m1, 500.00, 50", // scale must not matter
    "m1, 500.01, 100", // 50 base + 50 bonus
    "m1, 1000, 150", // 100 base + 50 bonus; the bonus is flat
    // SILVER (x1.5, rounded down)
    "m3, 10, 1", // 1 x 1.5 = 1.5 -> 1
    "m3, 20, 3", // 2 x 1.5 = 3
    "m3, 200, 30", // 20 x 1.5
    "m3, 250, 37", // 25 x 1.5 = 37.5 -> 37
    "m3, 501, 125", // 50 x 1.5 = 75, + 50 bonus
    // GOLD (x2)
    "m2, 10, 2",
    "m2, 250, 50", // 25 x 2
    "m2, 499.99, 98", // 49 x 2, no bonus
    "m2, 600, 170", // 60 x 2 = 120, + 50 bonus (the bonus is not doubled)
    // very large but still representable
    "m1, 15000000000, 1500000050" // 1,500,000,000 base + 50 bonus
  })
  void awardsPointsAccordingToTheBusinessRules(String memberId, String amount, int expected) {
    assertEquals(expected, points(memberId, amount));
  }

  @ParameterizedTest
  @CsvSource({
    "m2, 15000000000", // GOLD: 1,500,000,000 x 2 overflows the tier rule
    "m1, 21474836470", // base fits (2,147,483,647) but adding the 50 bonus overflows
    "m1, 21474836480", // base points alone overflow
    "m1, 1000000000000" // the old code returned -2147483599 for this
  })
  void rejectsAPurchaseWhosePointsCannotBeRepresented(String memberId, String amount) {
    InvalidPurchaseException e =
        assertThrows(InvalidPurchaseException.class, () -> points(memberId, amount));

    assertEquals(ArithmeticException.class, e.getCause().getClass());
  }

  @ParameterizedTest
  @CsvSource({"unknown", "M1", "m10"})
  void anIdThatIsNotExactlyAKnownMemberIsNotFound(String id) {
    assertThrows(MemberNotFoundException.class, () -> points(id, "100"));
  }

  /**
   * Guard against a class of bug. If someone adds a constant to {@link MembershipTier} and forgets
   * to register its rule, this fails here, in the build, instead of in production for the first
   * member on that tier.
   */
  @ParameterizedTest
  @EnumSource(MembershipTier.class)
  void everyMembershipTierHasARuleInTheDefaultConfiguration(MembershipTier tier) {
    assertEquals(tier, new TierRuleRegistry(config.defaultTierRules()).ruleFor(tier).tier());
  }

  @Test
  void sampleMembersHoldOneMemberPerTier() {
    MemberRepository repository = config.sampleMemberRepository();

    assertAll(
        () -> assertEquals(MembershipTier.STANDARD, repository.findById("m1").orElseThrow().tier()),
        () -> assertEquals(MembershipTier.GOLD, repository.findById("m2").orElseThrow().tier()),
        () -> assertEquals(MembershipTier.SILVER, repository.findById("m3").orElseThrow().tier()));
  }
}
