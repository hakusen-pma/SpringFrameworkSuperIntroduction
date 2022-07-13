package com.example.quiz.controller;

import com.example.quiz.entity.Quiz;
import com.example.quiz.form.QuizForm;
import com.example.quiz.service.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;
import java.util.Optional;

/** Quizコントローラ */
@Controller
@RequestMapping("/quiz")
public class QuizController {
    /** DI対象 */
    @Autowired
    QuizService quizService;
    /** メッセージ管理 */
    @Autowired
    private MessageSource messageSource;

    /** 「form-backing bean」の初期化 */
    @ModelAttribute
    public QuizForm setUpForm() {
        QuizForm form = new QuizForm();
        // ラジオボタンのデフォルト値設定
        form.setAnswer(true);
        return form;
    }

    /** クイズの一覧を表示 */
    @GetMapping
    public String showList(QuizForm quizForm, Model model) {
        // 新規登録設定
        quizForm.setNewQuiz(true);
        // 一覧を取得
        Iterable<Quiz> list = quizService.selectAll();
        // 表示用「Model」への格納
        model.addAttribute("list", list);
        model.addAttribute("title", messageSource.getMessage("title.input", new String[] {}, Locale.getDefault()));
        model.addAttribute("noListMsg", messageSource.getMessage("no.list.msg", new String[] {}, Locale.getDefault()));
        return "crud";
    }

    /** Quizデータを1件挿入 */
    @PostMapping("/insert")
    public String insert(@Validated QuizForm quizForm, BindingResult bindingResult,
                         Model model, RedirectAttributes redirectAttributes) {
        // FormからEntityへの詰め替え
        Quiz quiz = new Quiz();
        quiz.setQuestion(quizForm.getQuestion());
        quiz.setAnswer(quizForm.getAnswer());
        quiz.setAuthor(quizForm.getAuthor());

        // 入力チェック
        if (!bindingResult.hasErrors()) {
            quizService.insertQuiz(quiz);
            redirectAttributes.addFlashAttribute("complete", messageSource.getMessage("complete.input.msg", new String[] {}, Locale.getDefault()));
            return "redirect:/quiz";
        } else {
            // エラーがある場合は一覧表示処理を呼び出す。
            return showList(quizForm, model);
        }
    }

    /** クイズデータを1件取得し、フォーム内に表示する */
    @GetMapping("/{id}")
    public String showUpdate(QuizForm quizForm, @PathVariable Integer id, Model model) {
        // Quizを取得（Optionalでラップ）
        Optional<Quiz> quizOpt = quizService.selectOneById(id);
        // QuizFormへの詰め直し
        Optional<QuizForm> quizFormOpt = quizOpt.map(t -> makeQuizForm(t));
        // QuizFormがnullでなければ中身を取り出す
        if (quizFormOpt.isPresent()) {
            quizForm = quizFormOpt.get();
        }
        // 更新用のModelを作成する
        makeUpdateModel(quizForm, model);

        return "crud";
    }

    /** 更新用のModelを作成する */
    private void makeUpdateModel(QuizForm quizForm, Model model) {
        quizForm.setNewQuiz(false);
        model.addAttribute("quizForm", quizForm);
        model.addAttribute("title", messageSource.getMessage("title.update", new String[] {}, Locale.getDefault()));
    }

    /** idをキーにしてデータを更新する */
    @PostMapping("/update")
    public String update(@Validated QuizForm quizForm, BindingResult bindingResult,
                         Model model, RedirectAttributes redirectAttributes) {
        // QuizフォームからQuizに詰め直す
        Quiz quiz = makeQuiz(quizForm);
        // 入力チェック
        if (!bindingResult.hasErrors()) {
            // 更新処理、フラッシュスコープの使用、リダイレクト（個々の編集ページ）
            quizService.updateQuiz(quiz);
            redirectAttributes.addFlashAttribute("complete", messageSource.getMessage("complete.update.msg", new String[] {}, Locale.getDefault()));
            // 更新画面を表示する
            return "redirect:/quiz/" + quiz.getId();
        } else {
            // 更新用のModelを作成する
            makeUpdateModel(quizForm, model);

            return "crud";
        }
    }

    /** QuizFormからQuizに詰め直す */
    private Quiz makeQuiz(QuizForm quizForm) {
        Quiz quiz = new Quiz();
        quiz.setId(quizForm.getId());
        quiz.setQuestion(quizForm.getQuestion());
        quiz.setAnswer(quizForm.getAnswer());
        quiz.setAuthor(quizForm.getAuthor());

        return quiz;
    }

    /** QuizからQuizFormに詰め直す */
    private QuizForm makeQuizForm(Quiz quiz) {
        QuizForm quizForm = new QuizForm();
        quizForm.setId(quiz.getId());
        quizForm.setQuestion(quiz.getQuestion());
        quizForm.setAnswer(quiz.getAnswer());
        quizForm.setAuthor(quiz.getAuthor());

        return quizForm;
    }

    /** idをkeyにしてデータを削除する */
    @PostMapping("/delete")
    public String delete(@RequestParam("id") String id, Model model,
                         RedirectAttributes redirectAttributes) {
        // クイズを1件削除してリダイレクト
        quizService.deleteQuizById(Integer.parseInt(id));
        redirectAttributes.addFlashAttribute("delcomplete", messageSource.getMessage("complete.delete.msg", new String[] {}, Locale.getDefault()));

        return "redirect:/quiz";
    }

    /** Quizデータをランダムで1件取得し、画面に表示する */
    @GetMapping("/play")
    public String showQuiz(QuizForm quizForm, Model model) {
        // Quizを取得（Optionalでラップ）
        Optional<Quiz> quizOpt = quizService.selectOneRandomQuiz();
        // 値があるか判定
        if (quizOpt.isPresent()) {
            // QuizFormへ詰め直し
            Optional<QuizForm> quizFormOpt = quizOpt.map(t -> makeQuizForm(t));
            quizForm = quizFormOpt.get();
        } else {
            model.addAttribute("msg", messageSource.getMessage("no.quiz.msg", new String[] {}, Locale.getDefault()));
            return "play";
        }
        // 表示用「Model」へ格納
        model.addAttribute("quizForm", quizForm);

        return "play";
    }

    /** クイズの正解／不正解を判定 */
    @PostMapping("/check")
    public String checkQuiz(QuizForm quizForm, @RequestParam Boolean answer, Model model) {
        if (quizService.checkQuiz(quizForm.getId(), answer)) {
            model.addAttribute("msg", messageSource.getMessage("correct.msg", new String[] {}, Locale.getDefault()));
        } else {
            model.addAttribute("msg", messageSource.getMessage("incorrect.msg", new String[] {}, Locale.getDefault()));
        }

        return "answer";
    }
}