package com.brightline.loyalty.rules;

import com.brightline.loyalty.purchase.Purchase;

/** Decides how many points a purchase earns from its amount alone, before tier and bonus. */
public interface BasePointsPolicy {

  /** Returns the base points for the purchase. */
  int basePointsFor(Purchase purchase);
}
