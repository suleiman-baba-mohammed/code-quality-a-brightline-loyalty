package com.brightline.loyalty.rules;

import com.brightline.loyalty.member.MembershipTier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** A tier rule that multiplies base points by a fixed factor, rounding down to whole points. */
public final class MultiplierTierRule implements TierPointsRule {

  private final MembershipTier tier;
  private final BigDecimal multiplier;

  /**
   * Creates the rule.
   *
   * @param tier the tier it applies to
   * @param multiplier the factor, which must not be negative
   */
  public MultiplierTierRule(MembershipTier tier, BigDecimal multiplier) {
    this.tier = Objects.requireNonNull(tier, "tier");
    this.multiplier = Objects.requireNonNull(multiplier, "multiplier");
    if (multiplier.signum() < 0) {
      throw new IllegalArgumentException("Multiplier must not be negative: " + multiplier);
    }
  }

  @Override
  public MembershipTier tier() {
    return tier;
  }

  @Override
  public int apply(int basePoints) {
    return BigDecimal.valueOf(basePoints)
      .multiply(multiplier)
      .setScale(0, RoundingMode.DOWN)
      .intValueExact();
  }
}
