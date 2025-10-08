package org.llty.ranking_serveice_demo;

import org.llty.ranking_serveice_demo.dtos.RankInfo;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Tuple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 基于Redis ZSet的排行榜系统
 */
public class RankingServiceImpl implements RankingService {

    private static final String LEADERBOARD_KEY = "game_leaderboard";
    private static final long SCORE_FACTOR = 10000000000000L;
    private static final long MAX_TIMESTAMP = 4102416000000L;
    private final JedisPool jedisPool;

    public RankingServiceImpl(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }


    /**
     * 更新玩家分数
     */
    @Override
    public void updateScore(String playerId, int score, long timestamp) {
        try (Jedis jedis = jedisPool.getResource()) {
            double zsetScore = calculateZSetScore(score, timestamp);

            jedis.zadd(LEADERBOARD_KEY, zsetScore, playerId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 计算ZSet的score值
     * 公式：score * SCORE_FACTOR + (MAX_TIMESTAMP - current_timestamp)
     * 这样可以保证：
     * 1. 分数高的排在前面
     * 2. 分数相同时，先达到该分数的玩家排在前面
     */
    private double calculateZSetScore(int score, long timestamp) {
        return (double) score * SCORE_FACTOR + (MAX_TIMESTAMP - timestamp);
    }

    /**
     * 从ZSet的score解析出原始分数和时间戳
     */
    private RankInfo parseZSetScore(String playerId, double zsetScore, Long rank) {
        int score = (int) (zsetScore / SCORE_FACTOR);
        long timestamp = MAX_TIMESTAMP - (long) (zsetScore % SCORE_FACTOR);
        return new RankInfo(playerId, score, rank != null ? rank.intValue() + 1 : -1, timestamp);
    }

    /**
     * 查询玩家当前排名
     */
    @Override
    public RankInfo getPlayerRank(String playerId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Double zsetScore = jedis.zscore(LEADERBOARD_KEY, playerId);
            if (zsetScore == null) {
                return null; // 玩家不存在
            }

            Long rank = jedis.zrevrank(LEADERBOARD_KEY, playerId);
            return parseZSetScore(playerId, zsetScore, rank);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取前N名玩家的分数和名次
     */
    @Override
    public List<RankInfo> getTopNPlayers(int n) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<Tuple> topN = jedis.zrevrangeWithScores(LEADERBOARD_KEY, 0, n - 1);

            List<RankInfo> result = new ArrayList<>();
            int rank = 1;
            for (Tuple tuple : topN) {
                String playerId = tuple.getElement();
                double zsetScore = tuple.getScore();
                result.add(parseZSetScore(playerId, zsetScore, (long) (rank - 1)));
                rank++;
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * 查询自己名次前后共N名玩家的分数和名次
     */
    @Override
    public List<RankInfo> getPlayerRankRange(String playerId, int range) {
        try (Jedis jedis = jedisPool.getResource()) {
            // 先获取玩家自己的排名
            Long playerRank = jedis.zrevrank(LEADERBOARD_KEY, playerId);
            if (playerRank == null) {
                return Collections.emptyList(); // 玩家不存在
            }

            // 计算查询范围
            long start = Math.max(0, playerRank - range/2-1);
            long end = playerRank + range;

            // 获取范围内的玩家
            Set<Tuple> aroundPlayers = jedis.zrevrangeWithScores(LEADERBOARD_KEY, start, end);

            List<RankInfo> result = new ArrayList<>();
            long currentRank = start + 1; // 排名从1开始
            for (Tuple tuple : aroundPlayers) {
                String pid = tuple.getElement();
                double zsetScore = tuple.getScore();
                result.add(parseZSetScore(pid, zsetScore, currentRank - 1));
                currentRank++;
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

}