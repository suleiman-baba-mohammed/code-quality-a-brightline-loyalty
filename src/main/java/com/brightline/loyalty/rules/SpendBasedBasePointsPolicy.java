package com.brightline.loyalty.rules;

import com.brightline.loyalty.purchase.Purchase;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** One point for every whole GHS 10 spent; any remainder earns nothing. */
public final class SpendBasedBasePointsPolicy implements BasePointsPolicy {

  /** The spend, in GHS, that earns one point. */
  static final BigDecimal SPEND_PER_POINT = BigDecimal.TEN;

  @Override
  public int basePointsFor(Purchase purchase) {
    return purchase.amount().divide(SPEND_PER_POINT, 0, RoundingMode.DOWN).intValueExact();
  }
}
