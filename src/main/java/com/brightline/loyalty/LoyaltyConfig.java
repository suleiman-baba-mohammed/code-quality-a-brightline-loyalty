package com.brightline.loyalty;

import com.brightline.loyalty.member.InMemoryMemberRepository;
import com.brightline.loyalty.member.Member;
import com.brightline.loyalty.member.MemberRepository;
import com.brightline.loyalty.member.MembershipTier;
import com.brightline.loyalty.rules.BonusRule;
import com.brightline.loyalty.rules.LargePurchaseBonusRule;
import com.brightline.loyalty.rules.MultiplierTierRule;
import com.brightline.loyalty.rules.PointsRules;
import com.brightline.loyalty.rules.SpendBasedBasePointsPolicy;
import com.brightline.loyalty.rules.TierPointsRule;
import com.brightline.loyalty.rules.TierRuleRegistry;
import java.math.BigDecimal;
import java.util.List;

/**
 * Composition root: the one place that decides which concrete collaborators the calculator is built
 * from, and the numbers of the scheme. Everything else receives its dependencies through its
 * constructor.
 */
public final class LoyaltyConfig {

  private static final BigDecimal STANDARD_MULTIPLIER = BigDecimal.ONE;
  private static final BigDecimal SILVER_MULTIPLIER = new BigDecimal("1.5");
  private static final BigDecimal GOLD_MULTIPLIER = new BigDecimal("2");
  private static final BigDecimal LARGE_PURCHASE_THRESHOLD = new BigDecimal("500");
  private static final int LARGE_PURCHASE_BONUS_POINTS = 50;

  /** Builds a calculator backed by the in-memory sample members and the standard rules. */
  public PointsCalculator defaultCalculator() {
    return new PointsCalculator(sampleMemberRepository(), defaultRules());
  }

  /** The rules of the scheme: base points, the tier rules and the bonuses. */
  PointsRules defaultRules() {
    return new PointsRules(
      new SpendBasedBasePointsPolicy(),
      new TierRuleRegistry(defaultTierRules()),
      defaultBonusRules());
  }

  /** The tier rules of the scheme. Add the rule of a new tier here. */
  List<TierPointsRule> defaultTierRules() {
    return List.of(
      new MultiplierTierRule(MembershipTier.STANDARD, STANDARD_MULTIPLIER),
      new MultiplierTierRule(MembershipTier.SILVER, SILVER_MULTIPLIER),
      new MultiplierTierRule(MembershipTier.GOLD, GOLD_MULTIPLIER));
  }

  /** The purchase bonuses of the scheme. Add a new bonus here. */
  List<BonusRule> defaultBonusRules() {
    return List.of(
      new LargePurchaseBonusRule(LARGE_PURCHASE_THRESHOLD, LARGE_PURCHASE_BONUS_POINTS));
  }

  /** The sample members the demo runs against. */
  MemberRepository sampleMemberRepository() {
    return new InMemoryMemberRepository(sampleMembers());
  }

  /** Three sample members, one per tier. */
  List<Member> sampleMembers() {
    return List.of(
      new Member("m1", "Kofi Mensah", MembershipTier.STANDARD),
      new Member("m2", "Ama Owusu", MembershipTier.GOLD),
      new Member("m3", "Yaa Boateng", MembershipTier.SILVER));
  }
}
