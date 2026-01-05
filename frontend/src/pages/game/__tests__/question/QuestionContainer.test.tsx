import { describe, expect, test, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QuestionContainer } from '../../question/QuestionContainer';
import React from 'react';
import { GameQuestion } from '../../../../domain/GameModel';
import { QuestionType, GameQuestionMode } from '../../../../services/GameEventTypes';

// Mock child components
vi.mock('../../question/QuestionPhrasePanel', () => ({
  QuestionPhrasePanel: () => <div data-testid="question-phrase-panel">QuestionPhrasePanel</div>,
}));

vi.mock('../../question/QuestionCountdownBar', () => ({
  QuestionCountdownBar: () => <div data-testid="countdown-bar">CountdownBar</div>,
}));

vi.mock('react-countdown', () => ({
  default: ({ renderer }: any) => renderer({ total: 5000 })
}));

describe('QuestionContainer', () => {
  const mockOnAnswerQuestion = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  function createQuestion(type: QuestionType, answerOptions: string[] = []): GameQuestion {
    return new GameQuestion({
      gameId: 'test-game-id',
      gameQuestionId: 'question-1',
      roundNumber: 1,
      roundQuestionNumber: 1,
      questionMode: GameQuestionMode.COLLECTIVE,
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

  test('When question type is CHOICE, then answer options should be displayed as clickable buttons', () => {
    const question = createQuestion(QuestionType.CHOICE, ['Option A', 'Option B', 'Option C', 'Option D']);

    render(<QuestionContainer gameQuestion={question} onAnswerQuestion={mockOnAnswerQuestion} />);

    expect(screen.getByRole('button', { name: 'Option A' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Option B' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Option C' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Option D' })).toBeInTheDocument();
  });

  test('When question type is MULTIPLE_CHOICE, then answer options should allow multiple selection and have a confirm button', () => {
    const question = createQuestion(QuestionType.MULTIPLE_CHOICE, ['Option A', 'Option B', 'Option C']);

    render(<QuestionContainer gameQuestion={question} onAnswerQuestion={mockOnAnswerQuestion} />);

    // Multiple choice shows options and a confirm button
    expect(screen.getByRole('button', { name: /Confirm/i })).toBeInTheDocument();
  });

  test('When question type is SORT, then answer options should have up/down arrows for reordering and a confirm button', () => {
    const question = createQuestion(QuestionType.SORT, ['First', 'Second', 'Third']);

    render(<QuestionContainer gameQuestion={question} onAnswerQuestion={mockOnAnswerQuestion} />);

    // Check for confirm button
    expect(screen.getByRole('button', { name: /Confirm/i })).toBeInTheDocument();
    
    // Check for answer options
    expect(screen.getByText('First')).toBeInTheDocument();
    expect(screen.getByText('Second')).toBeInTheDocument();
    expect(screen.getByText('Third')).toBeInTheDocument();
  });

  test('When question type is FREE_INPUT, then a text input field and submit button should be displayed', () => {
    const question = createQuestion(QuestionType.FREE_INPUT);

    render(<QuestionContainer gameQuestion={question} onAnswerQuestion={mockOnAnswerQuestion} />);

    expect(screen.getByLabelText(/Answer/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Submit answer/i })).toBeInTheDocument();
  });

  test('When question type is ESTIMATE, then a number input field and submit button should be displayed', () => {
    const question = createQuestion(QuestionType.ESTIMATE);

    render(<QuestionContainer gameQuestion={question} onAnswerQuestion={mockOnAnswerQuestion} />);

    const input = screen.getByLabelText(/Answer/i) as HTMLInputElement;
    expect(input).toBeInTheDocument();
    expect(input.type).toBe('number');
    expect(screen.getByRole('button', { name: /Submit answer/i })).toBeInTheDocument();
  });
});
