package com.brightline.loyalty;

public class Member {
    private final String id;
    private final String name;
    private final String tier; // "STANDARD", "SILVER", "GOLD"

    public Member(String id, String name, String tier) {
        this.id = id;
        this.name = name;
        this.tier = tier;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getTier() { return tier; }
}
