package ru.iuturakulov.mybudgetbackend.routing

import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.github.smiley4.ktorswaggerui.dsl.routing.put
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.iuturakulov.mybudgetbackend.controller.transaction.TransactionController
import ru.iuturakulov.mybudgetbackend.extensions.AppException
import ru.iuturakulov.mybudgetbackend.extensions.AuditLogService
import ru.iuturakulov.mybudgetbackend.extensions.RoutingExtensions.apiResponse
import ru.iuturakulov.mybudgetbackend.extensions.RoutingExtensions.respondBadRequest
import ru.iuturakulov.mybudgetbackend.extensions.RoutingExtensions.respondUnauthorized
import ru.iuturakulov.mybudgetbackend.models.transaction.AddTransactionRequest
import ru.iuturakulov.mybudgetbackend.models.transaction.UpdateTransactionRequest
import ru.iuturakulov.mybudgetbackend.models.user.body.JwtTokenBody

fun Route.transactionRoute(transactionController: TransactionController, auditLogService: AuditLogService) {
    authenticate("auth-jwt") {
        route("projects/{projectId}/transactions") {

            // Получить список транзакций в проекте
            get({
                tags("Transactions")
                summary = "Получить список транзакций проекта"
                request {
                    pathParameter<String>("projectId") { description = "ID проекта" }
                }
                apiResponse()
            }) {
                try {
                    val userId = call.principal<JwtTokenBody>()?.userId ?: return@get call.respondUnauthorized()
                    val projectId =
                        call.parameters["projectId"] ?: return@get call.respondBadRequest("Project ID is required")

                    val transactions = transactionController.getProjectTransactions(userId, projectId)
                    auditLogService.logAction(userId, "Запрошен список транзакций проекта: $projectId")
                    call.respond(HttpStatusCode.OK, transactions)
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        e.localizedMessage
                    )
                }
            }

            // Получить одну транзакцию по ID
            get("{transactionId}", {
                tags("Transactions")
                protected = true
                summary = "Получить информацию о транзакции"
                request {
                    pathParameter<String>("projectId") { description = "ID проекта" }
                    pathParameter<String>("transactionId") { description = "ID транзакции" }
                }
                apiResponse()
            }) {
                try {
                    val userId = call.principal<JwtTokenBody>()?.userId ?: return@get call.respondUnauthorized()
                    val projectId =
                        call.parameters["projectId"] ?: return@get call.respondBadRequest("Project ID is required")
                    val transactionId =
                        call.parameters["transactionId"]
                            ?: return@get call.respondBadRequest("Transaction ID is required")

                    val transaction = transactionController.getTransactionById(userId, projectId, transactionId)
                    auditLogService.logAction(userId, "Запрошена транзакция $transactionId в проекте $projectId")
                    call.respond(HttpStatusCode.OK, transaction)
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        e.localizedMessage
                    )
                }
            }

            // Добавить новую транзакцию
            post({
                tags("Transactions")
                protected = true
                summary = "Добавить транзакцию"
                request {
                    pathParameter<String>("projectId") { description = "ID проекта" }
                    body<AddTransactionRequest>()
                }
                apiResponse()
            }) {
                try {
                    val userId = call.principal<JwtTokenBody>()?.userId ?: return@post call.respondUnauthorized()
                    val projectId =
                        call.parameters["projectId"] ?: return@post call.respondBadRequest("Project ID is required")
                    val requestBody = call.receive<AddTransactionRequest>().copy(projectId = projectId)

                    val transaction = transactionController.addTransaction(userId, requestBody)
                    auditLogService.logAction(userId, "Добавлена транзакция ${transaction.name} в проекте $projectId")
                    call.respond(HttpStatusCode.Created, transaction)
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        e.localizedMessage
                    )
                }
            }

            // Обновить транзакцию
            put("{transactionId}", {
                tags("Transactions")
                protected = true
                summary = "Обновить транзакцию"
                request {
                    pathParameter<String>("projectId") { description = "ID проекта" }
                    pathParameter<String>("transactionId") { description = "ID транзакции" }
                    body<UpdateTransactionRequest>()
                }
                apiResponse()
            }) {
                val userId = call.principal<JwtTokenBody>()?.userId ?: return@put call.respondUnauthorized()
                val projectId =
                    call.parameters["projectId"] ?: return@put call.respondBadRequest("Project ID is required")
                val transactionId =
                    call.parameters["transactionId"] ?: return@put call.respondBadRequest("Transaction ID is required")
                val requestBody = call.receive<UpdateTransactionRequest>().copy(transactionId = transactionId)

                try {
                    val success = transactionController.updateTransaction(userId, requestBody)
                    auditLogService.logAction(userId, "Обновлена транзакция $transactionId в проекте $projectId")
                    call.respond(HttpStatusCode.OK, success)
                } catch (e: AppException.Authorization) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        "Вы не можете удалить эту транзакцию"
                    )
                } catch (e: AppException.NotFound.Project) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        "Транзакция не найдена"
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        e.localizedMessage
                    )
                }
            }

            // Удалить транзакцию
            delete("{transactionId}", {
                tags("Transactions")
                protected = true
                summary = "Удалить транзакцию"
                request {
                    pathParameter<String>("projectId") { description = "ID проекта" }
                    pathParameter<String>("transactionId") { description = "ID транзакции" }
                }
                apiResponse()
            }) {
                try {
                    val userId = call.principal<JwtTokenBody>()?.userId ?: return@delete call.respondUnauthorized()
                    val projectId =
                        call.parameters["projectId"] ?: return@delete call.respondBadRequest("Project ID is required")
                    val transactionId =
                        call.parameters["transactionId"]
                            ?: return@delete call.respondBadRequest("Transaction ID is required")

                    transactionController.deleteTransaction(userId, transactionId)
                    auditLogService.logAction(userId, "Удалена транзакция $transactionId в проекте $projectId")
                    call.respond(HttpStatusCode.OK, "Транзакция удалена")
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        e.localizedMessage
                    )
                }
            }
        }
    }
}
