package com.eilaji.backend.controller

import com.eilaji.backend.dto.ApiResponse
import com.eilaji.backend.security.ApiException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.log
import io.ktor.server.request.uri
import io.ktor.server.response.respond

/** Answers with a generic 500 and keeps the real cause in the server log. */
internal suspend fun ApplicationCall.serverError(cause: Exception) {
    application.log.error("Unhandled error on ${request.uri}", cause)
    respond(
        HttpStatusCode.InternalServerError,
        ApiResponse<Unit>(success = false, error = "Internal server error")
    )
}

/** Turns a deliberate domain error into its HTTP response without touching the client stack. */
internal suspend fun ApplicationCall.apiError(cause: Exception) {
    if (cause is ApiException) {
        respond(cause.status, ApiResponse<Unit>(success = false, error = cause.message))
    } else {
        serverError(cause)
    }
}

