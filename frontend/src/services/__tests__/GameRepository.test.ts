import {afterEach, describe, expect, it, vi} from "vitest";
import {GameRepository, GameEventWrapper} from "../GameRepository";
import {GameCreatedEvent, PlayerJoinedGameEvent} from "../GameEventTypes";

const socket = vi.hoisted(() => ({
  connect: () => {},
  receipt: () => {},
  receive: (_: {body: string}) => {},
  disconnected: vi.fn(),
}));
vi.mock("@stomp/stompjs", () => ({
  Client: class {
    constructor(config: {onConnect: () => void}) { socket.connect = config.onConnect; }
    activate() {}
    deactivate() {}
    watchForReceipt(_id: string, callback: () => void) { socket.receipt = callback; }
    subscribe(_topic: string, callback: (message: {body: string}) => void) { socket.receive = callback; }
    forceDisconnect() { socket.disconnected(); }
  },
}));

const created = {
  gameId: "game", name: "Test", creatorUsername: "moderator", moderatorUsername: "moderator",
  rounds: [], config: {maxPlayers: 10, questionSetId: "test"},
} as GameCreatedEvent;
const creation = {gameId: "game", cursor: "9007199254740993", timestamp: "", eventType: "GameCreatedEvent",
  payload: created} satisfies GameEventWrapper;
function joined(id: string, cursor: string): GameEventWrapper {
  return {gameId: "game", cursor, timestamp: "", eventType: "PlayerJoinedGameEvent",
    payload: {gameId: "game", gamePlayerId: id, username: id} as PlayerJoinedGameEvent};
}
const flush = () => new Promise(resolve => setTimeout(resolve, 0));
afterEach(() => { vi.unstubAllGlobals(); vi.clearAllMocks(); });

describe("DCB event cursors", () => {
  it("deduplicates history and live delivery without assuming consecutive or JS-safe numbers", () => {
    const repository = new GameRepository();
    repository.currentGameState = repository["initializeGameModel"]([creation]);
    const handler = {onGameEvent: vi.fn()};
    const event = joined("first", "9007199254740995");
    repository["handleEvent"](event, handler);
    repository["handleEvent"](event, handler);
    repository["handleEvent"](joined("old", "9007199254740994"), handler);
    expect(repository.currentGameState.players.map(player => player.id)).toEqual(["first"]);
    expect(handler.onGameEvent).toHaveBeenCalledTimes(1);
    expect(repository.lastReceivedCursor).toBe(event.cursor);
  });

  it("merges catch-up and buffered live events in cursor order on connect and reconnect", async () => {
    const repository = new GameRepository();
    const handler = {onGameEvent: vi.fn()};
    const fetchMock = vi.fn().mockResolvedValueOnce({json: async () => [creation]});
    vi.stubGlobal("fetch", fetchMock);
    repository.subscribeToGame("game", handler);
    await flush();
    socket.connect();

    let resolveHistory!: (value: unknown) => void;
    fetchMock.mockImplementationOnce(() => new Promise(resolve => { resolveHistory = resolve; }));
    socket.receipt();
    const first = joined("first", "9007199254740995");
    const second = joined("second", "9007199254740999");
    socket.receive({body: JSON.stringify(second)});
    resolveHistory({ok: true, json: async () => [first, second]});
    await flush();
    expect(repository.currentGameState!.players.map(player => player.id)).toEqual(["first", "second"]);
    expect(handler.onGameEvent).toHaveBeenCalledTimes(2);
    expect(fetchMock).toHaveBeenLastCalledWith("/api/game/game/events?afterCursor=" + creation.cursor);

    const third = joined("third", "9007199254741010");
    fetchMock.mockResolvedValueOnce({ok: true, json: async () => [third]});
    socket.connect();
    socket.receipt();
    await flush();
    expect(fetchMock).toHaveBeenLastCalledWith("/api/game/game/events?afterCursor=" + second.cursor);
    expect(repository.currentGameState!.players.map(player => player.id)).toEqual(["first", "second", "third"]);
  });

  it("does not skip missing history when catch-up fails", async () => {
    const repository = new GameRepository();
    const handler = {onGameEvent: vi.fn()};
    vi.stubGlobal("fetch", vi.fn().mockResolvedValueOnce({json: async () => [creation]})
      .mockResolvedValueOnce({ok: false}));
    repository.subscribeToGame("game", handler);
    await flush();
    socket.connect();
    socket.receive({body: JSON.stringify(joined("later", "9007199254740999"))});
    socket.receipt();
    await flush();
    expect(repository.lastReceivedCursor).toBe(creation.cursor);
    expect(handler.onGameEvent).not.toHaveBeenCalled();
    expect(socket.disconnected).toHaveBeenCalledOnce();
  });
});
