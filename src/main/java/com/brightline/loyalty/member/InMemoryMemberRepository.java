package com.brightline.loyalty.member;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** A {@link MemberRepository} that keeps its members in memory. */
public class InMemoryMemberRepository implements MemberRepository {

  private final Map<String, Member> members = new HashMap<>();

  /** Creates a repository holding the given members; two members must not share an id. */
  public InMemoryMemberRepository(Collection<Member> initialMembers) {
    for (Member member : initialMembers) {
      if (members.put(member.id(), member) != null) {
        throw new IllegalArgumentException("Duplicate member id: " + member.id());
      }
    }
  }

  @Override
  public Optional<Member> findById(String id) {
    return Optional.ofNullable(members.get(id));
  }
}
