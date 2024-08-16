import {Game, Player} from "../../../domain/GameModel";
import {Box, IconButton, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography} from "@mui/material";
import CheckCircle from "@mui/icons-material/CheckCircle";
import Cancel from "@mui/icons-material/Cancel";
import React, {useState} from "react";
import {ArrowBack, ArrowForward} from "@mui/icons-material";
import useWindowDimensions from "../../../hooks/useWindowDimensions.tsx";
import Cookies from "js-cookie";


const comparePlayersByPointsAndName = function (p1: Player, p2: Player): number {
  if (p1.points < p2.points)
    return 1
  if (p1.points > p2.points)
    return -1

  return p1.name.localeCompare(p2.name)
}

export type ScoreboardProps = {
  game: Game,
}

export type ScoreboardPageProps = {
  game: Game,
  page: number,
  pageSize: number,
}

const ScoreboardPage = ({game, page, pageSize}: ScoreboardPageProps) => {
  const lastQuestion = game.currentQuestion
  const username = Cookies.get("username")

  const first = page * pageSize
  const last = (page + 1) * pageSize

  if (lastQuestion !== undefined) {
    return <Table style={{width: '100%', minWidth: 400, maxWidth: 1000}} size={"small"} aria-label="simple table">
      <TableHead>
        <TableRow>
          <TableCell></TableCell>
          <TableCell>Username</TableCell>
          <TableCell>Answer</TableCell>
          <TableCell align="right" colSpan={2}>Points</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {[...game.players].sort(comparePlayersByPointsAndName)
          .slice(first, last)
          .map((player, index) => {
            let icon
            let questionPoints
            const playerAnswer = lastQuestion?.answers.find(p => p.gamePlayerId === player.id)

            if (playerAnswer !== undefined) {
              icon = playerAnswer.points > 0 ? <CheckCircle sx={{verticalAlign: 'bottom'}} color='success'/> :
                <Cancel sx={{verticalAlign: 'bottom'}} color='error'/>
              questionPoints = "+" + playerAnswer.points
            } else {
              icon = <Cancel sx={{verticalAlign: 'bottom'}} color='error'/>
              questionPoints = "+0"
            }

            const fontWeight = player.name === username ? 'bold' : 'normal'

            return (
              <TableRow key={player.id} sx={{'&:last-child td, &:last-child th': {border: 0}}}>
                <TableCell width={20} align="left" sx={{fontWeight: fontWeight}}>#{index + 1 + page * pageSize}</TableCell>
                <TableCell>
                  <Typography variant="body1" component="div" sx={{fontWeight: fontWeight}}>
                    {player.name}
                  </Typography>
                </TableCell>
                <TableCell>
                  <Typography variant="body1" sx={{fontWeight: fontWeight}}>
                    {icon} {playerAnswer?.answer}
                  </Typography>
                </TableCell>
                <TableCell width={10} align="right" sx={{fontWeight: fontWeight}}>{questionPoints}</TableCell>
                <TableCell width={10} align="right" sx={{fontWeight: fontWeight}}>{player.points}</TableCell>
              </TableRow>
            )
          })}
      </TableBody>
    </Table>
  } else {
    return <Table style={{width: '100%', minWidth: 400, maxWidth: 1000}} size={"small"} aria-label="simple table">
      <TableHead>
        <TableRow>
          <TableCell></TableCell>
          <TableCell>Username</TableCell>
          <TableCell align="right">Points</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {[...game.players].sort(comparePlayersByPointsAndName)
          .slice(first, last)
          .map((player, index) => {
            const fontWeight = player.name === username ? 'bold' : 'normal'
            return (
              <TableRow key={player.id} sx={{'&:last-child td, &:last-child th': {border: 0}}}>
                <TableCell width={20} align="left" sx={{fontWeight: fontWeight}}>#{index + 1 + page * pageSize}</TableCell>
                <TableCell component="td" scope="row">
                  <Typography variant="body1" component="div" sx={{fontWeight: fontWeight}}>
                    {player.name}
                  </Typography>
                </TableCell>
                <TableCell width={10} align="right" sx={{fontWeight: fontWeight}}>{player.points}</TableCell>
              </TableRow>
            )
          })}
      </TableBody>
    </Table>
  }

}


export const Scoreboard = ({game}: ScoreboardProps) => {
  const [firstPage, setFirstPage] = useState(0)
  const {width, height} = useWindowDimensions();

  const numRows = Math.max(5, Math.floor((height - 400) / 36))
  const numColumns = Math.min(Math.floor(width / 550), Math.ceil(game.players.length / numRows))
  if (numColumns > 1) {
    const pageSize = numRows
    const pages = Math.ceil(game.players.length / pageSize)

    let pageControls
    if (numColumns < pages) {
      pageControls = <Box>
        <IconButton onClick={() => setFirstPage(Math.max(firstPage - 1, 0))}><ArrowBack/></IconButton>
        <IconButton onClick={() => setFirstPage(Math.min(firstPage + 1, pages - numColumns))}><ArrowForward/></IconButton>
      </Box>
    }

    return <Stack spacing={2} sx={{width: '100%'}} alignItems={"center"}>
      <Stack sx={{width: '100%'}} direction="row" justifyContent={"center"} spacing={2}>{
        [...Array(numColumns)].map((_, index) => {
          return <Box sx={{width: '100%'}}><ScoreboardPage game={game} page={firstPage + index} pageSize={pageSize}/></Box>
        })}
      </Stack>
      {pageControls}

    </Stack>
  } else {
    return (
      <Stack direction="row" justifyContent={"center"} spacing={2} sx={{width: '100%', maxWidth: 650}}>
        <ScoreboardPage game={game} page={0} pageSize={game.players.length}/>
      </Stack>)
  }
}
