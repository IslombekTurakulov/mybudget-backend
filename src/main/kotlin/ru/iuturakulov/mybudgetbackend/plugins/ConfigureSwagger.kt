package ru.iuturakulov.mybudgetbackend.plugins

import io.github.smiley4.ktorswaggerui.SwaggerUI
import io.github.smiley4.ktorswaggerui.data.AuthScheme
import io.github.smiley4.ktorswaggerui.data.AuthType
import io.github.smiley4.ktorswaggerui.dsl.routing.route
import io.github.smiley4.ktorswaggerui.routing.openApiSpec
import io.github.smiley4.ktorswaggerui.routing.swaggerUI
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.swagger.v3.oas.models.media.Schema
import java.io.File

fun Application.configureSwagger() {
    install(SwaggerUI) {
        security {
            securityScheme("jwtToken") {
                type = AuthType.HTTP
                scheme = AuthScheme.BEARER
            }
            defaultSecuritySchemeNames("jwtToken")
            defaultUnauthorizedResponse {
                description = "Unauthorized access"
            }
        }
        info {
            title = "MyBudget Backend"
            version = "1.0.0"
            description = "API documentation for MyBudget Diploma Project"
            contact {
                name = "Turakulov Islombek Ulugbekovich"
                email = "me@turakulov.ru"
            }
        }
        server {
            url = "http://localhost:8080/"
            description = "Dev"
        }
        server {
            url = ".."
            description = "Prod"
        }
        schemas {
            overwrite<File>(Schema<Any>().also {
                it.type = "string"
                it.format = "binary"
            })
        }
        routing {
            route("api.json") {
                openApiSpec()
            }
            route("swagger") {
                swaggerUI("/api.json")
            }
            get {
                call.respondRedirect("/swagger/index.html?url=/api.json", true)
            }
        }
    }
}