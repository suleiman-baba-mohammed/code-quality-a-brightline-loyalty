package com.brightline.loyalty.rules;

import java.util.List;
import java.util.Objects;

/**
 * The rules of the points scheme, grouped so that {@code PointsCalculator} needs only two
 * constructor arguments. The bonus list is copied, so later changes by the caller have no effect.
 *
 * @param basePolicy turns a purchase into base points
 * @param tierRules finds the rule of a member's tier
 * @param bonusRules extra points that do not depend on the tier; may be empty
 */
public record PointsRules(
  BasePointsPolicy basePolicy, TierRuleRegistry tierRules, List<BonusRule> bonusRules) {

  /** Rejects missing rules and copies the bonus list. */
  public PointsRules {
    Objects.requireNonNull(basePolicy, "basePolicy");
    Objects.requireNonNull(tierRules, "tierRules");
    bonusRules = List.copyOf(bonusRules);
  }
}
