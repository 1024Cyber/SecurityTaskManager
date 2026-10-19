package com.securitytaskmanager.core

import java.net.InetAddress

/** Parses /proc/net/{tcp,tcp6,udp,udp6}. */
object NetParser {

    private val TCP_STATES = mapOf(
        "01" to "ESTABLISHED", "02" to "SYN_SENT", "03" to "SYN_RECV", "04" to "FIN_WAIT1",
        "05" to "FIN_WAIT2", "06" to "TIME_WAIT", "07" to "CLOSE", "08" to "CLOSE_WAIT",
        "09" to "LAST_ACK", "0A" to "LISTEN", "0B" to "CLOSING"
    )

    fun parse(protocol: String, lines: List<String>): List<Connection> {
        val isUdp = protocol.startsWith("udp")
        val out = ArrayList<Connection>()
        for (l in lines) {
            val p = l.trim().split(Regex("\\s+"))
            // sl local rem st tx:rx tr:when retrnsmt uid timeout inode ...
            if (p.size < 10 || !p[0].endsWith(":")) continue
            val (la, lp) = parseAddr(p[1]) ?: continue
            val (ra, rp) = parseAddr(p[2]) ?: continue
            val st = p[3].uppercase()
            val state = if (isUdp && st == "07") "UNCONN" else TCP_STATES[st] ?: st
            val inode = p[9].toLongOrNull() ?: continue
            out.add(Connection(protocol, la, lp, ra, rp, state, inode))
        }
        return out
    }

    /** "0100007F:1F90" or 32-hex IPv6 form. Each 32-bit word is little-endian. */
    fun parseAddr(token: String): Pair<String, Int>? {
        val i = token.indexOf(':')
        if (i < 0) return null
        val hex = token.substring(0, i)
        val port = token.substring(i + 1).toIntOrNull(16) ?: return null
        if (hex.length != 8 && hex.length != 32) return null
        val bytes = ByteArray(hex.length / 2)
        // reverse bytes inside each 4-byte word
        for (w in 0 until hex.length / 8) {
            for (b in 0 until 4) {
                val src = w * 8 + b * 2
                val dst = w * 4 + (3 - b)
                bytes[dst] = (hex.substring(src, src + 2).toIntOrNull(16) ?: return null).toByte()
            }
        }
        return formatAddr(bytes) to port
    }

    private fun formatAddr(bytes: ByteArray): String {
        if (bytes.size == 4) return bytes.joinToString(".") { (it.toInt() and 0xFF).toString() }
        if (bytes.all { it == 0.toByte() }) return "::"
        val loop = ByteArray(16).also { it[15] = 1 }
        if (bytes.contentEquals(loop)) return "::1"
        // IPv4-mapped (::ffff:a.b.c.d)
        val mapped = (0 until 10).all { bytes[it] == 0.toByte() } &&
            bytes[10] == 0xFF.toByte() && bytes[11] == 0xFF.toByte()
        if (mapped) return bytes.copyOfRange(12, 16).joinToString(".") { (it.toInt() and 0xFF).toString() }
        return InetAddress.getByAddress(bytes).hostAddress?.substringBefore('%') ?: "?"
    }
}
