# ranking_serveice_demo
乐堡互娱后端开发笔试题

1. 系统主要基于redis实现
2. 对于第一个场景，实现类在org.llty.ranking_serveice_demo.RankingServiceImpl中
   1. 在这个场景下，主要基于redis的zset实现，通过设计分数的计算公式，即zsetScore=score * SCORE_FACTOR + (MAX_TIMESTAMP - current_timestamp)，使得排行榜的排序可以满足先按分数，再按达成时间排序的要求
3. 对于第二个场景，实现类在org.llty.ranking_serveice_demo.RankingServiceImplNew中
   1. 在这个场景，使用两个zset，其中，一个zset存储玩家以及对应的分数；另一个zset存储所有出现的去重分数值
   2. 在写入的时候，会通过redis事务，保证原子更新这三个数据
   3. 在查询的时候，会通过玩家id拿到玩家的分数，再根据玩家的分数到去重分数集合中拿到这个分数对应的排名，从而实现密集排行的效果
4. 由于系统的数据只存储在redis上，因此一致性得以满足。通过redis的持久化机制（RDB+AOF）满足数据的持久化要求；
