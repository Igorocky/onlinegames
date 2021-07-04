package org.igor.onlinegames.common;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GamePlayersCountsTest {
    @Test
    public void getAndUpdateOrderOfPlayers_distributes_counts_equally() {
        getAndUpdateOrderOfPlayers_distributes_counts_equally(5, 1000, 200);
        getAndUpdateOrderOfPlayers_distributes_counts_equally(4, 1000, 250);
        getAndUpdateOrderOfPlayers_distributes_counts_equally(3, 999, 333);
        getAndUpdateOrderOfPlayers_distributes_counts_equally(2, 1000, 500);
        getAndUpdateOrderOfPlayers_distributes_counts_equally(1, 1000, 1000);
    }

    private void getAndUpdateOrderOfPlayers_distributes_counts_equally(int numOfUserIds, int numOfIterations, int expectedCount) {
        //given
        final Set<UUID> userIds = new HashSet<>();
        for (int i = 0; i < numOfUserIds; i++) {
            userIds.add(UUID.randomUUID());
        }
        Stats stats = new Stats(userIds);
        GamePlayersCounts gamePlayersCounts = new GamePlayersCounts();

        //when
        for (int i = 0; i < numOfIterations; i++) {
            List<UUID> orderedUserIds = gamePlayersCounts.getAndUpdateOrderOfPlayers(userIds);
            for (int orderNumber = 0; orderNumber < orderedUserIds.size(); orderNumber++) {
                stats.update(orderNumber, orderedUserIds.get(orderNumber));
            }
        }

        //then
        assertEquals(1, gamePlayersCounts.getCounts().size());
        final Map<UUID, int[]> idToCounts = gamePlayersCounts.getCounts().get(userIds);
        assertEquals(numOfUserIds, idToCounts.size());
        for (int[] counts : idToCounts.values()) {
            assertEquals(numOfUserIds, counts.length);
            for (int count : counts) {
                assertTrue(expectedCount -1 <= count && count <= expectedCount + 1);
            }
        }

        for (int orderNumber = 0; orderNumber < numOfUserIds; orderNumber++) {
            for (UUID userId : userIds) {
                final Integer count = stats.stats.get(orderNumber).get(userId);
                assertTrue(expectedCount - 1 <= count && count <= expectedCount + 1);
            }
        }
    }

    private static class Stats {
        public Map<Integer, Map<UUID, Integer>> stats;

        public Stats(Set<UUID> ids) {
            stats = new HashMap<>();
            for (int orderNumber = 0; orderNumber < ids.size(); orderNumber++) {
                stats.put(
                        orderNumber,
                        new HashMap<>(
                                ids.stream().collect(Collectors.toMap(
                                        Function.identity(),
                                        any -> 0
                                ))
                        )
                );
            }
        }

        public void update(int orderNumber, UUID userId) {
            Integer currCnt = stats.get(orderNumber).get(userId);
            stats.get(orderNumber).put(userId, currCnt+1);
        }
    }

}