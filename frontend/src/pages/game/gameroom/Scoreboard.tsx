import {Game, GameQuestion, Player, QuestionStatus} from "../../../domain/GameModel";
import {Box, Button, CircularProgress, Grid, IconButton, Paper, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography} from "@mui/material";
import CheckCircle from "@mui/icons-material/CheckCircle";
import PlayArrow from "@mui/icons-material/PlayArrow";
import Cancel from "@mui/icons-material/Cancel";
import React, {useState} from "react";
import {ArrowBack, ArrowForward, Build, MarkEmailRead, QuestionMark, StopCircle} from "@mui/icons-material";
import {QuestionPhrasePanel} from "../question/QuestionPhrasePanel";
import Countdown from "react-countdown";
import {QuestionCountdownBar} from "../question/QuestionCountdownBar";
import {GameQuestionMode} from "../../../services/GameEventTypes";
import {useSnackbar} from "material-ui-snackbar-provider";
import {CorrectAnswerContainer} from "../question/CorrectAnswerContainer";
import {Simulate} from "react-dom/test-utils";
import play = Simulate.play;
import useWindowDimensions from "../../../hooks/useWindowDimensions.tsx";
import {gameCommandService, GameException} from "../../../services/GameCommandService.tsx";


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
  const lastQuestion = game.findLastQuestion()!

  const first = page * pageSize
  const last = (page + 1) * pageSize

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
          const playerAnswer = lastQuestion.answers.find(p => p.gamePlayerId === player.id)

          if (playerAnswer !== undefined) {
            icon = playerAnswer.points > 0 ? <CheckCircle sx={{verticalAlign: 'bottom'}} color='success'/> :
              <Cancel sx={{verticalAlign: 'bottom'}} color='error'/>
            questionPoints = "+" + playerAnswer.points
          } else {
            icon = <Cancel sx={{verticalAlign: 'bottom'}} color='error'/>
            questionPoints = "+0"
          }

          return (
            <TableRow key={player.id} sx={{'&:last-child td, &:last-child th': {border: 0}}}>
              <TableCell width={20} align="left">#{index + 1 + page * pageSize}</TableCell>
              <TableCell component="td" scope="row">
                <Typography variant="body1" component="div">
                  {player.name}
                </Typography>
              </TableCell>
              <TableCell component="td" scope="row">
                <Typography variant="body1">
                  {icon} {playerAnswer?.answer}
                </Typography>
              </TableCell>
              <TableCell width={10} align="right">{questionPoints}</TableCell>
              <TableCell width={10} align="right">{player.points}</TableCell>
            </TableRow>
          )
        })}
    </TableBody>
  </Table>

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
