package com.brightline.loyalty.member;

/**
 * The membership tiers of the loyalty scheme. Each tier needs a points rule (a {@code
 * TierPointsRule}) registered in the configuration.
 */
public enum MembershipTier {
  STANDARD,
  SILVER,
  GOLD
}
