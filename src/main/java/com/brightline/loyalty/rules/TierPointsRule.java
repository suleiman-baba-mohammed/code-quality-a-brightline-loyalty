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
   */
  int apply(int basePoints, Purchase purchase);
}
