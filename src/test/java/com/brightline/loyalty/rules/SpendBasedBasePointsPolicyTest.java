package com.brightline.loyalty.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.brightline.loyalty.purchase.Purchase;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SpendBasedBasePointsPolicyTest {

  private final SpendBasedBasePointsPolicy policy = new SpendBasedBasePointsPolicy();

  @ParameterizedTest
  @CsvSource({
    "0.01, 0",
    "9.99, 0", // just under one point
    "10, 1", // exactly one point
    "10.00, 1", // same amount, different scale
    "19.99, 1", // the remainder earns nothing
    "20, 2",
    "99.99, 9",
    "100, 10",
    "250, 25",
    "12345.67, 1234",
    "21474836479.99, 2147483647" // the largest amount whose points still fit in an int
  })
  void earnsOnePointPerWholeTenCedisAndDropsTheRemainder(BigDecimal amount, int expected) {
    assertEquals(expected, policy.basePointsFor(new Purchase(amount)));
  }

  @ParameterizedTest
  @CsvSource({"21474836480", "1000000000000"})
  void refusesAnAmountWhosePointsDoNotFitInAnInt(BigDecimal amount) {
    assertThrows(ArithmeticException.class, () -> policy.basePointsFor(new Purchase(amount)));
  }
}
