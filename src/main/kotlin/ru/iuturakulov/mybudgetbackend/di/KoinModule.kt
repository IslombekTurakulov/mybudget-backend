package ru.iuturakulov.mybudgetbackend.di

import org.koin.dsl.module
import ru.iuturakulov.mybudgetbackend.controller.user.UserController

//val repositoryModule = module {
//    single { UserRepository() }
//    single { UserProfileRepository() }
//}
//
//val serviceModule = module {
//    single { UserService(get()) }
//    single { UserProfileService(get()) }
//}

val controllerModule = module {
    single { UserController(get(), get()) }
}

val appModule = listOf(controllerModule)
