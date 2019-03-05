package io.feaggle.server

import io.feaggle.server.infrastructure.journal.NoopConfigurationInterest
import io.feaggle.server.infrastructure.journal.NoopJournalListener
import io.feaggle.server.infrastructure.resources.ReleaseToggleController
import io.vlingo.actors.World
import io.vlingo.http.resource.Resources
import io.vlingo.http.resource.Server
import io.vlingo.lattice.model.sourcing.SourcedTypeRegistry
import io.vlingo.symbio.store.DataFormat
import io.vlingo.symbio.store.common.jdbc.Configuration
import io.vlingo.symbio.store.journal.Journal
import io.vlingo.symbio.store.journal.jdbc.postgres.PostgresJournalActor

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

    private fun initRegistry() {
         registry = SourcedTypeRegistry(world)
    }

    private fun initServer() {
        val resources = Resources.are(
            ReleaseToggleController(10).asResource()
        )

        server = Server.startWith(world.stage(),
            resources,
            port,
            io.vlingo.http.resource.Configuration.Sizing.define(),
            io.vlingo.http.resource.Configuration.Timing.define())
    }
}