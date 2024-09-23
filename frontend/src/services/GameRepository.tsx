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
  client?: Client;
  currentGameState: Game | undefined
  lastReceivedSeqNo: number = -1

  public subscribeToGame(gameId: string, gameEventHandler: GameEventHandler) {
    if (this.client == null) {
      // find
      this.findGame(gameId, () => this.client = this.createStompClient(gameId, gameEventHandler))
    } else {
      console.log("Client already active - should not happen")
    }
  }

  // STOMP connect URL
  SOCKET_URL = (process.env.NODE_ENV == 'production') ?
    (window.location.protocol == 'https:' ? 'wss://' : 'ws://')
    + window.location.host + '/ws-message' : 'ws://localhost:8080/ws-message';

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
          const game = this.initializeGameModel(events);
          this.currentGameState = game

          successHandler(game)
        }
      })
      .catch((err) => {
        console.log(err.message);
        errorHandler()
      });
  }

  /**
   * Creates a STOMP-JS client and subscribes to messages on specified game channel

   * @param gameId the game to subscribe
   * @param gameEventHandler the event handler to forward the event and current state to
   * @return the activated client
   */
  private createStompClient(gameId: string, gameEventHandler: GameEventHandler): Client {
    const client = new Client({
      brokerURL: this.SOCKET_URL,
      onConnect: () => {
        this.client!.subscribe('/game/' + gameId, message => {
          const wrapper: GameEventWrapper = JSON.parse(message.body);
          this.handleEvent(wrapper, gameEventHandler)
        })
      },
      onWebSocketError: (e: Event) => {
        console.log(e)
      }
    });
    client.activate();

    return client;
  }

  /**
   * Uses the given wrapped event to evolve the current read model state to the next one.
   * @param wrappedEvent the wrapped GameEvent
   * @param gameEventHandler the eventHandler to forward the result to
   */
  private handleEvent(wrappedEvent: GameEventWrapper, gameEventHandler: GameEventHandler) {
    if (wrappedEvent.sequenceNumber <= this.lastReceivedSeqNo) {
      console.log("Ignoring duplicate event with SeqNo ", wrappedEvent.sequenceNumber)
    } else {
      this.lastReceivedSeqNo = wrappedEvent.sequenceNumber
      // evolve read model to next state
      this.currentGameState = this.currentGameState!.onGameEvent(wrappedEvent.payload, wrappedEvent.eventType)
      // forward event and new read model state to the eventHandler
      gameEventHandler.onGameEvent(wrappedEvent.payload, wrappedEvent.eventType, this.currentGameState)
    }
  }

  private initializeGameModel(events: GameEventWrapper[]) {
    // use first event to initialize read model
    let game = new Game(events[0].payload as GameCreatedEvent)
    // iterate over all following events to evolve the current read model state
    for (let i = 1; i < events.length; i++) {
      game = game.onGameEvent(events[i].payload, events[i].eventType)
    }
    return game;
  }
}

export const gameRepository = new GameRepository()
