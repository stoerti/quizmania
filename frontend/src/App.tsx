import React from 'react';

import {SnackbarProvider} from 'material-ui-snackbar-provider'
import {createTheme, CssBaseline, ThemeProvider} from "@mui/material";
import {RouterProvider} from "react-router";
import {router} from "./router.tsx";

const App = () => {
  const theme = createTheme({
    typography: {
      fontFamily: 'Klarheit Kurrent, Roboto, Arial'
    },
    palette: {
      mode: 'light',
      primary: {
        main: '#102924',
        contrastText: '#c8ffa5',
      },
      secondary: {
        main: '#c7cfeb',
      },
      text: {
        primary: '#102924',
      },
      error: {
        main: '#EB4B2D',
      },
      info: {
        main: '#C7CFEB',
      },
      success: {
        main: '#1EC85A',
      },
      warning: {
        main: '#ff9800',
      },
      divider: '#102924',
    },
    components: {
      MuiCssBaseline: {
        styleOverrides: `/* holi theme */
@font-face {
  font-family: 'Klarheit Kurrent';
  font-weight: 400;
  src: url(https://hap-cdn.holisticon.de/fonts/ESKlarheitKurrent-Rg.woff2) format("woff2");
}
@font-face {
  font-family: 'Klarheit Kurrent';
  font-weight: 500;
  src: url(https://hap-cdn.holisticon.de/fonts/ESKlarheitKurrent-Md.woff2) format("woff2");
}
@font-face {
  font-family: 'Klarheit Kurrent';
  font-weight: 600;
  src: url(https://hap-cdn.holisticon.de/fonts/ESKlarheitKurrent-Bd.woff2) format("woff2");
}
`
      },
      MuiButton: {
        styleOverrides: {
          root: {
            borderRadius: '20px'
          },
        },
      },
      MuiTable: {
        styleOverrides: {
          root: {
            backgroundColor: '#c8ffa5',
            borderRadius: '20px'
          }
        }
      },
      MuiTableCell: {
        styleOverrides: {
          root: {
            borderColor: '#102924',
          }
        }
      },
      MuiDialog: {
        styleOverrides: {
          paper: {
            backgroundColor: '#c8ffa5'
          }
        }
      }
    }
  })

  return (
    <div style={{backgroundColor: theme.palette.background.default, height: '100%'}}>
      <ThemeProvider theme={theme}>
        <CssBaseline/>
        <SnackbarProvider SnackbarProps={{autoHideDuration: 4000}}>
          <RouterProvider router={router}/>
        </SnackbarProvider>
      </ThemeProvider>
    </div>
  )
}

export default App;
