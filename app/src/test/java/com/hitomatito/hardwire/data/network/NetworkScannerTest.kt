package com.hitomatito.hardwire.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkScannerTest {

    @Test
    fun `calculateRange with 24-bit prefix returns addresses`() {
        val result = NetworkScanner.calculateRange("192.168.1.100", 24)
        assertTrue("Should return non-empty list", result.isNotEmpty())
        assertEquals("192.168.1.1", result.first())
        assertEquals("192.168.1.254", result.last())
        assertEquals(254, result.size)
    }

    @Test
    fun `calculateRange with 30-bit prefix returns 2 addresses`() {
        val result = NetworkScanner.calculateRange("192.168.1.100", 30)
        assertEquals(2, result.size)
        assertEquals(listOf("192.168.1.101", "192.168.1.102"), result)
    }

    @Test
    fun `calculateRange with 32-bit prefix returns empty list`() {
        val result = NetworkScanner.calculateRange("192.168.1.100", 32)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `calculateRange with too few octets returns empty list`() {
        val result = NetworkScanner.calculateRange("192.168.1", 24)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `calculateRange with empty string returns empty list`() {
        val result = NetworkScanner.calculateRange("", 24)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `calculateRange with negative prefix returns empty list`() {
        val result = NetworkScanner.calculateRange("192.168.1.1", -1)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `large subnet is rejected instead of scanning thousands of addresses`() {
        assertTrue(NetworkScanner.calculateRange("10.0.0.1", 16).isEmpty())
    }

    @Test
    fun `invalid input returns empty`() {
        assertTrue(NetworkScanner.calculateRange("not-an-ip", 24).isEmpty())
        assertTrue(NetworkScanner.calculateRange("192.168.1.1", 0).isEmpty())
        assertTrue(NetworkScanner.calculateRange("192.168.1.1", 31).isEmpty())
    }
}
