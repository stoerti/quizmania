import { describe, expect, test, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { SpectatorGameRoomPanel } from '../../gameroom/SpectatorGameRoomPanel';
import React from 'react';
import { 
  createGameWithRound, 
  createGameWithQuestion 
} from '../../../../test-utils/fixtures';
import { 
  RoundStatus, 
  QuestionStatus
} from '../../../../domain/GameModel';
import {
  GameQuestionMode,
  QuestionType
} from '../../../../services/GameEventTypes';

// Mock child components
vi.mock('../../question/QuestionPhrasePanel', () => ({
  QuestionPhrasePanel: () => <div data-testid="question-phrase-panel">QuestionPhrasePanel</div>,
}));

vi.mock('../../question/QuestionCountdownBar', () => ({
  QuestionCountdownBar: () => <div data-testid="countdown-bar">CountdownBar</div>,
}));

vi.mock('../../question/CorrectAnswerContainer', () => ({
  CorrectAnswerContainer: () => <div data-testid="correct-answer">CorrectAnswer</div>,
}));

vi.mock('../../gameroom/Scoreboard', () => ({
  Scoreboard: () => <div data-testid="scoreboard">Scoreboard</div>,
  ScoreboardMode: {
    QUESTION: 'QUESTION',
    ROUND: 'ROUND',
  }
}));

vi.mock('../../gameroom/PlayerAnswerLog', () => ({
  PlayerAnswerLog: () => <div data-testid="player-answer-log">PlayerAnswerLog</div>,
}));

vi.mock('../../gameroom/StartRoundPanel', () => ({
  StartRoundPanel: () => <div data-testid="start-round-panel">StartRoundPanel</div>,
}));

vi.mock('../../gameroom/ScoredQuestionResult', () => ({
  ScoredQuestionResult: () => <div data-testid="scored-result">ScoredResult</div>,
}));

vi.mock('react-countdown', () => ({
  default: ({ renderer }: any) => renderer({ total: 5000 })
}));

describe('SpectatorGameRoomPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test('When the round is undefined, then "Waiting for first round" message should be displayed', () => {
    const game = createGameWithRound([], RoundStatus.OPEN);
    const gameWithoutRound = game.copyWith({ currentRound: undefined });

    render(<SpectatorGameRoomPanel game={gameWithoutRound} />);

    expect(screen.getByText('Waiting for first round')).toBeInTheDocument();
  });

  test('When the round status is OPEN and no question is active, then the StartRoundPanel should be displayed with isModerator=false', () => {
    const game = createGameWithRound(
      [{ id: 'player1', name: 'Player 1' }],
      RoundStatus.OPEN
    );

    render(<SpectatorGameRoomPanel game={game} />);

    expect(screen.getByTestId('start-round-panel')).toBeInTheDocument();
  });

  test('When the round status is SCORED, then the round scoreboard should be displayed without any action buttons', () => {
    const game = createGameWithRound(
      [{ id: 'player1', name: 'Player 1' }],
      RoundStatus.SCORED
    );

    render(<SpectatorGameRoomPanel game={game} />);

    expect(screen.getByTestId('scoreboard')).toBeInTheDocument();
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  test('When question status is OPEN with COLLECTIVE mode and CHOICE type, then answer options should be displayed as disabled buttons', () => {
    const game = createGameWithQuestion(
      [{ id: 'player1', name: 'Player 1' }],
      QuestionStatus.OPEN,
      GameQuestionMode.COLLECTIVE,
      QuestionType.CHOICE
    );

    render(<SpectatorGameRoomPanel game={game} />);

    expect(screen.getByText('Option A')).toBeInTheDocument();
    expect(screen.getByText('Option B')).toBeInTheDocument();
    expect(screen.getByText('Option C')).toBeInTheDocument();
    expect(screen.getByText('Option D')).toBeInTheDocument();
  });

  test('When question status is OPEN with COLLECTIVE mode, then the countdown timer should be displayed', () => {
    const game = createGameWithQuestion(
      [{ id: 'player1', name: 'Player 1' }],
      QuestionStatus.OPEN,
      GameQuestionMode.COLLECTIVE,
      QuestionType.CHOICE
    );

    render(<SpectatorGameRoomPanel game={game} />);

    expect(screen.getByTestId('countdown-bar')).toBeInTheDocument();
  });

  test('When question status is OPEN with COLLECTIVE mode, then the PlayerAnswerLog should be displayed', () => {
    const game = createGameWithQuestion(
      [{ id: 'player1', name: 'Player 1' }],
      QuestionStatus.OPEN,
      GameQuestionMode.COLLECTIVE,
      QuestionType.FREE_INPUT
    );

    render(<SpectatorGameRoomPanel game={game} />);

    expect(screen.getByTestId('player-answer-log')).toBeInTheDocument();
  });

  test('When question status is CLOSED, then the correct answer and all player answers with correct/incorrect indicators should be displayed', () => {
    const game = createGameWithQuestion(
      [{ id: 'player1', name: 'Player 1' }],
      QuestionStatus.CLOSED,
      GameQuestionMode.COLLECTIVE,
      QuestionType.FREE_INPUT
    );

    render(<SpectatorGameRoomPanel game={game} />);

    expect(screen.getByTestId('correct-answer')).toBeInTheDocument();
  });

  test('When question status is SCORED, then the ScoredQuestionResult should be displayed', () => {
    const game = createGameWithQuestion(
      [{ id: 'player1', name: 'Player 1' }],
      QuestionStatus.SCORED,
      GameQuestionMode.COLLECTIVE,
      QuestionType.FREE_INPUT
    );

    render(<SpectatorGameRoomPanel game={game} />);

    expect(screen.getByTestId('scored-result')).toBeInTheDocument();
  });

  test('When question status is OPEN with BUZZER mode and no buzz winner, then "Waiting on players to hit the buzzer" message should be displayed', () => {
    const game = createGameWithQuestion(
      [{ id: 'player1', name: 'Player 1' }],
      QuestionStatus.OPEN,
      GameQuestionMode.BUZZER,
      QuestionType.FREE_INPUT
    );

    render(<SpectatorGameRoomPanel game={game} />);

    expect(screen.getByText('Waiting on players to hit the buzzer')).toBeInTheDocument();
  });

  test('When question status is OPEN with BUZZER mode and a buzz winner exists, then the buzz winner name should be displayed', () => {
    const game = createGameWithQuestion(
      [{ id: 'player1', name: 'Player 1' }],
      QuestionStatus.OPEN,
      GameQuestionMode.BUZZER,
      QuestionType.FREE_INPUT,
      {
        currentBuzzWinnerId: 'player1',
        buzzedPlayerIds: ['player1']
      }
    );

    render(<SpectatorGameRoomPanel game={game} />);

    expect(screen.getByText('Player 1')).toBeInTheDocument();
  });

  test('When question status is OPEN with BUZZER mode and CHOICE type, then answer options should be displayed', () => {
    const game = createGameWithQuestion(
      [{ id: 'player1', name: 'Player 1' }],
      QuestionStatus.OPEN,
      GameQuestionMode.BUZZER,
      QuestionType.CHOICE
    );

    render(<SpectatorGameRoomPanel game={game} />);

    expect(screen.getByText('Option A')).toBeInTheDocument();
    expect(screen.getByText('Option B')).toBeInTheDocument();
    expect(screen.getByText('Option C')).toBeInTheDocument();
    expect(screen.getByText('Option D')).toBeInTheDocument();
  });
});
