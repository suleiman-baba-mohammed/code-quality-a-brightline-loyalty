package com.brightline.loyalty.exception;

/** Thrown when the request is malformed: a missing id or amount, a negative or oversized amount. */
public class InvalidPurchaseException extends LoyaltyException {

  private static final long serialVersionUID = 1L;

  public InvalidPurchaseException(String message) {
    super(message);
  }

  public InvalidPurchaseException(String message, Throwable cause) {
    super(message, cause);
  }
}
