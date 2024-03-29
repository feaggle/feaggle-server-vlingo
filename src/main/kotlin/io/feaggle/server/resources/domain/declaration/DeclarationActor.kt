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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import io.feaggle.server.library.infrastructure.journal.register
import io.feaggle.server.library.infrastructure.journal.withConsumer
import io.feaggle.server.resources.domain.project.Project
import io.feaggle.server.resources.domain.project.Projects
import io.feaggle.server.resources.domain.release.Release
import io.feaggle.server.resources.domain.release.Releases
import io.vlingo.lattice.model.DomainEvent
import io.vlingo.lattice.model.sourcing.EventSourced
import io.vlingo.lattice.model.sourcing.SourcedTypeRegistry
import io.vlingo.symbio.store.journal.Journal
import java.time.LocalDateTime

private const val EXPECTED_VERSION = "0.0.1"

class DeclarationActor(
    val id: Declaration.DeclarationId,
    var resources: Set<String>
) : EventSourced(), Declaration {
    private val mapper = ObjectMapper(YAMLFactory())

    constructor(id: Declaration.DeclarationId) : this(id, emptySet())

    override fun streamName() = "/declaration/${id.name}"

    // Commands
    override fun build(declaration: String) {
        val tree = mapper.readTree(declaration)
        val declarationTree = tree["declaration"]
        val version = declarationTree["version"].asText()

        if (version != EXPECTED_VERSION) {
            return
        }

        val resourceDict = declarationTree["resources"]
        val resources = resourceDict.fields().asSequence().toList()

        val resourceNames = resources.map { it.key }.toSet()
        resources.forEach {
            val name = it.key
            val value = it.value

            when (value["is-a"].asText()) {
                "project" -> {
                    val projectId = Project.ProjectId(id.name)
                    val ownerDeclarations = value["owners"].map {
                        Project.ProjectOwnerDeclaration(it["name"].asText(), it["email"].asText())
                    }

                    val projectDeclaration = Project.ProjectDeclaration(
                        id.name,
                        name,
                        value["description"].asText(),
                        ownerDeclarations
                    )

                    Projects.oneOf(stage(), projectId)
                        .andThenConsume { it.build(projectDeclaration) }
                }

                "release" -> {
                    val projectId = value["in-project"].asText()
                    val description = value["description"].asText()
                    val active = value["active"].asBoolean()
                    val releaseId = Release.ReleaseId(projectId, name)

                    val releaseDeclaration =
                        Release.ReleaseDeclaration(id.name, projectId, name, description, active)

                    Releases.oneOf(stage(), releaseId)
                        .andThenConsume { it.build(releaseDeclaration) }
                }

            }
        }

        apply(dropNotFoundResources(resourceNames) + findNewResources(resourceNames))
    }

    private fun dropNotFoundResources(declaredResources: Set<String>): List<DomainEvent> {
        val droppedResources = resources.filter { !declaredResources.contains(it) }
        return droppedResources.map {
            Declaration.DeclarationResourceDropped(id, it, LocalDateTime.now())
        }
    }

    private fun findNewResources(declaredResources: Set<String>): List<DomainEvent> {
        val newResources = declaredResources.filter { !resources.contains(it) }
        return newResources.map {
            Declaration.DeclarationResourceFound(id, it, LocalDateTime.now())
        }
    }

    // Events
    fun whenResourceFound(event: Declaration.DeclarationResourceFound) {
        resources += event.resource
    }

    fun whenResourceDropped(event: Declaration.DeclarationResourceDropped) {
        resources -= event.resource
    }
}

fun bootstrapDeclarationActorConsumers(registry: SourcedTypeRegistry, journal: Journal<String>) {
    registry.register<DeclarationActor>(journal)
        .withConsumer(DeclarationActor::whenResourceFound)
        .withConsumer(DeclarationActor::whenResourceDropped)
}