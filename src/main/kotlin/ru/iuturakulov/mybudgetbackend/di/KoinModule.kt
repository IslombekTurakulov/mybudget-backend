package ru.iuturakulov.mybudgetbackend.di

import org.koin.dsl.module
import ru.iuturakulov.mybudgetbackend.controller.user.UserController
import ru.iuturakulov.mybudgetbackend.controller.user.UserProfileController

val controllerModule = module {
    single { UserController() }
    single { UserProfileController() }
}