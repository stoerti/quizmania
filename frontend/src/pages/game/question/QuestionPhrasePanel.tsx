import {GameQuestion} from "../../../domain/GameModel";
import {Paper, Stack, Typography, useTheme} from "@mui/material";
import React from "react";

export type QuestionPhrasePanelProps = {
  gameQuestion: GameQuestion,
}


export const QuestionPhrasePanel = ({gameQuestion}: QuestionPhrasePanelProps) => {
  const theme = useTheme()

  let questionImage
  if (gameQuestion.question.imagePath !== 'undefined') {
    questionImage = <img src={gameQuestion.question.imagePath} style={{maxWidth: '100%', maxHeight: '250px', margin: 'auto'}}/>
  }

  return <Paper sx={{padding: 2, backgroundColor: theme.palette.primary.contrastText, maxWidth: 650, width: '100%'}} elevation={5}>
    <Typography sx={{flex: '1 1 100%'}} variant="overline" component="div">
      Question {gameQuestion.roundQuestionNumber}
    </Typography>
    <Stack sx={{
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center'
    }}>
      <Typography sx={{flex: '1 1 100%'}} variant="h5" component="div">
        {gameQuestion.question.phrase}
      </Typography>
      {questionImage}
    </Stack>
  </Paper>
}
