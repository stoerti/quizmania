import {gameCommandService, GameException} from "../../services/GameCommandService";
import {
  AppBar,
  Button,
  IconButton,
  List,
  ListItem,
  ListItemIcon,
  ListItemText, Stack,
  Toolbar,
  Tooltip,
  Typography
} from "@mui/material";
import PlayArrow from "@mui/icons-material/PlayArrow";
import Logout from "@mui/icons-material/Logout";
import Numbers from "@mui/icons-material/Numbers";
import RecordVoiceOver from "@mui/icons-material/RecordVoiceOver";
import Person from "@mui/icons-material/Person";
import React from "react";
import {Game} from "../../domain/GameModel";
import {useSnackbar} from "notistack";
import {useUsername} from "../../hooks/useUsername.ts";
import LeaveGameDialog from "../LeaveGameDialog.tsx";

export type GameLobbyPageProps = {
  game: Game,
}

export const GameLobbyPage = (props: GameLobbyPageProps) => {
  const {enqueueSnackbar} = useSnackbar()
  const {username} = useUsername()
  const [leaveGameDialogOpen, setLeaveGameDialogOpen] = React.useState(false)

  const onConfirmLeaveGame = async () => {
    try {
      await gameCommandService.leaveGame(props.game.id)
    } catch (error) {
      if (error instanceof GameException) {
        enqueueSnackbar(error.message)
      }
    }
  }

  const onClickStartGame = async () => {
    try {
      await gameCommandService.startGame(props.game.id)
    } catch (error) {
      if (error instanceof GameException) {
        enqueueSnackbar(error.message)
      }
    }
  }

  let startButton;
  if (props.game.moderator === username || props.game.creator === username) {
    startButton = <div
      style={{
        display: "flex",
        alignItems: "center"
      }}
    >
      <Button id="startGame" sx={{margin: 'auto'}} startIcon={<PlayArrow/>} variant="contained" onClick={onClickStartGame}>Start
        game</Button>
    </div>
  }

  return (
    <div>
      <LeaveGameDialog
        open={leaveGameDialogOpen}
        onClose={() => setLeaveGameDialogOpen(false)}
        onLeaveGame={onConfirmLeaveGame}
      />
      <AppBar position="static">
        <Toolbar>
          <Typography
            sx={{flex: '1 1 100%'}}
            variant="h6"
            component="div"
          >
            {props.game.name}
          </Typography>
          <Tooltip title="Leave game">
            <IconButton color="inherit" onClick={() => setLeaveGameDialogOpen(true)}>
              <Logout/>
            </IconButton>
          </Tooltip>
        </Toolbar>
      </AppBar>
      <Stack spacing={2}>
        <List dense={false}>
          <ListItem key="numberOfQuestions">
            <ListItemIcon>
              <Numbers/>
            </ListItemIcon>
            <ListItemText
              primary={props.game.totalQuestions}
              secondary='number of questions'
            />
          </ListItem>
          <ListItem key="gameMaster">
            <ListItemIcon>
              <RecordVoiceOver/>
            </ListItemIcon>
            <ListItemText
              primary={props.game.moderator !== undefined && props.game.moderator != null ? props.game.moderator : 'Computer'}
              secondary='game master'
            />
          </ListItem>
        </List>
        {startButton}
        <Typography variant="h6" component="div" sx={{pl: 2}}>
          Participants ({props.game.players.length})
        </Typography>
        <List dense={true} data-testid="player-list">
          {props.game.players.map((row) => (
            <ListItem key={row.id} data-testid={`player-${row.id}`}>
              <ListItemIcon>
                <Person/>
              </ListItemIcon>
              <ListItemText
                primary={row.name}
              />
            </ListItem>
          ))}
        </List>
      </Stack>


    </div>
  )
}
