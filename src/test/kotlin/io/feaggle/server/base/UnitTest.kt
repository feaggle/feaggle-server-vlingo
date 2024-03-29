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
package io.feaggle.server.base

import com.google.common.flogger.FluentLogger
import com.google.gson.Gson
import io.feaggle.server.resources.domain.declaration.bootstrapDeclarationActorConsumers
import io.feaggle.server.resources.domain.project.bootstrapResourceProjectActorConsumers
import io.feaggle.server.resources.domain.release.bootstrapReleaseActorConsumers
import io.vlingo.actors.testkit.TestUntil
import io.vlingo.actors.testkit.TestWorld
import io.vlingo.lattice.model.DomainEvent
import io.vlingo.lattice.model.sourcing.SourcedTypeRegistry
import io.vlingo.symbio.Entry
import io.vlingo.symbio.State
import io.vlingo.symbio.store.journal.Journal
import io.vlingo.symbio.store.journal.JournalListener
import io.vlingo.symbio.store.journal.inmemory.InMemoryJournalActor
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.util.*

private const val DEFAULT_TIMEOUT: Long = 50000

abstract class UnitTest: JournalListener<String> {
    private val logger = FluentLogger.forEnclosingClass()

    private lateinit var mutexName: String
    private lateinit var testWorld: TestWorld
    private lateinit var journal: Journal<String>
    private lateinit var registry: SourcedTypeRegistry
    private lateinit var appliedEvents: List<Entry<String>>
    private lateinit var until: TestUntil

    @BeforeEach
    internal fun setUpWorld() {
        testWorld = TestWorld.startWithDefaults(javaClass.simpleName)
        journal = Journal.using(testWorld.stage(), InMemoryJournalActor::class.java, this)
        registry = SourcedTypeRegistry(testWorld.world())
        appliedEvents = emptyList()

        bootstrapResourceProjectActorConsumers(registry, journal)
        bootstrapReleaseActorConsumers(registry, journal)
        bootstrapDeclarationActorConsumers(registry, journal)

        mutexName = UUID.randomUUID().toString()
    }

    @AfterEach
    internal fun tearDownWorld() {
        testWorld.terminate()
    }

    fun waitForEvents(a: Int) {
        until = TestUntil.happenings(a)
    }

    fun world() = testWorld.world()

    fun appliedEvents(): List<Entry<String>> {
        until.completesWithin(DEFAULT_TIMEOUT)

        return appliedEvents
    }

    inline fun <reified T: DomainEvent> appliedEventAs(idx: Int): T = Gson().fromJson(appliedEvents()[idx].entryData, T::class.java)

    override fun appendedAll(entries: MutableList<Entry<String>>?) {
        logger.atInfo().log("appendedAll: %s", entries!!.joinToString())

        appliedEvents += entries
        entries.forEach { _ -> until.happened() }
    }

    override fun appendedAllWith(entries: MutableList<Entry<String>>?, snapshot: State<String>?) {
        logger.atInfo().log("appendedAllWith: %s", entries!!.joinToString())

        appliedEvents += entries
        entries.forEach { _ -> until.happened() }
    }

    override fun appended(entry: Entry<String>?) {
        logger.atInfo().log("appended: %s", entry)

        until.happened()
        appliedEvents += entry!!
    }

    override fun appendedWith(entry: Entry<String>?, snapshot: State<String>?) {
        logger.atInfo().log("appendedWith: %s", entry)

        until.happened()
        appliedEvents += entry!!
    }
}