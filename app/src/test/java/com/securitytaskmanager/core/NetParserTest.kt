package com.securitytaskmanager.core

import org.junit.Assert.*
import org.junit.Test

class NetParserTest {

    @Test fun ipv4() {
        assertEquals("127.0.0.1" to 8080, NetParser.parseAddr("0100007F:1F90"))
        assertEquals("192.168.1.5" to 443, NetParser.parseAddr("0501A8C0:01BB"))
        assertEquals("0.0.0.0" to 0, NetParser.parseAddr("00000000:0000"))
    }

    @Test fun ipv6() {
        assertEquals("::" to 22, NetParser.parseAddr("00000000000000000000000000000000:0016"))
        assertEquals("::1" to 80, NetParser.parseAddr("00000000000000000000000001000000:0050"))
        // ::ffff:10.0.0.2 as written by the kernel
        assertEquals("10.0.0.2" to 5555, NetParser.parseAddr("0000000000000000FFFF00000200000A:15B3"))
    }

    @Test fun badAddr() {
        assertNull(NetParser.parseAddr("zz:12")); assertNull(NetParser.parseAddr("0100007F")); assertNull(NetParser.parseAddr("0100:0050"))
    }

    @Test fun tcpLines() {
        val lines = listOf(
            "  sl  local_address rem_address   st tx_queue rx_queue tr tm->when retrnsmt   uid  timeout inode",
            "   0: 00000000:15B3 00000000:0000 0A 00000000:00000000 00:00000000 00000000     0        0 11111 1 0000000000000000 100 0 0 10 0",
            "   1: 0501A8C0:D2A4 0101A8C0:01BB 01 00000000:00000000 00:00000000 00000000 10123        0 22222 1 0000000000000000 20 4 30 10 -1")
        val c = NetParser.parse("tcp", lines)
        assertEquals(2, c.size)
        assertEquals("LISTEN", c[0].state); assertEquals(5555, c[0].localPort); assertTrue(c[0].isListening); assertTrue(c[0].isWildcardBind)
        assertEquals("ESTABLISHED", c[1].state); assertEquals("192.168.1.1", c[1].remoteAddr); assertEquals(443, c[1].remotePort)
        assertFalse(c[1].isListening); assertEquals(22222L, c[1].inode)
    }

    @Test fun udpUnconnected() {
        val c = NetParser.parse("udp", listOf("   3: 0100007F:0035 00000000:0000 07 00000000:00000000 00:00000000 00000000     0        0 33333 2 0"))
        assertEquals("UNCONN", c[0].state); assertTrue(c[0].isListening); assertFalse(c[0].isWildcardBind)
    }
}
