package org.quizmania

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class QuizmaniaApplication

fun main(args: Array<String>) {
    runApplication<QuizmaniaApplication>(*args)
}
