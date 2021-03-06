package org.igor.onlinegames.xogame.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@Data
public class XoGameErrorDto implements XoGameDto {
    private String errorDescription;
}
