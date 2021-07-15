package eu.deltacraft.deltacraftteams.listeners

import eu.deltacraft.deltacraftteams.DeltaCraftTeams
import eu.deltacraft.deltacraftteams.managers.ClientManager
import eu.deltacraft.deltacraftteams.types.ConnectionResponse
import eu.deltacraft.deltacraftteams.utils.enums.ValidateError
import io.ktor.client.call.receive
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent

class PlayerJoinAttemptListener(
    private val plugin: DeltaCraftTeams,
    private val clientManager: ClientManager
) : Listener {
    private val logger = plugin.logger

    @EventHandler
    fun onPlayerAttemptJoinAsync(playerJoinEvent: AsyncPlayerPreLoginEvent) {
        runBlocking {
            val client = clientManager.getClient()

            val httpRes =
                client.get<HttpResponse>(path = "api/plugin/validate") {
                    parameter("nick", playerJoinEvent.name)
                    parameter("uuid", playerJoinEvent.uniqueId)
                }

            client.close()

            val status = httpRes.status

            if (status != HttpStatusCode.OK && status != HttpStatusCode.BadRequest) {
                logger.warning("Validate request for player ${playerJoinEvent.name} returned HTTP ${status.value}")
                return@runBlocking
            }

            val response = httpRes.receive<ConnectionResponse>()

            if (!response.content) {

                val message = when (response.getErrorEnum()) {
                    ValidateError.NotRegistered -> "You have to be registered!"
                    ValidateError.MissingConsent -> "You have to accept our consent!"
                    ValidateError.NotInTeam -> "You have to join a team!"
                    else -> "Server error :-("
                }

                plugin.logger.warning("Player ${playerJoinEvent.name} tried to join, but error occurred: \"$message\"")
                playerJoinEvent.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                    Component.text(message)
                )
            }
        }
    }


}