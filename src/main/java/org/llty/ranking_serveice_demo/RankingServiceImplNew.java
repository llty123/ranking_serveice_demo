package org.llty.ranking_serveice_demo;

import org.llty.ranking_serveice_demo.dtos.RankInfo;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.Tuple;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;


public class RankingServiceImplNew implements RankingService{
    //存储 玩家id-得分的排行
    private static final String PLAYER_SCORE_KEY = "leaderboard:player_score";
    //存储 已经出现的不同得分的排行
    private static final String SCORE_RANK_KEY = "leaderboard:score_rank";

    private JedisPool jedisPool;

    public RankingServiceImplNew(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    /**
     * 更新玩家积分
     *
     * @return
     */
    @Override
    public void updateScore(String playerId, int score, long timestamp) {
        try (Jedis jedis = jedisPool.getResource()) {
            Transaction tx = jedis.multi();

            // 更新玩家分数
            tx.zadd(PLAYER_SCORE_KEY, score, playerId);

            // 确保分数在scoreRank中存在
            tx.zadd(SCORE_RANK_KEY, score, String.valueOf(score));

        }
    }

    /**
     * 查询玩家当前排名
     */
    @Override
    public RankInfo getPlayerRank(String playerId) {
        try (Jedis jedis = jedisPool.getResource()) {
            // 获取玩家分数
            Double score = jedis.zscore(PLAYER_SCORE_KEY, playerId);
            if (score == null) {
                return null;
            }

            // 计算排名
            long rank = calculateRank(jedis, score);

            return new RankInfo(playerId, score, rank);
        }
    }

    /**
     * 计算分数对应的排名
     */
    private long calculateRank(Jedis jedis, double score) {
        // 获取所有比当前分数大的不同分数数量
        Long higherScoreCount = jedis.zcount(SCORE_RANK_KEY, String.valueOf(score + 0.1), "+inf");
        // 排名 = 比当前分数大的不同分数数量 + 1
        return higherScoreCount + 1;
    }

    /**
     * 获取前N名玩家
     */
    @Override
    public List<RankInfo> getTopNPlayers(int n) {
        try (Jedis jedis = jedisPool.getResource()) {
            List<RankInfo> result = new ArrayList<>();

            // 获取前N个不同的分数（从高到低）
            Set<String> topScores = jedis.zrevrangeByScore(SCORE_RANK_KEY, "+inf", "-inf", 0, n);

            // 为每个分数获取对应的玩家
            for (String scoreStr : topScores) {
                double score = Double.parseDouble(scoreStr);
                long rank = calculateRank(jedis, score);

                // 获取该分数的所有玩家
                Set<String> players = jedis.zrangeByScore(PLAYER_SCORE_KEY, score, score);
                for (String playerId : players) {
                    result.add(new RankInfo(playerId, score, rank));
                }
            }

            return result;
        }
    }

    /**
     * 查询自己名次前后共N名玩家
     */
    @Override
    public List<RankInfo> getPlayerRankRange(String playerId, int range) {
        try (Jedis jedis = jedisPool.getResource()) {
            // 先获取玩家自己的排名
            Long playerRank = jedis.zrevrank(PLAYER_SCORE_KEY, playerId);
            if (playerRank == null) {
                return Collections.emptyList(); // 玩家不存在
            }

            // 计算查询范围
            long start = Math.max(0, playerRank - range/2-1);
            long end = playerRank + range;

            // 获取范围内的玩家
            Set<Tuple> aroundPlayers = jedis.zrevrangeWithScores(PLAYER_SCORE_KEY, start, end);

            List<RankInfo> result = new ArrayList<>();
            for (Tuple tuple : aroundPlayers) {
                String pid = tuple.getElement();
                double score = tuple.getScore();
                long rank = calculateRank(jedis, score);
                result.add(new RankInfo(pid, score, rank));
            }
            return result;
        }
    }
    
    
}