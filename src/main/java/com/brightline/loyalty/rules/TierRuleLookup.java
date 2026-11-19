package com.brightline.loyalty.rules;

import com.brightline.loyalty.exception.UnsupportedTierException;
import com.brightline.loyalty.member.MembershipTier;

/** Finds the points rule of a membership tier. */
public interface TierRuleLookup {

  /**
   * Returns the rule for the tier.
   *
   * @throws UnsupportedTierException if no rule is registered for the tier
   */
  TierPointsRule ruleFor(MembershipTier tier);
}
