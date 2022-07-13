package com.example.quiz.service;

import com.example.quiz.QuizApplication;
import com.example.quiz.entity.Quiz;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = QuizApplication.class)
@Slf4j
@Transactional
class QuizServiceImplTest {

    /** Repository：注入 */
    @Autowired
    QuizService quizService;

    @Test
    @Sql("/test/sql/setval.sql")
    void selectOneRandomQuiz() {
        // 異常
        // テストデータ削除
        quizService.deleteQuizById(5);
        Optional<Quiz> quizOpt1 = quizService.selectOneRandomQuiz();
        assertFalse(quizOpt1.isPresent());

        // 正常
        // テストデータ挿入
        Quiz testData = new Quiz();
        testData.setId(null);
        testData.setQuestion("テスト");
        testData.setAnswer(false);
        testData.setAuthor("テスト太郎");
        quizService.insertQuiz(testData);

        Optional<Quiz> quizOpt2 = quizService.selectOneRandomQuiz();
        Quiz quiz = quizOpt2.get();
        assertThat(quiz.getId(), anyOf(is(5), is(6)));
        assertThat(quiz.getQuestion(), anyOf(is("クイズ１"), is("テスト")));
        assertThat(quiz.getAnswer(), anyOf(is(true), is(false)));
        assertThat(quiz.getAuthor(), anyOf(is("クイズ太郎"), is("テスト太郎")));
    }

    @Test
    void checkQuiz() {
        // 存在しないクイズ
        assertFalse(quizService.checkQuiz(99, true));

        // 存在するクイズ
        assertTrue(quizService.checkQuiz(5, true));
    }
}