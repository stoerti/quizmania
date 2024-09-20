import Cookies from "js-cookie";

const username = () => {
  return Cookies.get('username')
}

const setUsername = (username: string) => {
  Cookies.set('username', username)
}

const removeUsername = () => {
  Cookies.remove('username');
}

export const useUsername = () => {
  return {
    username: username(),
    setUsername,
    removeUsername
  }
};
