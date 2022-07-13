package com.example.quiz.service;

import com.example.quiz.entity.Quiz;
import com.example.quiz.repository.QuizRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class QuizServiceImpl implements QuizService {

    /** Repository：注入 */
    @Autowired
    QuizRepository quizRepository;

    @Override
    public Iterable<Quiz> selectAll() {
        return quizRepository.findAll();
    }

    @Override
    public Optional<Quiz> selectOneById(Integer id) {
        return quizRepository.findById(id);
    }

    @Override
    public Optional<Quiz> selectOneRandomQuiz() {
        // 全件取得
        List<Quiz> quizList = new ArrayList<>();
        Iterable<Quiz> quizzes = quizRepository.findAll();

        if(!quizzes.iterator().hasNext()) {
            return Optional.empty();
        }
        quizzes.forEach(quizList::add);

        // 疑似乱数生成
        int pseudoRandomNum = new Random().ints(1, 0, quizList.size()).sum();

        return  Optional.of(quizList.get(pseudoRandomNum));
    }

    @Override
    public Boolean checkQuiz(Integer id, Boolean myAnswer) {
        // 正解／不正解判定用変数
        Boolean check = false;

        // 対象のクイズを取得
        Optional<Quiz> optQuiz = quizRepository.findById(id);

        // 値存在チェック
        if (optQuiz.isPresent()) {
            Quiz quiz = optQuiz.get();
            // クイズの解答チェック
            if (quiz.getAnswer().equals(myAnswer)) {
                check = true;
            }
        }

        return check;
    }

    @Override
    public void insertQuiz(Quiz quiz) {
        quizRepository.save(quiz);
    }

    @Override
    public void updateQuiz(Quiz quiz) {
        quizRepository.save(quiz);
    }

    @Override
    public void deleteQuizById(Integer id) {
        quizRepository.deleteById(id);
    }
}