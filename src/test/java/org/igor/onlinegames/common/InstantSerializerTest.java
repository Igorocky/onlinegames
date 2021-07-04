package org.igor.onlinegames.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.igor.onlinegames.xogame.dto.history.XoGameRecordDto;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class InstantSerializerTest {
    private ObjectMapper mapper = new ObjectMapper();
    @Test
    public void null_instant_values_are_serialized_and_deserialized_correctly() throws IOException {
        //given
        final XoGameRecordDto dto1 = XoGameRecordDto.builder()
                .startedAt(null)
                .build();
        final String dto1Str = mapper.writeValueAsString(dto1);

        //when
        final XoGameRecordDto dto2 = mapper.readValue(dto1Str, XoGameRecordDto.class);

        //then
        assertNull(dto2.getStartedAt());
    }

    @Test
    public void nonnull_instant_values_are_serialized_and_deserialized_correctly() throws IOException {
        //given
        final Instant expectedStartedAt = Instant.now();
        final XoGameRecordDto dto1 = XoGameRecordDto.builder()
                .startedAt(expectedStartedAt)
                .build();
        final String dto1Str = mapper.writeValueAsString(dto1);

        //when
        final XoGameRecordDto dto2 = mapper.readValue(dto1Str, XoGameRecordDto.class);

        //then
        assertEquals(expectedStartedAt, dto2.getStartedAt());
    }

}