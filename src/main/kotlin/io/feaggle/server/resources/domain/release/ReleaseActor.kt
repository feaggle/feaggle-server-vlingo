/**
 * This file is part of feaggle-server.
 *
 * feaggle-server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * feaggle-server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with feaggle-server.  If not, see <https://www.gnu.org/licenses/>.
 **/
package io.feaggle.server.resources.domain.release

import io.feaggle.server.library.infrastructure.journal.register
import io.feaggle.server.library.infrastructure.journal.withConsumer
import io.vlingo.lattice.model.sourcing.EventSourced
import io.vlingo.lattice.model.sourcing.SourcedTypeRegistry
import io.vlingo.symbio.store.journal.Journal
import java.time.LocalDateTime

class ReleaseActor(
    val id: Release.ReleaseId,
    var information: Release.ReleaseInformation,
    var status: Release.ReleaseStatus
): EventSourced(), Release {
    constructor(id: Release.ReleaseId): this(id, Release.ReleaseInformation(""), Release.ReleaseStatus(false))

    override fun streamName() = "/project/${id.project}/${id.name}"

    // Command
    override fun build(releaseDeclaration: Release.ReleaseDeclaration) {
        if (information.description != releaseDeclaration.description) {
            apply(Release.ReleaseDescriptionChanged(id, releaseDeclaration.description, LocalDateTime.now()))
        }

        if (status.active != releaseDeclaration.active) {
            apply(Release.ReleaseStatusChanged(id, releaseDeclaration.active, LocalDateTime.now()))
        }
    }

    override fun release(active: Boolean) {
        if (status.active != active) {
            apply(Release.ReleaseStatusChanged(id, active, LocalDateTime.now()))
        }
    }

    // Events
    fun whenDescriptionChanged(event: Release.ReleaseDescriptionChanged) {
        information = information.copy(description = event.newDescription)
    }

    fun whenStatusChanged(event: Release.ReleaseStatusChanged) {
        status = status.copy(active = event.newStatus)
    }
}

fun bootstrapReleaseActorConsumers(registry: SourcedTypeRegistry, journal: Journal<String>) {
    registry.register<ReleaseActor>(journal)
        .withConsumer(ReleaseActor::whenDescriptionChanged)
        .withConsumer(ReleaseActor::whenStatusChanged)
}