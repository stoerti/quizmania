const { I } = inject();

export function createGame(gameName: string, moderated: boolean = false) {
  I.click({id: 'createGame'});

  I.fillField({id: "gameName"}, gameName)
  I.fillField({id: "maxPlayers"}, 4)
  I.fillField({id: "numQuestions"}, 4)
  if (moderated) {
    I.click({id: "moderator"})
  }

  I.wait(1)

  I.click({id: "createGameSubmit"})

  I.waitForText("Participants")

  I.wait(1)
}

export function joinGame(gameName: string) {
  let joinGameButton = locate('Button').inside(locate('tr').withDescendant('div').withText(gameName))

  I.click(joinGameButton);
}

