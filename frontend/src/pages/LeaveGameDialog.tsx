import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  FormGroup,
  MenuItem,
  Select,
  Switch,
  TextField,
  useTheme
} from "@mui/material";
import React, {useEffect} from "react";
import {NewGameCommand} from "../services/GameCommandService";
import {QuestionSetDto} from "../services/QuestionSetServiceTypes";
import {questionSetService} from "../services/QuestionSetService";
import {useNavigate} from "react-router";

type LeaveGameDialogProps = {
  open: boolean
  onClose: () => void
  onLeaveGame: () => void
}

export const LeaveGameDialog = ({open, onClose, onLeaveGame}: LeaveGameDialogProps) => {
  const theme = useTheme()
  const handleClose = () => {
    onClose()
  }
  const handleCancel = () => {
    onClose();
  };

  const handleOk = () => {
    onLeaveGame()
  };

  return (
    <Dialog
      sx={{ width: '100%', maxHeight: 500 }}
      maxWidth="xs"
      open={open}
      onClose={handleClose}
    >
      <DialogTitle
        sx={{ backgroundColor: theme.palette.primary.main, color: theme.palette.primary.contrastText }}
      >Leave Game</DialogTitle>
      <DialogContent dividers>
        Do you really want to leave the current game?
      </DialogContent>
      <DialogActions>
        <Button autoFocus onClick={handleCancel} variant={"contained"}>Cancel</Button>
        <Button onClick={handleOk} variant={"outlined"}>Leave game</Button>
      </DialogActions>
    </Dialog>
  )
}
export default LeaveGameDialog
