import { Game, GameStatus } from '../domain/GameModel';
import { GameCreatedEvent, PlayerJoinedGameEvent } from '../services/GameEventTypes';

/**
 * Creates a basic game with the given status
 */
export function createMockGame(status: GameStatus, options?: {
  gameId?: string;
  name?: string;
  creatorUsername?: string;
  moderatorUsername?: string;
}): Game {
  const event: GameCreatedEvent = {
    gameId: options?.gameId || 'test-game-id',
    name: options?.name || 'Test Game',
    config: {
      maxPlayers: 10,
      questionSetId: 'test-questionset',
    },
    creatorUsername: options?.creatorUsername || 'creator',
    moderatorUsername: options?.moderatorUsername,
    rounds: [
      {
        name: 'Round 1',
        roundConfig: {
          useBuzzer: true,
          secondsToAnswer: 10,
        },
        questions: []
      }
    ]
  };
  
  const game = new Game(event);
  return game.copyWith({ status });
}

/**
 * Creates a game with a moderator and players
 */
export function createGameWithModerator(
  moderatorUsername: string, 
  players: { id: string; name: string }[],
  options?: {
    gameId?: string;
    name?: string;
    creatorUsername?: string;
  }
): Game {
  const event: GameCreatedEvent = {
    gameId: options?.gameId || 'test-game-id',
    name: options?.name || 'Test Game',
    config: {
      maxPlayers: 10,
      questionSetId: 'test-questionset',
    },
    creatorUsername: options?.creatorUsername || 'creator',
    moderatorUsername: moderatorUsername,
    rounds: [
      {
        name: 'Round 1',
        roundConfig: {
          useBuzzer: true,
          secondsToAnswer: 10,
        },
        questions: []
      }
    ]
  };
  
  let game = new Game(event);
  
  // Add players
  players.forEach(player => {
    const playerEvent: PlayerJoinedGameEvent = {
      gameId: options?.gameId || 'test-game-id',
      gamePlayerId: player.id,
      username: player.name,
    };
    game = game.onPlayerJoined(playerEvent);
  });
  
  return game;
}

/**
 * Creates a game with a creator and players
 */
export function createGameWithCreator(
  creatorUsername: string, 
  players: { id: string; name: string }[],
  options?: {
    gameId?: string;
    name?: string;
  }
): Game {
  const event: GameCreatedEvent = {
    gameId: options?.gameId || 'test-game-id',
    name: options?.name || 'Test Game',
    config: {
      maxPlayers: 10,
      questionSetId: 'test-questionset',
    },
    creatorUsername: creatorUsername,
    moderatorUsername: undefined,
    rounds: [
      {
        name: 'Round 1',
        roundConfig: {
          useBuzzer: true,
          secondsToAnswer: 10,
        },
        questions: []
      }
    ]
  };
  
  let game = new Game(event);
  
  // Add players
  players.forEach(player => {
    const playerEvent: PlayerJoinedGameEvent = {
      gameId: options?.gameId || 'test-game-id',
      gamePlayerId: player.id,
      username: player.name,
    };
    game = game.onPlayerJoined(playerEvent);
  });
  
  return game;
}
