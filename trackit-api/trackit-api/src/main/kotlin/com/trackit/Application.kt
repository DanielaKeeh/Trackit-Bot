package com.trackit

import com.trackit.plugins.configureDatabases
import com.trackit.plugins.configureSecurity
import com.trackit.plugins.configureSerialization
import com.trackit.plugins.configureStatusPages
import com.trackit.repositories.ObjectRepository
import com.trackit.repositories.ReminderRepository
import com.trackit.repositories.TaskRepository
import com.trackit.repositories.UserRepository
import com.trackit.routes.authRoutes
import com.trackit.routes.objectRoutes
import com.trackit.routes.predictRoutes
import com.trackit.routes.reminderRoutes
import com.trackit.routes.taskRoutes
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(CallLogging)
    install(CORS) {
        anyHost() // en producción: restringir al dominio/app real
        allowHeader("Authorization")
        allowHeader("Content-Type")
        allowMethod(io.ktor.http.HttpMethod.Get)
        allowMethod(io.ktor.http.HttpMethod.Post)
        allowMethod(io.ktor.http.HttpMethod.Put)
        allowMethod(io.ktor.http.HttpMethod.Delete)
    }

    configureSerialization()
    configureStatusPages()
    configureDatabases()
    configureSecurity()

    val userRepository = UserRepository()
    val objectRepository = ObjectRepository()
    val reminderRepository = ReminderRepository()
    val taskRepository = TaskRepository()

    routing {
        get("/health") { call.respond(mapOf("status" to "ok")) }

        authRoutes(userRepository)
        objectRoutes(objectRepository)
        reminderRoutes(reminderRepository)
        taskRoutes(taskRepository)
        predictRoutes(objectRepository)
    }
}
