package org.quizmania.game.api

import java.util.*


open class GameException(
    open val gameId: UUID,
    override val message: String
) : Exception(message);

class GameAlreadyFullException(override val gameId: UUID) :
        GameException(gameId, "")

class GameAlreadyStartedException(override val gameId: UUID) :
        GameException(gameId, "")

class GameAlreadyEndedException(override val gameId: UUID) :
        GameException(gameId, "")

class QuestionAlreadyClosedException(override val gameId: UUID, val gameQuestionId: UUID) :
        GameException(gameId, "")
