package com.f1.api.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class GapDeserializer extends JsonDeserializer<Double> {

    private static final Logger log = LoggerFactory.getLogger(GapDeserializer.class);

    @Override
    public Double deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            log.debug("Non-numeric gap value '{}', returning null", value);
            return null;
        }
    }
}
