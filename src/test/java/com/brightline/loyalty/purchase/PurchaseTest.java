package com.brightline.loyalty.purchase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.brightline.loyalty.exception.InvalidPurchaseException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PurchaseTest {

  @ParameterizedTest
  @CsvSource({"0.01", "12.50", "500", "1000000.99"})
  void acceptsAndKeepsAPositiveAmountExactly(BigDecimal amount) {
    assertEquals(amount, new Purchase(amount).amount());
  }

  @Test
  void rejectsAMissingAmount() {
    InvalidPurchaseException e =
        assertThrows(InvalidPurchaseException.class, () -> new Purchase(null));

    assertEquals("Purchase amount is required", e.getMessage());
  }

  @ParameterizedTest
  @CsvSource({"0", "0.00", "-0.01", "-5", "-100.00"})
  void rejectsAZeroOrNegativeAmountAndSaysWhichOne(BigDecimal amount) {
    InvalidPurchaseException e =
        assertThrows(InvalidPurchaseException.class, () -> new Purchase(amount));

    assertTrue(e.getMessage().contains(amount.toString()), e.getMessage());
  }
}
