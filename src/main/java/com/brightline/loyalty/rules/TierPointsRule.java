package com.brightline.loyalty.rules;

import com.brightline.loyalty.member.MembershipTier;
import com.brightline.loyalty.purchase.Purchase;

/**
 * The points rule of one membership tier. A new tier is added by writing one new implementation and
 * registering it; no existing rule or the calculator has to change.
 */
public interface TierPointsRule {

  /** The tier this rule applies to. */
  MembershipTier tier();

  /**
   * Turns the base points earned from spend into the points this tier awards. The purchase is
   * passed so a tier can have rules that depend on it (for example, a higher multiplier above a
   * certain amount).
   *
   * @param basePoints points earned from the purchase amount alone, never negative
   * @param purchase the purchase being rewarded
   * @return the points awarded for this tier, before any purchase bonus
   * @throws ArithmeticException only when the result does not fit in an {@code int}; the calculator
   *     reports this as an oversized purchase, so implementations must not throw it for any other
   *     reason
   */
  int apply(int basePoints, Purchase purchase);
}
