//
// Name                 Kamil Wierzbinski
// Student ID           S2433508
// Programme of Study   BSc (Hons) Computing
//
package org.me.gcu.wierzbinski_kamil_s2433508;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.me.gcu.wierzbinski_kamil_s2433508.ui.DisplayFormat;

import java.time.Duration;

/**
 * Local unit tests for the texts used for lengths of time (no Android needed).
 */
public class DisplayFormatTest {

    /** Lengths just under a whole number of years are rounded, not shown as "12 months". */
    @Test
    public void longDurationsAreRoundedToWholeMonths() {
        assertEquals("3 years", DisplayFormat.duration(Duration.ofDays(1094)));
        assertEquals("3 years", DisplayFormat.duration(Duration.ofDays(1096)));
        assertEquals("1 year 1 month", DisplayFormat.duration(Duration.ofDays(400)));
        assertEquals("3 years 11 months", DisplayFormat.duration(Duration.ofDays(1446)));
    }

    /** Shorter lengths show months and days, days, or hours. */
    @Test
    public void shortDurationsShowMonthsDaysOrHours() {
        assertEquals("1 month 15 days", DisplayFormat.duration(Duration.ofDays(45)));
        assertEquals("11 months 29 days", DisplayFormat.duration(Duration.ofDays(364)));
        assertEquals("15 days", DisplayFormat.duration(Duration.ofDays(15)));
        assertEquals("5 hours", DisplayFormat.duration(Duration.ofHours(5)));
        assertEquals("-", DisplayFormat.duration(null));
    }
}
