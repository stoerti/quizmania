import {Client} from "@stomp/stompjs";
import {GameCreatedEvent, GameEvent,} from "./GameEventTypes";
import {Game} from "../domain/GameModel";


export type GameEventType =
  | 'BuzzerCollectionStartedEvent'
  | 'GameCreatedEvent'
  | 'GameStartedEvent'
  | 'GameCanceledEvent'
  | 'GameEndedEvent'
  | 'RoundStartedEvent'
  | 'RoundScoredEvent'
  | 'RoundClosedEvent'
  | 'PlayerJoinedGameEvent'
  | 'PlayerLeftGameEvent'
  | 'QuestionAskedEvent'
  | 'QuestionAnsweredEvent'
  | 'QuestionAnswerOverriddenEvent'
  | 'QuestionBuzzedEvent'
  | 'QuestionBuzzerWonEvent'
  | 'QuestionBuzzerReopenedEvent'
  | 'QuestionClosedEvent'
  | 'QuestionScoredEvent';

export type GameEventWrapper = {
  gameId: string,
  eventType: GameEventType,
  cursor: string,
  timestamp: string,
  payload: GameEvent
}

interface GameEventHandler {
  onGameEvent(event: GameEvent, eventType: GameEventType, game: Game): void
}

export class GameRepository {
  client?: Client;
  currentGameState: Game | undefined
  lastReceivedCursor: string = "0"

  public subscribeToGame(gameId: string, gameEventHandler: GameEventHandler) {
    if (this.client == null) {
      // find
      this.findGame(gameId, () => this.client = this.createStompClient(gameId, gameEventHandler))
    } else {
      console.log("Client already active - should not happen")
    }
  }

  // STOMP connect URL
  SOCKET_URL = (import.meta.env.MODE === 'production') ?
    (window.location.protocol == 'https:' ? 'wss://' : 'ws://')
    + window.location.host + '/ws-message' : 'ws://localhost:8080/ws-message';

  public unsubscribeFromGame() {
    if (this.client != null) {
      this.client.deactivate()
      this.client = undefined
      this.currentGameState = undefined
      this.lastReceivedCursor = "0"
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
        // Subscribe first, then catch up from the REST cursor. Buffer live events
        // while catching up to close the initial-connect/reconnect delivery gap.
        const receipt = 'game-subscription-' + crypto.randomUUID();
        let catchingUp = true;
        const buffered: GameEventWrapper[] = [];
        client.watchForReceipt(receipt, () => {
          fetch('/api/game/' + gameId + '/events?afterCursor=' + this.lastReceivedCursor)
            .then(response => {
              if (!response.ok) throw new Error('Could not catch up game events');
              return response.json() as Promise<GameEventWrapper[]>;
            })
            .then(history => {
              if (this.client !== client) return;
              const events = [...history, ...buffered].sort((a, b) =>
                BigInt(a.cursor) < BigInt(b.cursor) ? -1 : BigInt(a.cursor) > BigInt(b.cursor) ? 1 : 0);
              events.forEach(event => this.handleEvent(event, gameEventHandler));
              catchingUp = false;
            })
            .catch(error => {
              console.error(error);
              // Reconnect and retry catch-up; never advance past missing history.
              if (this.client === client) client.forceDisconnect();
            });
        });
        client.subscribe('/game/' + gameId, message => {
          const wrapper: GameEventWrapper = JSON.parse(message.body);
          if (catchingUp) buffered.push(wrapper);
          else this.handleEvent(wrapper, gameEventHandler);
        }, {receipt});
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
    if (BigInt(wrappedEvent.cursor) <= BigInt(this.lastReceivedCursor)) {
      console.log("Ignoring duplicate event with cursor ", wrappedEvent.cursor)
    } else {
      this.lastReceivedCursor = wrappedEvent.cursor
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
    this.lastReceivedCursor = events[events.length - 1].cursor;
    return game;
  }
}

export const gameRepository = new GameRepository()
