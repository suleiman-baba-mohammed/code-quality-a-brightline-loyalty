package com.brightline.loyalty;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.brightline.loyalty.member.MembershipTier;
import com.brightline.loyalty.purchase.Purchase;
import com.brightline.loyalty.rules.PointsRules;
import com.brightline.loyalty.rules.SpendBasedBasePointsPolicy;
import com.brightline.loyalty.rules.TierPointsRule;
import com.brightline.loyalty.rules.TierRuleRegistry;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Proves the design absorbs the coming change. The business is adding a tier with its own points
 * rules. This test writes such a rule, which is NOT a simple multiplier, and plugs it in without
 * touching {@link PointsCalculator}, {@link TierRuleRegistry} or any existing rule.
 */
class TierExtensionTest {

  private final LoyaltyConfig config = new LoyaltyConfig();

  /** A made-up tier rule that is not a multiplier: base points plus a flat 5. */
  private static final class FlatPlusRule implements TierPointsRule {

    @Override
    public MembershipTier tier() {
      return MembershipTier.GOLD;
    }

    @Override
    public int apply(int basePoints) {
      return basePoints + 5;
    }
  }

  private PointsCalculator calculatorWith(TierPointsRule goldRule) {
    List<TierPointsRule> rules = new ArrayList<>(config.defaultTierRules());
    rules.removeIf(rule -> rule.tier() == MembershipTier.GOLD);
    rules.add(goldRule);
    PointsRules pointsRules =
      new PointsRules(
        new SpendBasedBasePointsPolicy(),
        new TierRuleRegistry(rules),
        config.defaultBonusRules());
    return new PointsCalculator(config.sampleMemberRepository(), pointsRules);
  }

  @Test
  void aNewKindOfTierRuleIsUsedForItsTier() {
    PointsCalculator calculator = calculatorWith(new FlatPlusRule());

    assertEquals(30, calculator.calculatePoints("m2", new Purchase(new BigDecimal("250"))));
  }

  @Test
  void aNewKindOfTierRuleStillGetsThePurchaseBonus() {
    PointsCalculator calculator = calculatorWith(new FlatPlusRule());

    // 60 base + 5 flat from the new rule + 50 bonus
    assertEquals(115, calculator.calculatePoints("m2", new Purchase(new BigDecimal("600"))));
  }

  @Test
  void theStandardTierIsUntouchedByReplacingTheGoldRule() {
    PointsCalculator calculator = calculatorWith(new FlatPlusRule());

    assertEquals(10, calculator.calculatePoints("m1", new Purchase(new BigDecimal("100"))));
  }

  @Test
  void theSilverTierIsUntouchedByReplacingTheGoldRule() {
    PointsCalculator calculator = calculatorWith(new FlatPlusRule());

    assertEquals(37, calculator.calculatePoints("m3", new Purchase(new BigDecimal("250"))));
  }
}
