package com.xnotes.core.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ToolbarGeometryResolverTest {
    @Test fun automaticDimensionsRemainAutomatic() {
        assertEquals(ToolbarResolvedSize(null, null), resolveToolbarSize(ToolbarGeometry()))
    }

    @Test fun manualDimensionsAreClampedToSafeRange() {
        val result = resolveToolbarSize(
            ToolbarGeometry(widthDp = 1, heightDp = 99999),
            minDp = 48,
            maxDp = 512,
        )
        assertEquals(ToolbarResolvedSize(48, 512), result)
    }

    @Test fun malformedNullsDoNotBecomeForcedDimensions() {
        val result = resolveToolbarSize(ToolbarGeometry(widthDp = null, heightDp = null))
        assertNull(result.widthDp)
        assertNull(result.heightDp)
    }
}
