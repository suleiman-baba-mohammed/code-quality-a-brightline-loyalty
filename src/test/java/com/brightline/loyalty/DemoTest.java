package com.brightline.loyalty;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;


class DemoTest {

  private static final String EXPECTED_OUTPUT =
      String.join(
              System.lineSeparator(),
              "Result m1 (STANDARD), GHS 100 -> 10 points",
              "Result m2 (GOLD), GHS 250 -> 50 points",
              "Result unknown member, GHS 100 -> REJECTED: Member not found: unknown",
              "Result m1, GHS -5 (malformed) -> REJECTED: "
                  + "Purchase amount must be greater than zero: -5")
          + System.lineSeparator();

  private final ByteArrayOutputStream captured = new ByteArrayOutputStream();
  private final PrintStream out = new PrintStream(captured, true, StandardCharsets.UTF_8);

  @Test
  void printsTheResultOfEachScenarioAndShowsRejectionsAsRejections() {
    new Demo(new LoyaltyConfig().defaultCalculator(), out).run();

    assertEquals(EXPECTED_OUTPUT, captured.toString(StandardCharsets.UTF_8));
  }

  @Test
  @ResourceLock(Resources.SYSTEM_OUT)
  void mainRunsTheDemoOnTheConsole() {
    PrintStream console = System.out;
    System.setOut(out);
    try {
      Demo.main(new String[0]);
    } finally {
      System.setOut(console);
    }

    assertEquals(EXPECTED_OUTPUT, captured.toString(StandardCharsets.UTF_8));
  }
}
