package org.igor.onlinegames.wordsgame.manager;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TextToken {
    private String value;

    private Boolean unsplittable;
    private Boolean ignored;
    private Boolean meta;
    private Boolean active;

    public boolean isIgnored() {
        return isTrue(ignored);
    }

    public boolean isUnsplittable() {
        return isTrue(unsplittable);
    }

    public boolean isMeta() {
        return isTrue(meta);
    }

    public boolean isActive() {
        return isTrue(active);
    }

    private boolean isTrue(Boolean flag) {
        return flag != null && flag;
    }
}
