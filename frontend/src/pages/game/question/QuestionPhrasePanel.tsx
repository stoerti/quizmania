import {GameQuestion} from "../../../domain/GameModel";
import {Box, Paper, Stack, Typography, useTheme} from "@mui/material";
import React from "react";

export type QuestionPhrasePanelProps = {
  gameQuestion: GameQuestion,
}


export const QuestionPhrasePanel = (props: QuestionPhrasePanelProps) => {
  const theme = useTheme()

  let questionImage
  if (props.gameQuestion.question.imagePath !== 'undefined') {
    questionImage = <img src={props.gameQuestion.question.imagePath} style={{maxWidth: '100%', maxHeight: '250px', margin: 'auto'}}/>
  }

  return (<Paper sx={{padding: 2, backgroundColor: theme.palette.primary.light}}>
    <Typography sx={{flex: '1 1 100%'}} variant="overline" component="div">
      Question {props.gameQuestion.gameQuestionNumber}
    </Typography>
    <Stack sx={{
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center'
    }}>
      <Typography sx={{flex: '1 1 100%'}} variant="h5" component="div">
        {props.gameQuestion.question.phrase}
      </Typography>
      {questionImage}
    </Stack>
  </Paper>)
}
