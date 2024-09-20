import {RouteObject} from "react-router";
import LoginPage from "./pages/Login.tsx";
import GameSelectionPage from "./pages/GameSelectionPage.tsx";
import GamePage from "./pages/game/GamePage.tsx";
import LogoutPage from "./pages/LogoutPage.tsx";
import { createBrowserRouter } from "react-router-dom";

const routes: RouteObject[] = [
  {
    path: '/login',
    element: <LoginPage />
  },
  {
    path: '/',
    element: <GameSelectionPage />
  },
  {
    path: '/game/:gameId',
    element: <GamePage />
  },
  {
    path: '/logout',
    element: <LogoutPage />
  }
]

export const router = createBrowserRouter(routes);
