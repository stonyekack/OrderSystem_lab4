package com.hsb.mkss1.adapters.console.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.NoSuchElementException;

public final class Input {
    private static final BufferedReader READER = new BufferedReader(new InputStreamReader(System.in));

    private Input() { }

    public static String readString() {
        try {
            return READER.readLine();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public static int readInt() {
        while (true) {
            String value = readString();
            if (value == null) throw new NoSuchElementException("Input closed");
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException exception) {
                System.out.print("Please enter a whole number: ");
            }
        }
    }
}
