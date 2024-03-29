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
package io.feaggle.server.resources.domain.declaration

import io.feaggle.server.library.infrastructure.world.actor
import io.feaggle.server.library.infrastructure.world.addressOfString
import io.feaggle.server.resources.domain.release.Release
import io.feaggle.server.resources.domain.release.ReleaseActor
import io.vlingo.actors.Definition
import io.vlingo.actors.Stage
import io.vlingo.common.Completes
import io.vlingo.lattice.model.DomainEvent
import java.time.LocalDateTime

interface Declaration {
    data class DeclarationId(val name: String) {
        fun toAddress() = "/declaration/$name"
    }

    data class DeclarationResourceFound(val id: DeclarationId, val resource: String, val happened: LocalDateTime) :
        DomainEvent(1)

    data class DeclarationResourceDropped(val id: DeclarationId, val resource: String, val happened: LocalDateTime) :
        DomainEvent(1)

    fun build(declaration: String)
}

object Declarations {
    fun oneOf(stage: Stage, id: Declaration.DeclarationId): Completes<Declaration> {
        val address = stage.world().addressOfString(id.toAddress())
        return stage.world().actor<Declaration, DeclarationActor>(arrayOf(id), address)
    }
}