import {Game, GameQuestion} from "../../../domain/GameModel";
import {Table, TableBody, TableCell, TableHead, TableRow} from "@mui/material";
import Cancel from "@mui/icons-material/Cancel";
import HelpOutline from "@mui/icons-material/HelpOutline";
import React from "react";

export type BuzzerListProps = {
  game: Game,
  question: GameQuestion
}

export const BuzzerList = ({game, question}: BuzzerListProps) => {
  // Create lookup maps for better performance
  const answeredPlayerIds = new Set(question.answers.map(a => a.gamePlayerId))

  return (
    <Table aria-label="buzzer list" size={"small"}>
      <TableHead>
        <TableRow>
          <TableCell></TableCell>
          <TableCell>Username</TableCell>
          <TableCell align="right">Status</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {question.buzzedPlayerIds.map((playerId, index) => {
          const playerName = game.findPlayerName(playerId)
          const hasAnswered = answeredPlayerIds.has(playerId)
          const isCurrentlyAnswering = question.currentBuzzWinnerId === playerId

          let statusIcon = null
          if (hasAnswered) {
            // Player already attempted and answered incorrectly - show X for rejected answer
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
                  {playerName}
              </TableCell>
              <TableCell align="right">{statusIcon}</TableCell>
            </TableRow>
          )
        })}
      </TableBody>
    </Table>
  )
}
