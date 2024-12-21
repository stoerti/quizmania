import {Game, Player} from "../../../domain/GameModel";
import {Box, IconButton, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography, useTheme} from "@mui/material";
import CheckCircle from "@mui/icons-material/CheckCircle";
import Cancel from "@mui/icons-material/Cancel";
import React, {useState} from "react";
import {ArrowBack, ArrowForward} from "@mui/icons-material";
import useWindowDimensions from "../../../hooks/useWindowDimensions.tsx";
import {useUsername} from "../../../hooks/useUsername.ts";


const comparePlayersByPointsAndAnswerTime = function (p1: Player, p2: Player): number {
  if (p1.points < p2.points)
    return 1
  if (p1.points > p2.points)
    return -1

  return p1.totalAnswerTime - p2.totalAnswerTime
}

export enum ScoreboardMode {
  QUESTION = 'QUESTION',
  ROUND = 'ROUND'
}

export type ScoreboardProps = {
  game: Game,
  mode: ScoreboardMode
}

export type ScoreboardPageProps = {
  game: Game,
  mode: ScoreboardMode,
  page: number,
  pageSize: number,
}

const ScoreboardPage = ({game, mode, page, pageSize}: ScoreboardPageProps) => {
  const lastQuestion = game.currentQuestion
  const lastRound = game.currentRound
  const {username} = useUsername()

  const theme = useTheme()

  const first = page * pageSize
  const last = (page + 1) * pageSize

  if (mode === ScoreboardMode.QUESTION && lastQuestion !== undefined) {
    return <Table style={{width: '100%', minWidth: 400, maxWidth: 1000, margin: 'auto'}} size={"small"} aria-label="simple table">
      <TableHead>
        <TableRow>
          <TableCell></TableCell>
          <TableCell>Username</TableCell>
          <TableCell>Answer</TableCell>
          <TableCell align="right" colSpan={2}>Points</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {[...game.players].sort(comparePlayersByPointsAndAnswerTime)
          .slice(first, last)
          .map((player, index) => {
            let icon
            let questionPoints
            const playerAnswer = lastQuestion?.answers.find(p => p.gamePlayerId === player.id)

            if (playerAnswer !== undefined) {
              if (playerAnswer.points > 0) {
                icon = <CheckCircle sx={{verticalAlign: 'bottom'}} color='success'/>
                questionPoints = <Typography color={theme.palette.text.primary}>+{playerAnswer.points}</Typography>
              } else if (playerAnswer.points < 0) {
                icon = <Cancel sx={{verticalAlign: 'bottom'}} color='error'/>
                questionPoints = <Typography color={theme.palette.error.main}>{playerAnswer.points}</Typography>
              } else {
                questionPoints = <Typography color={theme.palette.error.main}>+{playerAnswer.points}</Typography>
              }
            } else {
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
  } else if (mode === ScoreboardMode.ROUND && lastRound !== undefined && lastRound.status === 'SCORED') {
    return <Table style={{width: '100%', minWidth: 400, maxWidth: 1000, margin: 'auto'}} size={"small"} aria-label="simple table">
      <TableHead>
        <TableRow>
          <TableCell></TableCell>
          <TableCell>Username</TableCell>
          <TableCell align="right" colSpan={2}>Points</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {[...game.players].sort(comparePlayersByPointsAndAnswerTime)
          .slice(first, last)
          .map((player, index) => {
            let questionPoints
            const roundPlayer = lastRound?.players.find(p => p.id === player.id)

            if (roundPlayer !== undefined) {
              if (roundPlayer.points > 0) {
                questionPoints = <Typography color={theme.palette.text.primary}>+{roundPlayer.points}</Typography>
              } else if (roundPlayer.points < 0) {
                questionPoints = <Typography color={theme.palette.error.main}>{roundPlayer.points}</Typography>
              } else {
                questionPoints = <Typography color={theme.palette.error.main}>+{roundPlayer.points}</Typography>
              }
            } else {
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
                <TableCell width={10} align="right" sx={{fontWeight: fontWeight}}>{questionPoints}</TableCell>
                <TableCell width={10} align="right" sx={{fontWeight: fontWeight}}>{player.points}</TableCell>
              </TableRow>
            )
          })}
      </TableBody>
    </Table>
  } else {
    return <Table style={{width: '100%', minWidth: 400, maxWidth: 1000, margin: 'auto'}} size={"small"} aria-label="simple table">
      <TableHead>
        <TableRow>
          <TableCell></TableCell>
          <TableCell>Username</TableCell>
          <TableCell align="right">Points</TableCell>
          <TableCell align="right">ø</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {[...game.players].sort(comparePlayersByPointsAndAnswerTime)
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
                <TableCell width={10} align="right" sx={{fontWeight: fontWeight}}>{Math.round(game.findAverageAnswerTime(player.id)/10)/100}s</TableCell>
              </TableRow>
            )
          })}
      </TableBody>
    </Table>
  }

}


export const Scoreboard = ({game, mode}: ScoreboardProps) => {
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
          return <Box sx={{width: '100%'}}><ScoreboardPage game={game} mode={mode} page={firstPage + index} pageSize={pageSize}/></Box>
        })}
      </Stack>
      {pageControls}

    </Stack>
  } else {
    return (
      <Stack direction="row" justifyContent={"center"} alignItems={"center"} spacing={2} sx={{width: '100%', maxWidth: 650}}>
        <ScoreboardPage game={game} mode={mode} page={0} pageSize={game.players.length}/>
      </Stack>)
  }
}
