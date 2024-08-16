import {useTheme} from "@mui/material";
import React from "react";

export type QuestionCountdownBarProps = {
  totalTime: number,
  timeLeft: number,
}

export const QuestionCountdownBar = ({timeLeft, totalTime}: QuestionCountdownBarProps) => {
  const theme = useTheme()

  const percentage = timeLeft / totalTime
  const color = percentage < 0.2 ? theme.palette.error.main : theme.palette.primary.main

  return (<div style={{backgroundColor: color, width: percentage * 100 + '%', height: '10px'}}></div>)
}
