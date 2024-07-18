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
  gameUserId: string,
  userAnswerId: string,
  answer: string
}

export type BuzzQuestionCommand = {
  gameId: string,
  gameQuestionId: string,
  buzzerTimestamp: string
}

export type AnswerBuzzerQuestionCommand = {
  gameId: string,
  gameQuestionId: string,
  answerCorrect: boolean
}

type ProblemJson = {
  type: string,
  title: string | undefined,
  detail: string | undefined,
  context: any,
}

export class GameException extends Error {}

export class GameAlreadyFullException extends GameException {
  constructor() {
    super('The game is already full');
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

  public async rateQuestion(gameId: string, gameQuestionId: string) {
    await this.genericPost('api/game/' + gameId + '/question/' + gameQuestionId + '/rate')
  }

  public async overrideAnswer(answer: OverrideAnswerCommand) {
    await this.genericPost('/api/game/' + answer.gameId + '/override-answer', JSON.stringify(answer))
  }

  public async answerQuestion(answer: AnswerQuestionCommand) {
    await this.genericPost('/api/game/' + answer.gameId + '/answer-question', JSON.stringify(answer))
  }

  public async buzzQuestion(buzz: BuzzQuestionCommand) {
    await this.genericPost('/api/game/' + buzz.gameId + '/buzz-question', JSON.stringify(buzz))
  }

  public async answerBuzzerQuestion(answer: AnswerBuzzerQuestionCommand) {
    await this.genericPost('/api/game/' + answer.gameId + '/buzzer-answer-question', JSON.stringify(answer))
  }

  private async genericPost(path: string, body?: any) {
    return this.genericExchange('POST', path, body)
  }

  private async genericPut(path: string, body?: any) {
    return this.genericExchange('PUT', path, body)
  }

  private async genericExchange(method: string, path: string, body?: any) {
    const response = await fetch(path, {
      method: method,
      headers: {
        'Content-type': 'application/json; charset=UTF-8',
      },
      body: body
    })

    if (!response.ok && response.headers.get('Content-Type') === 'application/problem+json') {
      throw this.resolveProblemException(await response.json() as ProblemJson)
    } else if (!response.ok) {
      const error = await response.text()
      throw Error("Generic error happened: " + error)
    } else {
      return response
    }
  }

  private resolveProblemException(problem: ProblemJson): Error {
    switch (problem.type) {
      case 'urn:quizmania:game:alreadyFull': throw new GameAlreadyFullException()
      case 'urn:quizmania:game:invalidConfig': throw new GameConfigInvalidException(problem)
      default: throw new Error('Random error occurred')
    }
  }
}
