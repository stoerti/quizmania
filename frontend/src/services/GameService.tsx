import {Client} from "@stomp/stompjs";
import {
  GameCanceledEvent,
  GameDto,
  GameEndedEvent,
  GameStartedEvent,
  NewAnswerDto,
  NewGameDto, OverrideAnswerDto,
  QuestionAnsweredEvent, QuestionAnswerOverriddenEvent,
  QuestionAskedEvent,
  QuestionClosedEvent,
  QuestionRatedEvent,
  UserAddedEvent,
  UserRemovedEvent
} from "./GameServiceTypes";


export class GameService {
  SOCKET_URL = (process.env.NODE_ENV == 'production') ? 'wss://' + window.location.host + '/ws-message' : 'ws://localhost:8080/ws-message';

  client?: Client;

  public createNewGame(newGame: NewGameDto, responseHandler: (gameId: string) => void, errorHandler: (err: any) => void = () => {
  }) {
    fetch('/api/game/', {
      method: 'PUT',
      body: JSON.stringify(newGame),
      headers: {
        'Content-type': 'application/json; charset=UTF-8',
      },
    })
      .then((response) => response.text())
      .then(responseHandler)
      .catch((err) => {
        if (errorHandler !== undefined)
          errorHandler(err);
      });
  }

  public searchOpenGames(responseHandler: (games: GameDto[]) => void, errorHandler: (err: any) => void = () => {
  }) {
    fetch('/api/game/?gameStatus=CREATED', {
      method: 'GET',
      headers: {
        'Content-type': 'application/json; charset=UTF-8',
      },
    })
      .then((response) => response.json())
      .then(responseHandler)
      .catch((err) => {
        if (errorHandler !== undefined)
          errorHandler(err);
      });
  }

  public findGame(gameId: string, responseHandler: (game: GameDto) => void, errorHandler: (err: any) => void = () => {
  }) {
    fetch('/api/game/' + gameId, {
      method: 'GET',
      headers: {
        'Content-type': 'application/json; charset=UTF-8',
      },
    })
      .then((response) => response.json())
      .then(responseHandler)
      .catch((err) => {
        console.log(err.message);
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

  public closeQuestion(gameId: string, gameQuestionId: string, responseHandler: () => void, errorHandler: (err: any) => void = () => {
  }) {
    this.genericPostNoReturn('api/game/' + gameId + '/question/' + gameQuestionId + '/close', responseHandler, errorHandler)
  }

  public rateQuestion(gameId: string, gameQuestionId: string, responseHandler: () => void= () => {}, errorHandler: (err: any) => void = () => {
  }) {
    this.genericPostNoReturn('api/game/' + gameId + '/question/' + gameQuestionId + '/rate', responseHandler, errorHandler)
  }

  public overrideAnswer(answer: OverrideAnswerDto, responseHandler: () => void, errorHandler: (err: any) => void = () => {
  }) {
    fetch('/api/game/' + answer.gameId + '/override-answer', {
      method: 'POST',
      body: JSON.stringify({
        gameQuestionId: answer.gameQuestionId,
        gameUserId: answer.gameUserId,
        userAnswerId: answer.userAnswerId,
        answer: answer.answer
      }),
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

  public answerQuestion(answer: NewAnswerDto, responseHandler: () => void, errorHandler: (err: any) => void = () => {
  }) {
    fetch('/api/game/' + answer.gameId + '/answer-question', {
      method: 'POST',
      body: JSON.stringify({gameQuestionId: answer.gameQuestionId, answer: answer.answer}),
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

  private dispatchToGameEventHandler = (wrappedEvent: GameEventWrapper, gameEventHandler: GameEventHandler) => {
    switch (wrappedEvent.eventType) {
      case "UserAddedEvent":
        gameEventHandler.onUserAdded(wrappedEvent.payload as UserAddedEvent, wrappedEvent.game);
        break;
      case "UserRemovedEvent":
        gameEventHandler.onUserRemoved(wrappedEvent.payload as UserRemovedEvent, wrappedEvent.game);
        break;
      case "GameCanceledEvent":
        gameEventHandler.onGameCanceled(wrappedEvent.payload as GameCanceledEvent, wrappedEvent.game);
        break;
      case "GameEndedEvent":
        gameEventHandler.onGameEnded(wrappedEvent.payload as GameEndedEvent, wrappedEvent.game);
        break;
      case "QuestionAskedEvent":
        gameEventHandler.onQuestionAsked(wrappedEvent.payload as QuestionAskedEvent, wrappedEvent.game);
        break;
      case "QuestionAnsweredEvent":
        gameEventHandler.onQuestionAnswered(wrappedEvent.payload as QuestionAnsweredEvent, wrappedEvent.game);
        break;
      case "QuestionAnswerOverriddenEvent":
        gameEventHandler.onQuestionAnswerOverriddenEvent(wrappedEvent.payload as QuestionAnswerOverriddenEvent, wrappedEvent.game);
        break;
      case "QuestionClosedEvent":
        gameEventHandler.onQuestionClosed(wrappedEvent.payload as QuestionClosedEvent, wrappedEvent.game);
        break;
      case "QuestionRatedEvent":
        gameEventHandler.onQuestionRated(wrappedEvent.payload as QuestionRatedEvent, wrappedEvent.game);
        break;
    }
  }


  public subscribeToGame(gameId: string, gameEventHandler: GameEventHandler) {
    if (this.client == null) {
      this.client = new Client({
        brokerURL: this.SOCKET_URL,
        onConnect: () => {
          this.client?.subscribe('/game/' + gameId, message => {
            let wrapper: GameEventWrapper = JSON.parse(message.body);
            console.log(wrapper)
            this.dispatchToGameEventHandler(wrapper, gameEventHandler)
          })
        },
        onWebSocketError: (e: Event) => {
          console.log(e)
        }
      });

      this.client?.activate();
    } else {
      console.log("Client already active - should not happen")
    }
  }

  public unsubscribeFromGame() {
    if (this.client != null) {
      this.client.deactivate()
      this.client = undefined
    } else {
      console.log("Client not active")
    }
  }
}

interface GameEventHandler {
  onGameStarted(event: GameStartedEvent, game: GameDto): void

  onGameEnded(event: GameEndedEvent, game: GameDto): void

  onGameCanceled(event: GameCanceledEvent, game: GameDto): void

  onUserAdded(event: UserAddedEvent, game: GameDto): void

  onUserRemoved(event: UserRemovedEvent, game: GameDto): void

  onQuestionAsked(event: QuestionAskedEvent, game: GameDto): void

  onQuestionAnswered(event: QuestionAnsweredEvent, game: GameDto): void

  onQuestionAnswerOverriddenEvent(event: QuestionAnswerOverriddenEvent, game: GameDto): void

  onQuestionClosed(event: QuestionClosedEvent, game: GameDto): void

  onQuestionRated(event: QuestionRatedEvent, game: GameDto): void
}

type GameEventWrapper = {
  gameId: string,
  eventType: string,
  payload: any,
  game: GameDto
}

