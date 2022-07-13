package com.example.quiz.controller;

import com.example.quiz.QuizApplication;
import com.example.quiz.entity.Quiz;
import com.example.quiz.form.QuizForm;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@SpringBootTest(classes = QuizApplication.class)
@Slf4j
@Transactional
class QuizControllerTest {

    @Autowired
    QuizController quizController;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Validator validator;
    /** 検証結果を設定するBindingResult */
    private BindingResult bindingResult;

    /** 各テストメソッドを実行する前に実行する処理 */
    @BeforeEach
    public void setUp() {
        bindingResult = new BindException(new QuizForm(), "quizForm");
    }

    @Test
    void setUpForm() throws Exception {
        // quizFormの初期値チェック
        MvcResult result = this.mockMvc.perform(get("/quiz")).andReturn();
        QuizForm resultForm = (QuizForm) result.getModelAndView().getModel().get("quizForm");
        assertEquals(resultForm.getAnswer() ,true);
    }

    @Test
    void showList() throws Exception {
        // andDo(print())でリクエスト・レスポンスを表示
        this.mockMvc.perform(get("/quiz")).andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("crud"))
                // この組み合わせのデータが含まれているか
                .andExpect(model().attribute("list",
                    hasItem(allOf(hasProperty("id", is(5))
                                    ,hasProperty("question", is("クイズ１"))
                                    ,hasProperty("answer", is(true))
                                    ,hasProperty("author", is("クイズ太郎"))
                    ))
                ))
                .andExpect(model().attribute("title", "登録用フォーム"))
                .andExpect(model().attribute("noListMsg", "登録されているクイズはありません。"));

        // quizFormの初期値チェック
        MvcResult result = this.mockMvc.perform(get("/quiz")).andReturn();
        QuizForm resultForm = (QuizForm) result.getModelAndView().getModel().get("quizForm");
        assertEquals(resultForm.getNewQuiz(),true);
    }

    @Test
    void insert() throws Exception {
        // 異常
        MultiValueMap<String, String> params1 = new LinkedMultiValueMap<>();
        params1.add("id", "99");
        params1.add("question", "");
        params1.add("answer", "false");
        params1.add("author", "");
        MvcResult result = this.mockMvc.perform(post("/quiz/insert").params(params1)).andDo(print())
                                    .andExpect(view().name("crud"))
                                    // この組み合わせのデータが含まれているか
                                    .andExpect(model().attribute("list",
                                            hasItem(allOf(hasProperty("id", is(5))
                                                    ,hasProperty("question", is("クイズ１"))
                                                    ,hasProperty("answer", is(true))
                                                    ,hasProperty("author", is("クイズ太郎"))
                                            ))
                                    ))
                                    .andExpect(model().attribute("title", "登録用フォーム"))
                                    .andExpect(model().attribute("noListMsg", "登録されているクイズはありません。")).andReturn();
        QuizForm resultForm = (QuizForm) result.getModelAndView().getModel().get("quizForm");
        validator.validate(resultForm, this.bindingResult);

        assertEquals(resultForm.getNewQuiz(),true);
        assertTrue(this.bindingResult.hasErrors());
        assertEquals(2, this.bindingResult.getFieldErrors().size());
        assertEquals("{0}が未入力です。",
                this.bindingResult.getFieldErrors("question").get(0).getDefaultMessage());
        assertEquals("{0}が未入力です。",
                this.bindingResult.getFieldErrors("author").get(0).getDefaultMessage());

        // 正常
        MultiValueMap<String, String> params2 = new LinkedMultiValueMap<>();
        params2.add("id", "99");
        params2.add("question", "テスト");
        params2.add("answer", "false");
        params2.add("author", "テスト太郎");
        params2.add("newQuiz", "true");

        this.mockMvc.perform(post("/quiz/insert").params(params2)).andDo(print())
                .andExpect(status().isFound())
                .andExpect(view().name("redirect:/quiz"))
                .andExpect(redirectedUrl("/quiz"))
                .andExpect(flash().attribute("complete", "登録が完了しました。"));
    }

    @Test
    void showUpdate() throws Exception {
        // データが取得できた場合
        this.mockMvc.perform(get("/quiz/5")).andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("crud"))
                .andExpect(request().attribute("id", is(5)));

        MvcResult result1 = this.mockMvc.perform(get("/quiz/5")).andReturn();
        QuizForm resultForm1 = (QuizForm) result1.getModelAndView().getModel().get("quizForm");
        assertEquals(resultForm1.getId(),5);
        assertEquals(resultForm1.getQuestion(),"クイズ１");
        assertEquals(resultForm1.getAnswer(),true);
        assertEquals(resultForm1.getAuthor(),"クイズ太郎");

        // データが取得できなかった場合（リクエストのformを返却）
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("id", "50");
        params.add("question", "あ");
        params.add("answer", "false");
        params.add("author", "い");

        MvcResult result2 = this.mockMvc.perform(get("/quiz/50").params(params)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("crud"))
                .andExpect(request().attribute("id", is(50))).andReturn();

        QuizForm resultForm2 = (QuizForm) result2.getModelAndView().getModel().get("quizForm");
        assertEquals(resultForm2.getId(),50);
        assertEquals(resultForm2.getQuestion(),"あ");
        assertEquals(resultForm2.getAnswer(),false);
        assertEquals(resultForm2.getAuthor(),"い");
    }

    @Test
    void makeUpdateModel() throws Exception {
        MvcResult result = this.mockMvc.perform(get("/quiz/5")).andReturn();

        QuizForm resultForm = (QuizForm) result.getModelAndView().getModel().get("quizForm");
        assertEquals(resultForm.getId(),5);
        assertEquals(resultForm.getQuestion(),"クイズ１");
        assertEquals(resultForm.getAnswer(),true);
        assertEquals(resultForm.getAuthor(),"クイズ太郎");
        assertEquals(resultForm.getNewQuiz(),false);

        String resultTitle = (String) result.getModelAndView().getModel().get("title");
        assertEquals(resultTitle, "更新用フォーム");
    }

    @Test
    void update() throws Exception {
        // 異常
        MultiValueMap<String, String> params1 = new LinkedMultiValueMap<>();
        params1.add("id", "99");
        params1.add("question", "");
        params1.add("answer", "false");
        params1.add("author", "");

        MvcResult result1 = this.mockMvc.perform(post("/quiz/update").params(params1)).andDo(print())
                .andExpect(view().name("crud")).andReturn();

        QuizForm resultForm1 = (QuizForm) result1.getModelAndView().getModel().get("quizForm");
        validator.validate(resultForm1, this.bindingResult);
        assertTrue(this.bindingResult.hasErrors());
        assertEquals(2, this.bindingResult.getFieldErrors().size());
        assertEquals("{0}が未入力です。",
                this.bindingResult.getFieldErrors("question").get(0).getDefaultMessage());
        assertEquals("{0}が未入力です。",
                this.bindingResult.getFieldErrors("author").get(0).getDefaultMessage());
        assertEquals(resultForm1.getId(),99);
        assertEquals(resultForm1.getQuestion(),"");
        assertEquals(resultForm1.getAnswer(),false);
        assertEquals(resultForm1.getAuthor(),"");

        // 正常
        MultiValueMap<String, String> params2 = new LinkedMultiValueMap<>();
        params2.add("id", "5");
        params2.add("question", "クイズ１更新");
        params2.add("answer", "false");
        params2.add("author", "クイズ太郎更新");

        this.mockMvc.perform(post("/quiz/update").params(params2)).andDo(print())
                .andExpect(status().isFound())
                .andExpect(view().name("redirect:/quiz/" + params2.get("id").get(0)))
                .andExpect(redirectedUrl("/quiz/" + params2.get("id").get(0)))
                .andExpect(flash().attribute("complete", "更新が完了しました。"));

        MvcResult result2 = this.mockMvc.perform(get("/quiz/" + params2.get("id").get(0))).andDo(print()).andReturn();
        QuizForm resultForm2 = (QuizForm) result2.getModelAndView().getModel().get("quizForm");
        assertEquals(resultForm2.getId(),5);
        assertEquals(resultForm2.getQuestion(),"クイズ１更新");
        assertEquals(resultForm2.getAnswer(),false);
        assertEquals(resultForm2.getAuthor(),"クイズ太郎更新");
    }

    @Test
    void makeQuiz() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // テスト対象メソッドの情報を取得する
        Method method = QuizController.class.getDeclaredMethod("makeQuiz", QuizForm.class);
        // メソッドのアクセス制限を解除
        method.setAccessible(true);
        QuizForm quizForm = new QuizForm(100, "hoge", true, "huga", null);
        // メソッド呼び出し
        Quiz quiz = (Quiz) method.invoke(quizController, quizForm);
        // 結果をアサーション
        assertEquals(quiz.getId(),100);
        assertEquals(quiz.getQuestion(),"hoge");
        assertEquals(quiz.getAnswer(),true);
        assertEquals(quiz.getAuthor(),"huga");
    }

    @Test
    void makeQuizForm() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // テスト対象メソッドの情報を取得する
        Method method = QuizController.class.getDeclaredMethod("makeQuizForm", Quiz.class);
        // メソッドのアクセス制限を解除
        method.setAccessible(true);
        Quiz quiz = new Quiz(100, "hoge", true, "huga");
        // メソッド呼び出し
        QuizForm quizForm = (QuizForm) method.invoke(quizController, quiz);
        // 結果をアサーション
        assertEquals(quizForm.getId(),100);
        assertEquals(quizForm.getQuestion(),"hoge");
        assertEquals(quizForm.getAnswer(),true);
        assertEquals(quizForm.getAuthor(),"huga");
        assertEquals(quizForm.getNewQuiz(),null);
    }

    @Test
    void delete() throws Exception {
        this.mockMvc.perform(post("/quiz/delete").param("id", "5")).andDo(print())
                .andExpect(status().isFound())
                .andExpect(view().name("redirect:/quiz"))
                .andExpect(redirectedUrl("/quiz"))
                .andExpect(flash().attribute("delcomplete", "削除が完了しました。"));

        MvcResult result = this.mockMvc.perform(get("/quiz")).andDo(print()).andReturn();
        QuizForm resultForm = (QuizForm) result.getModelAndView().getModel().get("quizForm");
        assertNotEquals(resultForm.getId(), 5);
        assertNotEquals(resultForm.getQuestion(), "クイズ１");
        assertNotEquals(resultForm.getAnswer(), false);
        assertNotEquals(resultForm.getAuthor(), "クイズ太郎");
    }

    @Test
    @Sql("/test/sql/setval.sql")
    void showQuiz() throws Exception {
        // テストデータ挿入
        MultiValueMap<String, String> testParams = new LinkedMultiValueMap<>();
        testParams.add("id", "6");
        testParams.add("question", "テスト");
        testParams.add("answer", "false");
        testParams.add("author", "テスト太郎");
        testParams.add("newQuiz", "true");
        this.mockMvc.perform(post("/quiz/insert").params(testParams)).andDo(print());

        // 取得できたとき
        MvcResult result = this.mockMvc.perform(get("/quiz/play")).andDo(print())
                .andExpect(view().name("play")).andReturn();
        QuizForm resultForm = (QuizForm) result.getModelAndView().getModel().get("quizForm");
        assertThat(resultForm.getId(), anyOf(is(5), is(6)));
        assertThat(resultForm.getQuestion(), anyOf(is("クイズ１"), is("テスト")));
        assertThat(resultForm.getAnswer(), anyOf(is(true), is(false)));
        assertThat(resultForm.getAuthor(), anyOf(is("クイズ太郎"), is("テスト太郎")));

        // 取得できなかったとき
        this.mockMvc.perform(post("/quiz/delete").param("id", "5")).andDo(print());
        this.mockMvc.perform(post("/quiz/delete").param("id", "6")).andDo(print());
        this.mockMvc.perform(get("/quiz/play")).andDo(print())
                .andExpect(model().attribute("msg", "問題がありません・・・"))
                .andExpect(view().name("play"));
    }

    @Test
    void checkQuiz() throws Exception {
        // 正解のとき
        this.mockMvc.perform(post("/quiz/check").param("id", "5").param("answer", "true")).andDo(print())
                .andExpect(view().name("answer"))
                .andExpect(model().attribute("msg", "正解です！！！"));
        
        // 不正解のとき
        this.mockMvc.perform(post("/quiz/check").param("id", "5").param("answer", "false")).andDo(print())
                .andExpect(view().name("answer"))
                .andExpect(model().attribute("msg", "残念！不正解です！！！"));
    }
}