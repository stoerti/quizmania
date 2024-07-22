import {Game} from "../../domain/GameModel";
import {GameCommandService} from "../../services/GameCommandService";
import React from "react";
import {
  AppBar,
  Box,
  Button, Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Toolbar,
  Typography
} from "@mui/material";
import Logout from "@mui/icons-material/Logout";
import EmojiEvents from "@mui/icons-material/EmojiEvents";
import {amber, brown, grey} from "@mui/material/colors";

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
            <Table aria-label="simple table" sx={{width: '100%'}}>
              <TableHead>
                <TableRow>
                  <TableCell></TableCell>
                  <TableCell>Name</TableCell>
                  <TableCell align="right">Points</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {[...props.game.players].sort((u1, u2) => u2.points - u1.points).map((row, index) => {
                  let cup;
                  switch (index) {
                    case 0:
                      cup = <EmojiEvents sx={{color: amber[500]}}/>;
                      break;
                    case 1:
                      cup = <EmojiEvents sx={{color: grey[500]}}/>;
                      break;
                    case 2:
                      cup = <EmojiEvents sx={{color: brown[700]}}/>;
                      break;
                  }
                  return (
                    <TableRow
                      key={row.id}
                      sx={{'&:last-child td, &:last-child th': {border: 0}}}
                    >
                      <TableCell align="left">{cup}</TableCell>
                      <TableCell component="th" scope="row">
                        <Typography
                          variant="body1"
                          component="div"
                        >
                          {row.name}
                        </Typography>
                      </TableCell>
                      <TableCell align="right">{row.points}</TableCell>
                    </TableRow>
                  )
                })}
              </TableBody>
            </Table>
            <div style={{display: "flex", alignItems: "center"}}>
              <Button sx={{margin: 'auto'}} startIcon={<Logout/>} variant="contained"
                      onClick={props.onClickLeaveGame}>Leave game</Button>
            </div>
          </Stack>
        </Box>
      </Box>
    </div>
)
}
