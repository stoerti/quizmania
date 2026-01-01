import {AppBar, Box, IconButton, Table, TableBody, TableCell, TableHead, TableRow, Toolbar, Tooltip, Typography} from "@mui/material";
import Add from "@mui/icons-material/Add"
import Login from "@mui/icons-material/Login"
import Refresh from "@mui/icons-material/Refresh"
import React, {useCallback, useEffect} from "react";
import {GameCreationDialog} from "./GameCreationDialog";
import {useSnackbar} from "notistack";
import {GameAlreadyFullException, gameCommandService, GameException, NewGameCommand, UsernameAlreadyTakenException} from "../services/GameCommandService";
import {TransferWithinAStation, Visibility} from "@mui/icons-material";
import {GameDto, gameOverviewService, GameStatus} from "../services/GameOverviewService";
import {useNavigate} from "react-router";
import {useUsername} from "../hooks/useUsername";

type GameSelectionContainerProps = {
  games: GameDto[]
  onButtonClickJoinGame: (gameId: string) => void
  onButtonClickJoinGameAsSpectator: (gameId: string) => void
}

const GameSelectionContainer = (props: GameSelectionContainerProps) => {

  return (
    <Box
      sx={{
        display: "flex",
        justifyContent: "center",
        margin: 2
      }}>
      <Table sx={{maxWidth: 650, justifySelf: 'center'}}>
        <TableHead>
          <TableRow>
            <TableCell>Name</TableCell>
            <TableCell align="right">Players</TableCell>
            <TableCell></TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {props.games.map((row) => {
            const joinGameAction = () => props.onButtonClickJoinGame(row.id)

            return (
              <TableRow
                key={row.id}
                sx={{'&:last-child td, &:last-child th': {border: 0}}}
              >
                <TableCell component="th" scope="row" onClick={joinGameAction}>
                  <Typography
                    variant="body1"
                    component="div"
                  >
                    {row.name}
                  </Typography>
                  <Typography
                    variant="caption"
                    id="tableTitle"
                    component="div"
                  >
                    Creator: {row.creator}
                  </Typography>
                </TableCell>
                <TableCell align="right" onClick={joinGameAction}>{row.players.length}/{row.maxPlayers}</TableCell>
                <TableCell align="right">
                  {row.status == GameStatus.CREATED ? <IconButton name="join" onClick={() => props.onButtonClickJoinGame(row.id)}>
                    <Login/>
                  </IconButton> : null}
                  <IconButton name="spectator" onClick={() => props.onButtonClickJoinGameAsSpectator(row.id)}>
                    <Visibility/>
                  </IconButton>
                </TableCell>
              </TableRow>
            )
          })}
        </TableBody>
      </Table>
    </Box>
  )
}
const GameSelectionPage = () => {

  const navigate = useNavigate()
  const {username} = useUsername();

  const [newGameDialogOpen, setNewGameDialogOpen] = React.useState(false)
  const [games, setGames] = React.useState<GameDto[]>([])

  const {enqueueSnackbar} = useSnackbar()

  useEffect(() => {
    if (username === undefined) {
      console.log('no username set, redirect to login');
      navigate('/login');
    }
  }, [username, navigate]);

  useEffect(() => {
    gameOverviewService.searchOpenGames(setGames)
  }, []);

  const onGameSelected = useCallback((gameId: string) => {
    navigate(`/game/${gameId}`)
  }, [navigate])

  const onLogout = () => {
    navigate('/logout');
  }

  const onButtonClickNewGame = () => {
    setNewGameDialogOpen(true)
  }

  const onButtonClickReload = () => {
    gameOverviewService.searchOpenGames(setGames)
  }

  const onButtonClickJoinGame = async (gameId: string) => {
    try {
      await gameCommandService.joinGame(gameId)
      onGameSelected(gameId)
    } catch (error) {
      if (error instanceof GameAlreadyFullException) {
        enqueueSnackbar("The selected game is already full")
      } else if (error instanceof UsernameAlreadyTakenException) {
        enqueueSnackbar("The username is already taken in the selected game")
      } else {
        enqueueSnackbar("Something went wrong")
      }
    }
  }
  const onButtonClickJoinGameAsSpectator = async (gameId: string) => {
    onGameSelected(gameId)
  }

  const onCloseNewGameDialog = () => {
    setNewGameDialogOpen(false)
  }

  const onCreateNewGame = async (newGame: NewGameCommand) => {
    try {
      const gameId = await gameCommandService.createNewGame(newGame)
      onGameSelected(gameId)
      enqueueSnackbar(
        `Created game ${newGame.name} ${newGame.withModerator ? 'with' : 'without'} moderator`
      )
      setNewGameDialogOpen(false)
    } catch (error) {
      if (error instanceof GameException) {
        enqueueSnackbar(error.message)
      } else {
        enqueueSnackbar("Something went wrong: " + error)
      }
    }
  }

  return (
    <div>
      <GameCreationDialog open={newGameDialogOpen}
                          onClose={onCloseNewGameDialog}
                          onCreateGame={onCreateNewGame}/>
      <AppBar position="static">
        <Toolbar>
          <Typography
            sx={{flex: '1 1 100%'}}
            variant="h6"
            id="tableTitle"
            component="div"
          >
            Games
          </Typography>
          <Tooltip title="Change username">
            <IconButton id="changeUsername" color="inherit" onClick={onLogout}>
              <TransferWithinAStation/>
            </IconButton>
          </Tooltip>
          <Tooltip title="Create new game">
            <IconButton id="createGame" color="inherit" onClick={onButtonClickNewGame}>
              <Add/>
            </IconButton>
          </Tooltip>
          <Tooltip title="Reload">
            <IconButton id="reload" color="inherit" onClick={onButtonClickReload}>
              <Refresh/>
            </IconButton>
          </Tooltip>
        </Toolbar>
      </AppBar>
      <GameSelectionContainer games={games}
                              onButtonClickJoinGame={onButtonClickJoinGame}
                              onButtonClickJoinGameAsSpectator={onButtonClickJoinGameAsSpectator}/>
    </div>
  )
}

export default GameSelectionPage
