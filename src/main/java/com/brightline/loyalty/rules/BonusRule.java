package com.brightline.loyalty.rules;

import com.brightline.loyalty.purchase.Purchase;

/** A bonus that adds extra points to a purchase, independent of the member's tier. */
public interface BonusRule {

  /** Returns the bonus points for the purchase; zero when the rule does not apply. */
  int bonusFor(Purchase purchase);
}
