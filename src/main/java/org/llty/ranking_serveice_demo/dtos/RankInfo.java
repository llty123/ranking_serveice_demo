package org.llty.ranking_serveice_demo.dtos;

/**
 * 玩家积分信息
 */
public class RankInfo {
    private String playerId;
    private double score;
    private long rank;
    private long timestamp;

    public RankInfo(String playerId, int score, int rank, long timestamp) {
        this.playerId = playerId;
        this.score = score;
        this.rank = rank;
        this.timestamp = timestamp;
    }

    public RankInfo(String playerId, double score, long rank) {
        this.playerId = playerId;
        this.score = score;
        this.rank = rank;
    }

    public String getPlayerId() {
        return playerId;
    }

    public double getScore() {
        return score;
    }

    public long getRank() {
        return rank;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("Player{id=%s, score=%d, rank=%d, time=%d}",
                playerId, score, rank, timestamp);
    }
}
