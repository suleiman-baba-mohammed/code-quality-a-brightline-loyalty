package com.brightline.loyalty;

import com.brightline.loyalty.exception.LoyaltyException;
import com.brightline.loyalty.purchase.Purchase;
import java.io.PrintStream;
import java.math.BigDecimal;

/** A tiny runner so you can see the calculator work, including how it reports a failure. */
public final class Demo {

  private final PointsCalculator calculator;
  private final PrintStream out;

  Demo(PointsCalculator calculator, PrintStream out) {
    this.calculator = calculator;
    this.out = out;
  }

  /** Runs the demo against the sample members and prints to the console. */
  public static void main(String[] args) {
    new Demo(new LoyaltyConfig().defaultCalculator(), System.out).run();
  }

  void run() {
    show(new Scenario("m1 (STANDARD), GHS 100", "m1", "100"));
    show(new Scenario("m2 (GOLD), GHS 250", "m2", "250"));
    show(new Scenario("unknown member, GHS 100", "unknown", "100"));
    show(new Scenario("m1, GHS -5 (malformed)", "m1", "-5"));
  }

  private void show(Scenario scenario) {
    out.println("Result " + scenario.label() + " -> " + outcomeOf(scenario));
  }

  private String outcomeOf(Scenario scenario) {
    try {
      Purchase purchase = new Purchase(new BigDecimal(scenario.amount()));
      return calculator.calculatePoints(scenario.memberId(), purchase) + " points";
    } catch (LoyaltyException e) {
      return "REJECTED: " + e.getMessage();
    }
  }

  private record Scenario(String label, String memberId, String amount) {}
}
