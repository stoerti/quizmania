import { describe, expect, test, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { GameFinishedPage } from '../GameFinished';
import React from 'react';
import { createMockGame } from '../../../test-utils/fixtures';
import { GameStatus } from '../../../domain/GameModel';

// Mock Scoreboard component
vi.mock('../gameroom/Scoreboard', () => ({
  Scoreboard: () => <div data-testid="scoreboard">Scoreboard</div>,
  ScoreboardMode: {
    QUESTION: 'QUESTION',
    ROUND: 'ROUND',
  }
}));

describe('GameFinished', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test('When game status is ENDED, then the game name with " - Results" suffix should be displayed', () => {
    const game = createMockGame(GameStatus.ENDED, { name: 'My Test Game' });
    
    render(<GameFinishedPage game={game} onClickLeaveGame={() => {}} />);

    expect(screen.getByText('My Test Game - Results')).toBeInTheDocument();
  });

  test('When game status is ENDED, then the "Leave game" button should be visible', () => {
    const game = createMockGame(GameStatus.ENDED);
    
    render(<GameFinishedPage game={game} onClickLeaveGame={() => {}} />);

    expect(screen.getByRole('button', { name: /Leave game/i })).toBeInTheDocument();
  });

  test('When game status is ENDED, then the Scoreboard should be displayed in QUESTION mode', () => {
    const game = createMockGame(GameStatus.ENDED);
    
    render(<GameFinishedPage game={game} onClickLeaveGame={() => {}} />);

    expect(screen.getByTestId('scoreboard')).toBeInTheDocument();
  });
});
