package jfxtras.styles.jmetro.themetester;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBoxBuilder;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import jfxtras.styles.jmetro8.JMetro;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ThemeTester extends Application {
    public static final String TEST = "test";

    static {
        System.getProperties().put("javafx.pseudoClassOverrideEnabled", "true");
    }

    private static final String TEST_APP_CSS_URL = ThemeTester.class.getResource("TestApp.css").toExternalForm();

    private static String MODENA_STYLESHEET_URL;
    private static String MODENA_EMBEDDED_STYLESHEET_URL;
    private static String MODENA_STYLESHEET_BASE;
//    private static String CASPIAN_STYLESHEET_URL;
//    private static String CASPIAN_STYLESHEET_BASE;

    static {
        try {
            // these are not supported ways to find the platform themes and may
            // change release to release. Just used here for testing.
//            File caspianCssFile = new File("../../../modules/controls/src/main/resources/com/sun/javafx/scene/control/skin/caspian/caspian.css");
//            if (!caspianCssFile.exists()) {
//                caspianCssFile = new File("rt/modules/controls/src/main/resources/com/sun/javafx/scene/control/skin/caspian/caspian.css");
//            }
//            CASPIAN_STYLESHEET_URL = caspianCssFile.exists() ?
//                    caspianCssFile.toURI().toURL().toExternalForm() :
//                    com.sun.javafx.scene.control.skin.ButtonSkin.class.getResource("caspian/caspian.css").toExternalForm();
            File modenaCssFile = new File("../../../modules/controls/src/main/resources/com/sun/javafx/scene/control/skin/modena/modena.css");
            if (!modenaCssFile.exists()) {
                modenaCssFile = new File("rt/modules/controls/src/main/resources/com/sun/javafx/scene/control/skin/modena/modena.css");
                System.out.println("modenaCssFile = " + modenaCssFile);
                System.out.println("modenaCssFile = " + modenaCssFile.getAbsolutePath());
            }
            MODENA_STYLESHEET_URL = modenaCssFile.exists() ?
                    modenaCssFile.toURI().toURL().toExternalForm() :
                    com.sun.javafx.scene.control.skin.ButtonSkin.class.getResource("modena/modena.css").toExternalForm();
            MODENA_STYLESHEET_BASE = MODENA_STYLESHEET_URL.substring(0,MODENA_STYLESHEET_URL.lastIndexOf('/')+1);
//            CASPIAN_STYLESHEET_BASE = CASPIAN_STYLESHEET_URL.substring(0,CASPIAN_STYLESHEET_URL.lastIndexOf('/')+1);
            MODENA_EMBEDDED_STYLESHEET_URL = MODENA_STYLESHEET_BASE + "modena-embedded-performance.css";
            System.out.println("MODENA_EMBEDDED_STYLESHEET_URL = " + MODENA_EMBEDDED_STYLESHEET_URL);
        } catch (MalformedURLException ex) {
            Logger.getLogger(ThemeTester.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private BorderPane outerRoot;
    private BorderPane root;
    private SamplePageNavigation samplePageNavigation;
    private SamplePage samplePage;
    private Node mosaic;
    private Node heightTest;
    private Node combinationsTest;
    private Node customerTest;
    private Stage mainStage;
    private Color backgroundColor;
    private Color baseColor;
    private Color accentColor;
    private String fontName = null;
    private int fontSize = 13;
    private String styleSheetContent = "";
    private String styleSheetBase = "";
    private ToggleButton modenaButton,retinaButton,rtlButton,embeddedPerformanceButton;
    private TabPane contentTabs;
    private ComboBox<String> styleComboBox;

    private Scene scene;

    private boolean test = false;
    private boolean embeddedPerformanceMode = false;

    private final EventHandler<ActionEvent> rebuild = event -> Platform.runLater(() -> {
        updateTheme();
        rebuildUI(modenaButton.isSelected(), retinaButton.isSelected(),
                contentTabs.getSelectionModel().getSelectedIndex(),
                samplePageNavigation.getCurrentSection());
    });

    private static ThemeTester instance;

    public static ThemeTester getInstance() {
        return instance;
    }

    public Map<String, Node> getContent() {
        return samplePage.getContent();
    }

    public void setRetinaMode(boolean retinaMode) {
        if (retinaMode) {
            contentTabs.getTransforms().setAll(new Scale(2,2));
        } else {
            contentTabs.getTransforms().setAll(new Scale(1,1));
        }
        contentTabs.requestLayout();
    }

    public void restart() {
        mainStage.close();
        root = null;
        accentColor = null;
        baseColor = null;
        backgroundColor = null;
        fontName = null;
        fontSize = 13;
        try {
            start(new Stage());
        } catch (Exception ex) {
            throw new RuntimeException("Failed to start another Modena window", ex);
        }
    }

    @Override public void start(Stage stage) {
        if (getParameters().getRaw().contains(TEST)) {
            test = true;
        }
        mainStage = stage;
        // set user agent stylesheet
        updateTheme(Theme.MODENA, null);
        // build Menu Bar
        outerRoot = new BorderPane();
        outerRoot.setTop(buildMenuBar());
        outerRoot.setCenter(root);
        // build UI
        rebuildUI(true,false,0, null);
        // show UI
        scene = new Scene(outerRoot, 1024, 768);
        scene.getStylesheets().add(TEST_APP_CSS_URL);

        stage.setScene(scene);

        stage.setTitle("Theme Tester");
//        stage.setIconified(test); // TODO: Blocked by http://javafx-jira.kenai.com/browse/JMY-203
        stage.show(); // see SamplePage.java:110 comment on how test fails without having stage shown
        instance = this;
    }

    private MenuBar buildMenuBar() {
        MenuBar menuBar = new MenuBar();
        menuBar.setUseSystemMenuBar(true);
        Menu fontSizeMenu = new Menu("Font");
        ToggleGroup tg = new ToggleGroup();
        fontSizeMenu.getItems().addAll(
                buildFontRadioMenuItem("System Default", null, 0, tg),
                buildFontRadioMenuItem("Mac (13px)", "Lucida Grande", 13, tg),
                buildFontRadioMenuItem("Windows 100% (12px)", "Segoe UI", 12, tg),
                buildFontRadioMenuItem("Windows 125% (15px)", "Segoe UI", 15, tg),
                buildFontRadioMenuItem("Windows 150% (18px)", "Segoe UI", 18, tg),
                buildFontRadioMenuItem("Linux (13px)", "Lucida Sans", 13, tg),
                buildFontRadioMenuItem("Embedded Touch (22px)", "Arial", 22, tg),
                buildFontRadioMenuItem("Embedded Small (9px)", "Arial", 9, tg)
        );
        menuBar.getMenus().add(fontSizeMenu);
        return menuBar;
    }

    private void updateTheme() {
        updateTheme(modenaButton.isSelected() ? Theme.MODENA : Theme.JMETRO, styleComboBox.getValue().equals("Light") ? JMetro.Style.LIGHT : JMetro.Style.DARK);
    }

    private void updateTheme(Theme theme, JMetro.Style style) {
        final SamplePage.Section scrolledSection = samplePageNavigation == null ? null : samplePageNavigation.getCurrentSection();

//        styleSheetContent = theme == Theme.MODENA ?
//                loadUrl(MODENA_STYLESHEET_URL) :
//                "";
        styleSheetContent = loadUrl(MODENA_STYLESHEET_URL);


//        if (!(theme == Theme.MODENA) &&
//                (baseColor == null || baseColor == Color.TRANSPARENT) &&
//                (backgroundColor == null || backgroundColor == Color.TRANSPARENT) &&
//                (accentColor == null || accentColor == Color.TRANSPARENT) &&
//                (fontName == null)) {
//            // no customizations
//            System.out.println("USING NO CUSTIMIZATIONS TO CSS, stylesheet = " + theme.getThemeName());
//
//            // load theme
//            setUserAgentStylesheet("internal:stylesheet" + Math.random() + ".css");
//            if (root != null) root.requestLayout();
//            // restore scrolled section
//            Platform.runLater(() -> samplePageNavigation.setCurrentSection(scrolledSection));
//            return;
//        }

        if ((theme == Theme.MODENA) && embeddedPerformanceMode) {
            styleSheetContent += loadUrl(MODENA_EMBEDDED_STYLESHEET_URL);
        }

        styleSheetBase = theme == Theme.MODENA ? MODENA_STYLESHEET_BASE : null;

        styleSheetContent += "\n.root {\n";
        System.out.println("baseColor = "+baseColor);
        System.out.println("accentColor = " + accentColor);
        System.out.println("backgroundColor = " + backgroundColor);
        if (baseColor != null && baseColor != Color.TRANSPARENT) {
            styleSheetContent += "    -fx-base:" + colorToRGBA(baseColor) + ";\n";
        }
        if (backgroundColor != null && backgroundColor != Color.TRANSPARENT) {
            styleSheetContent += "    -fx-background:" + colorToRGBA(backgroundColor) + ";\n";
        }
        if (accentColor != null && accentColor != Color.TRANSPARENT) {
            styleSheetContent += "    -fx-accent:" + colorToRGBA(accentColor) + ";\n";
        }
        if (fontName != null) {
            styleSheetContent += "    -fx-font:"+fontSize+"px \""+fontName+"\";\n";
        }
        styleSheetContent += "}\n";

//        // set white background for caspian
//        if (!modena) {
//            styleSheetContent += ".needs-background {\n-fx-background-color: white;\n}";
//        }

        if (scene != null) {
            scene.getStylesheets().clear();
        }

        // load theme
        setUserAgentStylesheet("internal:stylesheet"+Math.random()+".css");

        if (theme == Theme.JMETRO) {
            new JMetro(style).applyTheme(scene);
        }

        if (root != null) root.requestLayout();

        // restore scrolled section
        Platform.runLater(() -> samplePageNavigation.setCurrentSection(scrolledSection));
    }

    private void rebuildUI(boolean modena, boolean retina, int selectedTab, final SamplePage.Section scrolledSection) {
        try {
            if (root == null) {
                root = new BorderPane();
                outerRoot.setCenter(root);
            } else {
                // clear out old UI
                root.setTop(null);
                root.setCenter(null);
            }
            // Create sample page and nav
            samplePageNavigation = new SamplePageNavigation();
            samplePage = samplePageNavigation.getSamplePage();
            // Create Content Area
            contentTabs = new TabPane();
            contentTabs.getTabs().addAll(
                    TabBuilder.create().text("All Controls").content( samplePageNavigation ).build(),
                    TabBuilder.create().text("UI Mosaic").content(
                            ScrollPaneBuilder.create().content(
                                    mosaic = (Node) FXMLLoader.load(ThemeTester.class.getResource("ui-mosaic.fxml"))
                            ).build()
                    ).build(),
                    TabBuilder.create().text("Alignment Test").content(
                            ScrollPaneBuilder.create().content(
                                    heightTest = (Node)FXMLLoader.load(ThemeTester.class.getResource("SameHeightTest.fxml"))
                            ).build()
                    ).build(),
                    TabBuilder.create().text("Combinations").content(
                            ScrollPaneBuilder.create().content(
                                    combinationsTest = (Node)FXMLLoader.load(ThemeTester.class.getResource("CombinationTest.fxml"))
                            ).build()
                    ).build(),
                    // Customer example from bug report http://javafx-jira.kenai.com/browse/DTL-5561
                    TabBuilder.create().text("Customer Example").content(
                            ScrollPaneBuilder.create().content(
                                    customerTest = (Node)FXMLLoader.load(ThemeTester.class.getResource("ScottSelvia.fxml"))
                            ).build()
                    ).build()
            );
            contentTabs.getSelectionModel().select(selectedTab);
            samplePage.setMouseTransparent(test);
            // height test set selection for
            Platform.runLater(() -> {
                for (Node n: heightTest.lookupAll(".choice-box")) {
                    ((ChoiceBox)n).getSelectionModel().selectFirst();
                }
                for (Node n: heightTest.lookupAll(".combo-box")) {
                    ((ComboBox)n).getSelectionModel().selectFirst();
                }
            });
            // Create Toolbar
            retinaButton = ToggleButtonBuilder.create()
                    .text("@2x")
                    .selected(retina)
                    .onAction(event -> {
                        ToggleButton btn = (ToggleButton)event.getSource();
                        setRetinaMode(btn.isSelected());
                    })
                    .build();
            ToggleGroup themesToggleGroup = new ToggleGroup();
            ToolBar toolBar = new ToolBar(
                    HBoxBuilder.create()
                            .children(
                                    modenaButton = ToggleButtonBuilder.create()
                                            .text("Modena")
                                            .toggleGroup(themesToggleGroup)
                                            .selected(modena)
                                            .onAction(rebuild)
                                            .styleClass("left-pill")
                                            .build(),
                                    ToggleButtonBuilder.create()
                                            .text("JMetro")
                                            .toggleGroup(themesToggleGroup)
                                            .selected(!modena)
                                            .onAction(rebuild)
                                            .styleClass("right-pill")
                                            .build()
                            )
                            .build(),
                    ButtonBuilder.create()
                            .graphic(new ImageView(new Image(ThemeTester.class.getResource("reload_12x14.png").toString())))
                            .onAction(rebuild)
                            .build(),
                    rtlButton = ToggleButtonBuilder.create()
                            .text("RTL")
                            .onAction(event -> root.setNodeOrientation(rtlButton.isSelected() ?
                                    NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT))
                            .build(),
                    embeddedPerformanceButton = ToggleButtonBuilder.create()
                            .text("EP")
                            .selected(embeddedPerformanceMode)
                            .tooltip(new Tooltip("Apply Embedded Performance extra stylesheet"))
                            .onAction(event -> {
                                embeddedPerformanceMode = embeddedPerformanceButton.isSelected();
                                rebuild.handle(event);
                            })
                            .build(),
                    new Separator(),
                    retinaButton,
                    new Label("Base:"),
                    createBaseColorPicker(),
                    new Label("Background:"),
                    createBackgroundColorPicker(),
                    new Label("Accent:"),
                    createAccentColorPicker(),
                    new Label ("Style:"),
                    styleComboBox = createStyleComboBox(),
                    new Separator(),
                    ButtonBuilder.create().text("Save...").onAction(saveBtnHandler).build(),
                    ButtonBuilder.create().text("Restart").onAction(event -> restart()).build()
            );
            toolBar.setId("TestAppToolbar");
            // Create content group used for scaleing @2x
            final Pane contentGroup = new Pane() {
                @Override protected void layoutChildren() {
                    double scale = contentTabs.getTransforms().isEmpty() ? 1 : ((Scale)contentTabs.getTransforms().get(0)).getX();
                    contentTabs.resizeRelocate(0,0,getWidth()/scale, getHeight()/scale);
                }
            };
            contentGroup.getChildren().add(contentTabs);
            // populate root
            root.setTop(toolBar);
            root.setCenter(contentGroup);

            samplePage.getStyleClass().add("needs-background");
            mosaic.getStyleClass().add("needs-background");
            heightTest.getStyleClass().add("needs-background");
            combinationsTest.getStyleClass().add("needs-background");
            customerTest.getStyleClass().add("needs-background");
            // apply retina scale
            if (retina) {
                contentTabs.getTransforms().setAll(new Scale(2,2));
            }
            root.applyCss();
            // update state
            Platform.runLater(() -> {
                // move focus out of the way
                modenaButton.requestFocus();
                samplePageNavigation.setCurrentSection(scrolledSection);
            });
        } catch (IOException ex) {
            Logger.getLogger(ThemeTester.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    public RadioMenuItem buildFontRadioMenuItem(String name, final String in_fontName, final int in_fontSize, ToggleGroup tg) {
        return RadioMenuItemBuilder.create().text(name).onAction(event -> setFont(in_fontName, in_fontSize)).style("-fx-font: " + in_fontSize + "px \"" + in_fontName + "\";").toggleGroup(tg).build();
    }

    public void setFont(String in_fontName, int in_fontSize) {
        System.out.println("===================================================================");
        System.out.println("==   SETTING FONT TO "+in_fontName+" "+in_fontSize+"px");
        System.out.println("===================================================================");
        fontName = in_fontName;
        fontSize = in_fontSize;
        updateTheme();
    }

    private ComboBox<String> createStyleComboBox() {
        ComboBox<String> styleComboBox = new ComboBox<>();
        styleComboBox.getItems().addAll("Light", "Dark");
        styleComboBox.setValue("Light");
        styleComboBox.valueProperty().addListener(((observable, oldValue, newValue) -> updateTheme(Theme.JMETRO, styleComboBox.getValue().equals("Light") ? JMetro.Style.LIGHT : JMetro.Style.DARK)));
        return styleComboBox;
    }


    private ColorPicker createBaseColorPicker() {
        ColorPicker colorPicker = new ColorPicker(Color.TRANSPARENT);
        colorPicker.getCustomColors().addAll(
                Color.TRANSPARENT,
                Color.web("#f3622d"),
                Color.web("#fba71b"),
                Color.web("#57b757"),
                Color.web("#41a9c9"),
                Color.web("#888"),
                Color.RED,
                Color.ORANGE,
                Color.YELLOW,
                Color.GREEN,
                Color.CYAN,
                Color.BLUE,
                Color.PURPLE,
                Color.MAGENTA,
                Color.BLACK
        );
        colorPicker.valueProperty().addListener((observable, oldValue, c) -> setBaseColor(c));
        return colorPicker;
    }

    public void setBaseColor(Color c) {
        if (c == null) {
            baseColor = null;
        } else {
            baseColor = c;
        }
        updateTheme();
    }

    private ColorPicker createBackgroundColorPicker() {
        ColorPicker colorPicker = new ColorPicker(Color.TRANSPARENT);
        colorPicker.getCustomColors().addAll(
                Color.TRANSPARENT,
                Color.web("#f3622d"),
                Color.web("#fba71b"),
                Color.web("#57b757"),
                Color.web("#41a9c9"),
                Color.web("#888"),
                Color.RED,
                Color.ORANGE,
                Color.YELLOW,
                Color.GREEN,
                Color.CYAN,
                Color.BLUE,
                Color.PURPLE,
                Color.MAGENTA,
                Color.BLACK
        );
        colorPicker.valueProperty().addListener((observable, oldValue, c) -> {
            if (c == null) {
                backgroundColor = null;
            } else {
                backgroundColor = c;
            }
            updateTheme();
        });
        return colorPicker;
    }

    private ColorPicker createAccentColorPicker() {
        ColorPicker colorPicker = new ColorPicker(Color.web("#0096C9"));
        colorPicker.getCustomColors().addAll(
                Color.TRANSPARENT,
                Color.web("#0096C9"),
                Color.web("#4fb6d6"),
                Color.web("#f3622d"),
                Color.web("#fba71b"),
                Color.web("#57b757"),
                Color.web("#41a9c9"),
                Color.web("#888"),
                Color.RED,
                Color.ORANGE,
                Color.YELLOW,
                Color.GREEN,
                Color.CYAN,
                Color.BLUE,
                Color.PURPLE,
                Color.MAGENTA,
                Color.BLACK
        );
        colorPicker.valueProperty().addListener((observable, oldValue, c) -> setAccentColor(c));
        return colorPicker;
    }

    public void setAccentColor(Color c) {
        if (c == null) {
            accentColor = null;
        } else {
            accentColor = c;
        }
        updateTheme();
    }

    private EventHandler<ActionEvent> saveBtnHandler = event -> {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
        File file = fc.showSaveDialog(mainStage);
        if (file != null) {
            try {
                samplePage.getStyleClass().add("root");
                int width = (int)(samplePage.getLayoutBounds().getWidth()+0.5d);
                int height = (int)(samplePage.getLayoutBounds().getHeight()+0.5d);
                BufferedImage imgBuffer = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = imgBuffer.createGraphics();
                for (int y=0; y<height; y+=2048) {
                    SnapshotParameters snapshotParameters = new SnapshotParameters();
                    int remainingHeight = Math.min(2048, height - y);
                    snapshotParameters.setViewport(new Rectangle2D(0,y,width,remainingHeight));
                    WritableImage img = samplePage.snapshot(snapshotParameters, null);
                    g2.drawImage(SwingFXUtils.fromFXImage(img,null),0,y,null);
                }
                g2.dispose();
                ImageIO.write(imgBuffer, "PNG", file);
                System.out.println("Written image: "+file.getAbsolutePath());
            } catch (IOException ex) {
                Logger.getLogger(ThemeTester.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    };

    public static void main(String[] args) {
        launch(args);
    }

    /** Utility method to load a URL into a string */
    private static String loadUrl(String url) {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
        } catch (IOException ex) {
            Logger.getLogger(ThemeTester.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sb.toString();
    }

    // =========================================================================
    // URL Handler to create magic "internal:stylesheet.css" url for our css string buffer
    {
        URL.setURLStreamHandlerFactory(new StringURLStreamHandlerFactory());
    }

    private String colorToRGBA(Color color) {
        return String.format((Locale) null, "rgba(%d, %d, %d, %f)",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255),
                color.getOpacity());
    }

    /**
     * Simple URLConnection that always returns the content of the cssBuffer
     */
    private class StringURLConnection extends URLConnection {
        public StringURLConnection(URL url){
            super(url);
        }

        @Override public void connect() {}

        @Override public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(styleSheetContent.getBytes("UTF-8"));
        }
    }

    private class StringURLStreamHandlerFactory implements URLStreamHandlerFactory {
        URLStreamHandler streamHandler = new URLStreamHandler(){
            @Override protected URLConnection openConnection(URL url) throws IOException {
                if (url.toString().toLowerCase().endsWith(".css")) {
                    return new StringURLConnection(url);
                } else {
                    return new URL(styleSheetBase+url.getFile()).openConnection();
                }
            }
        };
        @Override public URLStreamHandler createURLStreamHandler(String protocol) {
            if ("internal".equals(protocol)) {
                return streamHandler;
            }
            return null;
        }
    }

    private enum Theme {
        MODENA("Modena"),
        JMETRO("JMetro");

        private String themeName;

        Theme(String themeName) { this.themeName = themeName; }

        public String getThemeName() { return themeName; }
    }
}
