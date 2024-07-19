import {Box, Button, CssBaseline, Grid, Paper, TextField} from "@mui/material";
import React from "react";


type LoginPageProps = {
    loginSuccessAction: (username: string) => any
}
const LoginPage = (props: LoginPageProps) => {
    const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        const data = new FormData(event.currentTarget);

        const username: string = data.get('username')!.toString()

        console.log({
            username: username,
        });
        props.loginSuccessAction(username)
    };

    return (
            <Grid container component="main" sx={{height: '100vh', justifyContent: "center"}}>
                <CssBaseline/>
                <Paper elevation={8} sx={{
                    maxWidth: 400,
                    alignSelf: "center"
                }}>
                    <Box component="form" noValidate onSubmit={handleSubmit} sx={{
                        my: 4,
                        mx: 4,
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                    }}>
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
                        >Sign In</Button>
                    </Box>
                </Paper>
            </Grid>
    )
}

export default LoginPage