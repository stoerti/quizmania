import {QuestionType} from "../services/GameEventTypes";

/**
 * Splits a comma-separated answer string into individual items
 */
export const splitAnswerItems = (answer: string): string[] => {
  return answer.split(',').map(item => item.trim());
};

/**
 * Shortens answer text for display based on question type
 * For SORT questions, truncates each answer option to 5 characters with ellipsis
 */
export const formatAnswerForDisplay = (answer: string, questionType: QuestionType): string => {
  if (questionType === QuestionType.SORT) {
    // Split by comma, trim, take first 5 chars of each with ellipsis, and rejoin
    return splitAnswerItems(answer)
      .map(item => item.substring(0, 5) + (item.length > 5 ? '...' : ''))
      .join(', ');
  }
  return answer;
};
