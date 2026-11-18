package com.brightline.loyalty.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.brightline.loyalty.purchase.Purchase;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LargePurchaseBonusRuleTest {

  private final LargePurchaseBonusRule rule = new LargePurchaseBonusRule(new BigDecimal("500"), 50);

  @ParameterizedTest
  @CsvSource({
    "0.01, 0",
    "499.99, 0",
    "500, 0", // the threshold itself earns no bonus
    "500.0, 0", // same amount, different scale
    "500.00, 0",
    "500.01, 50", // strictly greater than the threshold
    "501, 50",
    "10000, 50" // the bonus is flat, it does not grow with the amount
  })
  void awardsTheBonusOnlyAboveTheThreshold(BigDecimal amount, int expected) {
    assertEquals(expected, rule.bonusFor(new Purchase(amount)));
  }

  @ParameterizedTest
  @CsvSource({"100, 0", "100.01, 7"})
  void usesTheThresholdAndBonusItWasGivenNotConstants(BigDecimal amount, int expected) {
    LargePurchaseBonusRule custom = new LargePurchaseBonusRule(new BigDecimal("100"), 7);

    assertEquals(expected, custom.bonusFor(new Purchase(amount)));
  }

  @Test
  void rejectsANullThreshold() {
    assertThrows(NullPointerException.class, () -> new LargePurchaseBonusRule(null, 50));
  }

  @Test
  void rejectsANegativeThreshold() {
    assertThrows(
      IllegalArgumentException.class,
      () -> new LargePurchaseBonusRule(new BigDecimal("-1"), 50));
  }

  @Test
  void rejectsNegativeBonusPoints() {
    assertThrows(
      IllegalArgumentException.class, () -> new LargePurchaseBonusRule(BigDecimal.TEN, -1));
  }
}
