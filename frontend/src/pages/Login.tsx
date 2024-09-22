import {Box, Button, CssBaseline, Grid, Paper, Stack, TextField, Typography, useTheme} from "@mui/material";
import React, {useEffect} from "react";
import {useUsername} from "../hooks/useUsername.ts";
import {useNavigate} from "react-router";

const LoginPage = () => {

  const {username, setUsername} = useUsername();
  const navigate = useNavigate();
  const theme = useTheme();

  useEffect(() => {
    if (username !== undefined) {
      console.log('already logged in as', username, 'continue to overview')
      navigate('/');
    }
  }, [username, navigate]);

  const loginSuccessAction = (username: string) => {
    setUsername(username);
    navigate('/');
  }

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const data = new FormData(event.currentTarget);

    const username: string = data.get('username')!.toString()

    console.log({
      username: username,
    });
    loginSuccessAction(username)
  };

  return (
    <Grid container component="main" sx={{height: '100vh', justifyContent: "center"}}>
      <CssBaseline/>
      <Paper elevation={8} sx={{
        maxWidth: 400,
        alignSelf: "center",
        backgroundColor: theme.palette.primary.contrastText,
        my: 4,
        mx: 4
      }}>
        <Stack spacing={5} component="form" noValidate onSubmit={handleSubmit} sx={{
          my: 4,
          mx: 4,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center'
        }}>
          <Typography variant={"h6"}>
            Welcome to Quizmania
          </Typography>
          <Typography variant={"body1"}>
            To play Quizmania, first choose a guest username. Then you can either create a new game or join an existing one.
          </Typography>
          <Typography variant={"body1"}>
            AxonIQ Conference special: Join the Game "AxonIQ Quiz" by clicking on the quiz name.
          </Typography>
          <TextField margin="normal"
                     required
                     fullWidth
                     id="username"
                     label="Username"
                     name="username"
                     autoComplete="username"
                     autoFocus
                     sx={{margin: 2}}
          />
          <Button
            id="submitLogin"
            type="submit"
            fullWidth
            variant="contained"
            sx={{ml: 2, mr: 2, mb: 2}}
          >Choose username</Button>
        </Stack>
      </Paper>
    </Grid>
  )
}

export default LoginPage
