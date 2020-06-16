package org.igor.onlinegames.xogame.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = XoGameStateDto.class, name = "state"),
        @JsonSubTypes.Type(value = XoGameMsgDto.class, name = "msg")
})
public interface XoGameDto {
}
