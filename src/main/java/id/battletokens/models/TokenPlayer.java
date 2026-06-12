package id.battletokens.models;

import java.util.UUID;

public class TokenPlayer {
    private final UUID uuid;
    private String name;
    private long tokens;
    private long totalEarned;
    private long lastUpdated;

    public TokenPlayer(UUID uuid, String name, long tokens, long totalEarned, long lastUpdated) {
        this.uuid = uuid;
        this.name = name;
        this.tokens = tokens;
        this.totalEarned = totalEarned;
        this.lastUpdated = lastUpdated;
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getTokens() { return tokens; }
    public long getTotalEarned() { return totalEarned; }
    public long getLastUpdated() { return lastUpdated; }

    public void addTokens(long amount) {
        this.tokens += amount;
        this.totalEarned += amount;
        this.lastUpdated = System.currentTimeMillis();
    }

    public boolean takeTokens(long amount) {
        if (tokens < amount) return false;
        this.tokens -= amount;
        this.lastUpdated = System.currentTimeMillis();
        return true;
    }

    public boolean hasTokens(long amount) { return tokens >= amount; }
}
