package com.brightline.loyalty.purchase;

import com.brightline.loyalty.exception.InvalidPurchaseException;
import java.math.BigDecimal;

/**
 * A purchase, with its amount in GHS. Money is a {@link BigDecimal}, never a double, and an invalid
 * purchase cannot be constructed: the amount must be present and greater than zero.
 */
public record Purchase(BigDecimal amount) {

  /** Validates the amount. */
  public Purchase {
    if (amount == null) {
      throw new InvalidPurchaseException("Purchase amount is required");
    }
    if (amount.signum() <= 0) {
      throw new InvalidPurchaseException("Purchase amount must be greater than zero: " + amount);
    }
  }
}
