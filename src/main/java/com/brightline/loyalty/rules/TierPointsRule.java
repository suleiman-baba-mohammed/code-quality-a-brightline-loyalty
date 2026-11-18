package com.brightline.loyalty.rules;

import com.brightline.loyalty.member.MembershipTier;

/**
 * The points rule of one membership tier. A new tier is added by writing one new implementation and
 * registering it; no existing rule or the calculator has to change.
 */
public interface TierPointsRule {

  /** The tier this rule applies to. */
  MembershipTier tier();

  /**
   * Turns the base points earned from spend into the points this tier awards.
   *
   * @param basePoints points earned from the purchase amount alone, never negative
   * @return the points awarded for this tier, before any purchase bonus
   */
  int apply(int basePoints);
}
