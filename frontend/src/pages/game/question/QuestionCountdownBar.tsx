import {useTheme} from "@mui/material";
import React from "react";

export type QuestionCountdownBarProps = {
  totalTime: number,
  timeLeft: number,
}

export const QuestionCountdownBar = (props: QuestionCountdownBarProps) => {
  const theme = useTheme()

  let percentage = props.timeLeft / props.totalTime
  let color = percentage < 0.2 ? theme.palette.error.main : theme.palette.primary.main

  return (<div style={{backgroundColor: color, width: percentage * 100 + '%', height: '10px'}}></div>)
}
