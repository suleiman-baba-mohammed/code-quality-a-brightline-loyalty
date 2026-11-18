package com.brightline.loyalty;

// A tiny runner so you can see the calculator work before you change anything.
public class Demo {
    public static void main(String[] args) {
        PointsCalculator calculator = new PointsCalculator();

        System.out.println("Result m1 (STANDARD), GHS 100  -> " + calculator.calculatePoints("m1", 100));
        System.out.println("Result m2 (GOLD), GHS 250      -> " + calculator.calculatePoints("m2", 250));
        System.out.println("Result unknown member, GHS 100 -> " + calculator.calculatePoints("unknown", 100));
    }
}
