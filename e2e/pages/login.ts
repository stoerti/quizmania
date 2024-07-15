const { I } = inject();

export function logInWithUsername(username: string) {
  I.amOnPage('/');
  I.wait(2); // wait for animation

  // enter username
  I.fillField('username', username);

  // click login
  I.click({id: 'submitLogin'});
}

export function logInAndJoin(username: string, gameName: string) {
  I.amOnPage('/');
  I.wait(2); // wait for animation

  // enter username
  I.fillField('username', username);

  // click login
  I.click({id: 'submitLogin'});
}
