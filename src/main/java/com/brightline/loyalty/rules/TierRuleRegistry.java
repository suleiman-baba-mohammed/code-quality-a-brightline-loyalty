package com.brightline.loyalty.rules;

import com.brightline.loyalty.exception.UnsupportedTierException;
import com.brightline.loyalty.member.MembershipTier;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/** A {@link TierRuleLookup} built once from the rules it is given. */
public final class TierRuleRegistry implements TierRuleLookup {

  private final Map<MembershipTier, TierPointsRule> rules = new EnumMap<>(MembershipTier.class);

  /** Registers the rules; two rules for the same tier are a configuration error. */
  public TierRuleRegistry(Collection<? extends TierPointsRule> tierRules) {
    for (TierPointsRule rule : tierRules) {
      if (rules.put(rule.tier(), rule) != null) {
        throw new IllegalArgumentException("Two rules registered for tier " + rule.tier());
      }
    }
  }

  @Override
  public TierPointsRule ruleFor(MembershipTier tier) {
    TierPointsRule rule = rules.get(tier);
    if (rule == null) {
      throw new UnsupportedTierException(tier);
    }
    return rule;
  }
}
