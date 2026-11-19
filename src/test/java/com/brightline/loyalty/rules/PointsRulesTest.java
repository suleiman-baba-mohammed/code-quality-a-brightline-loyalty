package com.brightline.loyalty.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PointsRulesTest {

  private final BasePointsPolicy policy = purchase -> 1;
  private final TierRuleRegistry registry = new TierRuleRegistry(List.of());

  @Test
  void rejectsAMissingBasePolicy() {
    assertThrows(NullPointerException.class, () -> new PointsRules(null, registry, List.of()));
  }

  @Test
  void rejectsMissingTierRules() {
    assertThrows(NullPointerException.class, () -> new PointsRules(policy, null, List.of()));
  }

  @Test
  void rejectsAMissingBonusList() {
    assertThrows(NullPointerException.class, () -> new PointsRules(policy, registry, null));
  }

  @Test
  void copiesTheBonusListSoLaterChangesByTheCallerHaveNoEffect() {
    List<BonusRule> bonuses = new ArrayList<>();
    PointsRules rules = new PointsRules(policy, registry, bonuses);

    bonuses.add(purchase -> 5);

    assertEquals(0, rules.bonusRules().size());
  }

  @Test
  void exposedBonusListCannotBeModified() {
    PointsRules rules = new PointsRules(policy, registry, List.of());

    assertThrows(UnsupportedOperationException.class, () -> rules.bonusRules().add(purchase -> 1));
  }
}
