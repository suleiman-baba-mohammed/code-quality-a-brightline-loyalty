package com.brightline.loyalty.rules;

import com.brightline.loyalty.purchase.Purchase;

/** Decides how many points a purchase earns from its amount alone, before tier and bonus. */
public interface BasePointsPolicy {

  /**
   * Returns the base points for the purchase.
   *
   * @param purchase the purchase being rewarded
   * @return the base points, never negative
   * @throws ArithmeticException only when the result does not fit in an {@code int}; the calculator
   *     reports this as an oversized purchase, so implementations must not throw it for any other
   *     reason
   */
  int basePointsFor(Purchase purchase);
}
