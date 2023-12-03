import {Client} from "@stomp/stompjs";
import {
    GameCanceledEvent, GameDto,
    GameEndedEvent,
    GameStartedEvent, NewAnswerDto, NewGameDto, QuestionAnsweredEvent, QuestionAskedEvent, QuestionClosedEvent,
    UserAddedEvent,
    UserRemovedEvent
} from "./GameServiceTypes";
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
