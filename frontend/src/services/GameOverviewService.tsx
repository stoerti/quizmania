export type GameDto = {
  id: string,
  name: string,
  maxPlayers: number,
  numQuestions: number,
  creator: string,
  moderator: string,
  status: GameStatus,
  users: GameUserDto[]
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
}

export class GameOverviewService {
  public searchOpenGames(responseHandler: (games: GameDto[]) => void, errorHandler: (err: any) => void = () => {
  }) {
    fetch('/api/game/?gameStatus=CREATED', {
      method: 'GET',
      headers: {
        'Content-type': 'application/json; charset=UTF-8',
      },
    })
      .then((response) => response.json())
      .then(responseHandler)
      .catch((err) => {
        if (errorHandler !== undefined)
          errorHandler(err);
      });
  }
}
