package ru.iuturakulov.mybudgetbackend.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudgetbackend.services.VerifiedEmailCache

fun Application.startEmailCleanupJob(cache: VerifiedEmailCache) {
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    environment.monitor.subscribe(ApplicationStarted) {
        scope.launch {
            while (isActive) {
                delay(60_000)
                cache.clearExpired()
            }
        }
    }

    environment.monitor.subscribe(ApplicationStopping) {
        scope.cancel()
    }
}