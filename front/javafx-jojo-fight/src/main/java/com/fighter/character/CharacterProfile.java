package com.fighter.character;

public record CharacterProfile(
        String id,
        String name,
        String avatar,
        double imageHeight,
        String[] idle,
        String[] punch,
        String[] kick,
        String[] guard,
        String[] hit
) {
    public String[] framesFor(String action) {
        return switch (action) {
            case "punch" -> punch;
            case "kick" -> kick;
            case "guard" -> guard;
            case "hit" -> hit;
            default -> idle;
        };
    }
}
