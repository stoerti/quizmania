Feature('create_game');

Scenario('multiplayer', ({I, loginPage, lobbyPage, gameRoomPage}) => {
  let gameName = "Game " + Math.floor(Math.random() * 100000)
  let username1 = "User 1" + Math.floor(Math.random() * 100000)
  let username2 = "User 2" + Math.floor(Math.random() * 100000)
  let username3 = "User 3" + Math.floor(Math.random() * 100000)
  let username4 = "User 4" + Math.floor(Math.random() * 100000)

  loginPage.logInWithUsername(username1)
  I.wait(1)

  lobbyPage.createGame(gameName, "werkstatt")
  I.wait(1)

  session('player2', () => {
    loginPage.logInWithUsername(username2)
    lobbyPage.joinGame(gameName)
  });

  session('player3', () => {
    loginPage.logInWithUsername(username3)
    lobbyPage.joinGame(gameName)
  });

  I.waitForText(username2)
  I.waitForText(username3)

});

Scenario('multiplayer_moderated', ({I, loginPage, lobbyPage, gameRoomPage}) => {
  let gameName = "Game " + Math.floor(Math.random() * 100000)
  let moderator = "Moderator " + Math.floor(Math.random() * 100000)
  let username1 = "User " + Math.floor(Math.random() * 100000)
  let username2 = "User " + Math.floor(Math.random() * 100000)
  let username3 = "User " + Math.floor(Math.random() * 100000)

  loginPage.logInWithUsername(moderator)
  I.wait(1)

  lobbyPage.createGame(gameName, "werkstatt", true)
  I.wait(1)

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

  I.wait(5000)

  gameRoomPage.startGame()

  I.wait(1000)
  session('player1', () => {
    gameRoomPage.answerChoiceQuestion("Banone")
  })

  I.wait(1)
  session('player2', () => {
    gameRoomPage.answerChoiceQuestion("Banone")
  })
  I.wait(1)
  session('player3', () => {
    gameRoomPage.answerChoiceQuestion("Gürkin")
  })

  I.waitForText("Banone", 5, "tr:has-text('"+username1+"')")
  I.waitForText("Banone", 5, "tr:has-text('"+username2+"')")
  I.waitForText("Gürkin", 5, "tr:has-text('"+username3+"')")

  I.wait(3)

  gameRoomPage.nextQuestion()

  I.wait(1)
  session('player1', () => {
    gameRoomPage.answerEstimateQuestion(800)
  })
  I.wait(1)
  session('player2', () => {
    gameRoomPage.answerEstimateQuestion(900)
  })
  I.wait(1)
  session('player3', () => {
    gameRoomPage.answerEstimateQuestion(1000)
  })

  I.waitForText("800", 5, "tr:has-text('"+username1+"')")
  I.waitForText("900", 5, "tr:has-text('"+username2+"')")
  I.waitForText("1000", 5, "tr:has-text('"+username3+"')")

  I.wait(10000)
  gameRoomPage.nextQuestion()

  session('player1', () => {
    gameRoomPage.answerFreeQuestion("Neil armstrong")
  })
  session('player2', () => {
    gameRoomPage.answerFreeQuestion("NielArmstrong")
  })
  I.wait(6)

  I.waitForText("Neil armstrong", 5, "tr:has-text('"+username1+"')")
  I.waitForText("NielArmstrong", 5, "tr:has-text('"+username2+"')")
  I.dontSeeElement("tr:has-text('"+username3+"')")


  gameRoomPage.rateQuestion()
  gameRoomPage.nextQuestion()

  I.waitForText("Results")

  I.wait(10)
});



Scenario('many_multiplayer_moderated', ({I, loginPage, lobbyPage, gameRoomPage}) => {
  const gameName = "Game " + Math.floor(Math.random() * 100000)
  const numPlayers = 50

  const moderator = "Moderator " + Math.floor(Math.random() * 100000)
  const players = [...Array(numPlayers)].map((_, index) => { return "User "+index + Math.floor(Math.random() * 100000)})


  loginPage.logInWithUsername(moderator)
  I.wait(1)

  lobbyPage.createGame(gameName, "test01", true)
  I.wait(1)

  players.forEach((player, index) => {
    session('player'+index, () => {
      loginPage.logInWithUsername(player)
      lobbyPage.joinGame(gameName)
    });
  })

  players.forEach((player) => {
    I.waitForText(player)
  })

  I.wait(5)

  gameRoomPage.startGame()

  I.wait(1)

  players.forEach((player, index) => {
    session('player'+index, () => {
      gameRoomPage.answerChoiceQuestion(Math.random() < 0.5 ? "Banone" : "Gürkin")
    })
  })

});
