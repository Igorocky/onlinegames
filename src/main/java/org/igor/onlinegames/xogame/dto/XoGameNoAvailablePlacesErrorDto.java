package org.igor.onlinegames.xogame.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class XoGameNoAvailablePlacesErrorDto extends XoGameErrorDto {
    public XoGameNoAvailablePlacesErrorDto(String errorDescription) {
        super(errorDescription);
    }
}
