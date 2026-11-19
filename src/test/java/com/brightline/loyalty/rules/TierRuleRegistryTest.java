package com.brightline.loyalty.rules;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.brightline.loyalty.exception.UnsupportedTierException;
import com.brightline.loyalty.member.MembershipTier;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class TierRuleRegistryTest {

  private final TierPointsRule silver =
      new MultiplierTierRule(MembershipTier.SILVER, new BigDecimal("1.5"));
  private final TierPointsRule gold =
      new MultiplierTierRule(MembershipTier.GOLD, new BigDecimal("2"));

  @Test
  void returnsTheRegisteredRuleForEachTier() {
    TierRuleRegistry registry = new TierRuleRegistry(List.of(silver, gold));

    assertAll(
        () -> assertSame(silver, registry.ruleFor(MembershipTier.SILVER)),
        () -> assertSame(gold, registry.ruleFor(MembershipTier.GOLD)));
  }

  @Test
  void failsLoudlyForATierWithNoRuleInsteadOfGuessingOne() {
    TierRuleRegistry registry = new TierRuleRegistry(List.of(silver));

    UnsupportedTierException e =
        assertThrows(
            UnsupportedTierException.class, () -> registry.ruleFor(MembershipTier.STANDARD));

    assertEquals(MembershipTier.STANDARD, e.getTier());
  }

  @Test
  void anEmptyRegistryKnowsNoTier() {
    TierRuleRegistry registry = new TierRuleRegistry(List.of());

    assertThrows(UnsupportedTierException.class, () -> registry.ruleFor(MembershipTier.GOLD));
  }

  @Test
  void refusesTwoRulesForTheSameTierBecauseTheWinnerWouldBeArbitrary() {
    TierPointsRule anotherGold = new MultiplierTierRule(MembershipTier.GOLD, new BigDecimal("3"));
    List<TierPointsRule> rules = List.of(gold, anotherGold);

    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> new TierRuleRegistry(rules));

    assertTrue(e.getMessage().contains("GOLD"), e.getMessage());
  }
}
