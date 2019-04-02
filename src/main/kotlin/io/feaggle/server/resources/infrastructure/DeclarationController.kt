package io.feaggle.server.resources.infrastructure

import com.google.common.flogger.FluentLogger
import io.feaggle.server.library.infrastructure.http.answerJson
import io.feaggle.server.resources.domain.declaration.Declaration
import io.feaggle.server.resources.domain.declaration.Declarations
import io.vlingo.actors.World
import io.vlingo.common.Completes
import io.vlingo.http.Response
import io.vlingo.http.resource.ResourceBuilder.put
import io.vlingo.http.resource.ResourceBuilder.resource

class DeclarationController(private val world: World) {
    private val logger = FluentLogger.forEnclosingClass()

    fun asResource(poolSize: Int) = resource("declarations", poolSize,
        put("/declaration/{name}")
            .param(String::class.java)
            .body(String::class.java)
            .handle(this::buildDeclaration)
            .onError(this::handleDeclarationError))

    private fun buildDeclaration(name: String, body: String): Completes<Response> {
        logger.atInfo().log("Received declaration `$name` to be built\n$body")
        return Declarations.oneOf(world.stage(), Declaration.DeclarationId(name))
            .andThenConsume { it.build(body) }
            .andThen { answerJson(Response.Status.Accepted) }

    }

    private fun handleDeclarationError(ex: Throwable): Completes<Response> {
        return Completes.withSuccess(answerJson(Response.Status.InternalServerError, ex.message ?: "Please, try again later."))
    }
}