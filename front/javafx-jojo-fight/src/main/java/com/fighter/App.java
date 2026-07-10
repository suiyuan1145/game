package com.fighter;

import com.fighter.model.Fighter;
import com.fighter.scenes.*;
import com.fighter.util.AudioManager;
import com.fighter.mugen.MugenCatalog;
import com.fighter.mugen.MugenStageInfo;
import com.fighter.mugen.MugenCharacterInfo;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * 游戏主入口
 * MENU → CHARACTER_SELECT → FIGHT
 * 
 * @author 课程设计
 * @version 6.0
 */
public class App extends Application {

    private Stage primaryStage;
    private SceneManager sceneManager;

    public enum GameScreen { MENU, CHARACTER_SELECT, LOADING, FIGHT }
    public enum GameMode { SINGLE_PLAYER, LOCAL_VERSUS }

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("JOJO 星穹乱斗 - JavaFX 完整版");
        stage.setResizable(true);
        stage.setMinWidth(800);
        stage.setMinHeight(520);
        stage.setOnCloseRequest(e -> Platform.exit());
        sceneManager = new SceneManager();
        MugenCatalog.getInstance().load();
        AudioManager.getInstance().configure(MugenCatalog.getInstance().home());
        try { AudioManager.getInstance().init(); } catch (Exception ignored) {}
        showScreen(GameScreen.MENU);
        stage.setWidth(1100);
        stage.setHeight(720);
        stage.centerOnScreen();
        stage.show();
    }

    public void showScreen(GameScreen screen) {
        Scene scene = switch (screen) {
            case MENU -> sceneManager.createMenuScene(this);
            case CHARACTER_SELECT -> sceneManager.createCharacterSelectScene(this);
            case LOADING -> sceneManager.createLoadingScene(this);
            case FIGHT -> sceneManager.createFightScene(this);
        };
        primaryStage.setScene(scene);
    }

    /** 开始战斗（带角色参数） */
    public void startFight(Fighter.CharType p1, Fighter.CharType p2) {
        sceneManager.setChars(p1, p2, MugenCatalog.getInstance().findCharacter(p1), MugenCatalog.getInstance().findCharacter(p2));
        showScreen(GameScreen.LOADING);
    }

    public void startFight(MugenCharacterInfo p1, MugenCharacterInfo p2) {
        MugenCatalog catalog = MugenCatalog.getInstance();
        sceneManager.setChars(catalog.archetypeFor(p1), catalog.archetypeFor(p2), p1, p2);
        showScreen(GameScreen.LOADING);
    }

    public void restartFight() { showScreen(GameScreen.LOADING); }

    public void enterFight() { showScreen(GameScreen.FIGHT); }

    public void selectGameMode(GameMode mode) {
        sceneManager.setGameMode(mode);
        showScreen(GameScreen.CHARACTER_SELECT);
    }

    public GameMode getGameMode() { return sceneManager.gameMode; }

    @Override
    public void stop() { AudioManager.getInstance().shutdown(); }

    public static class SceneManager {
        private MenuScene menuScene;
        private CharacterSelectScene selectScene;
        private Fighter.CharType p1Char = Fighter.CharType.BLUE;
        private Fighter.CharType p2Char = Fighter.CharType.RED;
        private GameMode gameMode = GameMode.SINGLE_PLAYER;
        private MugenStageInfo stage;
        private MugenCharacterInfo p1Mugen;
        private MugenCharacterInfo p2Mugen;

        public void setGameMode(GameMode gameMode) { this.gameMode = gameMode; }

        public void setChars(Fighter.CharType p1, Fighter.CharType p2) {
            setChars(p1, p2, MugenCatalog.getInstance().findCharacter(p1), MugenCatalog.getInstance().findCharacter(p2));
        }

        public void setChars(Fighter.CharType p1, Fighter.CharType p2, MugenCharacterInfo p1Mugen, MugenCharacterInfo p2Mugen) {
            this.p1Char = p1; this.p2Char = p2; this.p1Mugen = p1Mugen; this.p2Mugen = p2Mugen;
            var stages = MugenCatalog.getInstance().stages();
            this.stage = stages.isEmpty() ? null : stages.get((int) (Math.random() * stages.size()));
        }

        public Scene createMenuScene(App app) {
            if (menuScene == null) menuScene = new MenuScene(app);
            return menuScene.create();
        }

        public Scene createCharacterSelectScene(App app) {
            if (selectScene == null) selectScene = new CharacterSelectScene(app);
            else selectScene.reset();
            return selectScene.create();
        }

        public Scene createFightScene(App app) {
            FightScene fs = new FightScene(app, p1Char, p2Char, gameMode, stage, p1Mugen, p2Mugen);
            return fs.create();
        }

        public Scene createLoadingScene(App app) {
            return new LoadingScene(app, p1Char, p2Char, p1Mugen, p2Mugen, stage).create();
        }
    }
}
