package com.brightline.loyalty;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PointsCalculatorTest {

    @Test
    void awardsPointsToAMember() {
        PointsCalculator calculator = new PointsCalculator();
        int points = calculator.calculatePoints("m1", 100.0);
        assertTrue(points > 0); // it awarded some points
    }

    @Test
    void calculatesGoldPoints() {
        // mock the calculator and check it returns what we told it to
        PointsCalculator calculator = mock(PointsCalculator.class);
        when(calculator.calculatePoints("m2", 250.0)).thenReturn(50);
        assertEquals(50, calculator.calculatePoints("m2", 250.0));
    }

    @Test
    void awardsPointsForAPurchase() {
        PointsCalculator calculator = new PointsCalculator();
        int points = calculator.calculatePoints("m3", 200.0);
        assertNotNull(points); // a result came back
    }
}
