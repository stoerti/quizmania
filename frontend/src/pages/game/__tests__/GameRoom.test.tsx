import { describe, expect, test, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { GameRoomPage } from '../GameRoom';
import React from 'react';
import { useUsername } from '../../../hooks/useUsername';
import { createGameWithRound } from '../../../test-utils/fixtures';
import { GameStatus, RoundStatus } from '../../../domain/GameModel';

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

vi.mock('../gameroom/ModeratorGameRoomPanel', () => ({
  ModeratorGameRoomPanel: () => <div data-testid="moderator-panel">ModeratorPanel</div>,
}));

vi.mock('../gameroom/PlayerGameRoomPanel', () => ({
  PlayerGameRoomPanel: () => <div data-testid="player-panel">PlayerPanel</div>,
}));

vi.mock('../gameroom/SpectatorGameRoomPanel', () => ({
  SpectatorGameRoomPanel: () => <div data-testid="spectator-panel">SpectatorPanel</div>,
}));

describe('GameRoom', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test('When the current user is the moderator, then the ModeratorGameRoomPanel should be displayed', () => {
    vi.mocked(useUsername).mockReturnValue({ username: 'moderator' });

    const game = createGameWithRound(
      [
        { id: 'player1', name: 'Player 1' },
        { id: 'player2', name: 'Player 2' },
      ],
      RoundStatus.OPEN,
      { moderatorUsername: 'moderator' }
    );

    render(<GameRoomPage game={game} onLeaveGame={() => {}} />);

    expect(screen.getByTestId('moderator-panel')).toBeInTheDocument();
  });

  test('When the current user is a player in the game, then the PlayerGameRoomPanel should be displayed with the player object', () => {
    vi.mocked(useUsername).mockReturnValue({ username: 'Player 1' });

    const game = createGameWithRound(
      [
        { id: 'player1', name: 'Player 1' },
        { id: 'player2', name: 'Player 2' },
      ],
      RoundStatus.OPEN
    );

    render(<GameRoomPage game={game} onLeaveGame={() => {}} />);

    expect(screen.getByTestId('player-panel')).toBeInTheDocument();
  });

  test('When the current user is not a player and not the moderator, then the SpectatorGameRoomPanel should be displayed', () => {
    vi.mocked(useUsername).mockReturnValue({ username: 'spectator' });

    const game = createGameWithRound(
      [
        { id: 'player1', name: 'Player 1' },
        { id: 'player2', name: 'Player 2' },
      ],
      RoundStatus.OPEN
    );

    render(<GameRoomPage game={game} onLeaveGame={() => {}} />);

    expect(screen.getByTestId('spectator-panel')).toBeInTheDocument();
  });

  test('When the current user is a player, then the player name and points should be displayed in the bottom app bar', () => {
    vi.mocked(useUsername).mockReturnValue({ username: 'Player 1' });

    const game = createGameWithRound(
      [
        { id: 'player1', name: 'Player 1' },
        { id: 'player2', name: 'Player 2' },
      ],
      RoundStatus.OPEN
    );

    render(<GameRoomPage game={game} onLeaveGame={() => {}} />);

    expect(screen.getByText('Player 1')).toBeInTheDocument();
    expect(screen.getByText('Points: 0')).toBeInTheDocument();
  });
});
