import {Game, Player} from "../../../domain/GameModel";
import {Table, TableBody, TableCell, TableHead, TableRow, Typography} from "@mui/material";
import React from "react";
import {MarkEmailRead, QuestionMark} from "@mui/icons-material";

export type PlayerAnswerLogProps = {
  game: Game,
}

export const PlayerAnswerLog = ({game}: PlayerAnswerLogProps) => {

  const lastQuestion = game.findLastQuestion()!

  return             <Table aria-label="simple table">
    <TableHead>
      <TableRow>
        <TableCell></TableCell>
        <TableCell>Username</TableCell>
      </TableRow>
    </TableHead>
    <TableBody>
      {game.players.sort((a1, a2) => a1.name.localeCompare(a2.name)).map(player => {
        let icon;
        if (lastQuestion.hasPlayerAlreadyAnswered(player.id)) {
          icon = <MarkEmailRead color='success'/>
        } else {
          icon = <QuestionMark color='info'/>
        }
        return (
          <TableRow key={player.id} sx={{'&:last-child td, &:last-child th': {border: 0}}}>
            <TableCell align="left">{icon}</TableCell>
            <TableCell component="th" scope="row">
              <Typography variant="body1" component="div">
                {player.name}
              </Typography>
            </TableCell>
          </TableRow>
        )
      })}
    </TableBody>
  </Table>

}
