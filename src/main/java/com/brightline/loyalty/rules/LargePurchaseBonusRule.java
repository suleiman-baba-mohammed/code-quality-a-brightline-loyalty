package com.brightline.loyalty.rules;

import com.brightline.loyalty.purchase.Purchase;
import java.math.BigDecimal;
import java.util.Objects;

/** A flat bonus for a purchase strictly larger than a threshold. */
public final class LargePurchaseBonusRule implements BonusRule {

  private final BigDecimal threshold;
  private final int bonusPoints;

  /**
   * Creates the rule.
   *
   * @param threshold the amount a purchase must exceed; must not be negative
   * @param bonusPoints the points awarded; must not be negative
   */
  public LargePurchaseBonusRule(BigDecimal threshold, int bonusPoints) {
    this.threshold = Objects.requireNonNull(threshold, "threshold");
    if (threshold.signum() < 0) {
      throw new IllegalArgumentException("Threshold must not be negative: " + threshold);
    }
    if (bonusPoints < 0) {
      throw new IllegalArgumentException("Bonus points must not be negative: " + bonusPoints);
    }
    this.bonusPoints = bonusPoints;
  }

  @Override
  public int bonusFor(Purchase purchase) {
    // compareTo, not equals: 500.0 and 500.00 are the same amount
    return purchase.amount().compareTo(threshold) > 0 ? bonusPoints : 0;
  }
}
