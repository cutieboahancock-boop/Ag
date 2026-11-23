package com.example

import com.example.media.AspectPreset
import com.example.media.formatFileSize
import com.example.media.formatTimecode
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun formatTimecode_formatsMinutesSecondsTenths() {
        assertEquals("00:00.0", formatTimecode(0L))
        assertEquals("00:15.4", formatTimecode(15_400L))
        assertEquals("01:05.2", formatTimecode(65_200L))
        assertEquals("01:05", formatTimecode(65_200L, includeTenths = false))
    }

    @Test
    fun formatFileSize_formatsKbAndMb() {
        assertEquals("0 MB", formatFileSize(0L))
        assertEquals("512 KB", formatFileSize(512L * 1024L))
        assertEquals("4.5 MB", formatFileSize((4.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun aspectPreset_ratiosMatchExpectedShortsSpecs() {
        assertEquals(9f / 16f, AspectPreset.SHORTS_9_16.ratio!!, 0.001f)
        assertEquals(1f, AspectPreset.SQUARE_1_1.ratio!!, 0.001f)
        assertEquals(null, AspectPreset.ORIGINAL.ratio)
    }
}
