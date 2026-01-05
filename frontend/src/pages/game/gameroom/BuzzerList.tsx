import {Game, GameQuestion} from "../../../domain/GameModel";
import {Table, TableBody, TableCell, TableHead, TableRow, Typography} from "@mui/material";
import CheckCircle from "@mui/icons-material/CheckCircle";
import Cancel from "@mui/icons-material/Cancel";
import HelpOutline from "@mui/icons-material/HelpOutline";
import React from "react";

export type BuzzerListProps = {
  game: Game,
  question: GameQuestion
}

export const BuzzerList = ({game, question}: BuzzerListProps) => {
  return (
    <Table aria-label="buzzer list">
      <TableHead>
        <TableRow>
          <TableCell>Position</TableCell>
          <TableCell>Username</TableCell>
          <TableCell align="right">Status</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {question.buzzedPlayerIds.map((playerId, index) => {
          const playerName = game.findPlayerName(playerId)
          const hasAnswered = question.answers.find(a => a.gamePlayerId === playerId)
          const isCurrentlyAnswering = question.currentBuzzWinnerId === playerId
          
          let statusIcon = null
          if (hasAnswered) {
            // Player already answered - show X for wrong answer
            statusIcon = <Cancel color='error'/>
          } else if (isCurrentlyAnswering) {
            // Player is currently answering - show ?
            statusIcon = <HelpOutline color='primary'/>
          }
          // else: waiting in line - no icon
          
          return (
            <TableRow key={playerId} sx={{'&:last-child td, &:last-child th': {border: 0}}}>
              <TableCell align="left">#{index + 1}</TableCell>
              <TableCell component="th" scope="row">
                <Typography variant="body1" component="div">
                  {playerName}
                </Typography>
              </TableCell>
              <TableCell align="right">{statusIcon}</TableCell>
            </TableRow>
          )
        })}
      </TableBody>
    </Table>
  )
}
