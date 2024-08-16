import {Paper, Typography, useTheme} from "@mui/material";
import React from "react";

export type CorrectAnswerContainerProps = {
  correctAnswer: string
}

export const CorrectAnswerContainer = (props: CorrectAnswerContainerProps) => {
  const theme = useTheme()

  return <Paper sx={{backgroundColor: theme.palette.secondary.main, padding: 2, maxWidth: 650, width: '100%'}} elevation={5}>
    <Typography variant="body2" component="div">
      Answer
    </Typography>
    <Typography variant="h4" component="div">
      {props.correctAnswer}
    </Typography>
  </Paper>
}

