import { vi } from 'vitest';

/**
 * Common mock for react-router hooks
 */
export const mockReactRouter = () => {
  vi.mock('react-router', () => ({
    useParams: vi.fn(() => ({ gameId: 'test-game-id' })),
    useNavigate: vi.fn(() => vi.fn()),
  }));
};

/**
 * Common mock for notistack
 */
export const mockNotistack = () => {
  vi.mock('notistack', () => ({
    useSnackbar: vi.fn(() => ({ 
      enqueueSnackbar: vi.fn(),
      closeSnackbar: vi.fn(),
    })),
  }));
};

/**
 * Common mock for useUsername hook
 */
export const mockUseUsername = () => {
  vi.mock('../../../hooks/useUsername', () => ({
    useUsername: vi.fn(() => ({ username: 'test-user' })),
  }));
};

/**
 * Mock for GameRepository with controllable methods
 */
export const mockGameRepository = () => {
  const mockFindGame = vi.fn();
  const mockSubscribeToGame = vi.fn();
  const mockUnsubscribeFromGame = vi.fn();

  vi.mock('../../../services/GameRepository', () => {
    class MockGameRepository {
      findGame = mockFindGame;
      subscribeToGame = mockSubscribeToGame;
      unsubscribeFromGame = mockUnsubscribeFromGame;
    }
    
    return {
      GameRepository: MockGameRepository,
      GameEventType: {},
    };
  });

  return {
    mockFindGame,
    mockSubscribeToGame,
    mockUnsubscribeFromGame,
  };
};

/**
 * Mock for GameCommandService
 */
export const mockGameCommandService = () => {
  vi.mock('../../../services/GameCommandService', () => ({
    gameCommandService: {
      leaveGame: vi.fn(),
      startGame: vi.fn(),
    },
    GameException: class GameException extends Error {},
  }));
};

/**
 * Mock for LeaveGameDialog component
 */
export const mockLeaveGameDialog = () => {
  vi.mock('../LeaveGameDialog', () => ({
    __esModule: true,
    default: () => <div data-testid="leave-game-dialog">LeaveGameDialog</div>,
  }));
};
