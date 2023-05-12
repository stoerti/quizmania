export type NewGameDto = {
    name: string,
    config: GameConfig,
    withModerator: boolean
}

export type GameConfig = {
    maxPlayers: number,
    numQuestions: number,
}

export type NewAnswerDto = {
    gameId: string,
    gameQuestionId: string,
    answer: string
}

export type GameDto = {
    id: string,
    name: string,
    maxPlayers: number,
    creator: string,
    moderator: string,
    status: GameStatus,
    users: GameUserDto[]
    questions: GameQuestionDto[]
}

export enum GameStatus {
    CREATED = 'CREATED',
    STARTED = 'STARTED',
    ENDED = 'ENDED',
    CANCELED = 'CANCELED'
}

export type GameUserDto = {
    id: string
    name: string,
    points: number
}

export type GameQuestionDto = {
    id: string,
    questionNumber: number,
    phrase: string,
    open: boolean,
    correctAnswer: string,
    answerOptions: string[],
    userAnswers: UserAnswerDto[]
}

export type UserAnswerDto = {
    id: string,
    gameUserId: string,
    answer: string,
}

export type GameStartedEvent = {
    gameId: string,
    game: GameDto
}

export type GameEndedEvent = {
    gameId: string,
    game: GameDto
}

export type GameCanceledEvent = {
    gameId: string,
    game: GameDto
}

export type UserAddedEvent = {
    gameId: string,
    gameUserId: string,
    username: string,
    game: GameDto
}

export type UserRemovedEvent = {
    gameId: string,
    gameUserId: string,
    username: string,
    game: GameDto
}

export type QuestionAskedEvent = {
    gameId: string,
    questionId: number,
    question: string,
    answers: string[],
    game: GameDto
}

export type QuestionAnsweredEvent = {
    gameId: string,
    questionId: number,
    question: string,
    answers: string[],
    game: GameDto
}

export type QuestionClosedEvent = {
    gameId: string,
    questionId: number,
    correctAnswer: string,
    game: GameDto
}
