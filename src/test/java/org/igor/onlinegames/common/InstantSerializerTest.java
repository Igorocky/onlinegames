package org.igor.onlinegames.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.igor.onlinegames.xogame.dto.history.XoGameRecord;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;

public class InstantSerializerTest {
    private ObjectMapper mapper = new ObjectMapper();
    @Test
    public void null_instant_values_are_serialized_and_deserialized_correctly() throws IOException {
        //given
        final XoGameRecord dto1 = XoGameRecord.builder()
                .startedAt(null)
                .build();
        final String dto1Str = mapper.writeValueAsString(dto1);

        //when
        final XoGameRecord dto2 = mapper.readValue(dto1Str, XoGameRecord.class);

        //then
        Assert.assertNull(dto2.getStartedAt());
    }

    @Test
    public void nonnull_instant_values_are_serialized_and_deserialized_correctly() throws IOException {
        //given
        final Instant expectedStartedAt = Instant.now();
        final XoGameRecord dto1 = XoGameRecord.builder()
                .startedAt(expectedStartedAt)
                .build();
        final String dto1Str = mapper.writeValueAsString(dto1);

        //when
        final XoGameRecord dto2 = mapper.readValue(dto1Str, XoGameRecord.class);

        //then
        Assert.assertEquals(expectedStartedAt, dto2.getStartedAt());
    }

}