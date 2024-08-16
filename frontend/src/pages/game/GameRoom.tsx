import {Game} from "../../domain/GameModel";
import React from "react";
import Cookies from "js-cookie";
import {AppBar, Box, Toolbar, Typography} from "@mui/material";
import {ModeratorGameRoomPanel} from "./gameroom/ModeratorGameRoomPanel";
import {PlayerGameRoomPanel} from "./gameroom/PlayerGameRoomPanel";
import {SpectatorGameRoomPanel} from "./gameroom/SpectatorGameRoomPanel.tsx";


export type GameRoomPageProps = {
  game: Game
}

export const GameRoomPage = ({game}: GameRoomPageProps) => {
  const username = Cookies.get("username")

  const userIsPlayer = game.players.find(p => p.name === username)

  let container
  if (game.moderator == username) {
    container = <ModeratorGameRoomPanel game={game}/>
  } else if (!userIsPlayer) {
    container = <SpectatorGameRoomPanel game={game}/>
  } else {
    const currentPlayer = game.players.find(player => player.name === username)!
    container = <PlayerGameRoomPanel game={game} player={currentPlayer}/>
  }

  return (
    <div>
      <AppBar position="static">
        <Toolbar>
          <Typography sx={{flex: '1 1 100%'}} variant="h6" component="div">
            {game.name}
          </Typography>
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
