package org.igor.onlinegames.common;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class GamePlayersCounts {
    private final Random rnd = new Random();
    private Map<Set<UUID>,Map<UUID,int[]>> counts = new HashMap<>();

    public synchronized List<UUID> getAndUpdateOrderOfPlayers(Collection<UUID> userIds) {
        final HashSet<UUID> uniqueUserIds = new HashSet<>(userIds);
        final ArrayList<UUID> result = new ArrayList<>();
        for (int orderNumber = 0; orderNumber < userIds.size(); orderNumber++) {
            result.add(definePlayerForOrder(orderNumber, uniqueUserIds, result));
        }
        updateCounts(result);
        return result;
    }

    protected Map<Set<UUID>, Map<UUID, int[]>> getCounts() {
        return counts;
    }

    private void updateCounts(List<UUID> orderedUserIds) {
        final Set<UUID> uniqueUserIds = new HashSet<>(orderedUserIds);
        final Map<UUID, int[]> existingCounts = counts.get(uniqueUserIds);
        if (existingCounts == null) {
            Map<UUID,int[]> newCounts = new HashMap<>();
            for (int orderNumber = 0; orderNumber < orderedUserIds.size(); orderNumber++) {
                final UUID userId = orderedUserIds.get(orderNumber);
                newCounts.put(userId, new int[orderedUserIds.size()]);
                newCounts.get(userId)[orderNumber] = 1;
            }
            counts.put(uniqueUserIds, newCounts);
        } else {
            for (int orderNumber = 0; orderNumber < orderedUserIds.size(); orderNumber++) {
                final UUID userId = orderedUserIds.get(orderNumber);
                existingCounts.get(userId)[orderNumber] += 1;
            }
        }
    }

    private UUID definePlayerForOrder(int orderNumber, HashSet<UUID> uniqueUserIds, List<UUID> alreadySelectedUsers) {
        if (alreadySelectedUsers.size() != orderNumber) {
            log.error("alreadySelectedUsers.size() != orderNumber");
        }
        final Set<UUID> availableUserIds = uniqueUserIds.stream()
                .filter(userId -> !alreadySelectedUsers.contains(userId))
                .collect(Collectors.toSet());
        final Map<UUID, int[]> existingCounts = counts.get(uniqueUserIds);
        if (existingCounts == null) {
            return new ArrayList<>(availableUserIds).get(rnd.nextInt(availableUserIds.size()));
        } else {
            final Map<UUID, Integer> countsForOrder = existingCounts.entrySet().stream()
                    .filter(userIdToCounts -> availableUserIds.contains(userIdToCounts.getKey()))
                    .collect(Collectors.toMap(
                            userIdToCounts -> userIdToCounts.getKey(),
                            userIdToCounts -> userIdToCounts.getValue()[orderNumber]
                    ));
            final Integer minCnt = countsForOrder.values().stream().min(Integer::compareTo).get();
            final List<UUID> availableUserIdsWithMinCnt = countsForOrder.entrySet().stream()
                    .filter(userIdAndCount -> minCnt.equals(userIdAndCount.getValue()))
                    .map(userIdAndCount -> userIdAndCount.getKey())
                    .collect(Collectors.toList());
            return availableUserIdsWithMinCnt.get(rnd.nextInt(availableUserIdsWithMinCnt.size()));
        }
    }
}
