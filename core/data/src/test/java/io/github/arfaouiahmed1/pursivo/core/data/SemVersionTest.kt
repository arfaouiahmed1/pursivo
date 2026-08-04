package io.github.arfaouiahmed1.pursivo.core.data

import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class SemVersionTest {
    @Test fun stableBeatsPrerelease() {
        assertTrue(SemVersion.parse("1.0.0")!! > SemVersion.parse("1.0.0-beta.3")!!)
    }

    @Test fun prereleaseNumbersSortNumerically() {
        assertTrue(SemVersion.parse("0.1.0-beta.10")!! > SemVersion.parse("0.1.0-beta.2")!!)
    }

    @Test fun prereleaseChannelOrderingStillAllowsNewerStable() {
        assertTrue(SemVersion.parse("0.2.0")!! > SemVersion.parse("0.2.0-rc.9")!!)
        assertTrue(SemVersion.parse("0.2.0-alpha.2")!! > SemVersion.parse("0.2.0-alpha.1")!!)
    }

    @Test fun malformedVersionsFailClosed() {
        assertNull(SemVersion.parse("v1.0"))
        assertNull(SemVersion.parse("1.0.0.1"))
        assertNull(SemVersion.parse("latest"))
    }
}
