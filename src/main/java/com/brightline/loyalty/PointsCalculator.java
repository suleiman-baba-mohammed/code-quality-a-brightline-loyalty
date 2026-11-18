package com.brightline.loyalty;

public class PointsCalculator {

    // The calculator makes its own repository. Convenient, but it means callers
    // and tests cannot give it a different one.
    private final MemberRepository repository = new InMemoryMemberRepository();

    public int calculatePoints(String memberId, double amount) {
        try {
            Member member = repository.findById(memberId);

            // base points: one point for every GHS 10 spent
            int points = (int) (amount / 10);

            // apply the member's tier multiplier
            if (member.getTier().equals("STANDARD")) {
                points = points * 1;
            } else if (member.getTier().equals("SILVER")) {
                points = (int) (points * 1.5);
            } else if (member.getTier().equals("GOLD")) {
                points = points * 2;
            }

            // loyalty bonus for big spenders
            if (amount > 500) {
                points = points + 50;
            }

            System.out.println("Awarded " + points + " points to " + member.getName());
            return points;
        } catch (Exception e) {
            // something went wrong (for example the member was not found) - award nothing
            return 0;
        }
    }
}
