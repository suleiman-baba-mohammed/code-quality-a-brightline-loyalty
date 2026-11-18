package com.brightline.loyalty.exception;

/**
 * Thrown when the member store itself failed (as opposed to the member simply not existing). The
 * original failure is kept as the cause.
 */
public class MemberLookupException extends LoyaltyException {

  private static final long serialVersionUID = 1L;

  public MemberLookupException(String memberId, Throwable cause) {
    super("Could not look up member: " + memberId, cause);
  }
}
