package app.michaelwuensch.bitbanana.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import app.michaelwuensch.bitbanana.util.Version;

public class VersionTest {

    @Test
    public void verifyVersionResults() {

        Version a = new Version("1.1");
        Version b = new Version("1.1.1");
        assertEquals(-1, a.compareTo(b)); // return -1 (a<b)
        assertNotEquals(a, b);

        a = new Version("2.0");
        b = new Version("1.9.9");
        assertEquals(1, a.compareTo(b)); // return 1 (a>b)
        assertNotEquals(a, b);

        a = new Version("1.0");
        b = new Version("1");
        assertEquals(0, a.compareTo(b)); // return 0 (a=b)
        assertEquals(a, b);

        a = new Version("1");
        b = null;
        assertEquals(1, a.compareTo(b)); // return 1 (a>b)
        assertNotEquals(a, b);

        a = new Version("2.1");
        b = new Version("2.10");
        assertEquals(-1, a.compareTo(b)); // return 1 (a<b)
        assertNotEquals(a, b);
    }

}