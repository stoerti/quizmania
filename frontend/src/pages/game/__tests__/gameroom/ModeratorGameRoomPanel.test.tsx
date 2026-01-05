import { describe, expect, test, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ModeratorGameRoomPanel } from '../../gameroom/ModeratorGameRoomPanel';
import React from 'react';
import { 
  createGameWithRound, 
  createGameWithQuestion 
} from '../../../../test-utils/fixtures';
import { 
  RoundStatus, 
  QuestionStatus,
  GameStatus 
} from '../../../../domain/GameModel';
import {
  GameQuestionMode,
  QuestionType
} from '../../../../services/GameEventTypes';

// Mock dependencies
vi.mock('notistack', () => ({
  useSnackbar: vi.fn(() => ({ 
    enqueueSnackbar: vi.fn(),
    closeSnackbar: vi.fn(),
  })),
}));

vi.mock('../../../../services/GameCommandService', () => ({
  gameCommandService: {
    closeQuestion: vi.fn(),
    answerBuzzerQuestion: vi.fn(),
    scoreQuestion: vi.fn(),
    askNextQuestion: vi.fn(),
    closeRound: vi.fn(),
  },
  GameException: class GameException extends Error {},
}));

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

vi.mock('react-countdown', () => ({
  default: ({ renderer }: any) => renderer({ total: 5000 })
}));

describe('ModeratorGameRoomPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test('When the round is undefined, then "Waiting for first round" message should be displayed', () => {
    const game = createGameWithRound([], RoundStatus.OPEN);
    // Create a game in STARTED state but without a round
    const gameWithoutRound = game.copyWith({ currentRound: undefined });

    render(<ModeratorGameRoomPanel game={gameWithoutRound} />);

    expect(screen.getByText('Waiting for first round')).toBeInTheDocument();
  });

  test('When the round status is OPEN and no question is active, then the StartRoundPanel should be displayed', () => {
    const game = createGameWithRound(
      [{ id: 'player1', name: 'Player 1' }],
      RoundStatus.OPEN
    );

    render(<ModeratorGameRoomPanel game={game} />);

    expect(screen.getByTestId('start-round-panel')).toBeInTheDocument();
  });

  test('When the round status is SCORED, then the round scoreboard and "Close Round" button should be displayed', () => {
    const game = createGameWithRound(
      [{ id: 'player1', name: 'Player 1' }],
      RoundStatus.SCORED
    );

    render(<ModeratorGameRoomPanel game={game} />);

    expect(screen.getByTestId('scoreboard')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Close Round/i })).toBeInTheDocument();
  });

  test('When question status is OPEN with COLLECTIVE mode and CHOICE type, then answer options should be displayed as buttons', () => {
    const game = createGameWithQuestion(
      [{ id: 'player1', name: 'Player 1' }],
      QuestionStatus.OPEN,
      GameQuestionMode.COLLECTIVE,
      QuestionType.CHOICE
    );

    render(<ModeratorGameRoomPanel game={game} />);

    expect(screen.getByText('Option A')).toBeInTheDocument();
    expect(screen.getByText('Option B')).toBeInTheDocument();
    expect(screen.getByText('Option C')).toBeInTheDocument();
    expect(screen.getByText('Option D')).toBeInTheDocument();
  });

  test('When question status is OPEN with COLLECTIVE mode, then the "Close question" button should be visible', () => {
    const game = createGameWithQuestion(
      [{ id: 'player1', name: 'Player 1' }],
      QuestionStatus.OPEN,
      GameQuestionMode.COLLECTIVE,
      QuestionType.FREE_INPUT
    );

    render(<ModeratorGameRoomPanel game={game} />);

    expect(screen.getByRole('button', { name: /Close question/i })).toBeInTheDocument();
  });

  test('When question status is OPEN with COLLECTIVE mode, then the countdown timer should be displayed', () => {
    const game = createGameWithQuestion(
      [{ id: 'player1', name: 'Player 1' }],
      QuestionStatus.OPEN,
      GameQuestionMode.COLLECTIVE,
      QuestionType.FREE_INPUT
    );

    render(<ModeratorGameRoomPanel game={game} />);

    expect(screen.getByTestId('countdown-bar')).toBeInTheDocument();
  });

  test('When question status is CLOSED, then the "Rate question" button, correct answer, and player answers table should be displayed', () => {
    const game = createGameWithQuestion(
      [{ id: 'player1', name: 'Player 1' }],
      QuestionStatus.CLOSED,
      GameQuestionMode.COLLECTIVE,
      QuestionType.FREE_INPUT
    );

    render(<ModeratorGameRoomPanel game={game} />);

    expect(screen.getByRole('button', { name: /Rate question/i })).toBeInTheDocument();
    expect(screen.getByTestId('correct-answer')).toBeInTheDocument();
  });

  test('When question status is SCORED, then the "Next question" button and question scoreboard should be displayed', () => {
    const game = createGameWithQuestion(
      [{ id: 'player1', name: 'Player 1' }],
      QuestionStatus.SCORED,
      GameQuestionMode.COLLECTIVE,
      QuestionType.FREE_INPUT
    );

    render(<ModeratorGameRoomPanel game={game} />);

    expect(screen.getByRole('button', { name: /Next question/i })).toBeInTheDocument();
    expect(screen.getByTestId('scoreboard')).toBeInTheDocument();
  });

  test('When question status is OPEN with BUZZER mode and no buzz winner, then "Waiting on players to hit the buzzer" message should be displayed', () => {
    const game = createGameWithQuestion(
      [{ id: 'player1', name: 'Player 1' }],
      QuestionStatus.OPEN,
      GameQuestionMode.BUZZER,
      QuestionType.FREE_INPUT
    );

    render(<ModeratorGameRoomPanel game={game} />);

    expect(screen.getByText('Waiting on players to hit the buzzer')).toBeInTheDocument();
  });

  test('When question status is OPEN with BUZZER mode and a buzz winner exists, then the buzz winner name and accept/reject buttons should be displayed', () => {
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

    render(<ModeratorGameRoomPanel game={game} />);

    expect(screen.getByText('Player 1')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Accept answer/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Reject answer/i })).toBeInTheDocument();
  });

  test('When question status is OPEN with BUZZER mode and CHOICE type, then answer options should be displayed as buttons', () => {
    const game = createGameWithQuestion(
      [{ id: 'player1', name: 'Player 1' }],
      QuestionStatus.OPEN,
      GameQuestionMode.BUZZER,
      QuestionType.CHOICE
    );

    render(<ModeratorGameRoomPanel game={game} />);

    expect(screen.getByText('Option A')).toBeInTheDocument();
    expect(screen.getByText('Option B')).toBeInTheDocument();
    expect(screen.getByText('Option C')).toBeInTheDocument();
    expect(screen.getByText('Option D')).toBeInTheDocument();
  });
});
