import {Client} from "@stomp/stompjs";
import {GameCreatedEvent, GameEvent,} from "./GameEventTypes";
import {Game} from "../domain/GameModel";


export type GameEventType =
  | 'GameCreatedEvent'
  | 'GameStartedEvent'
  | 'GameCanceledEvent'
  | 'GameEndedEvent'
  | 'PlayerJoinedGameEvent'
  | 'PlayerLeftGameEvent'
  | 'QuestionAskedEvent'
  | 'QuestionAnsweredEvent'
  | 'QuestionAnswerOverriddenEvent'
  | 'QuestionBuzzedEvent'
  | 'QuestionBuzzerWonEvent'
  | 'QuestionClosedEvent'
  | 'QuestionScoredEvent';

type GameEventWrapper = {
  gameId: string,
  eventType: GameEventType,
  sequenceNumber: number,
  timestamp: string,
  payload: GameEvent
}

interface GameEventHandler {
  onGameEvent(event: GameEvent, eventType: GameEventType, game: Game): void
}

export class GameRepository {
  SOCKET_URL = (process.env.NODE_ENV == 'production') ?
    ( window.location.protocol == 'https:' ? 'wss://' : 'ws://' )
    + window.location.host + '/ws-message' : 'ws://localhost:8080/ws-message';

  client?: Client;
  currentGameState: Game | undefined
  lastReceivedSeqNo: number = -1

  public subscribeToGame(gameId: string, gameEventHandler: GameEventHandler) {
    if (this.client == null) {
      this.findGame(gameId, () => {
        this.client = new Client({
          brokerURL: this.SOCKET_URL,
          onConnect: () => {
            this.client!.subscribe('/game/' + gameId, message => {
              const wrapper: GameEventWrapper = JSON.parse(message.body);
              console.log(wrapper)
              this.handleEvent(wrapper, gameEventHandler)
            })
          },
          onWebSocketError: (e: Event) => {
            console.log(e)
          }
        });
        this.client.activate();
      })
    } else {
      console.log("Client already active - should not happen")
    }
  }

  public unsubscribeFromGame() {
    if (this.client != null) {
      this.client.deactivate()
      this.client = undefined
      this.currentGameState = undefined
      this.lastReceivedSeqNo = -1
    } else {
      console.log("Client not active")
    }
  }

  private handleEvent(wrappedEvent: GameEventWrapper, gameEventHandler: GameEventHandler) {
    if (wrappedEvent.sequenceNumber <= this.lastReceivedSeqNo) {
      console.log("Ignoring event with SeqNo ", wrappedEvent.sequenceNumber)
      return
    }
    this.lastReceivedSeqNo = wrappedEvent.sequenceNumber
    this.currentGameState = this.currentGameState!.onGameEvent(wrappedEvent.payload, wrappedEvent.eventType)
    gameEventHandler.onGameEvent(wrappedEvent.payload, wrappedEvent.eventType, this.currentGameState)
  }

  public findGame(gameId: string, successHandler: (game: Game) => void, errorHandler: () => void = () => {
  }) {
    fetch('/api/game/' + gameId + '/events', {
      method: 'GET',
      headers: {
        'Content-type': 'application/json; charset=UTF-8',
      },
    })
      .then((response) => response.json() as Promise<GameEventWrapper[]>)
      .then((events) => {
        if (events.length == 0) {
          console.log("game does no exist");
          errorHandler()
        } else {
          let game = new Game(events[0].payload as GameCreatedEvent)
          for (let i = 1; i < events.length; i++) {
            game = game.onGameEvent(events[i].payload, events[i].eventType)
          }
          this.currentGameState = game

          successHandler(game)
        }
      })
      .catch((err) => {
        console.log(err.message);
        errorHandler()
      });
  }
}

export const gameRepository = new GameRepository()
