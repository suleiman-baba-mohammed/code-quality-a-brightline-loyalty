package com.brightline.loyalty.member;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MemberTest {

  @Test
  void keepsItsFields() {
    Member member = new Member("m9", "Efua Asante", MembershipTier.SILVER);

    assertAll(
        () -> assertEquals("m9", member.id()),
        () -> assertEquals("Efua Asante", member.name()),
        () -> assertEquals(MembershipTier.SILVER, member.tier()));
  }

  @Test
  void rejectsANullId() {
    assertThrows(
        IllegalArgumentException.class, () -> new Member(null, "Efua", MembershipTier.GOLD));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "\t"})
  void rejectsABlankId(String id) {
    assertThrows(IllegalArgumentException.class, () -> new Member(id, "Efua", MembershipTier.GOLD));
  }

  @Test
  void rejectsANullName() {
    assertThrows(IllegalArgumentException.class, () -> new Member("m9", null, MembershipTier.GOLD));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "\t"})
  void rejectsABlankName(String name) {
    assertThrows(IllegalArgumentException.class, () -> new Member("m9", name, MembershipTier.GOLD));
  }

  @Test
  void rejectsANullTier() {
    assertThrows(IllegalArgumentException.class, () -> new Member("m9", "Efua", null));
  }
}
