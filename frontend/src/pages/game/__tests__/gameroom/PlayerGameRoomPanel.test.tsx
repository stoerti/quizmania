import { describe, expect, test, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { PlayerGameRoomPanel } from '../../gameroom/PlayerGameRoomPanel';
import React from 'react';
import { 
  createGameWithRound, 
  createGameWithQuestion 
} from '../../../../test-utils/fixtures';
import { 
  RoundStatus, 
  QuestionStatus,
  Player
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

vi.mock('../../../../hooks/useUsername', () => ({
  useUsername: vi.fn(() => ({ username: 'Player 1' })),
}));

vi.mock('../../../../services/GameCommandService', () => ({
  gameCommandService: {
    answerQuestion: vi.fn(),
    buzzQuestion: vi.fn(),
    askNextQuestion: vi.fn(),
    closeRound: vi.fn(),
  },
  GameException: class GameException extends Error {},
}));

// Mock child components
vi.mock('../../question/QuestionContainer', () => ({
  QuestionContainer: () => <div data-testid="question-container">QuestionContainer</div>,
}));

vi.mock('../../question/BuzzerQuestionContainer', () => ({
  BuzzerQuestionContainer: () => <div data-testid="buzzer-container">BuzzerContainer</div>,
}));

vi.mock('../../question/QuestionPhrasePanel', () => ({
  QuestionPhrasePanel: () => <div data-testid="question-phrase-panel">QuestionPhrasePanel</div>,
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

vi.mock('../../gameroom/StartRoundPanel', () => ({
  StartRoundPanel: () => <div data-testid="start-round-panel">StartRoundPanel</div>,
}));

vi.mock('../../gameroom/ScoredQuestionResult', () => ({
  ScoredQuestionResult: () => <div data-testid="scored-result">ScoredResult</div>,
}));

describe('PlayerGameRoomPanel', () => {
  const player: Player = new Player('player1', 'Player 1');

  beforeEach(() => {
    vi.clearAllMocks();
  });

  test('When the round is undefined, then "Waiting for first round" message should be displayed', () => {
    const game = createGameWithRound(
      [{ id: 'player1', name: 'Player 1' }],
      RoundStatus.OPEN
    );
    const gameWithoutRound = game.copyWith({ currentRound: undefined });

    render(<PlayerGameRoomPanel game={gameWithoutRound} player={player} />);

    expect(screen.getByText('Waiting for first round')).toBeInTheDocument();
  });

  test('When the round status is OPEN and no question is active, then the StartRoundPanel should be displayed with creator check', () => {
    const game = createGameWithRound(
      [{ id: 'player1', name: 'Player 1' }],
      RoundStatus.OPEN,
      { creatorUsername: 'Player 1' }
    );

    render(<PlayerGameRoomPanel game={game} player={player} />);

    expect(screen.getByTestId('start-round-panel')).toBeInTheDocument();
  });

  test('When the round status is SCORED and current user is the creator, then the "Close Round" button should be visible', () => {
    const game = createGameWithRound(
      [{ id: 'player1', name: 'Player 1' }],
      RoundStatus.SCORED,
      { creatorUsername: 'Player 1' }
    );

    render(<PlayerGameRoomPanel game={game} player={player} />);

    expect(screen.getByRole('button', { name: /Close Round/i })).toBeInTheDocument();
    expect(screen.getByTestId('scoreboard')).toBeInTheDocument();
  });

  test('When the round status is SCORED and current user is not the creator, then the "Close Round" button should not be visible', () => {
    const game = createGameWithRound(
      [
        { id: 'player1', name: 'Player 1' },
        { id: 'player2', name: 'Player 2' }
      ],
      RoundStatus.SCORED,
      { creatorUsername: 'Player 2' }
    );

    render(<PlayerGameRoomPanel game={game} player={player} />);

    expect(screen.queryByRole('button', { name: /Close Round/i })).not.toBeInTheDocument();
    expect(screen.getByTestId('scoreboard')).toBeInTheDocument();
  });

  test('When question status is OPEN with COLLECTIVE mode and player has not answered, then the QuestionContainer should be displayed', () => {
    const game = createGameWithQuestion(
      [{ id: 'player1', name: 'Player 1' }],
      QuestionStatus.OPEN,
      GameQuestionMode.COLLECTIVE,
      QuestionType.CHOICE
    );

    render(<PlayerGameRoomPanel game={game} player={player} />);

    expect(screen.getByTestId('question-container')).toBeInTheDocument();
  });

  test('When question status is OPEN with COLLECTIVE mode and player has already answered, then "waiting for other players" message and progress indicator should be displayed', () => {
    let game = createGameWithQuestion(
      [
        { id: 'player1', name: 'Player 1' },
        { id: 'player2', name: 'Player 2' }
      ],
      QuestionStatus.OPEN,
      GameQuestionMode.COLLECTIVE,
      QuestionType.FREE_INPUT
    );

    // Simulate player answering
    game = game.onQuestionAnswered({
      gameId: 'test-game-id',
      gameQuestionId: 'question-1',
      gamePlayerId: 'player1',
      playerAnswerId: 'answer-1',
      answer: 'My answer',
      timeToAnswer: 1000
    });

    render(<PlayerGameRoomPanel game={game} player={player} />);

    expect(screen.getByText(/1 of 2 players answered/i)).toBeInTheDocument();
  });

  test('When question status is CLOSED, then the correct answer and all player answers should be displayed', () => {
    const game = createGameWithQuestion(
      [{ id: 'player1', name: 'Player 1' }],
      QuestionStatus.CLOSED,
      GameQuestionMode.COLLECTIVE,
      QuestionType.FREE_INPUT
    );

    render(<PlayerGameRoomPanel game={game} player={player} />);

    expect(screen.getByTestId('correct-answer')).toBeInTheDocument();
  });

  test('When question status is SCORED and current user is the creator, then the "Next question" button should be visible', () => {
    const game = createGameWithQuestion(
      [{ id: 'player1', name: 'Player 1' }],
      QuestionStatus.SCORED,
      GameQuestionMode.COLLECTIVE,
      QuestionType.FREE_INPUT,
      { creatorUsername: 'Player 1' }
    );

    render(<PlayerGameRoomPanel game={game} player={player} />);

    expect(screen.getByRole('button', { name: /Next question/i })).toBeInTheDocument();
    expect(screen.getByTestId('scoreboard')).toBeInTheDocument();
  });

  test('When question status is SCORED and current user is not the creator, then the "Next question" button should not be visible', () => {
    const game = createGameWithQuestion(
      [
        { id: 'player1', name: 'Player 1' },
        { id: 'player2', name: 'Player 2' }
      ],
      QuestionStatus.SCORED,
      GameQuestionMode.COLLECTIVE,
      QuestionType.FREE_INPUT,
      { creatorUsername: 'Player 2' }
    );

    render(<PlayerGameRoomPanel game={game} player={player} />);

    expect(screen.queryByRole('button', { name: /Next question/i })).not.toBeInTheDocument();
    expect(screen.getByTestId('scoreboard')).toBeInTheDocument();
  });

  test('When question status is OPEN with BUZZER mode and player has not buzzed, then the BuzzerQuestionContainer should be displayed', () => {
    const game = createGameWithQuestion(
      [{ id: 'player1', name: 'Player 1' }],
      QuestionStatus.OPEN,
      GameQuestionMode.BUZZER,
      QuestionType.FREE_INPUT
    );

    render(<PlayerGameRoomPanel game={game} player={player} />);

    expect(screen.getByTestId('buzzer-container')).toBeInTheDocument();
  });
});
