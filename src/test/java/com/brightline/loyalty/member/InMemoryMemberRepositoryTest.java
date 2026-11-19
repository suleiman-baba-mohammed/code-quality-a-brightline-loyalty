package com.brightline.loyalty.member;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InMemoryMemberRepositoryTest {

  private final Member ama = new Member("a1", "Ama Owusu", MembershipTier.GOLD);

  @Test
  void findsAMemberById() {
    InMemoryMemberRepository repository = new InMemoryMemberRepository(List.of(ama));

    assertEquals(Optional.of(ama), repository.findById("a1"));
  }

  @Test
  void returnsEmptyNotNullForAnUnknownId() {
    InMemoryMemberRepository repository = new InMemoryMemberRepository(List.of(ama));

    assertEquals(Optional.empty(), repository.findById("nobody"));
  }

  @Test
  void returnsEmptyForANullId() {
    InMemoryMemberRepository repository = new InMemoryMemberRepository(List.of(ama));

    assertEquals(Optional.empty(), repository.findById(null));
  }

  @Test
  void isNotAffectedWhenTheCallerLaterChangesTheListItWasGiven() {
    List<Member> source = new ArrayList<>(List.of(ama));
    InMemoryMemberRepository repository = new InMemoryMemberRepository(source);

    source.clear();

    assertEquals(Optional.of(ama), repository.findById("a1"));
  }

  @Test
  void refusesTwoMembersWithTheSameId() {
    Member impostor = new Member("a1", "Someone Else", MembershipTier.STANDARD);
    List<Member> members = List.of(ama, impostor);

    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> new InMemoryMemberRepository(members));

    assertTrue(e.getMessage().contains("a1"), e.getMessage());
  }

  @Test
  void refusesAMissingMemberList() {
    assertThrows(NullPointerException.class, () -> new InMemoryMemberRepository(null));
  }
}
