import { Game, GameStatus, QuestionStatus, RoundStatus } from '../domain/GameModel';
import { 
  GameCreatedEvent, 
  PlayerJoinedGameEvent, 
  RoundStartedEvent, 
  QuestionAskedEvent,
  GameQuestionMode,
  QuestionType,
  RoundScoredEvent
} from '../services/GameEventTypes';

/**
 * Creates a basic game with the given status
 */
export function createMockGame(status: GameStatus, options?: {
  gameId?: string;
  name?: string;
  creatorUsername?: string;
  moderatorUsername?: string;
}): Game {
  const event: GameCreatedEvent = {
    gameId: options?.gameId || 'test-game-id',
    name: options?.name || 'Test Game',
    config: {
      maxPlayers: 10,
      questionSetId: 'test-questionset',
    },
    creatorUsername: options?.creatorUsername || 'creator',
    moderatorUsername: options?.moderatorUsername,
    rounds: [
      {
        name: 'Round 1',
        roundConfig: {
          useBuzzer: true,
          secondsToAnswer: 10,
        },
        questions: []
      }
    ]
  };
  
  const game = new Game(event);
  return game.copyWith({ status });
}

/**
 * Creates a game with a moderator and players
 */
export function createGameWithModerator(
  moderatorUsername: string, 
  players: { id: string; name: string }[],
  options?: {
    gameId?: string;
    name?: string;
    creatorUsername?: string;
  }
): Game {
  const event: GameCreatedEvent = {
    gameId: options?.gameId || 'test-game-id',
    name: options?.name || 'Test Game',
    config: {
      maxPlayers: 10,
      questionSetId: 'test-questionset',
    },
    creatorUsername: options?.creatorUsername || 'creator',
    moderatorUsername: moderatorUsername,
    rounds: [
      {
        name: 'Round 1',
        roundConfig: {
          useBuzzer: true,
          secondsToAnswer: 10,
        },
        questions: []
      }
    ]
  };
  
  let game = new Game(event);
  
  // Add players
  players.forEach(player => {
    const playerEvent: PlayerJoinedGameEvent = {
      gameId: options?.gameId || 'test-game-id',
      gamePlayerId: player.id,
      username: player.name,
    };
    game = game.onPlayerJoined(playerEvent);
  });
  
  return game;
}

/**
 * Creates a game with a creator and players
 */
export function createGameWithCreator(
  creatorUsername: string, 
  players: { id: string; name: string }[],
  options?: {
    gameId?: string;
    name?: string;
  }
): Game {
  const event: GameCreatedEvent = {
    gameId: options?.gameId || 'test-game-id',
    name: options?.name || 'Test Game',
    config: {
      maxPlayers: 10,
      questionSetId: 'test-questionset',
    },
    creatorUsername: creatorUsername,
    moderatorUsername: undefined,
    rounds: [
      {
        name: 'Round 1',
        roundConfig: {
          useBuzzer: true,
          secondsToAnswer: 10,
        },
        questions: []
      }
    ]
  };
  
  let game = new Game(event);
  
  // Add players
  players.forEach(player => {
    const playerEvent: PlayerJoinedGameEvent = {
      gameId: options?.gameId || 'test-game-id',
      gamePlayerId: player.id,
      username: player.name,
    };
    game = game.onPlayerJoined(playerEvent);
  });
  
  return game;
}

/**
 * Creates a started game with a round
 */
export function createGameWithRound(
  players: { id: string; name: string }[],
  roundStatus: RoundStatus,
  options?: {
    gameId?: string;
    name?: string;
    creatorUsername?: string;
    moderatorUsername?: string;
    roundNumber?: number;
    numQuestions?: number;
  }
): Game {
  let game = createMockGame(GameStatus.STARTED, {
    gameId: options?.gameId,
    name: options?.name,
    creatorUsername: options?.creatorUsername,
    moderatorUsername: options?.moderatorUsername,
  });
  
  // Add players
  players.forEach(player => {
    const playerEvent: PlayerJoinedGameEvent = {
      gameId: options?.gameId || 'test-game-id',
      gamePlayerId: player.id,
      username: player.name,
    };
    game = game.onPlayerJoined(playerEvent);
  });
  
  // Start a round
  const roundStartedEvent: RoundStartedEvent = {
    gameId: options?.gameId || 'test-game-id',
    gameRoundId: 'round-1',
    roundNumber: options?.roundNumber || 1,
    roundName: 'Round 1',
    roundConfig: {
      useBuzzer: true,
      secondsToAnswer: 10,
    },
    questions: Array(options?.numQuestions || 3).fill('').map((_, i) => `question-${i + 1}`)
  };
  
  game = game.onRoundStarted(roundStartedEvent);
  
  if (roundStatus === RoundStatus.SCORED) {
    const roundScoredEvent: RoundScoredEvent = {
      gameId: options?.gameId || 'test-game-id',
    };
    game = game.onRoundScored(roundScoredEvent);
  }
  
  return game;
}

/**
 * Creates a game with an active question
 */
export function createGameWithQuestion(
  players: { id: string; name: string }[],
  questionStatus: QuestionStatus,
  questionMode: GameQuestionMode,
  questionType: QuestionType,
  options?: {
    gameId?: string;
    name?: string;
    creatorUsername?: string;
    moderatorUsername?: string;
    hasAnswerOptions?: boolean;
    currentBuzzWinnerId?: string;
    buzzedPlayerIds?: string[];
  }
): Game {
  let game = createGameWithRound(players, RoundStatus.OPEN, {
    gameId: options?.gameId,
    name: options?.name,
    creatorUsername: options?.creatorUsername,
    moderatorUsername: options?.moderatorUsername,
  });
  
  // Ask a question
  const answerOptions = options?.hasAnswerOptions !== false && 
    (questionType === QuestionType.CHOICE || 
     questionType === QuestionType.MULTIPLE_CHOICE ||
     questionType === QuestionType.SORT)
    ? ['Option A', 'Option B', 'Option C', 'Option D']
    : [];
  
  const questionAskedEvent: QuestionAskedEvent = {
    gameId: options?.gameId || 'test-game-id',
    gameQuestionId: 'question-1',
    roundNumber: 1,
    roundQuestionNumber: 1,
    questionMode: questionMode,
    questionTimestamp: new Date().toISOString(),
    timeToAnswer: 10000,
    question: {
      type: questionType,
      phrase: 'What is the test question?',
      imagePath: undefined,
      answerImagePath: undefined,
      correctAnswer: 'Test Answer',
      answerOptions: answerOptions
    }
  };
  
  game = game.onQuestionAsked(questionAskedEvent);
  
  // Handle buzzer state
  if (options?.buzzedPlayerIds) {
    options.buzzedPlayerIds.forEach(playerId => {
      game = game.onQuestionBuzzed({ 
        gameId: options?.gameId || 'test-game-id',
        gameQuestionId: 'question-1',
        gamePlayerId: playerId
      });
    });
  }
  
  if (options?.currentBuzzWinnerId) {
    game = game.onQuestionBuzzerWon({
      gameId: options?.gameId || 'test-game-id',
      gameQuestionId: 'question-1',
      gamePlayerId: options.currentBuzzWinnerId
    });
  }
  
  // Update question status if needed
  if (questionStatus === QuestionStatus.CLOSED) {
    game = game.onQuestionClosed({
      gameId: options?.gameId || 'test-game-id',
      gameQuestionId: 'question-1'
    });
  } else if (questionStatus === QuestionStatus.SCORED) {
    game = game.onQuestionClosed({
      gameId: options?.gameId || 'test-game-id',
      gameQuestionId: 'question-1'
    });
    game = game.onQuestionScored({
      gameId: options?.gameId || 'test-game-id',
      gameQuestionId: 'question-1',
      points: {}
    });
  }
  
  return game;
}
