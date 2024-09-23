import {ProblemJson, registerProblemFactory} from "./problem/ProblemInterceptor.tsx";
import {getCurrentServerTime} from "./ServerTimeSync.tsx";

export type NewGameCommand = {
  name: string,
  config: GameConfig,
  withModerator: boolean
}

export type GameConfig = {
  maxPlayers: number,
  numQuestions: number,
  secondsToAnswer: number,
  questionSetId: string
  useBuzzer: boolean,
}

export type AnswerQuestionCommand = {
  gameId: string,
  gameQuestionId: string,
  answer: string
}

export type OverrideAnswerCommand = {
  gameId: string,
  gameQuestionId: string,
  gamePlayerId: string,
  playerAnswerId: string,
  answer: string
}

export type BuzzQuestionCommand = {
  gameId: string,
  gameQuestionId: string,
}

export type AnswerBuzzerQuestionCommand = {
  gameId: string,
  gameQuestionId: string,
  answerCorrect: boolean
}

export class GameException extends Error {}

export class GameAlreadyFullException extends GameException {
  constructor() {
    super('The game is already full');
  }
}

export class UsernameAlreadyTakenException extends GameException {
  constructor() {
    super('The username is already taken in the game');
  }
}

export class GameConfigInvalidException extends GameException {
  constructor(problem: ProblemJson) {
    super('The game config is invalid: ' + problem.detail);
  }
}

export class GameCommandService {
  public async createNewGame(newGame: NewGameCommand) {
    const response = await this.genericPut('/api/game/', JSON.stringify(newGame))
    return await response.text()
  }

  public async joinGame(gameId: string) {
    await this.genericPost('/api/game/' + gameId + '/join')
  }

  public async leaveGame(gameId: string) {
    await this.genericPost('/api/game/' + gameId + '/leave')
  }

  public async startGame(gameId: string) {
    await this.genericPost('/api/game/' + gameId + '/start')
  }

  public async askNextQuestion(gameId: string) {
    await this.genericPost('/api/game/' + gameId + '/ask-next-question')
  }

  public async closeQuestion(gameId: string, gameQuestionId: string) {
    await this.genericPost('api/game/' + gameId + '/question/' + gameQuestionId + '/close')
  }

  public async scoreQuestion(gameId: string, gameQuestionId: string) {
    await this.genericPost('api/game/' + gameId + '/question/' + gameQuestionId + '/score')
  }

  public async overrideAnswer(answer: OverrideAnswerCommand) {
    await this.genericPost('/api/game/' + answer.gameId + '/override-answer', JSON.stringify(answer))
  }

  public async answerQuestion(answer: AnswerQuestionCommand) {
    await this.genericPost('/api/game/' + answer.gameId + '/answer-question',
      JSON.stringify({
        gameId: answer.gameId,
        gameQuestionId: answer.gameQuestionId,
        answer: answer.answer,
        answerTimestamp: getCurrentServerTime().toISOString()
      }))
  }

  public async buzzQuestion(buzz: BuzzQuestionCommand) {
    await this.genericPost('/api/game/' + buzz.gameId + '/buzz-question', JSON.stringify({
      gameId: buzz.gameId,
      gameQuestionId: buzz.gameQuestionId,
      buzzerTimestamp: getCurrentServerTime().toISOString()
    }))
  }

  public async answerBuzzerQuestion(answer: AnswerBuzzerQuestionCommand) {
    await this.genericPost('/api/game/' + answer.gameId + '/buzzer-answer-question', JSON.stringify(answer))
  }

  private async genericPost(path: string, body?: BodyInit | null | undefined) {
    return this.genericExchange('POST', path, body)
  }

  private async genericPut(path: string, body?: BodyInit | null | undefined) {
    return this.genericExchange('PUT', path, body)
  }

  private async genericExchange(method: string, path: string, body?: BodyInit | null | undefined) {
    const response = await fetch(path, {
      method: method,
      headers: {
        'Content-type': 'application/json; charset=UTF-8',
      },
      body: body
    })

    if (!response.ok) {
      const error = await response.text()
      throw Error("Generic error happened: " + error)
    } else {
      return response
    }
  }
}

registerProblemFactory('urn:quizmania:game:alreadyFull', (_) => new GameAlreadyFullException())
registerProblemFactory('urn:quizmania:game:usernameTaken', (_) => new UsernameAlreadyTakenException())
registerProblemFactory('urn:quizmania:game:invalidConfig', (problem) => new GameConfigInvalidException(problem))

export const gameCommandService = new GameCommandService()
