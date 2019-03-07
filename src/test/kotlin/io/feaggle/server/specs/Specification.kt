package io.feaggle.server.specs

import io.feaggle.server.ApplicationServer
import io.restassured.RestAssured
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.testcontainers.containers.PostgreSQLContainer

abstract class Specification {
    companion object {
        private val postgreSql = PostgreSQLContainer<Nothing>()
        private val port = 9092
        private val baseUrl = "http://localhost:$port"

        init {
            postgreSql.start()
            RestAssured.baseURI = baseUrl
            RestAssured.config.httpClientConfig.dontReuseHttpClientInstance()
        }
    }

    private lateinit var applicationServer: ApplicationServer

    @BeforeEach
    internal fun setUp() {
        applicationServer = ApplicationServer(postgreSql.jdbcUrl, postgreSql.username, postgreSql.password, port)
        applicationServer.start()
    }

    @AfterEach
    internal fun tearDown() {
        applicationServer.stop()
    }
}