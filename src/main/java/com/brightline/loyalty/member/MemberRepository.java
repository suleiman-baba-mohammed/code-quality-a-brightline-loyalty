package com.brightline.loyalty.member;

import java.util.Optional;

/** Where members are kept. */
public interface MemberRepository {

  /**
   * Finds a member.
   *
   * @param id the member id
   * @return the member, or empty when there is no such member
   */
  Optional<Member> findById(String id);
}
