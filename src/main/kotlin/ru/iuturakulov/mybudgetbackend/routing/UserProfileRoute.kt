package ru.iuturakulov.mybudgetbackend.routing

import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.put
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.iuturakulov.mybudgetbackend.controller.user.UserProfileController
import ru.iuturakulov.mybudgetbackend.extensions.ApiExtensions.currentUser
import ru.iuturakulov.mybudgetbackend.extensions.ApiResponseState
import ru.iuturakulov.mybudgetbackend.extensions.RoutingExtensions.apiResponse
import ru.iuturakulov.mybudgetbackend.models.user.body.UserProfileBody

fun Route.userProfileRoute(userProfileController: UserProfileController) {
    route("user") {
        get({
            tags("User")
            apiResponse()
        }) {
            call.respond(
                ApiResponseState.success(
                    userProfileController.getProfile(call.currentUser().userId), HttpStatusCode.OK
                )
            )
        }
        put({
            tags("User")
            request {
                queryParameter<String>("firstName")
                queryParameter<String>("lastName")
                queryParameter<String>("userDescription")
            }
            apiResponse()
        }) {
            val params = UserProfileBody(
                firstName = call.parameters["firstName"],
                lastName = call.parameters["lastName"],
                userDescription = call.parameters["description"],
            )
            call.respond(
                ApiResponseState.success(
                    userProfileController.updateProfileInfo(call.currentUser().userId, params), HttpStatusCode.OK
                )
            )
        }
    }
}