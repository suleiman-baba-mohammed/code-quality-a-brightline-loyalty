package com.brightline.loyalty.exception;

/** Thrown when no member exists for the given id. Not the same thing as "earned zero points". */
public class MemberNotFoundException extends LoyaltyException {

  private static final long serialVersionUID = 1L;

  private final String memberId;

  public MemberNotFoundException(String memberId) {
    super("Member not found: " + memberId);
    this.memberId = memberId;
  }

  public String getMemberId() {
    return memberId;
  }
}
