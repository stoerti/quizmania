import React, {useEffect} from "react";
import {
    GameCanceledEvent,
    GameDto,
    GameStartedEvent,
    GameStatus,
    UserAddedEvent,
    UserRemovedEvent
} from "../../services/GameServiceTypes";
import {useSnackbar} from "material-ui-snackbar-provider";
import Cookies from "js-cookie";
import {GameLobbyPage} from "./GameLobby";
import {GameRoomPage} from "./GameRoom";
import {GameService} from "../../services/GameService";
import {GameFinishedPage} from "./GameFinished";

type GamePageProps = {
    gameId: string,
    onGameEnded(): void
}

const GamePage = (props: GamePageProps) => {

    const [game, setGame] = React.useState<GameDto | undefined>(undefined)

    const gameService = new GameService()
    const snackbar = useSnackbar()

    useEffect(() => {
        gameService.findGame(props.gameId, setGame)
        gameService.subscribeToGame(props.gameId, {
            onGameStarted(event: GameStartedEvent) {
                setGame({
                    ...game!,
                    status: GameStatus.STARTED
                })
            },
            onGameEnded(event, game) {
                setGame(game)
            },
            onGameCanceled(event: GameCanceledEvent) {
                gameService.unsubscribeFromGame()
                props.onGameEnded()
                snackbar.showMessage("Game was canceled by creator or system")
            },
            onUserAdded(event: UserAddedEvent) {
                setGame({
                    ...game!,
                    users: [...game!.users, {id: event.gameUserId, name: event.username, points: 0}]
                })
            },
            onUserRemoved(event: UserRemovedEvent) {
                if (event.username === Cookies.get('username')) {
                    // I left the game - return to game selection page
                    gameService.unsubscribeFromGame()
                    props.onGameEnded()
                } else {
                    // someone else left the game - remove user
                    setGame({
                        ...game!,
                        users: game!.users
                            .filter((user, index) => {
                                return user.id !== event.gameUserId
                            })
                            .map((user) => {
                                return {...user}
                            })
                    })
                }
            },
            onQuestionAsked(event, game) {
                setGame(game)
            },
            onQuestionAnswered(event, game) {
                setGame(game)
            },
            onQuestionClosed(event, game) {
                setGame(game)
            }
        })
    }, []);

    let page
    if (game === undefined) {
        page = <div>Loading...</div>
    } else if (game.status === GameStatus.CREATED) {
        page = <GameLobbyPage game={game} gameService={gameService}/>
    } else if (game.status === GameStatus.STARTED) {
        page = <GameRoomPage game={game} gameService={gameService}/>
    } else if (game.status === GameStatus.ENDED) {
        page = <GameFinishedPage game={game} gameService={gameService} onClickLeaveGame={() => {
            gameService.unsubscribeFromGame();
            props.onGameEnded();
        }}/>
    } else {
        page = <div>Unknown game state {game.status}</div>
    }

    return (
        <div>
            {page}
        </div>
    )
}

export default GamePage