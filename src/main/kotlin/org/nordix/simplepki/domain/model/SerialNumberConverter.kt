package org.nordix.simplepki.domain.model

import java.math.BigInteger

object SerialNumberConverter {

    @JvmStatic
    fun toString(serialNumber: BigInteger): String {
        return toString(serialNumber.toLong())
    }

    @JvmStatic
    fun toString(serialNumber: Long): String {
        return String.format("%016X", serialNumber)
    }

    fun fromString(serialNumber: String): Long {
        return serialNumber.toLong(16)
    }

    const val REGEXP = "^[\\dA-F]{16}$"
}
