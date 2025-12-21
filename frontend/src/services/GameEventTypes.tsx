export type GameConfig = {
  maxPlayers: number,
  questionSetId: string,
}

export type RoundConfig = {
  useBuzzer: boolean,
  secondsToAnswer: number,
}

export type Round = {
  name: string,
  roundConfig: RoundConfig,
  questions: Question[]
}

export enum GameQuestionMode {
  BUZZER = 'BUZZER',
  COLLECTIVE = 'COLLECTIVE',
}

export enum QuestionType {
  CHOICE = 'CHOICE',
  MULTIPLE_CHOICE = 'MULTIPLE_CHOICE',
  FREE_INPUT = 'FREE_INPUT',
  ESTIMATE = 'ESTIMATE',
  SORT = 'SORT',
}

export type Question = {
  type: QuestionType,
  phrase: string,
  imagePath: string | undefined,
  answerImagePath: string | undefined,
  correctAnswer: string,
  answerOptions: string[]
}

export type GameCreatedEvent = {
  gameId: string,
  name: string,
  config: GameConfig,
  creatorUsername: string,
  moderatorUsername: string | undefined,
  rounds: Round[]
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

export type RoundStartedEvent = {
  gameId: string,
  gameRoundId: string,
  roundNumber: number,
  roundName: string,
  roundConfig: RoundConfig,
  questions: string[]
}

export type RoundScoredEvent = {
  gameId: string,
}

export type RoundClosedEvent = {
  gameId: string,
}

export type PlayerLeftGameEvent = {
  gameId: string,
  gamePlayerId: string,
  username: string,
}

export type QuestionAskedEvent = {
  gameId: string,
  roundNumber: number,
  roundQuestionNumber: number,
  gameQuestionId: string,
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

