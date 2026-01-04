import { describe, expect, test, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import GamePage from '../GamePage';
import { Game, GameStatus } from '../../../domain/GameModel';
import { GameCreatedEvent } from '../../../services/GameEventTypes';
import React from 'react';
import { GameRepository } from '../../../services/GameRepository';

// Mock dependencies
vi.mock('react-router', () => ({
  useParams: vi.fn(() => ({ gameId: 'test-game-id' })),
  useNavigate: vi.fn(() => vi.fn()),
}));

vi.mock('notistack', () => ({
  useSnackbar: vi.fn(() => ({ 
    enqueueSnackbar: vi.fn(),
    closeSnackbar: vi.fn(),
  })),
}));

vi.mock('../../../hooks/useUsername', () => ({
  useUsername: vi.fn(() => ({ username: 'test-user' })),
}));

let mockFindGame = vi.fn();
let mockSubscribeToGame = vi.fn();
let mockUnsubscribeFromGame = vi.fn();

vi.mock('../../../services/GameRepository', () => {
  class MockGameRepository {
    findGame = mockFindGame;
    subscribeToGame = mockSubscribeToGame;
    unsubscribeFromGame = mockUnsubscribeFromGame;
  }
  
  return {
    GameRepository: MockGameRepository,
    GameEventType: {},
  };
});

vi.mock('../GameLobby', () => ({
  GameLobbyPage: ({ game }: { game: Game }) => <div data-testid="game-lobby">GameLobby</div>,
}));

vi.mock('../GameFinished', () => ({
  GameFinishedPage: ({ game }: { game: Game }) => <div data-testid="game-finished">GameFinished</div>,
}));

vi.mock('../GameRoom', () => ({
  GameRoomPage: ({ game }: { game: Game }) => <div data-testid="game-room">GameRoom</div>,
}));

describe('GamePage', () => {
  beforeEach(() => {
    // Reset mocks before each test
    vi.clearAllMocks();
  });

  test('When a game is in state CREATED, then the GameLobby page should be displayed', () => {
    const mockGame = createMockGame(GameStatus.CREATED);
    mockFindGame.mockImplementation((gameId, callback) => {
      callback(mockGame);
    });

    render(<GamePage />);

    expect(screen.getByTestId('game-lobby')).toBeInTheDocument();
  });

  test('When a game is in state ENDED, then the GameFinished page should be displayed', () => {
    const mockGame = createMockGame(GameStatus.ENDED);
    mockFindGame.mockImplementation((gameId, callback) => {
      callback(mockGame);
    });

    render(<GamePage />);

    expect(screen.getByTestId('game-finished')).toBeInTheDocument();
  });
});

function createMockGame(status: GameStatus): Game {
  const event: GameCreatedEvent = {
    gameId: 'test-game-id',
    name: 'Test Game',
    config: {
      maxPlayers: 10,
      questionSetId: 'test-questionset',
    },
    creatorUsername: 'creator',
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
  
  const game = new Game(event);
  return game.copyWith({ status });
}
