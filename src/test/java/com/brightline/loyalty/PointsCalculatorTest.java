package com.brightline.loyalty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.brightline.loyalty.exception.InvalidPurchaseException;
import com.brightline.loyalty.exception.MemberLookupException;
import com.brightline.loyalty.exception.MemberNotFoundException;
import com.brightline.loyalty.exception.UnsupportedTierException;
import com.brightline.loyalty.member.Member;
import com.brightline.loyalty.member.MemberRepository;
import com.brightline.loyalty.member.MembershipTier;
import com.brightline.loyalty.purchase.Purchase;
import com.brightline.loyalty.rules.BasePointsPolicy;
import com.brightline.loyalty.rules.BonusRule;
import com.brightline.loyalty.rules.PointsRules;
import com.brightline.loyalty.rules.TierPointsRule;
import com.brightline.loyalty.rules.TierRuleRegistry;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests the orchestration in isolation: every collaborator is a mock, so each expected number below
 * is decided by the test, not by the production rules. The real rules are tested on their own and
 * together in {@link LoyaltyConfigTest}.
 */
@ExtendWith(MockitoExtension.class)
class PointsCalculatorTest {

  private static final String ID = "g1";

  @Mock private MemberRepository repository;
  @Mock private TierPointsRule goldRule;
  @Mock private BasePointsPolicy basePolicy;
  @Mock private BonusRule bonusA;
  @Mock private BonusRule bonusB;

  private final Purchase purchase = new Purchase(new BigDecimal("123.45"));
  private final Member gold = new Member(ID, "Ama Owusu", MembershipTier.GOLD);
  private TierRuleRegistry registry;

  @BeforeEach
  void setUp() {
    when(goldRule.tier()).thenReturn(MembershipTier.GOLD);
    registry = new TierRuleRegistry(List.of(goldRule));
  }

  private PointsCalculator calculatorWith(BonusRule... bonuses) {
    return new PointsCalculator(
        repository, new PointsRules(basePolicy, registry, List.of(bonuses)));
  }

  @Test
  void awardsTheTierPointsPlusEveryBonus() {
    when(repository.findById(ID)).thenReturn(Optional.of(gold));
    when(basePolicy.basePointsFor(purchase)).thenReturn(7);
    when(goldRule.apply(7, purchase)).thenReturn(20); // the tier rule receives the BASE points
    when(bonusA.bonusFor(purchase)).thenReturn(5);
    when(bonusB.bonusFor(purchase)).thenReturn(3);

    assertEquals(28, calculatorWith(bonusA, bonusB).calculatePoints(ID, purchase));
  }

  @Test
  void bonusIsAddedAfterTheTierRuleNotMultipliedByIt() {
    when(repository.findById(ID)).thenReturn(Optional.of(gold));
    when(basePolicy.basePointsFor(purchase)).thenReturn(10);
    when(goldRule.apply(10, purchase)).thenReturn(20);
    when(bonusA.bonusFor(purchase)).thenReturn(50);

    assertEquals(70, calculatorWith(bonusA).calculatePoints(ID, purchase));
  }

  @Test
  void withNoBonusRulesTheResultIsJustTheTierPoints() {
    when(repository.findById(ID)).thenReturn(Optional.of(gold));
    when(basePolicy.basePointsFor(purchase)).thenReturn(4);
    when(goldRule.apply(4, purchase)).thenReturn(8);

    assertEquals(8, calculatorWith().calculatePoints(ID, purchase));
  }

  @Test
  void aPurchaseThatEarnsNothingReturnsZeroNotAnError() {
    when(repository.findById(ID)).thenReturn(Optional.of(gold));
    when(basePolicy.basePointsFor(purchase)).thenReturn(0);
    when(goldRule.apply(0, purchase)).thenReturn(0);

    assertEquals(0, calculatorWith().calculatePoints(ID, purchase));
  }

  @Test
  void anUnknownMemberIsRejectedNotReportedAsZeroPoints() {
    when(repository.findById("ghost")).thenReturn(Optional.empty());

    MemberNotFoundException e =
        assertThrows(
            MemberNotFoundException.class,
            () -> calculatorWith().calculatePoints("ghost", purchase));

    assertEquals("ghost", e.getMemberId());
    assertEquals("Member not found: ghost", e.getMessage());
  }

  @Test
  void aStoreFailureIsWrappedAsLookupFailureAndKeepsItsCause() {
    IllegalStateException outage = new IllegalStateException("connection refused");
    when(repository.findById(ID)).thenThrow(outage);

    MemberLookupException e =
        assertThrows(
            MemberLookupException.class, () -> calculatorWith().calculatePoints(ID, purchase));

    assertSame(outage, e.getCause());
    assertTrue(e.getMessage().contains(ID), e.getMessage());
  }

  @Test
  void aMemberWhoseTierHasNoRuleIsAConfigurationErrorNotAZero() {
    Member silver = new Member("s1", "Yaa Boateng", MembershipTier.SILVER);
    when(repository.findById("s1")).thenReturn(Optional.of(silver));
    when(basePolicy.basePointsFor(purchase)).thenReturn(5);

    UnsupportedTierException e =
        assertThrows(
            UnsupportedTierException.class, () -> calculatorWith().calculatePoints("s1", purchase));

    assertEquals(MembershipTier.SILVER, e.getTier());
  }

  @Test
  void additionOverflowIsReportedAsAnInvalidPurchaseWithTheArithmeticCause() {
    when(repository.findById(ID)).thenReturn(Optional.of(gold));
    when(basePolicy.basePointsFor(purchase)).thenReturn(1);
    when(goldRule.apply(1, purchase)).thenReturn(Integer.MAX_VALUE);
    when(bonusA.bonusFor(purchase)).thenReturn(1);

    InvalidPurchaseException e =
        assertThrows(
            InvalidPurchaseException.class,
            () -> calculatorWith(bonusA).calculatePoints(ID, purchase));

    assertInstanceOf(ArithmeticException.class, e.getCause());
    assertTrue(e.getMessage().contains("123.45"), e.getMessage());
  }

  @Test
  void bonusesThatOverflowWhenAddedTogetherAreAnInvalidPurchase() {
    when(repository.findById(ID)).thenReturn(Optional.of(gold));
    when(basePolicy.basePointsFor(purchase)).thenReturn(1);
    when(goldRule.apply(1, purchase)).thenReturn(0);
    when(bonusA.bonusFor(purchase)).thenReturn(Integer.MAX_VALUE);
    when(bonusB.bonusFor(purchase)).thenReturn(1);

    assertThrows(
        InvalidPurchaseException.class,
        () -> calculatorWith(bonusA, bonusB).calculatePoints(ID, purchase));
  }

  @Test
  void anArithmeticFailureInTheBasePolicyIsAlsoAnInvalidPurchase() {
    ArithmeticException overflow = new ArithmeticException("too big");
    when(repository.findById(ID)).thenReturn(Optional.of(gold));
    when(basePolicy.basePointsFor(purchase)).thenThrow(overflow);

    InvalidPurchaseException e =
        assertThrows(
            InvalidPurchaseException.class, () -> calculatorWith().calculatePoints(ID, purchase));

    assertSame(overflow, e.getCause());
  }

  @Test
  void aNullMemberIdIsRejectedWithoutTouchingTheStore() {
    InvalidPurchaseException e =
        assertThrows(
            InvalidPurchaseException.class, () -> calculatorWith().calculatePoints(null, purchase));

    assertEquals("Member id is required", e.getMessage());
    verifyNoInteractions(repository);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "\t"})
  void aBlankMemberIdIsRejectedWithoutTouchingTheStore(String blank) {
    assertThrows(
        InvalidPurchaseException.class, () -> calculatorWith().calculatePoints(blank, purchase));

    verifyNoInteractions(repository);
  }

  @Test
  void aNullPurchaseIsRejectedWithoutTouchingTheStore() {
    InvalidPurchaseException e =
        assertThrows(
            InvalidPurchaseException.class, () -> calculatorWith().calculatePoints(ID, null));

    assertEquals("Purchase is required", e.getMessage());
    verifyNoInteractions(repository);
  }

  @Test
  void refusesAMissingRepositoryAtConstructionNotAtFirstUse() {
    PointsRules rules = new PointsRules(basePolicy, registry, List.of());

    assertThrows(NullPointerException.class, () -> new PointsCalculator(null, rules));
  }

  @Test
  void refusesMissingRulesAtConstructionNotAtFirstUse() {
    assertThrows(NullPointerException.class, () -> new PointsCalculator(repository, null));
  }
}
