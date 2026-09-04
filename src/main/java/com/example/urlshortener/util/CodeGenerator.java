package com.example.urlshortener.util;

import org.apache.commons.text.RandomStringGenerator;

public class CodeGenerator {
    private static final RandomStringGenerator gen = new RandomStringGenerator.Builder()
            .withinRange('0', 'z')
            .filteredBy(ch -> (ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z'))
            .build();

    public static String randomCode(int length) {
        return gen.generate(length);
    }
}
