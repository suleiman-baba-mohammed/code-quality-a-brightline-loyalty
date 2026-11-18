package com.brightline.loyalty.member;

/** A loyalty scheme member. Id and name must not be blank and the tier must be present. */
public record Member(String id, String name, MembershipTier tier) {

  /** Rejects an invalid member so one can never exist. */
  public Member {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("Member id must not be blank");
    }
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Member name must not be blank");
    }
    if (tier == null) {
      throw new IllegalArgumentException("Member tier is required");
    }
  }
}
