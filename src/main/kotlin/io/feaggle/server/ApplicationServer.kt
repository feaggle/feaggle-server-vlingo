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
package io.feaggle.server

import io.feaggle.server.library.domain.library.bootstrapLibraryConsumers
import io.feaggle.server.library.infrastructure.journal.NoopConfigurationInterest
import io.feaggle.server.library.infrastructure.journal.NoopJournalListener
import io.feaggle.server.library.infrastructure.resources.ProjectController
import io.feaggle.server.resources.domain.declaration.bootstrapDeclarationActorConsumers
import io.feaggle.server.resources.domain.project.bootstrapResourceProjectActorConsumers
import io.feaggle.server.resources.domain.release.bootstrapReleaseActorConsumers
import io.feaggle.server.resources.infrastructure.DeclarationController
import io.vlingo.actors.World
import io.vlingo.http.resource.Resources
import io.vlingo.http.resource.Server
import io.vlingo.lattice.model.sourcing.SourcedTypeRegistry
import io.vlingo.symbio.store.DataFormat
import io.vlingo.symbio.store.common.jdbc.Configuration
import io.vlingo.symbio.store.journal.Journal
import io.vlingo.symbio.store.journal.jdbc.postgres.PostgresJournalActor
import org.flywaydb.core.Flyway

class ApplicationServer(
    private val dbUrl: String,
    private val dbUser: String,
    private val dbPassword: String,
    private val port: Int
) {
    private lateinit var world: World
    private lateinit var journal: Journal<String>
    private lateinit var registry: SourcedTypeRegistry
    private lateinit var server: Server

    fun start() {
        initWorld()
        initJournal()
        initRegistry()
        initJournalConsumers()
        initServer()
    }

    fun stop() {
        world.terminate()
        server.shutDown()
    }

    private fun initWorld() {
        world = World.start("feaggle")
    }

    private fun initJournal() {
        Flyway(
            Flyway.configure()
                .dataSource(dbUrl, dbUser, dbPassword)
        ).migrate()

        val journalConfiguration = Configuration(
            NoopConfigurationInterest(), // You will need to create your own ConfigurationInterest
            "org.postgresql.Driver",
            DataFormat.Text,
            dbUrl,
            "",
            dbUser,
            dbPassword,
            false,
            "",
            false
        )

        journal = Journal.using(world.stage(), PostgresJournalActor::class.java, NoopJournalListener(), journalConfiguration)
    }

    private fun initJournalConsumers() {
        bootstrapLibraryConsumers(registry, journal)
        bootstrapResourceProjectActorConsumers(registry, journal)
        bootstrapReleaseActorConsumers(registry, journal)
        bootstrapDeclarationActorConsumers(registry, journal)
    }

    private fun initRegistry() {
         registry = SourcedTypeRegistry(world)
    }

    private fun initServer() {
        val resources = Resources.are(
            ProjectController(world, journal).asResource(10),
            DeclarationController(world).asResource(10)
        )

        server = Server.startWith(world.stage(),
            resources,
            port,
            io.vlingo.http.resource.Configuration.Sizing.define(),
            io.vlingo.http.resource.Configuration.Timing.define())
    }
}