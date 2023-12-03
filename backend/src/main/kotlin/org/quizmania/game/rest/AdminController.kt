package org.quizmania.game.rest

import org.axonframework.commandhandling.gateway.CommandGateway
import org.quizmania.game.api.AddUserCommand
import org.quizmania.game.api.CreateGameCommand
import org.quizmania.game.common.GameConfig
import org.slf4j.LoggerFactory
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
    private val log = LoggerFactory.getLogger(this.javaClass)
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
            val shuffledUsers = usernames.shuffled();

            val isModerated = Random.nextBoolean()
            val maxPlayers = Random.nextInt(5, 10)
            val numPlayers = Random.nextInt(4, maxPlayers)

            val gameId = UUID.randomUUID()
            val gameNumber = Random.nextInt(1000, 9999)
            val gameName = if (isModerated) "${shuffledUsers[0]}'s game $gameNumber" else "Game $gameNumber"
            val users = shuffledUsers.subList(1, numPlayers)

            commandGateway.sendAndWait<Void>(
                CreateGameCommand(
                    gameId = gameId,
                    name = gameName,
                    config = GameConfig(
                        maxPlayers = maxPlayers,
                        numQuestions = 10,
                        questionSetId = UUID.fromString("40d28946-be06-47d7-814c-e1914c142ae4")
                    ),
                    creatorUsername = shuffledUsers[1],
                    moderatorUsername = if (isModerated) shuffledUsers[0] else null
                )
            )

            users.forEach {
                commandGateway.sendAndWait<Void>(
                    AddUserCommand(
                        gameId = gameId,
                        username = it
                    )
                )
            }
        }
    }
}
