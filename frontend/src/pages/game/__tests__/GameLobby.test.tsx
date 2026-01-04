import { describe, expect, test, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { GameLobbyPage } from '../GameLobby';
import { Game, GameStatus, Player } from '../../../domain/GameModel';
import { GameCreatedEvent, PlayerJoinedGameEvent } from '../../../services/GameEventTypes';
import React from 'react';
import { useUsername } from '../../../hooks/useUsername';

// Mock dependencies
vi.mock('notistack', () => ({
  useSnackbar: vi.fn(() => ({ 
    enqueueSnackbar: vi.fn(),
    closeSnackbar: vi.fn(),
  })),
}));

vi.mock('../../../hooks/useUsername', () => ({
  useUsername: vi.fn(),
}));

vi.mock('../../../services/GameCommandService', () => ({
  gameCommandService: {
    leaveGame: vi.fn(),
    startGame: vi.fn(),
  },
  GameException: class GameException extends Error {},
}));

vi.mock('../LeaveGameDialog', () => ({
  __esModule: true,
  default: () => <div data-testid="leave-game-dialog">LeaveGameDialog</div>,
}));

describe('GameLobby', () => {
  beforeEach(() => {
    // Reset mocks before each test
    vi.clearAllMocks();
  });

  test('When a game is in state CREATED with a moderator and 3 players and the current player is the moderator, then all three players appear in the list of players but not the moderator', () => {
    vi.mocked(useUsername).mockReturnValue({ username: 'moderator' });

    const game = createGameWithModerator('moderator', [
      { id: 'player1', name: 'Player 1' },
      { id: 'player2', name: 'Player 2' },
      { id: 'player3', name: 'Player 3' },
    ]);

    render(<GameLobbyPage game={game} />);

    // All 3 players should be in the list
    expect(screen.getByText('Player 1')).toBeInTheDocument();
    expect(screen.getByText('Player 2')).toBeInTheDocument();
    expect(screen.getByText('Player 3')).toBeInTheDocument();

    // Verify the count shows 3 players (not including moderator)
    expect(screen.getByText(/Participants \(3\)/)).toBeInTheDocument();
    
    // Moderator should appear as game master, not in the player list
    expect(screen.getByText('game master')).toBeInTheDocument();
  });

  test('When a game is in state CREATED with a moderator and 3 players and the current player is the moderator, then the startGame button is visible', () => {
    vi.mocked(useUsername).mockReturnValue({ username: 'moderator' });

    const game = createGameWithModerator('moderator', [
      { id: 'player1', name: 'Player 1' },
      { id: 'player2', name: 'Player 2' },
      { id: 'player3', name: 'Player 3' },
    ]);

    render(<GameLobbyPage game={game} />);

    const startButton = screen.getByRole('button', { name: /Start game/i });
    expect(startButton).toBeInTheDocument();
  });

  test('When a game is in state CREATED with 3 players and the current player is the creator, then the startGame button is visible', () => {
    vi.mocked(useUsername).mockReturnValue({ username: 'creator' });

    const game = createGameWithCreator('creator', [
      { id: 'player1', name: 'Player 1' },
      { id: 'player2', name: 'Player 2' },
      { id: 'player3', name: 'Player 3' },
    ]);

    render(<GameLobbyPage game={game} />);

    const startButton = screen.getByRole('button', { name: /Start game/i });
    expect(startButton).toBeInTheDocument();
  });

  test('When a game is in state CREATED with 3 players and the current player is not the creator, then the startGame button is not visible', () => {
    vi.mocked(useUsername).mockReturnValue({ username: 'player1' });

    const game = createGameWithCreator('creator', [
      { id: 'player1', name: 'Player 1' },
      { id: 'player2', name: 'Player 2' },
      { id: 'player3', name: 'Player 3' },
    ]);

    render(<GameLobbyPage game={game} />);

    const startButton = screen.queryByRole('button', { name: /Start game/i });
    expect(startButton).not.toBeInTheDocument();
  });
});

function createGameWithModerator(moderatorUsername: string, players: { id: string; name: string }[]): Game {
  const event: GameCreatedEvent = {
    gameId: 'test-game-id',
    name: 'Test Game',
    config: {
      maxPlayers: 10,
      questionSetId: 'test-questionset',
    },
    creatorUsername: 'creator',
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
      gameId: 'test-game-id',
      gamePlayerId: player.id,
      username: player.name,
    };
    game = game.onPlayerJoined(playerEvent);
  });
  
  return game;
}

function createGameWithCreator(creatorUsername: string, players: { id: string; name: string }[]): Game {
  const event: GameCreatedEvent = {
    gameId: 'test-game-id',
    name: 'Test Game',
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
      gameId: 'test-game-id',
      gamePlayerId: player.id,
      username: player.name,
    };
    game = game.onPlayerJoined(playerEvent);
  });
  
  return game;
}
