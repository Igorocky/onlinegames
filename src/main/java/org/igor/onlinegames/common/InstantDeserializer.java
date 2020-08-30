package org.igor.onlinegames.common;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.Instant;

public class InstantDeserializer extends StdDeserializer<Instant> {

    public InstantDeserializer() {
        this(null);
    }

    protected InstantDeserializer(Class<Instant> t) {
        super(t);
    }

    @Override
    public Instant deserialize(JsonParser jsonparser, DeserializationContext context) throws IOException {
        String instantStr = jsonparser.getText();
        if (instantStr == null) {
            return null;
        } else {
            return Instant.parse(instantStr);
        }
    }
}