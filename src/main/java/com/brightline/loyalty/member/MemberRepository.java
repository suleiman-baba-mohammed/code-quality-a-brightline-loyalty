package com.brightline.loyalty.member;

import java.util.Optional;

/** Where members are kept. */
public interface MemberRepository {

  /**
   * Finds a member.
   *
   * @param id the member id
   * @return the member, or empty when there is no such member
   * @throws RuntimeException if the store itself fails; callers treat any such failure as a store
   *     failure and wrap it (see {@code MemberLookupException})
   */
  Optional<Member> findById(String id);
}
