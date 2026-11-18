package com.brightline.loyalty.exception;

/**
 * Base class of every failure this service raises on purpose. Callers who want to handle "anything
 * the loyalty service rejected" catch this one type; callers who care which failure it was catch
 * the subclass. It is unchecked: none of these failures can be repaired at the call site, so
 * declaring them on every signature would only add noise.
 */
public abstract class LoyaltyException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  protected LoyaltyException(String message) {
    super(message);
  }

  protected LoyaltyException(String message, Throwable cause) {
    super(message, cause);
  }
}
