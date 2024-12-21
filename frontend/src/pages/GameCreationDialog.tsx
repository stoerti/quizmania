import {Box, Button, Dialog, DialogActions, DialogTitle, FormControlLabel, FormGroup, MenuItem, Select, Switch, TextField} from "@mui/material";
import React, {useEffect} from "react";
import {NewGameCommand} from "../services/GameCommandService";
import {QuestionSetDto} from "../services/QuestionSetServiceTypes";
import {questionSetService} from "../services/QuestionSetService";

type GameCreationDialogProps = {
  open: boolean
  onClose: () => void
  onCreateGame: (newGame: NewGameCommand) => void
}


export const GameCreationDialog = (props: GameCreationDialogProps) => {
  const {open, onClose, onCreateGame} = props
  const [questionSets, setQuestionSets] = React.useState<QuestionSetDto[]>([])

  useEffect(() => {
    questionSetService.searchQuestionSets(setQuestionSets)
  }, []);

  const handleClose = () => {
    onClose()
  }

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    const data = new FormData(event.currentTarget);

    const gameName: string = data.get('gameName')!.toString()
    const numSecondsToAnswer: number = parseInt(data.get('numSecondsToAnswer')!.toString())
    const questionSetId: string = data.get('questionSet')!.toString()
    const withModerator: boolean = data.get('moderator') === 'on'

    onCreateGame({
      name: gameName,
      config: {
        questionSetId: questionSetId,
        secondsToAnswer: numSecondsToAnswer,
      },
      withModerator: withModerator
    })
  }

  return (
    <Dialog onClose={handleClose} open={open}>
      <DialogTitle>Create new game</DialogTitle>
      <Box component="form" noValidate onSubmit={handleSubmit} sx={{
        my: 4,
        mx: 4,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
      }}>
        <FormGroup>
          <TextField margin="dense"
                     required
                     fullWidth
                     id="gameName"
                     label="Game name"
                     name="gameName"
                     autoFocus
          />
          <TextField margin="dense"
                     required
                     fullWidth
                     id="numSecondsToAnswer"
                     label="Question timer"
                     name="numSecondsToAnswer"
                     type="number"
                     defaultValue="15"
          />
          <Select margin="dense"
                  required
                  fullWidth
                  id="questionSet"
                  name="questionSet"
                  label="Question set"
                  defaultValue={questionSets != undefined && questionSets.length > 0 ? questionSets[0].id : undefined}
          >
            {questionSets.map((row) => (
              <MenuItem key={row.id} value={row.id}>{row.name}</MenuItem>
            ))}
          </Select>
          <FormControlLabel control={<Switch id="moderator" name="moderator" defaultValue="false"/>}
                            label="Moderator"/>
          <Button
            id="createGameSubmit"
            type="submit"
            fullWidth
            variant="contained"
            sx={{ml: 2, mr: 2, mb: 2}}
          >Create new game</Button>
        </FormGroup>
      </Box>
    </Dialog>
  )
}
export default GameCreationDialog
