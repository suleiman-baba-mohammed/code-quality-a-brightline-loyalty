package com.brightline.loyalty.rules;

import com.brightline.loyalty.exception.UnsupportedTierException;
import com.brightline.loyalty.member.MembershipTier;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/** Finds the {@link TierPointsRule} for a tier. Built once from the rules it is given. */
public final class TierRuleRegistry {

  private final Map<MembershipTier, TierPointsRule> rules = new EnumMap<>(MembershipTier.class);

  /** Registers the rules; two rules for the same tier are a configuration error. */
  public TierRuleRegistry(Collection<? extends TierPointsRule> tierRules) {
    for (TierPointsRule rule : tierRules) {
      if (rules.put(rule.tier(), rule) != null) {
        throw new IllegalArgumentException("Two rules registered for tier " + rule.tier());
      }
    }
  }

  /**
   * Returns the rule for the tier.
   *
   * @throws UnsupportedTierException if no rule is registered for the tier
   */
  public TierPointsRule ruleFor(MembershipTier tier) {
    TierPointsRule rule = rules.get(tier);
    if (rule == null) {
      throw new UnsupportedTierException(tier);
    }
    return rule;
  }
}
