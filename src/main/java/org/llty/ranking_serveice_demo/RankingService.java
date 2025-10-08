package org.llty.ranking_serveice_demo;

import org.llty.ranking_serveice_demo.dtos.RankInfo;

import java.util.List;

public interface RankingService {
    void updateScore(String playerId, int score, long timestamp);

    RankInfo getPlayerRank(String playerId);

    List<RankInfo> getTopNPlayers(int n);

    List<RankInfo> getPlayerRankRange(String playerId, int range);
}
