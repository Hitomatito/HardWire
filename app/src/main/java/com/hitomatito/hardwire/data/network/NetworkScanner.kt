package com.hitomatito.hardwire.data.network

import android.content.Context
import android.util.Log
import android.net.ConnectivityManager
import android.net.LinkProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Socket

object NetworkScanner {

    private const val MAX_SCAN_HOSTS = 1024

    data class WifiInfo(val ip: String, val prefixLength: Int)

    fun getLocalNetwork(context: Context): WifiInfo? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return null
        val net = cm.activeNetwork ?: return null
        val props: LinkProperties = cm.getLinkProperties(net) ?: return null
        for (link in props.linkAddresses) {
            val addr = link.address
            if (addr is Inet4Address && !addr.isLoopbackAddress) {
                val ip = addr.hostAddress ?: continue
                return WifiInfo(ip, link.prefixLength)
            }
        }
        return null
    }

    fun calculateRange(ip: String, prefixLength: Int): List<String> {
        val parts = ip.split(".").map { it.toIntOrNull() ?: return emptyList() }
        if (parts.size != 4 || parts.any { it !in 0..255 }) return emptyList()
        if (prefixLength !in 1..30) return emptyList()

        var ipLong = 0L
        for (part in parts) {
            ipLong = (ipLong shl 8) or part.toLong()
        }

        val mask = (0xFFFFFFFFL shl (32 - prefixLength)) and 0xFFFFFFFFL
        val network = ipLong and mask
        val broadcast = network or (mask.inv() and 0xFFFFFFFFL)
        val first = network + 1
        val last = broadcast - 1

        if (first > last) return emptyList()
        if (last - first + 1 > MAX_SCAN_HOSTS) return emptyList()

        return (first..last).map(::longToIp)
    }

    private fun longToIp(value: Long): String {
        return listOf(
            (value shr 24) and 0xFF,
            (value shr 16) and 0xFF,
            (value shr 8) and 0xFF,
            value and 0xFF
        ).joinToString(".")
    }

    suspend fun scanAdbDevices(context: Context, port: Int = 5555): List<String> = withContext(Dispatchers.IO) {
        val info = getLocalNetwork(context) ?: return@withContext emptyList()
        Log.d("HW:Net", "[scan] IP local=${info.ip}, prefix=${info.prefixLength}")
        val range = calculateRange(info.ip, info.prefixLength)
        Log.d("HW:Net", "[scan] rango=${range.size} ips (${if (range.isNotEmpty()) "${range.first()}..${range.last()}" else "vacio"})")
        if (range.isEmpty()) return@withContext emptyList()

        val semaphore = Semaphore(50)
        val found = mutableListOf<String>()

        range.map { ip ->
            async {
                semaphore.acquire()
                try {
                    try {
                        val socket = Socket()
                        socket.connect(InetSocketAddress(ip, port), 300)
                        socket.close()
                        synchronized(found) { found.add(ip) }
                    } catch (_: Exception) {}
                } finally {
                    semaphore.release()
                }
            }
        }.awaitAll()

        found.sorted()
    }
}
