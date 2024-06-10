const { I } = inject();

export function startGame() {
  I.click({id: 'startGame'});
}

export function nextQuestion() {
  I.click({id: 'nextQuestion'});
}

export function rateQuestion() {
  I.click({id: 'rateQuestion'});
}

export function closeQuestion() {
  I.click({id: 'closeQuestion'});
}

export function answerChoiceQuestion(answer: string) {
  I.click('button:has-text("'+answer+'")');
}

export function answerFreeQuestion(answer: string) {
  I.fillField({id: 'freeChoiceAnswer'}, answer);
  I.click({id: 'submitAnswer'});
}

export function answerEstimateQuestion(answer: number) {
  I.fillField({id: 'estimationAnswer'}, answer);
  I.click({id: 'submitAnswer'});
}

