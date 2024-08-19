import {Game} from "../../domain/GameModel";
import React from "react";
import Cookies from "js-cookie";
import {AppBar, Box, IconButton, Toolbar, Tooltip, Typography} from "@mui/material";
import {ModeratorGameRoomPanel} from "./gameroom/ModeratorGameRoomPanel";
import {PlayerGameRoomPanel} from "./gameroom/PlayerGameRoomPanel";
import {SpectatorGameRoomPanel} from "./gameroom/SpectatorGameRoomPanel.tsx";
import Logout from "@mui/icons-material/Logout";
import {gameCommandService, GameException} from "../../services/GameCommandService.tsx";
import {useSnackbar} from "material-ui-snackbar-provider";


export type GameRoomPageProps = {
  game: Game
  onLeaveGame: () => void
}

export const GameRoomPage = ({game, onLeaveGame}: GameRoomPageProps) => {
  const snackbar = useSnackbar()
  const username = Cookies.get("username")

  const userIsPlayer = game.players.find(p => p.name === username)

  let container
  let onClickLeaveGame
  if (game.moderator == username) {
    container = <ModeratorGameRoomPanel game={game}/>
    onClickLeaveGame = async () => {
      try {
        await gameCommandService.leaveGame(game.id)
      } catch (error) {
        if (error instanceof GameException) {
          snackbar.showMessage(error.message)
        }
      }
    }
  } else if (!userIsPlayer) {
    container = <SpectatorGameRoomPanel game={game}/>
    onClickLeaveGame = onLeaveGame
  } else {
    const currentPlayer = game.players.find(player => player.name === username)!
    container = <PlayerGameRoomPanel game={game} player={currentPlayer}/>
    onClickLeaveGame = async () => {
      try {
        await gameCommandService.leaveGame(game.id)
      } catch (error) {
        if (error instanceof GameException) {
          snackbar.showMessage(error.message)
        }
      }
    }
  }

  return (
    <div>
      <AppBar position="static">
        <Toolbar>
          <Typography sx={{flex: '1 1 100%'}} variant="h6" component="div">
            {game.name}
          </Typography>
          <Tooltip title="Leave game">
            <IconButton color="inherit" onClick={onClickLeaveGame}>
              <Logout/>
            </IconButton>
          </Tooltip>
        </Toolbar>
      </AppBar>
      <Box sx={{
        display: "flex",
        justifyContent: "center",
        margin: 2
      }}>{container}</Box>
    </div>
  )
}
