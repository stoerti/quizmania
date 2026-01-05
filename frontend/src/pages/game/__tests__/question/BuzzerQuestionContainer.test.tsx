import { describe, expect, test, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { BuzzerQuestionContainer } from '../../question/BuzzerQuestionContainer';
import React from 'react';
import { GameQuestion, Player } from '../../../../domain/GameModel';
import { QuestionType, GameQuestionMode } from '../../../../services/GameEventTypes';

// Mock child components
vi.mock('../../question/QuestionPhrasePanel', () => ({
  QuestionPhrasePanel: () => <div data-testid="question-phrase-panel">QuestionPhrasePanel</div>,
}));

describe('BuzzerQuestionContainer', () => {
  const mockOnBuzzQuestion = vi.fn();
  const player = new Player('player1', 'Player 1');

  beforeEach(() => {
    vi.clearAllMocks();
  });

  function createBuzzerQuestion(type: QuestionType, answerOptions: string[] = []): GameQuestion {
    return new GameQuestion({
      gameId: 'test-game-id',
      gameQuestionId: 'question-1',
      roundNumber: 1,
      roundQuestionNumber: 1,
      questionMode: GameQuestionMode.BUZZER,
      questionTimestamp: new Date().toISOString(),
      timeToAnswer: 10000,
      question: {
        type: type,
        phrase: 'What is the test question?',
        imagePath: undefined,
        answerImagePath: undefined,
        correctAnswer: 'Test Answer',
        answerOptions: answerOptions
      }
    });
  }

  test('When player has not buzzed yet, then the buzzer should show "HIT THE BUZZER" and be clickable', () => {
    const question = createBuzzerQuestion(QuestionType.FREE_INPUT);

    render(<BuzzerQuestionContainer gameQuestion={question} player={player} onBuzzQuestion={mockOnBuzzQuestion} />);

    expect(screen.getByText('HIT THE BUZZER')).toBeInTheDocument();
  });

  test('When player has buzzed but no winner selected, then the buzzer should show "WAITING..."', () => {
    let question = createBuzzerQuestion(QuestionType.FREE_INPUT);
    
    // Simulate player buzzing
    question = question.onQuestionBuzzed({
      gameId: 'test-game-id',
      gameQuestionId: 'question-1',
      gamePlayerId: 'player1'
    });

    render(<BuzzerQuestionContainer gameQuestion={question} player={player} onBuzzQuestion={mockOnBuzzQuestion} />);

    expect(screen.getByText('WAITING...')).toBeInTheDocument();
  });

  test('When player has buzzed and won the buzzer, then the buzzer should show "ANSWER QUESTION"', () => {
    let question = createBuzzerQuestion(QuestionType.FREE_INPUT);
    
    // Simulate player buzzing
    question = question.onQuestionBuzzed({
      gameId: 'test-game-id',
      gameQuestionId: 'question-1',
      gamePlayerId: 'player1'
    });
    
    // Simulate player winning the buzz
    question = question.onQuestionBuzzerWon({
      gameId: 'test-game-id',
      gameQuestionId: 'question-1',
      gamePlayerId: 'player1'
    });

    render(<BuzzerQuestionContainer gameQuestion={question} player={player} onBuzzQuestion={mockOnBuzzQuestion} />);

    expect(screen.getByText('ANSWER QUESTION')).toBeInTheDocument();
  });

  test('When player has buzzed but someone else won, then the buzzer should show "SOMEONE ELSE WAS FASTER"', () => {
    let question = createBuzzerQuestion(QuestionType.FREE_INPUT);
    
    // Simulate player buzzing
    question = question.onQuestionBuzzed({
      gameId: 'test-game-id',
      gameQuestionId: 'question-1',
      gamePlayerId: 'player1'
    });
    
    // Simulate another player winning the buzz
    question = question.onQuestionBuzzerWon({
      gameId: 'test-game-id',
      gameQuestionId: 'question-1',
      gamePlayerId: 'player2'
    });

    render(<BuzzerQuestionContainer gameQuestion={question} player={player} onBuzzQuestion={mockOnBuzzQuestion} />);

    expect(screen.getByText('SOMEONE ELSE WAS FASTER')).toBeInTheDocument();
  });
});
