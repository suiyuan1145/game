package com.shooter;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Main extends Application {

    private final List<Game> games = createGames();
    private final List<Game> cart = new ArrayList<>();
    private final List<Game> owned = new ArrayList<>();
    private final List<String> orders = new ArrayList<>();
    private final Map<String, String> accounts = new HashMap<>();

    private BorderPane appShell;
    private FlowPane gameGrid;
    private VBox detailBox;
    private VBox userPanel;
    private Label cartCountLabel;
    private Label balanceLabel;
    private Label userNameLabel;
    private TextField searchField;
    private ComboBox<String> categoryBox;

    private String currentUser = "游客";
    private double balance = 420.00;
    private boolean loggedIn = false;
    private Game selectedGame;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Nebula Game Store - JavaFX 游戏购买系统");
        stage.setMinWidth(1280);
        stage.setMinHeight(820);
        accounts.put("zihan", "123456");

        appShell = new BorderPane();
        appShell.getStyleClass().add("app-root");
        appShell.setLeft(buildNavigation());
        appShell.setTop(buildHeader());
        appShell.setCenter(buildStoreArea());
        appShell.setRight(buildUserPanel());

        StackPane root = new StackPane(appShell);
        root.getChildren().add(buildLoginOverlay(root));

        Scene scene = new Scene(root, 1360, 860);
        scene.getStylesheets().add(stylesheet());
        stage.setScene(scene);
        stage.show();

        selectedGame = games.get(0);
        refreshGames();
        showDetails(selectedGame);
        refreshUserPanel();
    }

    private VBox buildNavigation() {
        VBox nav = new VBox(18);
        nav.setPadding(new Insets(24, 18, 24, 18));
        nav.getStyleClass().add("nav");
        nav.setPrefWidth(210);

        Label brand = new Label("NEBULA");
        brand.getStyleClass().add("brand");
        Label sub = new Label("GAME MARKET");
        sub.getStyleClass().add("brand-sub");

        nav.getChildren().addAll(brand, sub, new Separator());
        nav.getChildren().addAll(
                navButton("商城首页", true),
                navButton("我的账户", false),
                navButton("购物车", false),
                navButton("已购游戏", false),
                navButton("订单记录", false),
                navButton("客服中心", false)
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Label tip = new Label("深色科幻主题\n内置 12 款游戏\n支持免费领取与折扣购买");
        tip.getStyleClass().add("nav-tip");
        nav.getChildren().addAll(spacer, tip);
        return nav;
    }

    private Button navButton(String text, boolean active) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add(active ? "nav-button-active" : "nav-button");
        if ("购物车".equals(text)) {
            button.setOnAction(e -> showCartPage());
        } else if ("已购游戏".equals(text)) {
            button.setOnAction(e -> showLibraryPage());
        } else if ("订单记录".equals(text)) {
            button.setOnAction(e -> showOrdersPage());
        } else if ("我的账户".equals(text)) {
            button.setOnAction(e -> showAccountPage());
        } else if ("商城首页".equals(text)) {
            button.setOnAction(e -> showStorePage());
        } else {
            button.setOnAction(e -> info("客服中心", "在线客服：support@nebula.store\n服务时间：09:00 - 22:00"));
        }
        return button;
    }

    private VBox buildHeader() {
        VBox header = new VBox(14);
        header.setPadding(new Insets(16, 26, 16, 26));
        header.getStyleClass().add("header");

        HBox topLine = new HBox(18);
        topLine.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("游戏购买系统");
        title.getStyleClass().add("page-title");
        Label slogan = new Label("参考京东商品流与 Steam 游戏库体验");
        slogan.getStyleClass().add("muted");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        userNameLabel = new Label();
        userNameLabel.getStyleClass().add("user-chip");
        topLine.getChildren().addAll(title, slogan, spacer, userNameLabel);

        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll("全部分类", "角色扮演", "开放世界", "动作冒险", "模拟经营", "射击竞技", "免费游戏", "折扣商品", "可玩游戏");
        categoryBox.getSelectionModel().selectFirst();
        categoryBox.setOnAction(e -> refreshGames());

        searchField = new TextField();
        searchField.setPromptText("搜索游戏、厂商、标签");
        searchField.textProperty().addListener((obs, old, value) -> refreshGames());
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button search = new Button("搜索");
        search.getStyleClass().add("primary-button");
        search.setOnAction(e -> refreshGames());

        Button cartButton = new Button("购物车");
        cartButton.getStyleClass().add("ghost-button");
        cartButton.setOnAction(e -> showCartPage());

        toolbar.getChildren().addAll(categoryBox, searchField, search, cartButton);
        header.getChildren().addAll(topLine, toolbar);
        return header;
    }

    private BorderPane buildStoreArea() {
        BorderPane store = new BorderPane();
        store.getStyleClass().add("store");

        gameGrid = new FlowPane(18, 18);
        gameGrid.setPadding(new Insets(24));
        gameGrid.setPrefWrapLength(760);

        ScrollPane scrollPane = new ScrollPane(gameGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("store-scroll");

        detailBox = new VBox(14);
        detailBox.setPadding(new Insets(24));
        detailBox.getStyleClass().add("detail");
        detailBox.setPrefWidth(360);

        store.setCenter(scrollPane);
        store.setBottom(detailBox);
        return store;
    }

    private void showStorePage() {
        appShell.setCenter(buildStoreArea());
        refreshGames();
        showDetails(selectedGame == null ? games.get(0) : selectedGame);
    }

    private void showAccountPage() {
        appShell.setCenter(pageShell("账户中心", "管理余额、游戏库和订单状态", accountContent()));
    }

    private void showCartPage() {
        appShell.setCenter(pageShell("购物车", "确认商品后即可进入支付流程", cartContent()));
    }

    private void showLibraryPage() {
        appShell.setCenter(pageShell("已购游戏库", "像 Steam 库一样管理已经领取和购买的游戏", libraryContent()));
    }

    private void showOrdersPage() {
        appShell.setCenter(pageShell("订单记录", "查看免费领取、余额支付和扫码支付流水", ordersContent()));
    }

    private ScrollPane pageShell(String titleText, String subtitle, VBox content) {
        VBox page = new VBox(22);
        page.setPadding(new Insets(26, 28, 36, 28));
        page.getStyleClass().add("page");

        HBox hero = new HBox(18);
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.getStyleClass().add("page-hero");

        VBox copy = new VBox(6);
        Label title = new Label(titleText);
        title.getStyleClass().add("page-hero-title");
        Label sub = new Label(subtitle);
        sub.getStyleClass().add("muted");
        copy.getChildren().addAll(title, sub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label stats = new Label("余额 ￥" + String.format("%.2f", balance) + "    已购 " + owned.size() + "    购物车 " + cart.size());
        stats.getStyleClass().add("user-chip");
        hero.getChildren().addAll(copy, spacer, stats);

        page.getChildren().addAll(hero, content);
        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("store-scroll");
        return scroll;
    }

    private VBox accountContent() {
        VBox content = new VBox(18);
        GridPane metrics = new GridPane();
        metrics.setHgap(16);
        metrics.setVgap(16);
        metrics.add(metricCard("当前用户", currentUser, "已登录演示账户"), 0, 0);
        metrics.add(metricCard("账户余额", String.format("￥%.2f", balance), "可用于余额支付"), 1, 0);
        metrics.add(metricCard("已购游戏", owned.size() + " 款", "包含免费领取游戏"), 2, 0);
        metrics.add(metricCard("购物车", cart.size() + " 件", "待支付商品数量"), 3, 0);

        HBox actions = new HBox(12);
        Button recharge = new Button("充值 ￥100");
        recharge.getStyleClass().add("primary-button");
        recharge.setOnAction(e -> {
            balance += 100;
            refreshUserPanel();
            showAccountPage();
        });
        Button library = new Button("查看游戏库");
        library.getStyleClass().add("ghost-button");
        library.setOnAction(e -> showLibraryPage());
        Button ordersButton = new Button("订单记录");
        ordersButton.getStyleClass().add("ghost-button");
        ordersButton.setOnAction(e -> showOrdersPage());
        actions.getChildren().addAll(recharge, library, ordersButton);

        content.getChildren().addAll(metrics, sectionTitle("账户功能"), actions);
        return content;
    }

    private VBox cartContent() {
        VBox content = new VBox(16);
        if (cart.isEmpty()) {
            content.getChildren().add(emptyState("购物车暂无商品", "回到商城挑选折扣游戏或免费游戏。", "去商城", this::showStorePage));
            return content;
        }

        for (Game game : cart) {
            content.getChildren().add(cartRow(game));
        }

        double total = cart.stream().mapToDouble(Game::salePrice).sum();
        HBox checkoutBar = new HBox(14);
        checkoutBar.setAlignment(Pos.CENTER_RIGHT);
        checkoutBar.getStyleClass().add("checkout-bar");
        Label totalLabel = new Label(String.format("合计：￥%.2f", total));
        totalLabel.getStyleClass().add("price-big");
        Button checkout = new Button("立即支付");
        checkout.getStyleClass().add("primary-button");
        checkout.setOnAction(e -> payFor(new ArrayList<>(cart)));
        checkoutBar.getChildren().addAll(totalLabel, checkout);
        content.getChildren().add(checkoutBar);
        return content;
    }

    private VBox libraryContent() {
        VBox content = new VBox(24);
        if (owned.isEmpty()) {
            content.getChildren().add(emptyState("还没有已购游戏", "购买或领取游戏后，这里会展示你的游戏库。", "浏览商城", this::showStorePage));
            return content;
        }

        Game featured = owned.get(owned.size() - 1);
        HBox featuredRow = new HBox(18);
        featuredRow.getStyleClass().add("library-featured");
        featuredRow.setAlignment(Pos.CENTER_LEFT);
        featuredRow.getChildren().addAll(libraryPoster(featured, 320, 170), libraryFeaturedInfo(featured));

        FlowPane libraryGrid = new FlowPane(18, 18);
        libraryGrid.getStyleClass().add("library-grid");
        for (Game game : owned) {
            libraryGrid.getChildren().add(libraryTile(game));
        }

        FlowPane recommended = new FlowPane(18, 18);
        games.stream().filter(game -> !owned.contains(game)).limit(4).forEach(game -> recommended.getChildren().add(recommendTile(game)));

        content.getChildren().addAll(sectionTitle("最近入库"), featuredRow, sectionTitle("我的游戏"), libraryGrid, sectionTitle("接下来畅玩"), recommended);
        return content;
    }

    private VBox ordersContent() {
        VBox content = new VBox(14);
        if (orders.isEmpty()) {
            content.getChildren().add(emptyState("暂无订单记录", "完成购买或免费领取后，这里会显示流水。", "去购买", this::showStorePage));
            return content;
        }
        for (int i = orders.size() - 1; i >= 0; i--) {
            HBox row = new HBox(14);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("order-row");
            Label icon = new Label("OK");
            icon.getStyleClass().add("order-icon");
            Label title = new Label(orders.get(i));
            title.getStyleClass().add("game-title");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label status = new Label("已完成");
            status.getStyleClass().add("free-tag");
            row.getChildren().addAll(icon, title, spacer, status);
            content.getChildren().add(row);
        }
        return content;
    }

    private VBox metricCard(String titleText, String valueText, String noteText) {
        VBox card = new VBox(8);
        card.setPrefSize(190, 118);
        card.getStyleClass().add("metric-card");
        Label title = new Label(titleText);
        title.getStyleClass().add("muted");
        Label value = new Label(valueText);
        value.getStyleClass().add("metric-value");
        Label note = new Label(noteText);
        note.getStyleClass().add("panel-line");
        card.getChildren().addAll(title, value, note);
        return card;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    private VBox emptyState(String titleText, String noteText, String actionText, Runnable action) {
        VBox empty = new VBox(14);
        empty.setAlignment(Pos.CENTER);
        empty.setMinHeight(360);
        empty.getStyleClass().add("empty-state");
        Label title = new Label(titleText);
        title.getStyleClass().add("empty-title");
        Label note = new Label(noteText);
        note.getStyleClass().add("muted");
        Button button = new Button(actionText);
        button.getStyleClass().add("primary-button");
        button.setOnAction(e -> action.run());
        empty.getChildren().addAll(title, note, button);
        return empty;
    }

    private HBox cartRow(Game game) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("cart-row");
        row.getChildren().add(libraryPoster(game, 170, 92));

        VBox info = new VBox(6);
        Label title = new Label(game.title());
        title.getStyleClass().add("game-title");
        Label meta = new Label(game.category() + " / " + game.publisher() + " / 评分 " + game.rating());
        meta.getStyleClass().add("muted");
        Label desc = new Label(game.description());
        desc.getStyleClass().add("panel-line");
        desc.setWrapText(true);
        info.getChildren().addAll(title, meta, desc);
        HBox.setHgrow(info, Priority.ALWAYS);

        VBox priceBox = new VBox(8);
        priceBox.setAlignment(Pos.CENTER_RIGHT);
        Label price = new Label(game.priceText());
        price.getStyleClass().add("price-big");
        Button remove = new Button("移除");
        remove.getStyleClass().add("ghost-button");
        remove.setOnAction(e -> {
            cart.remove(game);
            refreshUserPanel();
            showCartPage();
        });
        priceBox.getChildren().addAll(price, remove);

        row.getChildren().addAll(info, priceBox);
        return row;
    }

    private StackPane libraryPoster(Game game, double width, double height) {
        StackPane poster = new StackPane();
        poster.setPrefSize(width, height);
        poster.getStyleClass().add("library-poster");
        poster.getChildren().add(coverImageView(game, width, height));
        return poster;
    }

    private ImageView coverImageView(Game game, double width, double height) {
        ImageView view;
        var resource = getClass().getResource(game.imagePath());
        if (resource == null) {
            Rectangle fallback = new Rectangle(width, height);
            fallback.setFill(new LinearGradient(0, 0, 1, 1, true, null,
                    new Stop(0, game.colorA()),
                    new Stop(0.58, game.colorB()),
                    new Stop(1, Color.web("#06101f"))));
            fallback.setArcWidth(8);
            fallback.setArcHeight(8);
            StackPane snapshot = new StackPane(fallback);
            view = new ImageView(snapshot.snapshot(null, null));
        } else {
            view = new ImageView(new Image(resource.toExternalForm(), width, height, false, true));
        }
        view.setFitWidth(width);
        view.setFitHeight(height);
        view.getStyleClass().add("cover-image");
        return view;
    }

    private VBox libraryFeaturedInfo(Game game) {
        VBox info = new VBox(12);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label title = new Label(game.title());
        title.getStyleClass().add("page-hero-title");
        Label desc = new Label(game.description());
        desc.setWrapText(true);
        desc.getStyleClass().add("detail-desc");
        Label meta = new Label("最近入库 / " + game.category() + " / " + game.publisher() + " / 评分 " + game.rating());
        meta.getStyleClass().add("muted");
        HBox actions = new HBox(10);
        Button play = new Button(game.playable() ? "启动小游戏" : "启动游戏");
        play.getStyleClass().add("play-button");
        play.setOnAction(e -> {
            if (game.playable()) {
                launchPlayableGame(game);
            } else {
                info("启动游戏", game.title() + " 正在启动中...");
            }
        });
        Button detail = new Button("查看详情");
        detail.getStyleClass().add("ghost-button");
        detail.setOnAction(e -> {
            selectedGame = game;
            showStorePage();
            showDetails(game);
        });
        actions.getChildren().addAll(play, detail);
        info.getChildren().addAll(title, desc, meta, actions);
        return info;
    }

    private StackPane libraryTile(Game game) {
        StackPane tile = new StackPane();
        tile.setPrefSize(175, 235);
        tile.getStyleClass().add("library-tile");
        VBox box = new VBox(10);
        box.getChildren().add(libraryPoster(game, 175, 150));
        Label title = new Label(game.title());
        title.getStyleClass().add("game-title");
        title.setWrapText(true);
        Label status = new Label("已入库");
        status.getStyleClass().add("free-tag");
        box.getChildren().addAll(title, status);
        tile.getChildren().add(box);
        return tile;
    }

    private StackPane recommendTile(Game game) {
        StackPane tile = new StackPane();
        tile.setPrefSize(220, 168);
        tile.getStyleClass().add("recommend-tile");
        VBox box = new VBox(8);
        box.getChildren().add(libraryPoster(game, 220, 106));
        HBox line = new HBox(8);
        line.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(game.title());
        title.getStyleClass().add("game-title");
        Label price = new Label(game.priceText());
        price.getStyleClass().add(game.isFree() ? "free-price" : "price");
        line.getChildren().addAll(title, price);
        box.getChildren().add(line);
        tile.getChildren().add(box);
        tile.setOnMouseClicked(e -> {
            selectedGame = game;
            showStorePage();
            showDetails(game);
        });
        return tile;
    }

    private VBox buildUserPanel() {
        userPanel = new VBox(16);
        userPanel.setPadding(new Insets(22));
        userPanel.getStyleClass().add("user-panel");
        userPanel.setPrefWidth(300);
        return userPanel;
    }

    private VBox buildLoginOverlay(StackPane root) {
        VBox card = new VBox(14);
        card.setMaxWidth(390);
        card.setPadding(new Insets(30));
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("login-card");

        Label title = new Label("登录 Nebula 账户");
        title.getStyleClass().add("login-title");
        Label hint = new Label("默认账户：zihan / 123456，也可以注册新账户");
        hint.getStyleClass().add("muted");
        TextField name = new TextField("zihan");
        name.setPromptText("用户名");
        PasswordField password = new PasswordField();
        password.setPromptText("密码");
        password.setText("123456");
        Button login = new Button("立即登录");
        login.setMaxWidth(Double.MAX_VALUE);
        login.getStyleClass().add("primary-button");
        Button register = new Button("注册新账户");
        register.setMaxWidth(Double.MAX_VALUE);
        register.getStyleClass().add("ghost-button");

        login.setOnAction(e -> {
            String user = name.getText().trim();
            String pass = password.getText();
            if (accounts.containsKey(user) && accounts.get(user).equals(pass)) {
                loginUser(user);
                root.getChildren().remove(card);
            } else {
                info("登录失败", "用户名或密码不正确。可以使用默认账户 zihan / 123456，或注册新账户。");
            }
        });
        register.setOnAction(e -> showRegisterDialog(root, card));

        card.getChildren().addAll(title, hint, name, password, login, register);
        StackPane.setAlignment(card, Pos.CENTER);
        return card;
    }

    private void showRegisterDialog(StackPane root, VBox loginCard) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("注册 Nebula 账户");

        VBox box = new VBox(14);
        box.setPadding(new Insets(26));
        box.getStyleClass().add("dialog");
        Label title = new Label("创建新账户");
        title.getStyleClass().add("dialog-title");
        TextField user = new TextField();
        user.setPromptText("用户名");
        PasswordField pass = new PasswordField();
        pass.setPromptText("密码");
        PasswordField confirm = new PasswordField();
        confirm.setPromptText("确认密码");
        Label tip = new Label("注册成功后将自动登录，并获得 ￥420.00 演示余额。");
        tip.getStyleClass().add("muted");

        HBox actions = new HBox(10);
        Button submit = new Button("完成注册");
        submit.getStyleClass().add("primary-button");
        Button cancel = new Button("取消");
        cancel.getStyleClass().add("ghost-button");
        actions.getChildren().addAll(submit, cancel);

        submit.setOnAction(e -> {
            String name = user.getText().trim();
            if (name.length() < 3) {
                info("注册失败", "用户名至少需要 3 个字符。");
                return;
            }
            if (accounts.containsKey(name)) {
                info("注册失败", "该用户名已经存在，请换一个。");
                return;
            }
            if (pass.getText().length() < 6) {
                info("注册失败", "密码至少需要 6 位。");
                return;
            }
            if (!pass.getText().equals(confirm.getText())) {
                info("注册失败", "两次输入的密码不一致。");
                return;
            }
            accounts.put(name, pass.getText());
            loginUser(name);
            root.getChildren().remove(loginCard);
            dialog.close();
        });
        cancel.setOnAction(e -> dialog.close());

        box.getChildren().addAll(title, user, pass, confirm, tip, actions);
        Scene scene = new Scene(box, 420, 330);
        scene.getStylesheets().add(stylesheet());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void loginUser(String user) {
        currentUser = user;
        loggedIn = true;
        balance = 420.00;
        cart.clear();
        owned.clear();
        orders.clear();
        refreshUserPanel();
        showStorePage();
    }

    private void refreshGames() {
        gameGrid.getChildren().clear();
        String keyword = searchField == null ? "" : searchField.getText().toLowerCase(Locale.ROOT).trim();
        String category = categoryBox == null ? "全部分类" : categoryBox.getValue();

        games.stream()
                .filter(game -> keyword.isEmpty()
                        || game.title().toLowerCase(Locale.ROOT).contains(keyword)
                        || game.publisher().toLowerCase(Locale.ROOT).contains(keyword)
                        || game.category().toLowerCase(Locale.ROOT).contains(keyword))
                .filter(game -> "全部分类".equals(category)
                        || game.category().equals(category)
                        || ("免费游戏".equals(category) && game.isFree())
                        || ("折扣商品".equals(category) && game.discount() > 0)
                        || ("可玩游戏".equals(category) && game.playable()))
                .forEach(game -> gameGrid.getChildren().add(gameCard(game)));
    }

    private StackPane gameCard(Game game) {
        StackPane card = new StackPane();
        card.setPrefSize(240, 250);
        card.getStyleClass().add("game-card");
        card.setOnMouseClicked(e -> showDetails(game));

        VBox box = new VBox(10);
        box.setPadding(new Insets(10));

        StackPane cover = new StackPane();
        cover.setPrefHeight(130);
        cover.getStyleClass().add("cover");
        ImageView art = coverImageView(game, 220, 130);
        Label tag = new Label(game.isFree() ? "免费" : "-" + game.discount() + "%");
        tag.getStyleClass().add(game.isFree() ? "free-tag" : "discount-tag");
        StackPane.setAlignment(tag, Pos.TOP_LEFT);
        StackPane.setMargin(tag, new Insets(8));
        Label playable = new Label("可玩");
        playable.getStyleClass().add("playable-tag");
        StackPane.setAlignment(playable, Pos.TOP_RIGHT);
        StackPane.setMargin(playable, new Insets(8));
        cover.getChildren().add(art);
        cover.getChildren().add(tag);
        if (game.playable()) {
            cover.getChildren().add(playable);
        }

        Label title = new Label(game.title());
        title.getStyleClass().add("game-title");
        title.setWrapText(true);
        Label meta = new Label(game.category() + " / " + game.publisher());
        meta.getStyleClass().add("muted");

        HBox priceLine = new HBox(8);
        priceLine.setAlignment(Pos.CENTER_LEFT);
        Label price = new Label(game.priceText());
        price.getStyleClass().add(game.isFree() ? "free-price" : "price");
        Label origin = new Label(game.originalPriceText());
        origin.getStyleClass().add("origin-price");
        priceLine.getChildren().addAll(price);
        if (game.discount() > 0 && !game.isFree()) {
            priceLine.getChildren().add(origin);
        }

        Button buy = new Button(owned.contains(game) ? "已入库" : game.isFree() ? "领取" : "购买");
        buy.setMaxWidth(Double.MAX_VALUE);
        buy.getStyleClass().add("primary-button");
        buy.setDisable(owned.contains(game));
        buy.setOnAction(e -> {
            e.consume();
            selectedGame = game;
            if (game.isFree()) {
                claimFreeGame(game);
            } else {
                addToCart(game);
            }
        });

        box.getChildren().addAll(cover, title, meta, priceLine, buy);
        card.getChildren().add(box);
        return card;
    }

    private void showDetails(Game game) {
        selectedGame = game;
        detailBox.getChildren().clear();
        Label name = new Label(game.title());
        name.getStyleClass().add("detail-title");
        Label desc = new Label(game.description());
        desc.setWrapText(true);
        desc.getStyleClass().add("detail-desc");
        Label tags = new Label("类型：" + game.category() + "    发行商：" + game.publisher() + "    评分：" + game.rating());
        tags.getStyleClass().add("muted");

        HBox actions = new HBox(12);
        Button cartButton = new Button(game.isFree() ? "免费领取" : "加入购物车");
        cartButton.getStyleClass().add("primary-button");
        cartButton.setDisable(owned.contains(game));
        cartButton.setOnAction(e -> {
            if (game.isFree()) {
                claimFreeGame(game);
            } else {
                addToCart(game);
            }
        });
        Button buyNow = new Button(game.isFree() ? "领取并入库" : "立即购买");
        buyNow.getStyleClass().add("ghost-button");
        buyNow.setDisable(owned.contains(game));
        buyNow.setOnAction(e -> {
            if (game.isFree()) {
                claimFreeGame(game);
            } else {
                payFor(List.of(game));
            }
        });
        actions.getChildren().addAll(cartButton, buyNow);
        if (game.playable()) {
            Button play = new Button("试玩小游戏");
            play.getStyleClass().add("play-button");
            play.setOnAction(e -> launchPlayableGame(game));
            actions.getChildren().add(play);
        }

        detailBox.getChildren().addAll(name, desc, tags, actions);
    }

    private void refreshUserPanel() {
        if (userPanel == null) {
            return;
        }
        userPanel.getChildren().clear();
        userNameLabel.setText(loggedIn ? "账户：" + currentUser : "未登录");

        Label title = new Label("用户面板");
        title.getStyleClass().add("panel-title");
        Label name = new Label(currentUser);
        name.getStyleClass().add("account-name");
        balanceLabel = new Label(String.format("余额：￥%.2f", balance));
        balanceLabel.getStyleClass().add("balance");
        cartCountLabel = new Label("购物车：" + cart.size() + " 件");
        cartCountLabel.getStyleClass().add("panel-line");
        Label ownedLabel = new Label("游戏库：" + owned.size() + " 款");
        ownedLabel.getStyleClass().add("panel-line");

        Button recharge = new Button("账户充值");
        recharge.getStyleClass().add("ghost-button");
        recharge.setMaxWidth(Double.MAX_VALUE);
        recharge.setOnAction(e -> {
            balance += 100;
            refreshUserPanel();
            info("充值成功", "账户已增加 ￥100.00");
        });

        Button checkout = new Button("去支付");
        checkout.getStyleClass().add("primary-button");
        checkout.setMaxWidth(Double.MAX_VALUE);
        checkout.setDisable(cart.isEmpty());
        checkout.setOnAction(e -> payFor(new ArrayList<>(cart)));

        Label cartTitle = new Label("最近加入");
        cartTitle.getStyleClass().add("panel-title-small");
        VBox cartPreview = new VBox(8);
        if (cart.isEmpty()) {
            Label empty = new Label("购物车暂无商品");
            empty.getStyleClass().add("muted");
            cartPreview.getChildren().add(empty);
        } else {
            cart.stream().limit(4).forEach(game -> {
                Label row = new Label(game.title() + "  " + game.priceText());
                row.getStyleClass().add("panel-line");
                cartPreview.getChildren().add(row);
            });
        }

        userPanel.getChildren().addAll(title, name, balanceLabel, cartCountLabel, ownedLabel,
                recharge, checkout, new Separator(), cartTitle, cartPreview);
    }

    private void addToCart(Game game) {
        ensureLogin();
        if (owned.contains(game)) {
            info("已拥有", "该游戏已经在你的游戏库中。");
            return;
        }
        if (!cart.contains(game)) {
            cart.add(game);
        }
        refreshUserPanel();
        refreshGames();
        info("加入购物车", game.title() + " 已加入购物车。");
    }

    private void claimFreeGame(Game game) {
        ensureLogin();
        if (!owned.contains(game)) {
            owned.add(game);
            orders.add("免费领取：" + game.title());
        }
        cart.remove(game);
        refreshUserPanel();
        refreshGames();
        showLibraryPage();
    }

    private void payFor(List<Game> targets) {
        ensureLogin();
        List<Game> payable = targets.stream().filter(game -> !owned.contains(game)).toList();
        if (payable.isEmpty()) {
            info("无需支付", "选择的游戏已在游戏库中。");
            return;
        }
        double total = payable.stream().mapToDouble(Game::salePrice).sum();

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("支付订单");

        VBox box = new VBox(14);
        box.setPadding(new Insets(24));
        box.getStyleClass().add("dialog");
        Label title = new Label("确认支付");
        title.getStyleClass().add("dialog-title");
        Label items = new Label(payable.stream().map(Game::title).reduce((a, b) -> a + "\n" + b).orElse(""));
        items.getStyleClass().add("dialog-text");
        Label amount = new Label(String.format("订单金额：￥%.2f", total));
        amount.getStyleClass().add("price-big");
        Label remain = new Label(String.format("账户余额：￥%.2f", balance));
        remain.getStyleClass().add("muted");

        HBox buttons = new HBox(10);
        Button wallet = new Button("余额支付");
        wallet.getStyleClass().add("primary-button");
        Button gateway = new Button("模拟扫码支付");
        gateway.getStyleClass().add("ghost-button");
        Button cancel = new Button("取消");
        cancel.getStyleClass().add("ghost-button");

        wallet.setOnAction(e -> {
            if (balance < total) {
                info("余额不足", "请先在右侧用户面板充值。");
                return;
            }
            balance -= total;
            completePayment(payable, total, "余额支付");
            dialog.close();
        });
        gateway.setOnAction(e -> {
            completePayment(payable, total, "模拟扫码支付");
            dialog.close();
        });
        cancel.setOnAction(e -> dialog.close());
        buttons.getChildren().addAll(wallet, gateway, cancel);

        box.getChildren().addAll(title, items, amount, remain, buttons);
        Scene scene = new Scene(box, 430, 310);
        scene.getStylesheets().add(stylesheet());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void completePayment(List<Game> payable, double total, String method) {
        owned.addAll(payable);
        cart.removeAll(payable);
        orders.add(String.format("%s：%d 款游戏，合计 ￥%.2f", method, payable.size(), total));
        refreshUserPanel();
        refreshGames();
        showLibraryPage();
    }

    private void ensureLogin() {
        if (!loggedIn) {
            loggedIn = true;
            currentUser = "zihan";
            refreshUserPanel();
        }
    }

    private void showCartDialog() {
        String body = cart.isEmpty()
                ? "购物车暂无商品。"
                : cart.stream().map(game -> game.title() + "  " + game.priceText()).reduce((a, b) -> a + "\n" + b).orElse("");
        info("购物车", body);
    }

    private void showLibraryDialog() {
        String body = owned.isEmpty()
                ? "游戏库暂无已购商品。"
                : owned.stream().map(Game::title).reduce((a, b) -> a + "\n" + b).orElse("");
        info("已购游戏", body);
    }

    private void showOrdersDialog() {
        String body = orders.isEmpty()
                ? "暂无订单记录。"
                : orders.stream().reduce((a, b) -> a + "\n" + b).orElse("");
        info("订单记录", body);
    }

    private void showAccountDialog() {
        info("账户功能", String.format("用户名：%s\n账户余额：￥%.2f\n购物车：%d 件\n游戏库：%d 款",
                currentUser, balance, cart.size(), owned.size()));
    }

    private void launchPlayableGame(Game game) {
        Stage gameStage = new Stage();
        gameStage.setTitle(game.title() + " - 可玩小游戏");
        gameStage.setResizable(false);

        Canvas canvas = new Canvas(900, 650);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, 900, 650, Color.rgb(10, 10, 46));

        GameEngine engine = new GameEngine(gc, canvas);
        engine.init();

        scene.setOnKeyPressed(e -> {
            KeyCode code = e.getCode();
            if (code == KeyCode.LEFT || code == KeyCode.A) {
                engine.pressLeft(true);
            }
            if (code == KeyCode.RIGHT || code == KeyCode.D) {
                engine.pressRight(true);
            }
            if (code == KeyCode.SPACE) {
                engine.pressSpace(true);
            }
            if (code == KeyCode.F || code == KeyCode.F12) {
                engine.toggleAutoFire();
            }
            if ((code == KeyCode.ENTER || code == KeyCode.SPACE) && !engine.isRunning()) {
                engine.startGame();
            }
            if (code.isDigitKey()) {
                String ch = code.getName();
                if (ch.length() == 1) {
                    engine.handleKeyTyped(ch.charAt(0));
                }
            }
        });

        scene.setOnKeyReleased(e -> {
            KeyCode code = e.getCode();
            if (code == KeyCode.LEFT || code == KeyCode.A) {
                engine.pressLeft(false);
            }
            if (code == KeyCode.RIGHT || code == KeyCode.D) {
                engine.pressRight(false);
            }
            if (code == KeyCode.SPACE) {
                engine.pressSpace(false);
            }
        });

        scene.setOnKeyTyped(e -> {
            if (!e.getCharacter().isEmpty()) {
                char c = e.getCharacter().charAt(0);
                if (c >= '0' && c <= '9') {
                    engine.handleKeyTyped(c);
                }
            }
        });

        canvas.setOnMouseMoved(e -> engine.mouseMove(e.getX()));
        canvas.setOnMouseClicked(e -> {
            if (!engine.isRunning()) {
                engine.startGame();
            } else {
                engine.fireBullet();
            }
        });

        gameStage.setScene(scene);
        gameStage.setOnCloseRequest(e -> engine.stopLoop());
        gameStage.show();
        engine.startLoop();
        scene.getRoot().requestFocus();
    }

    private void info(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private List<Game> createGames() {
        return List.of(
                new Game("艾尔登法环", "RING", "角色扮演", "FromSoftware", "开放世界魂系角色扮演游戏，探索交界地、挑战半神，并收集强大的装备与法术。", 398, 25, 9.7, Color.web("#0b1a18"), Color.web("#c7952b"), "/covers/elden.jpg", false),
                new Game("赛博朋克2077", "2077", "开放世界", "CD Projekt", "霓虹都市、义体改造和高自由度任务线，体验夜之城的未来冒险。", 299, 50, 9.1, Color.web("#f6e900"), Color.web("#1b1b2b"), "/covers/cyberpunk.jpg", false),
                new Game("Counter-Strike 2", "CS2", "射击竞技", "Valve", "免费多人竞技射击游戏，经典爆破模式、精准枪法和团队配合是胜负关键。", 0, 0, 9.1, Color.web("#24445f"), Color.web("#d89a34"), "/covers/cs2.jpg", false),
                new Game("黑神话：悟空", "WUK", "动作冒险", "Game Science", "东方神话题材动作冒险游戏，化身天命人，面对妖王与古老传说。", 268, 0, 9.6, Color.web("#1a140f"), Color.web("#6f5238"), "/covers/wukong.jpg", false),
                new Game("星露谷物语", "FARM", "模拟经营", "ConcernedApe", "经营农场、采矿、钓鱼、社交，打造属于自己的乡村生活。", 68, 29, 9.4, Color.web("#79d44d"), Color.web("#3aa0ff"), "/covers/stardew.jpg", false),
                new Game("只狼：影逝二度", "SEKI", "动作冒险", "FromSoftware", "高强度剑戟动作游戏，凭借格挡、忍具与身法击败强敌。", 268, 0, 9.5, Color.web("#1c0f0a"), Color.web("#a84a20"), "/covers/sekiro.jpg", false),
                new Game("巫师3：狂猎", "W3", "角色扮演", "CD Projekt", "扮演猎魔人杰洛特，调查怪物、政治阴谋与命运之子的踪迹。", 199, 50, 9.6, Color.web("#e8f3f4"), Color.web("#5e7983"), "/covers/witcher.jpg", false),
                new Game("最终幻想7 重制版", "FF7", "角色扮演", "Square Enix", "经典 RPG 的现代化重制，在米德加展开电影级冒险与即时战斗。", 446, 0, 9.0, Color.web("#d69a72"), Color.web("#8cc4ff"), "/covers/ff7.jpg", false),
                new Game("博德之门3", "BG3", "角色扮演", "Larian Studios", "基于队伍和骰子的史诗 RPG，自由选择会改变每段旅程。", 298, 10, 9.8, Color.web("#22334f"), Color.web("#c07a2a"), "/covers/baldur.jpg", false),
                new Game("幻兽帕鲁", "PAL", "开放世界", "Pocketpair", "捕获帕鲁、建造基地、探索岛屿，并与好友一起冒险。", 108, 20, 9.0, Color.web("#41d8ff"), Color.web("#8b47ff"), "/covers/palworld.jpg", false),
                new Game("空洞骑士", "HK", "动作冒险", "Team Cherry", "手绘地下王国探索游戏，挑战首领、发现隐藏路线与古老秘密。", 48, 30, 9.3, Color.web("#10131f"), Color.web("#4c5d8e"), "/covers/hollow.jpg", false),
                new Game("星际拓荒者", "STAR", "动作冒险", "Nebula Lab", "内置可玩小游戏商品。购买或直接试玩后，可启动之前开发的 JavaFX 太空射击小游戏。", 128, 35, 9.2, Color.web("#21d4fd"), Color.web("#b721ff"), "/covers/star.jpg", true)
        );
    }

    private String stylesheet() {
        var resource = getClass().getResource("/store.css");
        if (resource != null) {
            return resource.toExternalForm();
        }
        Path sourceCss = Path.of("src", "main", "resources", "store.css").toAbsolutePath();
        if (Files.exists(sourceCss)) {
            return sourceCss.toUri().toString();
        }
        return "";
    }

    private record Game(String title, String shortName, String category, String publisher, String description,
                        double originalPrice, int discount, double rating, Color colorA, Color colorB,
                        String imagePath, boolean playable) {
        boolean isFree() {
            return originalPrice <= 0;
        }

        double salePrice() {
            return isFree() ? 0 : originalPrice * (100 - discount) / 100.0;
        }

        String priceText() {
            return isFree() ? "免费领取" : String.format("￥%.2f", salePrice());
        }

        String originalPriceText() {
            return isFree() ? "" : String.format("￥%.2f", originalPrice);
        }
    }
}
