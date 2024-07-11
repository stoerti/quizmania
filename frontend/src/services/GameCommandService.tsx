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

export class GameCommandService {
  public createNewGame(newGame: NewGameCommand, responseHandler: (gameId: string) => void, errorHandler: (err: any) => void = () => {
  }) {
    fetch('/api/game/', {
      method: 'PUT',
      body: JSON.stringify(newGame),
      headers: {
        'Content-type': 'application/json; charset=UTF-8',
      },
    })
      .then((response) => {
        if (response.ok) {
          response.text().then(responseHandler)
        } else {
          response.text().then(errorHandler)
        }
      })
      .catch((err) => {
        if (errorHandler !== undefined)
          errorHandler(err);
      });
  }

  public joinGame(gameId: string, responseHandler: () => void, errorHandler: (err: any) => void = () => {
  }) {
    this.genericPostNoReturn('/api/game/' + gameId + '/join', responseHandler, errorHandler)
  }

  public leaveGame(gameId: string, responseHandler: () => void, errorHandler: (err: any) => void = () => {
  }) {
    this.genericPostNoReturn('/api/game/' + gameId + '/leave', responseHandler, errorHandler)
  }

  public startGame(gameId: string, responseHandler: () => void, errorHandler: (err: any) => void = () => {
  }) {
    this.genericPostNoReturn('/api/game/' + gameId + '/start', responseHandler, errorHandler)
  }

  public cancelGame(gameId: string, responseHandler: () => void, errorHandler: (err: any) => void = () => {
  }) {
    this.genericPostNoReturn('/api/game/' + gameId + '/cancel', responseHandler, errorHandler)
  }

  public askNextQuestion(gameId: string, responseHandler: () => void, errorHandler: (err: any) => void = () => {
  }) {
    this.genericPostNoReturn('/api/game/' + gameId + '/ask-next-question', responseHandler, errorHandler)
  }

  public closeQuestion(gameId: string, gameQuestionId: string, responseHandler: () => void = () => {
  }, errorHandler: (err: any) => void = () => {
  }) {
    this.genericPostNoReturn('api/game/' + gameId + '/question/' + gameQuestionId + '/close', responseHandler, errorHandler)
  }

  public rateQuestion(gameId: string, gameQuestionId: string, responseHandler: () => void = () => {
  }, errorHandler: (err: any) => void = () => {
  }) {
    this.genericPostNoReturn('api/game/' + gameId + '/question/' + gameQuestionId + '/rate', responseHandler, errorHandler)
  }

  public overrideAnswer(answer: OverrideAnswerCommand, responseHandler: () => void, errorHandler: (err: any) => void = () => {
  }) {
    fetch('/api/game/' + answer.gameId + '/override-answer', {
      method: 'POST',
      body: JSON.stringify(answer),
      headers: {
        'Content-type': 'application/json; charset=UTF-8',
      },
    })
      .then((response) => {
        if (response.ok) responseHandler()
        else console.log(response)
      })
      .catch((err) => {
        if (errorHandler !== undefined)
          errorHandler(err);
      });
  }

  public answerQuestion(answer: AnswerQuestionCommand, responseHandler: () => void, errorHandler: (err: any) => void = () => {
  }) {
    fetch('/api/game/' + answer.gameId + '/answer-question', {
      method: 'POST',
      body: JSON.stringify(answer),
      headers: {
        'Content-type': 'application/json; charset=UTF-8',
      },
    })
      .then((response) => {
        if (response.ok) responseHandler()
        else console.log(response)
      })
      .catch((err) => {
        if (errorHandler !== undefined)
          errorHandler(err);
      });
  }

  public buzzQuestion(buzz: BuzzQuestionCommand, responseHandler: () => void = () => {}, errorHandler: (err: any) => void = () => {
  }) {
    fetch('/api/game/' + buzz.gameId + '/buzz-question', {
      method: 'POST',
      body: JSON.stringify(buzz),
      headers: {
        'Content-type': 'application/json; charset=UTF-8',
      },
    })
      .then((response) => {
        if (response.ok) responseHandler()
        else console.log(response)
      })
      .catch((err) => {
        if (errorHandler !== undefined)
          errorHandler(err);
      });
  }

  public answerBuzzerQuestion(answer: AnswerBuzzerQuestionCommand, responseHandler: () => void = () => {}, errorHandler: (err: any) => void = () => {
  }) {
    fetch('/api/game/' + answer.gameId + '/buzzer-answer-question', {
      method: 'POST',
      body: JSON.stringify(answer),
      headers: {
        'Content-type': 'application/json; charset=UTF-8',
      },
    })
      .then((response) => {
        if (response.ok) responseHandler()
        else console.log(response)
      })
      .catch((err) => {
        if (errorHandler !== undefined)
          errorHandler(err);
      });
  }

  private genericPostNoReturn(path: string, responseHandler: () => void, errorHandler: (err: any) => void = () => {
  }) {
    fetch(path, {
      method: 'POST',
      headers: {
        'Content-type': 'application/json; charset=UTF-8',
      },
    })
      .then((response) => {
        if (response.ok) responseHandler()
        else console.log(response)
      })
      .catch((err) => {
        console.log(err.message);
        if (errorHandler !== undefined)
          errorHandler(err);
      });
  }
}
