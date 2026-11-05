package com.connectsdk.service.capability;

import java.util.regex.Pattern;

public interface CapabilityMethods {
    Pattern ANY_PATTERN = Pattern.compile(".+\\.(?=Any)");

    enum CapabilityPriorityLevel {
        NOT_SUPPORTED(0),
        VERY_LOW(1),
        LOW(25),
        NORMAL(50),
        HIGH(75),
        VERY_HIGH(100);

        private final int value;

        CapabilityPriorityLevel(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
