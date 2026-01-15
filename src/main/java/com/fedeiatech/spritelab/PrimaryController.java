package com.fedeiatech.spritelab;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.controlsfx.control.RangeSlider;

public class PrimaryController implements Initializable {

    @FXML private BorderPane rootPane;
    @FXML private Button btnCargar, btnPreview, btnExportar, btnVerSheet, btnReset, btnLang, btnVerGif, btnExportarGif, btnAbout;
    @FXML private ToggleButton btnGotero, btnZoomLock, btnManualCrop;
    @FXML private Label lblArchivoNombre, lblInstrucciones, lblZoomFactor, lblInfoGrilla, lblResolucionTotal, lblDuracionRecorte;
    @FXML private Label section1, section2, section3, section4, lblAltura, lblAncho, lblFps, lblTolerancia, lblEstado;
    @FXML private Slider sliderFps, sliderTolerancia;
    @FXML private TextField txtFps, txtAltura, txtAnchoCalc, txtStart, txtEnd;
    @FXML private CheckBox chkChroma;
    @FXML private ColorPicker colorPicker;
    @FXML private ImageView imgPreview, imgLinkIcon;
    @FXML private StackPane previewContainer;
    @FXML private Pane selectionPane;
    @FXML private ScrollPane scrollPreview;
    @FXML private VBox rangeSliderContainer;

    private RangeSlider timeRangeSlider;
    private File archivoVideoActual;
    private final FFmpegService ffmpegService = new FFmpegService();
    private FFmpegService.VideoMeta metaDatosActuales;
    private PauseTransition debounceTimer;

    private double currentZoomScale = 1.0;
    private String cropActual = null;
    private Rectangle selectionRect;
    private boolean esEspanol = true;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        corregirEstilosVisuales();
        crearFondoAjedrez();
        configurarDragAndDrop();
        configurarSincronizacionFPS();
        inicializarRangeSlider();
        configurarZoom();
        configurarGotero();
        configurarRecorte();
        
        try {
            imgLinkIcon.setImage(new Image(getClass().getResourceAsStream("/com/fedeiatech/spritelab/img/link.png")));
        } catch (Exception e) {}
        
        debounceTimer = new PauseTransition(Duration.millis(500));
        debounceTimer.setOnFinished(e -> onRefrescarPreview());

        btnPreview.setDisable(true); 
        btnExportar.setDisable(true); 
        btnVerSheet.setDisable(true);
        btnVerGif.setDisable(true); 
        btnExportarGif.setDisable(true);
        colorPicker.setValue(Color.WHITE);

        btnManualCrop.disableProperty().bind(btnZoomLock.selectedProperty().not().or(btnReset.disableProperty().not()));
        btnReset.setDisable(true);

        txtAltura.textProperty().addListener((o, old, n) -> { calcularAnchoProporcional(); calcularInfoGrilla(); });
        sliderFps.valueProperty().addListener(o -> { calcularInfoGrilla(); debounceTimer.playFromStart(); });
        sliderTolerancia.valueProperty().addListener(o -> debounceTimer.playFromStart());
        colorPicker.valueProperty().addListener(o -> debounceTimer.playFromStart());
        
        chkChroma.selectedProperty().addListener(o -> onRefrescarPreview());
        actualizarIdioma();
    }

    @FXML
    private void cambiarIdioma() {
        esEspanol = !esEspanol;
        actualizarIdioma();
    }

    private void actualizarIdioma() {
        if (esEspanol) {
            btnLang.setText("EN");
            section1.setText("1. ARCHIVO DE ORIGEN");
            btnCargar.setText("📂 Cargar Video");
            section2.setText("2. RECORTE DE TIEMPO");
            section3.setText("3. CONFIGURACIÓN SPRITE");
            lblAltura.setText("Altura (px)");
            lblAncho.setText("Ancho (px)");
            btnManualCrop.setText("✂️ Recortar");
            btnReset.setText("🔄 Restaurar");
            section4.setText("4. LIMPIEZA DE FONDO");
            chkChroma.setText("Activar Chroma Key");
            btnGotero.setText("🖌 Gotero");
            lblTolerancia.setText("Tolerancia:");
            btnPreview.setText("🔄 Frame");
            btnVerSheet.setText("👁 Ver Sheet");
            btnVerGif.setText("🎬 Previa GIF");
            btnExportarGif.setText("🎁 GIF");
            btnExportar.setText("🚀 EXPORTAR PNG");
            lblEstado.setText("ESTADO:");
            lblInstrucciones.setText("Carga un video o arrástralo aquí\n(Formatos: .MP4, .AVI, .MOV)");
            btnZoomLock.setText(btnZoomLock.isSelected() ? "🔒 Ajustar" : "🔓 Libre");
        } else {
            btnLang.setText("ES");
            section1.setText("1. SOURCE FILE");
            btnCargar.setText("📂 Load Video");
            section2.setText("2. TIME CLIPPING");
            section3.setText("3. SPRITE SETTINGS");
            lblAltura.setText("Height (px)");
            lblAncho.setText("Width (px)");
            btnManualCrop.setText("✂️ Crop");
            btnReset.setText("🔄 Restore");
            section4.setText("4. BACKGROUND CLEANING");
            chkChroma.setText("Enable Chroma Key");
            btnGotero.setText("🖌 Picker");
            lblTolerancia.setText("Tolerance:");
            btnPreview.setText("🔄 Refresh");
            btnVerSheet.setText("👁 View Sheet");
            btnVerGif.setText("🎬 Preview GIF");
            btnExportarGif.setText("🎁 GIF");
            btnExportar.setText("🚀 EXPORT PNG");
            lblEstado.setText("STATUS:");
            lblInstrucciones.setText("Load a video or drag it here\n(Formats: .MP4, .AVI, .MOV)");
            btnZoomLock.setText(btnZoomLock.isSelected() ? "🔒 Fit" : "🔓 Free");
        }
    }

    private void configurarZoom() {
        btnZoomLock.selectedProperty().addListener((obs, old, isLocked) -> {
            if (isLocked) {
                btnZoomLock.setText(esEspanol ? "🔒 Ajustar" : "🔒 Fit");
                imgPreview.fitWidthProperty().bind(previewContainer.widthProperty().subtract(20));
                imgPreview.fitHeightProperty().bind(previewContainer.heightProperty().subtract(20));
                lblZoomFactor.setText("FIT");
                currentZoomScale = 1.0;
                scrollPreview.setPannable(false);
            } else {
                btnZoomLock.setText(esEspanol ? "🔓 Libre" : "🔓 Free");
                imgPreview.fitWidthProperty().unbind(); 
                imgPreview.fitHeightProperty().unbind();
                lblZoomFactor.setText("100%");
                scrollPreview.setPannable(true);
            }
            limpiarCapaRecorte();
        });

        scrollPreview.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (!btnZoomLock.isSelected()) {
                event.consume();
                double delta = (event.getDeltaY() > 0) ? 1.1 : 1 / 1.1;
                currentZoomScale = Math.clamp(currentZoomScale * delta, 0.05, 15.0);
                if (imgPreview.getImage() != null) {
                    imgPreview.setFitWidth(imgPreview.getImage().getWidth() * currentZoomScale);
                    imgPreview.setFitHeight(imgPreview.getImage().getHeight() * currentZoomScale);
                }
                lblZoomFactor.setText(String.format("%.0f%%", currentZoomScale * 100));
            }
        });
    }

    private void configurarRecorte() {
        selectionRect = new Rectangle(0, 0, 0, 0);
        selectionRect.setStroke(Color.LIME); 
        selectionRect.setStrokeWidth(2);
        selectionRect.setFill(Color.rgb(0, 255, 0, 0.15));
        selectionRect.setVisible(false);
        selectionPane.getChildren().add(selectionRect);

        btnManualCrop.selectedProperty().addListener((o, old, isSelected) -> {
            selectionPane.setMouseTransparent(!isSelected);
            rootPane.setCursor(isSelected ? Cursor.CROSSHAIR : Cursor.DEFAULT);
            if (isSelected) btnGotero.setSelected(false);
        });

        selectionPane.setOnMousePressed(e -> {
            selectionRect.setX(e.getX()); selectionRect.setY(e.getY());
            selectionRect.setWidth(0); selectionRect.setHeight(0);
            selectionRect.setVisible(true);
        });

        selectionPane.setOnMouseDragged(e -> {
            selectionRect.setWidth(Math.abs(e.getX() - selectionRect.getX()));
            selectionRect.setHeight(Math.abs(e.getY() - selectionRect.getY()));
        });
        
        selectionPane.setOnMouseReleased(e -> {
            if (selectionRect.getWidth() > 10) {
                cropActual = calcularCropRobusto();
                if (cropActual != null) {
                    btnReset.setDisable(false);
                    onRefrescarPreview();
                }
            }
            btnManualCrop.setSelected(false);
            rootPane.setCursor(Cursor.DEFAULT);
        });
    }

    private String calcularCropRobusto() {
        Image img = imgPreview.getImage();
        if (img == null) return null;
        Bounds b = imgPreview.getBoundsInParent();
        double sX = img.getWidth() / b.getWidth();
        double sY = img.getHeight() / b.getHeight();
        int x = (int) ((selectionRect.getX() - b.getMinX()) * sX);
        int y = (int) ((selectionRect.getY() - b.getMinY()) * sY);
        int w = (int) (selectionRect.getWidth() * sX);
        int h = (int) (selectionRect.getHeight() * sY);
        return String.format("crop=%d:%d:%d:%d", w, h, x, y);
    }

    private void configurarGotero() {
        btnGotero.selectedProperty().addListener((o, old, isSelected) -> {
            if (isSelected) btnManualCrop.setSelected(false);
            rootPane.setCursor(isSelected ? Cursor.CROSSHAIR : Cursor.DEFAULT);
        });

        previewContainer.setOnMouseClicked(e -> {
            if (!btnGotero.isSelected() || imgPreview.getImage() == null) return;
            try {
                Bounds b = imgPreview.getBoundsInParent();
                double sX = imgPreview.getImage().getWidth() / b.getWidth();
                double sY = imgPreview.getImage().getHeight() / b.getHeight();
                int px = (int) ((e.getX() - b.getMinX()) * sX);
                int py = (int) ((e.getY() - b.getMinY()) * sY);
                colorPicker.setValue(imgPreview.getImage().getPixelReader().getColor(px, py));
            } catch (Exception ex) {}
            btnGotero.setSelected(false); 
            rootPane.setCursor(Cursor.DEFAULT); 
            onRefrescarPreview();
        });
    }

    @FXML private void onResetCrop() { cropActual = null; btnReset.setDisable(true); onRefrescarPreview(); }

    @FXML private void onRefrescarPreview() {
        if (archivoVideoActual == null) return;
        Task<File> task = new Task<>() {
            @Override protected File call() throws Exception {
                return ffmpegService.generarPreview(archivoVideoActual, (int)sliderFps.getValue(), Integer.parseInt(txtAltura.getText()), (chkChroma.isSelected() ? getColorHex() : null), sliderTolerancia.getValue(), timeRangeSlider.getLowValue(), cropActual);
            }
        };
        task.setOnSucceeded(e -> { 
            imgPreview.setImage(new Image(task.getValue().toURI().toString())); 
            limpiarCapaRecorte(); calcularAnchoProporcional(); calcularInfoGrilla();
            btnPreview.setText(esEspanol ? "🔄 Frame" : "🔄 Refresh"); 
        });
        new Thread(task).start();
    }

    private void cargarVideo(File file) {
        archivoVideoActual = file; lblArchivoNombre.setText(file.getName()); lblInstrucciones.setVisible(false);
        Task<FFmpegService.VideoMeta> task = new Task<>() { @Override protected FFmpegService.VideoMeta call() throws Exception { return ffmpegService.obtenerMetadatos(file); } };
        task.setOnSucceeded(e -> {
            metaDatosActuales = task.getValue(); txtAltura.setText(String.valueOf(metaDatosActuales.height));
            timeRangeSlider.setMax(metaDatosActuales.duration); timeRangeSlider.setLowValue(0); timeRangeSlider.setHighValue(metaDatosActuales.duration);
            btnZoomLock.setSelected(true); 
            btnPreview.setDisable(false); btnExportar.setDisable(false); btnVerSheet.setDisable(false); 
            btnVerGif.setDisable(false); btnExportarGif.setDisable(false);
            onRefrescarPreview();
        });
        new Thread(task).start();
    }

    private void calcularAnchoProporcional() {
        if (metaDatosActuales == null) return;
        try {
            int h = Integer.parseInt(txtAltura.getText());
            double wImg = (imgPreview.getImage() != null) ? imgPreview.getImage().getWidth() : metaDatosActuales.width;
            double hImg = (imgPreview.getImage() != null) ? imgPreview.getImage().getHeight() : metaDatosActuales.height;
            txtAnchoCalc.setText(String.valueOf((int)(h * (wImg / hImg))));
        } catch (Exception e) {}
    }

    private void calcularInfoGrilla() {
        if (metaDatosActuales == null) return;
        try {
            double dur = timeRangeSlider.getHighValue() - timeRangeSlider.getLowValue();
            int total = (int) Math.ceil(dur * (int) sliderFps.getValue());
            int col = (int) Math.ceil(Math.sqrt(total));
            int h = Integer.parseInt(txtAltura.getText()), w = Integer.parseInt(txtAnchoCalc.getText());
            lblInfoGrilla.setText(String.format("FRAMES: %d | GRILLA: %dx%d", total, col, (int)Math.ceil((double)total/col)));
            lblResolucionTotal.setText(String.format("%d x %d px", col * w, (int)Math.ceil((double)total/col) * h));
        } catch (Exception e) {}
    }

    private void limpiarCapaRecorte() { if (selectionRect != null) { selectionRect.setVisible(false); selectionRect.setWidth(0); } }
    private void corregirEstilosVisuales() { lblInstrucciones.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold; -fx-font-size: 20px;"); }
    private void crearFondoAjedrez() { int sz = 20; WritableImage p = new WritableImage(sz*2, sz*2); PixelWriter pw = p.getPixelWriter(); for (int x=0; x<sz*2; x++) for (int y=0; y<sz*2; y++) pw.setColor(x,y, ((x<sz&&y<sz)||(x>=sz&&y>=sz)) ? Color.WHITE : Color.web("#cccccc")); previewContainer.setBackground(new Background(new BackgroundFill(new ImagePattern(p,0,0,sz*2,sz*2,false), null, null))); }
    private void configurarDragAndDrop() { rootPane.setOnDragOver(e -> { if (e.getDragboard().hasFiles()) e.acceptTransferModes(TransferMode.COPY); e.consume(); }); rootPane.setOnDragDropped(e -> { if (e.getDragboard().hasFiles()) { cargarVideo(e.getDragboard().getFiles().get(0)); e.setDropCompleted(true); } e.consume(); }); }
    private void configurarSincronizacionFPS() { sliderFps.valueProperty().addListener((o, old, n) -> txtFps.setText(String.valueOf(n.intValue()))); }
    private void inicializarRangeSlider() { timeRangeSlider = new RangeSlider(0, 100, 0, 100); rangeSliderContainer.getChildren().add(timeRangeSlider); timeRangeSlider.lowValueProperty().addListener((o, old, n) -> { txtStart.setText(String.format("%.2f", n.doubleValue()).replace(",", ".")); calcularInfoGrilla(); debounceTimer.playFromStart(); }); timeRangeSlider.highValueProperty().addListener((o, old, n) -> { txtEnd.setText(String.format("%.2f", n.doubleValue()).replace(",", ".")); calcularInfoGrilla(); }); }
    @FXML private void onCargarVideo() { FileChooser fc = new FileChooser(); File file = fc.showOpenDialog(rootPane.getScene().getWindow()); if (file != null) cargarVideo(file); }
    private String getColorHex() { Color c = colorPicker.getValue(); return String.format("0x%02X%02X%02X", (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255)); }
    
    @FXML private void onVerMiniatura() { 
        Task<File> t = new Task<>() { @Override protected File call() throws Exception { 
            File f = File.createTempFile("sheet", ".png"); 
            ffmpegService.exportarSpriteSheet(archivoVideoActual, f, (int)sliderFps.getValue(), Integer.parseInt(txtAltura.getText()), (chkChroma.isSelected() ? getColorHex() : null), sliderTolerancia.getValue(), timeRangeSlider.getLowValue(), timeRangeSlider.getHighValue(), cropActual); 
            return f; 
        } }; 
        t.setOnSucceeded(e -> mostrarVentanaSheet(t.getValue())); 
        new Thread(t).start(); 
    }

    private void mostrarVentanaSheet(File f) {
        Stage s = new Stage();
        s.initModality(Modality.APPLICATION_MODAL);
        s.setTitle(esEspanol ? "Vista Previa" : "Preview");
        Image img = new Image(f.toURI().toString());
        ImageView iv = new ImageView(img);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        StackPane container = new StackPane(iv);
        container.setStyle("-fx-background-color: #1a1a1a;");
        Scene scene = new Scene(container, 800, 600);
        iv.fitWidthProperty().bind(scene.widthProperty().subtract(50));
        iv.fitHeightProperty().bind(scene.heightProperty().subtract(50));
        s.setScene(scene);
        s.show();
    }

    @FXML private void onVerGif() {
        Task<File> t = new Task<>() { @Override protected File call() throws Exception {
            File f = File.createTempFile("preview", ".gif");
            ffmpegService.exportarGif(archivoVideoActual, f, (int)sliderFps.getValue(), Integer.parseInt(txtAltura.getText()), timeRangeSlider.getLowValue(), timeRangeSlider.getHighValue(), cropActual);
            return f;
        } };
        t.setOnSucceeded(e -> {
            mostrarVentanaSheet(t.getValue());
            btnVerGif.setText(esEspanol ? "🎬 Previa GIF" : "🎬 Preview GIF");
        });
        t.setOnFailed(e -> {
            btnVerGif.setText(esEspanol ? "🎬 Previa GIF" : "🎬 Preview GIF");
        });
        btnVerGif.setText("⏳...");
        new Thread(t).start();
    }

    @FXML private void onExportarGif() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("GIF", "*.gif"));
        File dest = fc.showSaveDialog(rootPane.getScene().getWindow());
        if (dest != null) {
            Task<Void> t = new Task<>() { @Override protected Void call() throws Exception {
                ffmpegService.exportarGif(archivoVideoActual, dest, (int)sliderFps.getValue(), Integer.parseInt(txtAltura.getText()), timeRangeSlider.getLowValue(), timeRangeSlider.getHighValue(), cropActual);
                return null;
            } };
            t.setOnSucceeded(e -> { 
                new Alert(Alert.AlertType.INFORMATION, "GIF OK").show(); 
                btnExportarGif.setText(esEspanol ? "🎁 GIF" : "🎁 GIF"); 
            });
            t.setOnFailed(e -> {
                btnExportarGif.setText(esEspanol ? "🎁 GIF" : "🎁 GIF");
            });
            btnExportarGif.setText("⏳...");
            new Thread(t).start();
        }
    }

    @FXML private void onExportar() { 
        FileChooser fc = new FileChooser(); File dest = fc.showSaveDialog(rootPane.getScene().getWindow()); 
        if (dest != null) { 
            Task<Void> t = new Task<>() { @Override protected Void call() throws Exception { 
                ffmpegService.exportarSpriteSheet(archivoVideoActual, dest, (int)sliderFps.getValue(), Integer.parseInt(txtAltura.getText()), (chkChroma.isSelected() ? getColorHex() : null), sliderTolerancia.getValue(), timeRangeSlider.getLowValue(), timeRangeSlider.getHighValue(), cropActual); 
                return null; 
            } }; 
            t.setOnSucceeded(e -> { new Alert(Alert.AlertType.INFORMATION, "OK").show(); }); 
            new Thread(t).start(); 
        } 
    }

    @FXML
    private void onAbout() {
        Stage s = new Stage();
        s.initModality(Modality.APPLICATION_MODAL);
        s.setTitle(esEspanol ? "Acerca de Sprite Lab" : "About Sprite Lab");
        
        VBox layout = new VBox(15);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(25));
        layout.setStyle("-fx-background-color: #1a1a1a;");

        Label title = new Label("Sprite Lab v0.3");
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 20px;");

        Label dev = new Label(esEspanol ? "Desarrollado por FedeiaTech" : "Developed by FedeiaTech");
        dev.setStyle("-fx-text-fill: #98c379; -fx-font-weight: bold;");

        TextArea credits = new TextArea();
        credits.setEditable(false);
        credits.setWrapText(true);
        credits.setPrefHeight(150);
        credits.setStyle("-fx-control-inner-background: #2a2a2a; -fx-text-fill: #cccccc; -fx-background-color: transparent;");
        
        String creditsText = esEspanol ? 
            "Esta herramienta utiliza FFmpeg para el procesamiento de video y generación de assets.\n\n" +
            "FFmpeg es una marca registrada de Fabrice Bellard, originador del proyecto FFmpeg.\n\n" +
            "Sprite Lab es software libre bajo licencia MIT. Hecho para la comunidad de desarrolladores indie." :
            "This tool uses FFmpeg for video processing and asset generation.\n\n" +
            "FFmpeg is a trademark of Fabrice Bellard, originator of the FFmpeg project.\n\n" +
            "Sprite Lab is free software under the MIT license. Made for the indie developer community.";
            
        credits.setText(creditsText);

        Button btnClose = new Button(esEspanol ? "Cerrar" : "Close");
        btnClose.setOnAction(e -> s.close());

        layout.getChildren().addAll(title, dev, credits, btnClose);
        s.setScene(new Scene(layout, 400, 350));
        s.show();
    }
}