package org.nordix.simplepki.domain.model

import java.math.BigInteger
import java.util.*

object SerialNumberConverter {

    @JvmStatic
    fun toString(serialNumber: BigInteger): String {
        return toString(serialNumber.toLong())
    }

    @JvmStatic
    fun toString(serialNumber: Long): String {
        return String.format(Locale.getDefault(Locale.Category.FORMAT), "%016X", serialNumber)
    }

    fun fromString(serialNumber: String): Long {
        return serialNumber.toLong(HEX_RADIX)
    }

    const val REGEXP = "^[\\dA-F]{16}$"
    private const val HEX_RADIX = 16
}
