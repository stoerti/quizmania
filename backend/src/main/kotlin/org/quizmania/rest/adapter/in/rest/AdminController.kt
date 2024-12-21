package org.quizmania.rest.adapter.`in`.rest

import org.axonframework.commandhandling.gateway.CommandGateway
import org.quizmania.game.api.JoinGameCommand
import org.quizmania.game.api.CreateGameCommand
import org.quizmania.game.api.GameConfig
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*
import kotlin.random.Random

@RestController
@RequestMapping(value = ["/api/admin"], produces = [MediaType.APPLICATION_JSON_VALUE])
class AdminController(
    val commandGateway: CommandGateway
) {
    private val usernames = setOf(
        "Frodo",
        "Samwise",
        "Merry",
        "Pippin",
        "Gandalf",
        "Boromir",
        "Legolas",
        "Aragorn",
        "Gimli",
        "Bruce_banner",
        "tony_stark",
        "peter_parker",
        "steve_rogers",
        "wanda_maximoff",
        "clint_barton",
        "kate_bishop",
        "luke_skywalker",
        "leia_skywalker",
        "han_solo",
        "lando_calrissian",
        "chewbakka",
        "yoda",
        "anakin_skywalker",
        "rey",
        "odin",
        "loki",
        "thor",
        "freya",
        "Heimdall",
        "Zeus",
        "Hera",
        "Ares",
        "Poseidon",
        "Artemis",
        "Apollon",
        "Hermes",
        "Athene",
    )

    @PutMapping("/create-random-games")
    fun createRandomGames(@RequestParam("numberOfGames") numberOfGames: Int) {
        for (i in 1..numberOfGames) {
            val shuffledPlayers = usernames.shuffled()

            val isModerated = Random.nextBoolean()
            val maxPlayers = Random.nextInt(5, 10)
            val numPlayers = Random.nextInt(4, maxPlayers)

            val gameId = UUID.randomUUID()
            val gameNumber = Random.nextInt(1000, 9999)
            val gameName = if (isModerated) "${shuffledPlayers[0]}'s game $gameNumber" else "Game $gameNumber"
            val players = shuffledPlayers.subList(1, numPlayers)

            commandGateway.sendAndWait<Void>(
                CreateGameCommand(
                    gameId = gameId,
                    name = gameName,
                    config = GameConfig(
                        maxPlayers = maxPlayers,
                        questionSetId = "test01"
                    ),
                    creatorUsername = shuffledPlayers[1],
                    moderatorUsername = if (isModerated) shuffledPlayers[0] else null
                )
            )

            players.forEach {
                commandGateway.sendAndWait<Void>(
                    JoinGameCommand(
                        gameId = gameId,
                        username = it
                    )
                )
            }
        }
    }
}
