import {QuestionType} from "../services/GameEventTypes";

/**
 * Shortens answer text for display based on question type
 * For SORT questions, truncates each answer option to 5 characters
 */
export const formatAnswerForDisplay = (answer: string, questionType: QuestionType): string => {
  if (questionType === QuestionType.SORT) {
    // Split by comma, trim, take first 5 chars of each, and rejoin
    return answer.split(',')
      .map(item => item.trim().substring(0, 5))
      .join(', ');
  }
  return answer;
};
