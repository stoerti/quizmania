import {QuestionSetDto} from "./QuestionSetServiceTypes";

export class QuestionSetService {

    public searchQuestionSets(responseHandler: (questionSets: QuestionSetDto[]) => void, errorHandler: (err: any) => void = () => {
    }) {
        fetch('/api/questionset/', {
            method: 'GET',
            headers: {
                'Content-type': 'application/json; charset=UTF-8',
            },
        })
            .then((response) => response.json())
            .then(responseHandler)
            .catch((err) => {
                if (errorHandler !== undefined)
                    errorHandler(err);
            });
    }
}
