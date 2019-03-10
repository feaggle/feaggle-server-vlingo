package io.feaggle.server.resources.domain.release

import io.feaggle.server.base.UnitTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ReleaseTest: UnitTest() {
    private lateinit var release: Release

    @BeforeEach
    internal fun setUp() {
        release = world().actorFor(
            Release::class.java, ReleaseActor::class.java, Release.ReleaseId("boundary", "project", "my-release")
        )
    }

    @Test
    internal fun shouldDetectChangesInDescription() {
        waitForEvents(1)

        release.build(Release.ReleaseDeclaration("boundary", "project", "my-release", "new-description"))

        assertEquals(
            "new-description",
            appliedEventAs<Release.ReleaseDescriptionChanged>(0).newDescription
        )
    }
}