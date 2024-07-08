Feature('create_game');

Scenario('Start single player game',  ({ I , loginPage, lobbyPage}) => {
    loginPage.logInWithUsername("Christian")
    I.wait(1)

    lobbyPage.createGame("Some Game 2", "test01")
    I.wait(5)

});
