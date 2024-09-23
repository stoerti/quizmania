import {
  GameCanceledEvent,
  GameConfig,
  GameCreatedEvent, GameEndedEvent, GameEvent, GameQuestionMode,
  GameStartedEvent,
  Question,
  QuestionAnsweredEvent,
  QuestionAnswerOverriddenEvent,
  QuestionAskedEvent, QuestionBuzzedEvent, QuestionBuzzerWonEvent,
  QuestionClosedEvent,
  QuestionScoredEvent,
  PlayerJoinedGameEvent,
  PlayerLeftGameEvent
} from "../services/GameEventTypes";
import {GameEventType} from "../services/GameRepository";

export enum GameStatus {
  CREATED = 'CREATED',
  STARTED = 'STARTED',
  ENDED = 'ENDED',
  CANCELED = 'CANCELED'
}

export enum QuestionStatus {
  OPEN = 'OPEN',
  CLOSED = 'CLOSED',
  SCORED = 'SCORED',
}


export class Game {
  readonly id: string;
  readonly name: string;
  readonly config: GameConfig;
  readonly creator: string;
  readonly moderator: string | undefined;
  readonly status: GameStatus;
  readonly players: Player[];
  readonly numQuestions: number = 0;
  readonly currentQuestion: GameQuestion | undefined;

  constructor(event: GameCreatedEvent) {
    this.id = event.gameId
    this.name = event.name
    this.config = event.config
    this.creator = event.creatorUsername
    this.moderator = event.moderatorUsername
    this.status = GameStatus.CREATED
    this.players = []
  }

  public copyWith(modifyObject: { [P in keyof Game]?: Game[P] }): Game {
    return Object.assign(Object.create(Game.prototype), {...this, ...modifyObject});
  }

  public onGameEvent(event: GameEvent, eventType: GameEventType): Game {
    switch (eventType) {
      case "GameStartedEvent":
        return this.onGameStarted(event as GameStartedEvent)
      case "GameEndedEvent":
        return this.onGameEnded(event as GameEndedEvent)
      case "GameCanceledEvent":
        return this.onGameCanceled(event as GameCanceledEvent)
      case "PlayerJoinedGameEvent":
        return this.onPlayerJoined(event as PlayerJoinedGameEvent)
      case "PlayerLeftGameEvent":
        return this.onPlayerLeft(event as PlayerLeftGameEvent)
      case "QuestionAskedEvent":
        return this.onQuestionAsked(event as QuestionAskedEvent)
      case "QuestionAnsweredEvent":
        return this.onQuestionAnswered(event as QuestionAnsweredEvent)
      case "QuestionAnswerOverriddenEvent":
        return this.onQuestionAnswerOverridden(event as QuestionAnswerOverriddenEvent)
      case "QuestionBuzzedEvent":
        return this.onQuestionBuzzed(event as QuestionBuzzedEvent)
      case "QuestionBuzzerWonEvent":
        return this.onQuestionBuzzerWon(event as QuestionBuzzerWonEvent)
      case "QuestionClosedEvent":
        return this.onQuestionClosed(event as QuestionClosedEvent)
      case "QuestionScoredEvent":
        return this.onQuestionScored(event as QuestionScoredEvent)
    }

    throw Error("Unknown eventType " + eventType)
  }

  public onGameStarted(event: GameStartedEvent): Game {
    return this.copyWith({
      status: GameStatus.STARTED
    })
  }

  public onGameCanceled(event: GameCanceledEvent): Game {
    return this.copyWith({
      status: GameStatus.CANCELED,
      currentQuestion: undefined
    })
  }

  public onGameEnded(event: GameEndedEvent): Game {
    return this.copyWith({
      status: GameStatus.ENDED,
      currentQuestion: undefined
    })
  }

  public onPlayerJoined(event: PlayerJoinedGameEvent): Game {
    return this.copyWith({
      players: [
        ...this.players,
        new Player(event.gamePlayerId, event.username)
      ]
    })
  }

  public onPlayerLeft(event: PlayerLeftGameEvent): Game {
    return this.copyWith({
      players: this.players.filter(player => player.id != event.gamePlayerId)
    })
  }

  public onQuestionAsked(event: QuestionAskedEvent): Game {
    return this.copyWith({
      numQuestions: this.numQuestions + 1,
      currentQuestion:
        new GameQuestion(event)
    })
  }

  private updateQuestion(questionId: string, questionUpdater: (question: GameQuestion) => GameQuestion): Game {
    if (this.currentQuestion === undefined || this.currentQuestion.gameQuestionId === questionId) {
      return this.copyWith({
        currentQuestion: questionUpdater(this.currentQuestion!)
      })
    } else {
      return this
    }
  }

  public onQuestionAnswered(event: QuestionAnsweredEvent): Game {
    const game = this.updateQuestion(event.gameQuestionId, question => question.onQuestionAnswered(event))

    const newPlayers = game.players.map(player => {
      if (event.gamePlayerId === player.id) {
        return {
          ...player,
          totalAnswerTime: player.totalAnswerTime + event.timeToAnswer
        }
      } else {
        return player
      }
    })

    return game.copyWith({
      players: newPlayers
    })
  }

  public onQuestionAnswerOverridden(event: QuestionAnswerOverriddenEvent): Game {
    return this.updateQuestion(event.gameQuestionId, question => question.onQuestionAnswerOverridden(event))
  }

  public onQuestionBuzzed(event: QuestionBuzzedEvent): Game {
    return this.updateQuestion(event.gameQuestionId, question => question.onQuestionBuzzed(event))
  }

  public onQuestionBuzzerWon(event: QuestionBuzzerWonEvent): Game {
    return this.updateQuestion(event.gameQuestionId, question => question.onQuestionBuzzerWon(event))
  }

  public onQuestionClosed(event: QuestionClosedEvent): Game {
    return this.updateQuestion(event.gameQuestionId, question => question.onQuestionClosed(event))
  }

  public onQuestionScored(event: QuestionScoredEvent): Game {
    // TODO optionally optimize, game is copied twice here
    const game = this.updateQuestion(event.gameQuestionId, question => question.onQuestionScored(event))

    const newPlayers = game.players.map(player => {
      if (event.points[player.id] != undefined) {
        return {
          ...player,
          points: player.points + event.points[player.id]
        }
      } else {
        return player
      }
    })

    return game.copyWith({
      players: newPlayers
    })
  }

  public findPlayerName(gamePlayerId: string): string {
    return this.players.find(player => player.id === gamePlayerId)?.name ?? 'unknown'
  }

  public findPlayerPoints(gamePlayerId: string): number {
    return this.players.find(player => player.id === gamePlayerId)?.points ?? 0
  }

  public findAverageAnswerTime(gamePlayerId: string): number {
    const totalAnswerTime = this.players.find(player => player.id === gamePlayerId)?.totalAnswerTime ?? 0
    return totalAnswerTime / this.numQuestions
  }
}

export class GameQuestion {
  readonly gameQuestionId: string;
  readonly gameQuestionNumber: number;
  readonly question: Question;
  readonly questionMode: GameQuestionMode;
  readonly answers: Answer[] = [];
  readonly buzzedPlayerIds: string[] = [];
  readonly currentBuzzWinnerId: string | undefined = undefined;
  readonly status: QuestionStatus;
  readonly questionAsked: Date;
  readonly questionTimeout: number;

  constructor(event: QuestionAskedEvent) {
    this.gameQuestionId = event.gameQuestionId
    this.gameQuestionNumber = event.gameQuestionNumber
    this.question = event.question
    this.questionMode = event.questionMode
    this.status = QuestionStatus.OPEN
    this.questionAsked = new Date(event.questionTimestamp)
    this.questionTimeout = event.timeToAnswer
  }

  public copyWith(modifyObject: { [P in keyof GameQuestion]?: GameQuestion[P] }): GameQuestion {
    return Object.assign(Object.create(GameQuestion.prototype), {...this, ...modifyObject});
  }

  public hasPlayerAlreadyAnswered(gamePlayerId: string): boolean {
    return this.answers.find(answer => answer.gamePlayerId === gamePlayerId) !== undefined
  }

  public onQuestionAnswered(event: QuestionAnsweredEvent): GameQuestion {
    return this.copyWith({
      answers: [
        ...this.answers,
        new Answer(event)
      ]
    })
  }

  public onQuestionAnswerOverridden(event: QuestionAnswerOverriddenEvent): GameQuestion {
    return this.copyWith({
      answers: [
        ...this.answers.filter(answer => answer.gamePlayerId != event.gamePlayerId),
        new Answer({...event, timeToAnswer: 0})
      ]
    })
  }

  public onQuestionBuzzed(event: QuestionBuzzedEvent): GameQuestion {
    return this.copyWith({
      buzzedPlayerIds: [...this.buzzedPlayerIds, event.gamePlayerId]
    })
  }

  public onQuestionBuzzerWon(event: QuestionBuzzerWonEvent): GameQuestion {
    return this.copyWith({
      currentBuzzWinnerId: event.gamePlayerId
    })
  }

  public onQuestionClosed(event: QuestionClosedEvent): GameQuestion {
    return this.copyWith({
      status: QuestionStatus.CLOSED
    })
  }

  public onQuestionScored(event: QuestionScoredEvent): GameQuestion {
    const newAnswers = this.answers.map(answer => {
      if (event.points[answer.gamePlayerId] != undefined) {
        return {
          ...answer,
          points: event.points[answer.gamePlayerId]
        }
      } else {
        return answer
      }
    })

    return this.copyWith({
      status: QuestionStatus.SCORED,
      answers: newAnswers
    })
  }

}

export class Answer {
  readonly id: string;
  readonly gamePlayerId: string;
  readonly answer: string;
  readonly points: number = 0;

  constructor(event: QuestionAnsweredEvent) {
    this.id = event.playerAnswerId
    this.gamePlayerId = event.gamePlayerId
    this.answer = event.answer
  }
}

export class Player {
  readonly id: string;
  readonly name: string;
  readonly points: number = 0;
  readonly totalAnswerTime: number = 0;

  constructor(id: string, name: string) {
    this.id = id
    this.name = name
  }
}
