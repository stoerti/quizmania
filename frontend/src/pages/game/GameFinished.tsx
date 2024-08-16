import {Game} from "../../domain/GameModel";
import React from "react";
import {AppBar, Box, Button, Stack, Toolbar, Typography} from "@mui/material";
import Logout from "@mui/icons-material/Logout";
import {Scoreboard} from "./gameroom/Scoreboard.tsx";

export type GameFinishedPageProps = {
  game: Game,
  onClickLeaveGame: () => void
}

export const GameFinishedPage = (props: GameFinishedPageProps) => {

  return (
    <div>
      <AppBar position="static">
        <Toolbar>
          <Typography
            sx={{flex: '1 1 100%'}}
            variant="h6"
            component="div"
          >
            {props.game.name} - Results
          </Typography>
        </Toolbar>
      </AppBar>
      <Box sx={{
        display: "flex",
        alignItems: "center",
        margin: 2
      }}>
        <Box sx={{maxWidth: '650px', width: '100%', margin: 'auto'}}>
          <Stack spacing={2}>
            <div style={{display: "flex", alignItems: "center"}}>
              <Button sx={{margin: 'auto'}} startIcon={<Logout/>} variant="contained"
                      onClick={props.onClickLeaveGame}>Leave game</Button>
            </div>
            <Scoreboard game={props.game} />
          </Stack>
        </Box>
      </Box>
    </div>
  )
}
