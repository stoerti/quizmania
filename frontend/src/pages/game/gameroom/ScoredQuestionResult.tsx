import {Game} from "../../../domain/GameModel";
import {Grid, Paper, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography, useTheme} from "@mui/material";
import React from "react";
import {Scoreboard, ScoreboardMode} from "./Scoreboard";
import {QuestionType} from "../../../services/GameEventTypes";
import {splitAnswerItems} from "../../../utils/answerFormatter";

export type ScoredQuestionResultProps = {
  game: Game,
}

/**
 * Component that displays scored question results based on question type
 * For SORT questions or questions with answerImagePath: two-column layout with answer on left, scoreboard on right
 * For other questions: standard layout with correct answer above scoreboard
 */
export const ScoredQuestionResult = ({game}: ScoredQuestionResultProps) => {
  const question = game.currentQuestion;
  const theme = useTheme();

  if (!question) {
    return null;
  }

  // For SORT questions, use two-column layout
  if (question.question.type === QuestionType.SORT) {
    const correctAnswers = splitAnswerItems(question.question.correctAnswer);

    return (
      <Grid container={true} spacing={2} sx={{width: '100%', maxWidth: 1200}}>
        <Grid item xs={12} md={4}>
          <Table size={"small"} aria-label="simple table">
            <TableHead>
              <TableRow>
                <TableCell></TableCell>
                <TableCell>Correct order</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {correctAnswers.map((answer, index) => (
                <TableRow key={index} sx={{'&:last-child td, &:last-child th': {border: 0}}}>
                  <TableCell width={20} align="left">#{index + 1}</TableCell>
                  <TableCell component="td" scope="row">
                    <Typography variant="body1" component="div">
                      {answer}
                    </Typography>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Grid>
        <Grid item xs={12} md={8}>
          <Scoreboard game={game} mode={ScoreboardMode.QUESTION}/>
        </Grid>
      </Grid>
    );
  }

  // For questions with answerImagePath, use two-column layout with answer text and image
  if (question.question.answerImagePath && question.question.answerImagePath !== 'undefined') {
    return (
      <Grid container={true} spacing={2} sx={{width: '100%', maxWidth: 1200}}>
        <Grid item xs={12} md={4}>
          <Paper sx={{padding: 2}}>
            <Typography variant="body2" component="div">
              Answer
            </Typography>
            <Typography variant="h4" component="div" sx={{marginBottom: 2}}>
              {question.question.correctAnswer}
            </Typography>
            <img 
              src={question.question.answerImagePath} 
              alt="Answer illustration" 
              style={{maxWidth: '100%', maxHeight: '350px', margin: 'auto', display: 'block'}}
            />
          </Paper>
        </Grid>
        <Grid item xs={12} md={8}>
          <Scoreboard game={game} mode={ScoreboardMode.QUESTION}/>
        </Grid>
      </Grid>
    );
  }

  // For other question types, use standard layout
  return (
    <Stack spacing={2} sx={{width: '100%'}} alignItems={"center"}>
      <Paper sx={{padding: 2, maxWidth: 650, width: '100%'}}>
        <Typography variant="body2" component="div">
          Answer
        </Typography>
        <Typography variant="h4" component="div">
          {question.question.correctAnswer}
        </Typography>
      </Paper>
      <Scoreboard game={game} mode={ScoreboardMode.QUESTION}/>
    </Stack>
  );
};
