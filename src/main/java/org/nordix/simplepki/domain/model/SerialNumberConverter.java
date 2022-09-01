package org.nordix.simplepki.domain.model;

import lombok.experimental.UtilityClass;

import java.math.BigInteger;

@UtilityClass
public class SerialNumberConverter {

    public static final String REGEXP = "^[\\dA-F]{16}$";

    public String toString(BigInteger serialNumber) {
        return toString(serialNumber.longValue());
    }

    public String toString(long serialNumber) {
        return String.format("%016X", serialNumber);
    }

    public long fromString(String serialNumber) {
        return Long.parseLong(serialNumber, 16);
    }
}
