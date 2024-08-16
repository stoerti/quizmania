const { I } = inject();

export function createGame(gameName: string, questionSet: string, moderated: boolean = false, useBuzzer: boolean = false) {
  I.click({id: 'createGame'});

  I.fillField({id: "gameName"}, gameName)
  I.fillField({id: "maxPlayers"}, 50)
  I.fillField({id: "numQuestions"}, 3)
  I.fillField({id: "numSecondsToAnswer"}, 5)

  I.click({id: 'questionSet'})
  I.click({xpath: '//ul//li[@data-value="'+questionSet+'"]'})

  if (moderated) {
    I.click({id: "moderator"})
  }

  if (useBuzzer) {
    I.click({id: "useBuzzer"})
  }

  I.wait(1)

  I.click({id: "createGameSubmit"})

  I.waitForText("Participants")

  I.wait(1)
}

export function joinGame(gameName: string) {
  I.waitForText(gameName)
  let joinGameButton = locate('Button').inside(locate('tr').withDescendant('div').withText(gameName))

  I.click(joinGameButton);
  I.waitForText("Participants")
}

