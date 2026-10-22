package com.hitomatito.hardwire.data.device

import org.junit.Assert.assertTrue
import org.junit.Test

class CpuClusterTest {

    @Test
    fun `labels two plus six clusters using frequencies`() {
        val parts = List(2) { "0x804" } + List(6) { "0x805" }
        val frequencies = List(2) { 2_304_000L } + List(6) { 1_804_800L }
        val indices = (0 until 8).toList()

        val result = buildClusterInfo(
            cpuParts = parts,
            cpuImplementers = List(8) { "0x51" },
            socName = "Qualcomm Snapdragon 732G",
            cpuMaxFreqs = frequencies,
            cpuIndices = indices
        )

        assertTrue(result.contains("2 x Kryo 470 (Gold)"))
        assertTrue(result.contains("6 x Kryo 470 (Silver)"))
    }

    @Test
    fun `labels four plus four clusters using frequencies`() {
        val parts = List(4) { "0xd03" } + List(4) { "0xd09" }
        val frequencies = List(4) { 1_800_000L } + List(4) { 2_200_000L }
        val indices = (0 until 8).toList()

        val result = buildClusterInfo(
            cpuParts = parts,
            cpuImplementers = List(8) { "0x41" },
            cpuMaxFreqs = frequencies,
            cpuIndices = indices
        )

        assertTrue(result.contains("4 x Cortex-A73 (Gold)"))
        assertTrue(result.contains("4 x Cortex-A53 (Silver)"))
    }

    @Test
    fun `falls back to largest cluster when frequencies are unavailable`() {
        val parts = List(2) { "0x804" } + List(6) { "0x805" }

        val result = buildClusterInfo(
            cpuParts = parts,
            cpuImplementers = List(8) { "0x51" },
            socName = "Qualcomm Snapdragon 732G"
        )

        assertTrue(result.contains("2 x Kryo 470 (Gold)"))
        assertTrue(result.contains("6 x Kryo 470 (Silver)"))
    }
}
