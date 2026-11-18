package com.brightline.loyalty.exception;

import com.brightline.loyalty.member.MembershipTier;

/** Thrown when a member's tier has no points rule registered: a configuration error. */
public class UnsupportedTierException extends LoyaltyException {

  private static final long serialVersionUID = 1L;

  private final MembershipTier tier;

  public UnsupportedTierException(MembershipTier tier) {
    super("No points rule registered for tier " + tier);
    this.tier = tier;
  }

  public MembershipTier getTier() {
    return tier;
  }
}
