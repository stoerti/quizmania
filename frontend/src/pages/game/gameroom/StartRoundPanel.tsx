import {Game} from "../../../domain/GameModel";
import {Box, Button, Grid, Paper, Stack, Typography, useTheme} from "@mui/material";
import React from "react";
import {gameCommandService, GameException} from "../../../services/GameCommandService.tsx";
import PlayArrow from "@mui/icons-material/PlayArrow";
import {useSnackbar} from "material-ui-snackbar-provider";

export type StartRoundPanelProps = {
  game: Game,
  isModerator: boolean
}

export const StartRoundPanel = ({game, isModerator}: StartRoundPanelProps) => {

  const currentRound = game.currentRound!
  const theme = useTheme()
  const snackbar = useSnackbar()

  let questionMode
  if ( currentRound.roundConfig.useBuzzer ) {
    questionMode = "Buzzer"
  } else {
    questionMode = "Everyone answers"
  }
  let askQuestionButton;
  if (isModerator) {
    const onAskNextQuestion = async () => {
      try {
        await gameCommandService.askNextQuestion(game.id)
      } catch (error) {
        if (error instanceof GameException) {
          snackbar.showMessage(error.message)
        }
      }
    }
    askQuestionButton = <div style={{display: "flex", alignItems: "center"}}>
      <Button id="startRound" sx={{margin: 'auto'}} startIcon={<PlayArrow/>} variant="contained"
              onClick={onAskNextQuestion}>Start</Button>
    </div>
  }

  return <Stack spacing={10} alignItems={"center"} sx={{width: '100%'}}>
    <Paper sx={{padding: 2, backgroundColor: theme.palette.primary.contrastText, maxWidth: 650, width: '100%'}} elevation={5}>
      <Box sx={{display: 'block', m: 'auto', alignContent: 'center'}}>
        <Typography sx={{flex: '1 1 100%', textAlign: 'center'}} variant="h4" component="div">
          Round {currentRound!.roundNumber}
        </Typography>
        <Typography sx={{flex: '1 1 100%', textAlign: 'center'}} variant="h2" component="div">
          {currentRound.roundName}
        </Typography>
      </Box>
    </Paper>

    {askQuestionButton}

    <Box sx={{ flexGrow: 1 }}>
      <Grid container spacing={2} columns={2}>
        <Grid item xs={1}>
          <Typography sx={{flex: '1 1 100%', textAlign: 'right', fontWeight: 700}} variant="h5" component="div">
            Questions:
          </Typography>
        </Grid>
        <Grid item xs={1}>
          <Typography sx={{flex: '1 1 100%', textAlign: 'left'}} variant="h5" component="div">
            {currentRound.numQuestions}
          </Typography>
        </Grid>
        <Grid item xs={1}>
          <Typography sx={{flex: '1 1 100%', textAlign: 'right', fontWeight: 700}} variant="h5" component="div">
            Answer time:
          </Typography>
        </Grid>
        <Grid item xs={1}>
          <Typography sx={{flex: '1 1 100%', textAlign: 'left'}} variant="h5" component="div">
            {currentRound.roundConfig.secondsToAnswer} seconds
          </Typography>
        </Grid>
        <Grid item xs={1}>
          <Typography sx={{flex: '1 1 100%', textAlign: 'right', fontWeight: 700}} variant="h5" component="div">
            Question mode:
          </Typography>
        </Grid>
        <Grid item xs={1}>
          <Typography sx={{flex: '1 1 100%', textAlign: 'left'}} variant="h5" component="div">
            {questionMode}
          </Typography>
        </Grid>
      </Grid>
    </Box>
  </Stack>

}
