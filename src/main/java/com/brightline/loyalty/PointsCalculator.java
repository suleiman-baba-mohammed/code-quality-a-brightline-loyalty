package com.brightline.loyalty;

import com.brightline.loyalty.exception.InvalidPurchaseException;
import com.brightline.loyalty.exception.MemberLookupException;
import com.brightline.loyalty.exception.MemberNotFoundException;
import com.brightline.loyalty.exception.UnsupportedTierException;
import com.brightline.loyalty.member.Member;
import com.brightline.loyalty.member.MemberRepository;
import com.brightline.loyalty.member.MembershipTier;
import com.brightline.loyalty.purchase.Purchase;
import com.brightline.loyalty.rules.PointsRules;
import java.util.Objects;
import java.util.Optional;

/**
 * Works out the points a member earns on a purchase. It only orchestrates: the member store, the
 * base-points policy, the tier rules and the bonus rules are all injected collaborators.
 */
public class PointsCalculator {

  private final MemberRepository repository;
  private final PointsRules rules;

  /** Creates a calculator from its collaborators; neither may be null. */
  public PointsCalculator(MemberRepository repository, PointsRules rules) {
    this.repository = Objects.requireNonNull(repository, "repository");
    this.rules = Objects.requireNonNull(rules, "rules");
  }

  /**
   * Calculates the points a member earns on a purchase.
   *
   * @param memberId the member's id
   * @param purchase the purchase
   * @return the points earned: the tier points plus every bonus, never negative
   * @throws InvalidPurchaseException if the id or purchase is missing, or the amount is too large
   *     to turn into points
   * @throws MemberNotFoundException if there is no member with that id
   * @throws MemberLookupException if the member store failed
   * @throws UnsupportedTierException if the member's tier has no registered rule
   */
  public int calculatePoints(String memberId, Purchase purchase) {
    if (memberId == null || memberId.isBlank()) {
      throw new InvalidPurchaseException("Member id is required");
    }
    if (purchase == null) {
      throw new InvalidPurchaseException("Purchase is required");
    }
    Member member = findMember(memberId);
    return pointsFor(member.tier(), purchase);
  }

  private Member findMember(String memberId) {
    return lookUp(memberId).orElseThrow(() -> new MemberNotFoundException(memberId));
  }

  private Optional<Member> lookUp(String memberId) {
    try {
      return repository.findById(memberId);
    } catch (RuntimeException e) {
      throw new MemberLookupException(memberId, e);
    }
  }

  private int pointsFor(MembershipTier tier, Purchase purchase) {
    try {
      int basePoints = rules.basePolicy().basePointsFor(purchase);
      int tierPoints = rules.tierRules().ruleFor(tier).apply(basePoints);
      return Math.addExact(tierPoints, bonusPointsFor(purchase));
    } catch (ArithmeticException e) {
      throw new InvalidPurchaseException(
        "Purchase amount is too large to convert to points: " + purchase.amount(), e);
    }
  }

  private int bonusPointsFor(Purchase purchase) {
    return rules.bonusRules().stream()
      .map(bonus -> bonus.bonusFor(purchase))
      .reduce(0, Math::addExact);
  }
}
