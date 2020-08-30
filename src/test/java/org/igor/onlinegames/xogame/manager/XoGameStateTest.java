package org.igor.onlinegames.xogame.manager;

import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

public class XoGameStateTest {
    @Test
    public void historyFilePathIsCorrect() {
        //given
        final Instant currTime = Instant.from(ZonedDateTime.of(
                2020, 8, 30, 15, 45, 23, 0, ZoneOffset.UTC
        ));
        final UUID gameId = UUID.fromString("4d5c9617-cb79-4a4e-8d9c-8e3518de49b2");

        //when
        final String path = XoGameState.getGameHistoryFilePath("app_dir/games_history/xo_game", currTime, gameId);

        //then
        Assert.assertEquals(
                "app_dir/games_history/xo_game/2020_08_30/xogame-4d5c9617-cb79-4a4e-8d9c-8e3518de49b2.json",
                path
        );
    }

}