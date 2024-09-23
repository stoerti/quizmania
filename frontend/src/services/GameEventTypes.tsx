export type GameConfig = {
  maxPlayers: number,
  numQuestions: number,
  secondsToAnswer: number,
  questionSetId: string,
  useBuzzer: boolean,
}

export enum GameQuestionMode {
  BUZZER = 'BUZZER',
  COLLECTIVE = 'COLLECTIVE',
}

export enum QuestionType {
  CHOICE = 'CHOICE',
  FREE_INPUT = 'FREE_INPUT',
  ESTIMATE = 'ESTIMATE',
}

export type Question = {
  type: QuestionType,
  phrase: string,
  imagePath: string | undefined,
  correctAnswer: string,
  answerOptions: string[]
}

export type GameCreatedEvent = {
  gameId: string,
  name: string,
  config: GameConfig,
  creatorUsername: string,
  moderatorUsername: string | undefined,
}

export type GameStartedEvent = {
  gameId: string,
}

export type GameEndedEvent = {
  gameId: string,
}

export type GameCanceledEvent = {
  gameId: string,
}

export type PlayerJoinedGameEvent = {
  gameId: string,
  gamePlayerId: string,
  username: string,
}

export type PlayerLeftGameEvent = {
  gameId: string,
  gamePlayerId: string,
  username: string,
}

export type QuestionAskedEvent = {
  gameId: string,
  gameQuestionId: string,
  gameQuestionNumber: number,
  questionMode: GameQuestionMode,
  questionTimestamp: string,
  timeToAnswer: number,
  question: Question,
}

export type QuestionAnsweredEvent = {
  gameId: string,
  gameQuestionId: string,
  gamePlayerId: string,
  playerAnswerId: string,
  answer: string,
  timeToAnswer: number
}

export type QuestionAnswerOverriddenEvent = {
  gameId: string,
  gameQuestionId: string,
  gamePlayerId: string,
  playerAnswerId: string,
  answer: string
}

export type QuestionBuzzedEvent = {
  gameId: string,
  gameQuestionId: string,
  gamePlayerId: string,
  buzzerTimestamp: string
}

export type QuestionBuzzerWonEvent = {
  gameId: string,
  gameQuestionId: string,
  gamePlayerId: string,
}

export type QuestionClosedEvent = {
  gameId: string,
  gameQuestionId: string,
}

export interface GamePoints {
  [key: string]: number
}

export type QuestionScoredEvent = {
  gameId: string,
  gameQuestionId: string,
  points: GamePoints,
}

export type GameEvent = GameStartedEvent
  | GameEndedEvent

