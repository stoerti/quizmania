Feature('create_game');

Scenario('multiplayer_buzzer', ({I, loginPage, lobbyPage, gameRoomPage}) => {
  let gameName = "Test quiz " + Math.floor(Math.random() * 10000)
  let moderator = "Elrond"
  let spectator = "Boromir"
  let username1 = "Frodo"
  let username2 = "Gimli"
  let username3 = "Legolas"

  loginPage.logInWithUsername(moderator)
  I.wait(1)

  lobbyPage.createGame(gameName, "test2", true, true)

  session('spectator', () => {
    loginPage.logInWithUsername(spectator)
    lobbyPage.joinGameAsSpectator(gameName)
  });

  session('player1', () => {
    loginPage.logInWithUsername(username1)
    lobbyPage.joinGame(gameName)
  });

  session('player2', () => {
    loginPage.logInWithUsername(username2)
    lobbyPage.joinGame(gameName)
  });

  session('player3', () => {
    loginPage.logInWithUsername(username3)
    lobbyPage.joinGame(gameName)
  });

  I.waitForText(username1)
  I.waitForText(username2)
  I.waitForText(username3)

  I.wait(3)

  gameRoomPage.startGame()
  I.wait(2)
  gameRoomPage.startRound()

  I.wait(2)

  // -----------------------------
  // QUESTION 1
  // -----------------------------
  session('player1', () => {
    gameRoomPage.answerChoiceQuestion("Werder Bremen")
  })
  I.wait(1)
  session('player2', () => {
    gameRoomPage.answerChoiceQuestion("Werder Bremen")
  })
  I.wait(1)
  session('player3', () => {
    gameRoomPage.answerChoiceQuestion("Hamburger SV")
  })
  I.wait(3)

  gameRoomPage.nextQuestion()
  // -----------------------------
  // QUESTION 2
  // -----------------------------
  session('player1', () => {
    gameRoomPage.answerChoiceQuestion("Weitsprung")
  })
  I.wait(1)
  session('player2', () => {
    gameRoomPage.answerChoiceQuestion("Hammelsprung")
  })
  I.wait(1)
  session('player3', () => {
    gameRoomPage.answerChoiceQuestion("Hammelsprung")
  })
  I.wait(3)

  gameRoomPage.nextQuestion()
  I.wait(2)

  gameRoomPage.closeRound()
  I.wait(2)

  gameRoomPage.startRound()
  I.wait(2)

  // -----------------------------
  // QUESTION 3
  // -----------------------------

  session('player1', () => {
    gameRoomPage.buzz()
  })
  session('player2', () => {
    gameRoomPage.buzz()
  })
  session('player1', () => {
    gameRoomPage.wonBuzz()
  })
  session('player2', () => {
    gameRoomPage.lostBuzz()
  })

  I.waitForText(username1, 5, {id: 'buzzWinner'})

  I.wait(3)

  gameRoomPage.acceptBuzzerAnswer()
  gameRoomPage.nextQuestion()

  // -----------------------------
  // QUESTION 4
  // -----------------------------

  session('player2', () => {
    gameRoomPage.buzz()
  })
  session('player1', () => {
    gameRoomPage.buzz()
  })
  session('player3', () => {
    gameRoomPage.buzz()
  })

  I.waitForText(username2, 5, {id: 'buzzWinner'})
  gameRoomPage.rejectBuzzerAnswer()
  I.waitForText(username1, 5, {id: 'buzzWinner'})
  gameRoomPage.rejectBuzzerAnswer()
  I.waitForText(username3, 5, {id: 'buzzWinner'})
  gameRoomPage.acceptBuzzerAnswer()
  gameRoomPage.nextQuestion()

  I.wait(2000)

  gameRoomPage.closeRound()

  I.waitForText("Results")
  I.wait(1)
});
