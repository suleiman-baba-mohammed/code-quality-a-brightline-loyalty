package com.brightline.loyalty.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.brightline.loyalty.member.MembershipTier;
import com.brightline.loyalty.purchase.Purchase;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MultiplierTierRuleTest {
  private static final Purchase PURCHASE = new Purchase(BigDecimal.TEN);

  @Test
  void reportsTheTierItBelongsTo() {
    assertEquals(
        MembershipTier.SILVER,
        new MultiplierTierRule(MembershipTier.SILVER, BigDecimal.ONE).tier());
  }

  @ParameterizedTest
  @CsvSource({
    "1, 0, 0",
    "1, 7, 7",
    "1.5, 0, 0",
    "1.5, 1, 1", // 1.5 rounds DOWN, not to nearest
    "1.5, 2, 3",
    "1.5, 25, 37", // 37.5 -> 37
    "1.5, 100, 150",
    "2, 0, 0",
    "2, 60, 120",
    "0, 50, 0"
  })
  void multipliesAndRoundsDownToWholePoints(BigDecimal multiplier, int base, int expected) {
    MultiplierTierRule rule = new MultiplierTierRule(MembershipTier.GOLD, multiplier);

    assertEquals(expected, rule.apply(base, PURCHASE));
  }

  @Test
  void refusesAResultThatDoesNotFitInAnInt() {
    MultiplierTierRule rule = new MultiplierTierRule(MembershipTier.GOLD, new BigDecimal("2"));

    assertThrows(ArithmeticException.class, () -> rule.apply(Integer.MAX_VALUE, PURCHASE));
  }

  @Test
  void rejectsANullTier() {
    assertThrows(NullPointerException.class, () -> new MultiplierTierRule(null, BigDecimal.ONE));
  }

  @Test
  void rejectsANullMultiplier() {
    assertThrows(
        NullPointerException.class, () -> new MultiplierTierRule(MembershipTier.GOLD, null));
  }

  @Test
  void rejectsANegativeMultiplier() {
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> new MultiplierTierRule(MembershipTier.GOLD, new BigDecimal("-1")));

    assertTrue(e.getMessage().contains("-1"), e.getMessage());
  }
}
