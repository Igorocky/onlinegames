package org.igor.onlinegames.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class StateInfoDto {
    private UUID stateId;
    private String stateType;
    private String createdAt;
    private String lastInMsgAt;
    private String lastOutMsgAt;
    private Object viewRepresentation;
}
