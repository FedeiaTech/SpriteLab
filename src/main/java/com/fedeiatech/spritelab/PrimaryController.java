package com.fedeiatech.spritelab;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*; 
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.controlsfx.control.RangeSlider;

public class PrimaryController implements Initializable {

    @FXML private BorderPane rootPane;
    @FXML private Button btnCargar, btnPreview, btnExportar, btnVerSheet;
    @FXML private ToggleButton btnGotero, btnZoomLock;
    @FXML private Label lblArchivoNombre, lblInstrucciones;
    @FXML private Slider sliderFps, sliderTolerancia;
    @FXML private TextField txtFps, txtAltura, txtAnchoCalc, txtStart, txtEnd;
    @FXML private CheckBox chkChroma;
    @FXML private ColorPicker colorPicker;
    @FXML private ImageView imgPreview;
    @FXML private StackPane previewContainer; // <--- AQUÍ APLICAREMOS EL AJEDREZ
    @FXML private ScrollPane scrollPreview;
    @FXML private Label lblZoomFactor;
    
    @FXML private VBox rangeSliderContainer;
    @FXML private Label lblDuracionRecorte;
    @FXML private Label lblInfoGrilla, lblResolucionTotal;

    private RangeSlider timeRangeSlider;
    private File archivoVideoActual;
    private final FFmpegService ffmpegService = new FFmpegService();
    private FFmpegService.VideoMeta metaDatosActuales;
    private PauseTransition debounceTimer;

    private double currentZoomScale = 1.0;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // 1. FORZAR ESTILOS VISUALES (Corrección crítica de texto)
        corregirEstilosVisuales();
        
        // 2. GENERAR FONDO DE AJEDREZ
        crearFondoAjedrez();

        configurarDragAndDrop();
        configurarSincronizacionFPS();
        inicializarRangeSlider();
        configurarZoomYPaneo(); 
        configurarGotero();     

        debounceTimer = new PauseTransition(Duration.millis(500));
        debounceTimer.setOnFinished(e -> onRefrescarPreview());

        btnPreview.setDisable(true);
        btnExportar.setDisable(true);
        btnVerSheet.setDisable(true);
        colorPicker.setValue(Color.WHITE);

        // Listeners
        txtAltura.textProperty().addListener((obs, oldVal, newVal) -> {
            calcularAnchoProporcional();
            calcularInfoGrilla();
        });
        sliderFps.valueProperty().addListener(o -> {
            calcularInfoGrilla();
            debounceTimer.playFromStart();
        });
        sliderTolerancia.valueProperty().addListener(o -> debounceTimer.playFromStart());
        colorPicker.valueProperty().addListener(o -> debounceTimer.playFromStart());
        
        chkChroma.selectedProperty().addListener(o -> {
             boolean activo = chkChroma.isSelected();
             colorPicker.setDisable(!activo);
             btnGotero.setDisable(!activo);
             sliderTolerancia.setDisable(!activo);
             onRefrescarPreview();
        });
    }

    private void corregirEstilosVisuales() {
        // --- CORRECCIÓN DEFINITIVA DEL COLOR DE TEXTO ---
        // Usamos setStyle en lugar de setTextFill porque el CSS en línea tiene mayor prioridad 
        // que la hoja de estilos externa (styles.css).
        
        lblInstrucciones.setStyle(
            "-fx-text-fill: #333333;" +  // Color Gris Muy Oscuro (Casi Negro)
            "-fx-font-weight: bold;" +
            "-fx-font-size: 20px;" +
            // Sombra blanca sólida alrededor para garantizar lectura en fondo oscuro
            "-fx-effect: dropshadow(three-pass-box, rgba(255,255,255,0.9), 5, 0.8, 0, 0);" 
        );
        
        // Forzar transparencia del ScrollPane para que se vea el fondo ajedrez
        scrollPreview.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
    }

    private void crearFondoAjedrez() {
        int size = 20; // Tamaño del cuadro
        WritableImage pattern = new WritableImage(size * 2, size * 2);
        PixelWriter pw = pattern.getPixelWriter();
        
        Color c1 = Color.web("#ffffff"); // Blanco puro
        Color c2 = Color.web("#cccccc"); // Gris visible
        
        for (int x = 0; x < size * 2; x++) {
            for (int y = 0; y < size * 2; y++) {
                boolean isWhite = (x < size && y < size) || (x >= size && y >= size);
                pw.setColor(x, y, isWhite ? c1 : c2);
            }
        }
        
        // APLICAR AL CONTENEDOR INTERNO
        if (previewContainer != null) {
            ImagePattern imgPattern = new ImagePattern(pattern, 0, 0, size * 2, size * 2, false);
            previewContainer.setBackground(new Background(new BackgroundFill(imgPattern, null, null)));
        }
    }

    private void configurarZoomYPaneo() {
        btnZoomLock.selectedProperty().addListener((obs, wasLocked, isLocked) -> {
            if (isLocked) {
                btnZoomLock.setText("🔒 Ajustar");
                imgPreview.fitWidthProperty().bind(previewContainer.widthProperty().subtract(20));
                imgPreview.fitHeightProperty().bind(previewContainer.heightProperty().subtract(20));
                imgPreview.setScaleX(1);
                imgPreview.setScaleY(1);
                scrollPreview.setPannable(false);
                lblZoomFactor.setText("FIT");
                currentZoomScale = 1.0;
            } else {
                btnZoomLock.setText("🔓 Libre");
                imgPreview.fitWidthProperty().unbind();
                imgPreview.fitHeightProperty().unbind();
                if (imgPreview.getImage() != null) {
                    imgPreview.setFitWidth(imgPreview.getImage().getWidth());
                    imgPreview.setFitHeight(imgPreview.getImage().getHeight());
                }
                scrollPreview.setPannable(true);
                lblZoomFactor.setText("100%");
            }
        });

        previewContainer.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (!btnZoomLock.isSelected()) { 
                event.consume(); 
                double delta = event.getDeltaY();
                double zoomFactor = 1.1;
                
                if (delta < 0) currentZoomScale /= zoomFactor;
                else currentZoomScale *= zoomFactor;
                
                if (currentZoomScale < 0.05) currentZoomScale = 0.05;
                if (currentZoomScale > 20.0) currentZoomScale = 20.0;

                if (imgPreview.getImage() != null) {
                    imgPreview.setFitWidth(imgPreview.getImage().getWidth() * currentZoomScale);
                    imgPreview.setFitHeight(imgPreview.getImage().getHeight() * currentZoomScale);
                }
                lblZoomFactor.setText(String.format("%.0f%%", currentZoomScale * 100));
            }
        });
        
        imgPreview.fitWidthProperty().bind(previewContainer.widthProperty().subtract(20));
        imgPreview.fitHeightProperty().bind(previewContainer.heightProperty().subtract(20));
    }

    private void configurarGotero() {
        btnGotero.setOnAction(e -> {
            if (btnGotero.isSelected()) rootPane.setCursor(Cursor.CROSSHAIR); 
            else rootPane.setCursor(Cursor.DEFAULT);
        });

        imgPreview.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (btnGotero.isSelected()) {
                Image img = imgPreview.getImage();
                if (img != null) {
                    double displayW = imgPreview.getLayoutBounds().getWidth();
                    double displayH = imgPreview.getLayoutBounds().getHeight();
                    double actualW = img.getWidth();
                    double actualH = img.getHeight();
                    
                    double xRatio = e.getX() / displayW;
                    double yRatio = e.getY() / displayH;
                    
                    int pixelX = (int) (xRatio * actualW);
                    int pixelY = (int) (yRatio * actualH);
                    
                    if (pixelX >= 0 && pixelX < actualW && pixelY >= 0 && pixelY < actualH) {
                        try {
                            PixelReader pr = img.getPixelReader();
                            Color color = pr.getColor(pixelX, pixelY);
                            colorPicker.setValue(color);
                            btnGotero.setSelected(false);
                            rootPane.setCursor(Cursor.DEFAULT);
                            onRefrescarPreview();
                        } catch (Exception ex) {}
                    }
                }
            }
        });
    }

    private void inicializarRangeSlider() {
        timeRangeSlider = new RangeSlider(0, 100, 0, 100);
        timeRangeSlider.setShowTickMarks(true);
        timeRangeSlider.setShowTickLabels(false);
        rangeSliderContainer.getChildren().add(timeRangeSlider);
        
        timeRangeSlider.lowValueProperty().addListener((obs, oldVal, newVal) -> {
            if (!txtStart.isFocused()) txtStart.setText(String.format("%.2f", newVal.doubleValue()).replace(",", "."));
            calcularInfoGrilla();
            debounceTimer.playFromStart();
        });
        timeRangeSlider.highValueProperty().addListener((obs, oldVal, newVal) -> {
            if (!txtEnd.isFocused()) txtEnd.setText(String.format("%.2f", newVal.doubleValue()).replace(",", "."));
            calcularInfoGrilla();
        });
    }

    private void configurarDragAndDrop() {
        rootPane.setOnDragOver((DragEvent event) -> {
            if (event.getDragboard().hasFiles()) event.acceptTransferModes(TransferMode.COPY);
            event.consume();
        });
        rootPane.setOnDragDropped((DragEvent event) -> {
            boolean success = false;
            if (event.getDragboard().hasFiles()) {
                List<File> files = event.getDragboard().getFiles();
                if (!files.isEmpty()) {
                    File file = files.get(0);
                    if (file.getName().toLowerCase().endsWith(".mp4") || file.getName().toLowerCase().endsWith(".avi")) {
                        cargarVideo(file);
                        success = true;
                    }
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void configurarSincronizacionFPS() {
        sliderFps.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!txtFps.isFocused()) txtFps.setText(String.format("%d", newVal.intValue()));
        });
        txtFps.setOnAction(e -> {
            try {
                double val = Double.parseDouble(txtFps.getText());
                sliderFps.setValue(val);
            } catch (NumberFormatException ex) {
                txtFps.setText(String.format("%d", (int)sliderFps.getValue()));
            }
            rootPane.requestFocus(); 
        });
    }

    @FXML
    private void onCargarVideo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Abrir Video");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Videos", "*.mp4", "*.avi", "*.mov"));
        File file = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
        if (file != null) {
            cargarVideo(file);
        }
    }

    private void cargarVideo(File file) {
        archivoVideoActual = file;
        lblArchivoNombre.setText(file.getName());
        lblInstrucciones.setVisible(false);
        
        Task<FFmpegService.VideoMeta> task = new Task<>() {
            @Override protected FFmpegService.VideoMeta call() throws Exception {
                return ffmpegService.obtenerMetadatos(file);
            }
        };
        
        task.setOnSucceeded(e -> {
            metaDatosActuales = task.getValue();
            txtAltura.setText(String.valueOf(metaDatosActuales.height));
            double duracion = metaDatosActuales.duration;
            timeRangeSlider.setMin(0);
            timeRangeSlider.setMax(duracion);
            timeRangeSlider.setLowValue(0);
            timeRangeSlider.setHighValue(duracion);
            timeRangeSlider.setMajorTickUnit(duracion > 60 ? 10 : 1);
            txtStart.setText("0.0");
            txtEnd.setText(String.format("%.2f", duracion).replace(",", "."));
            
            calcularAnchoProporcional();
            calcularInfoGrilla();
            btnPreview.setDisable(false);
            btnExportar.setDisable(false);
            btnVerSheet.setDisable(false);
            onRefrescarPreview();
        });
        new Thread(task).start();
    }

    private void calcularAnchoProporcional() {
        if (metaDatosActuales == null) return;
        try {
            int nuevaAltura = Integer.parseInt(txtAltura.getText());
            double ratio = (double) metaDatosActuales.width / metaDatosActuales.height;
            int nuevoAncho = (int) (nuevaAltura * ratio);
            txtAnchoCalc.setText(nuevoAncho + " px");
        } catch (NumberFormatException e) {
            txtAnchoCalc.setText("Err");
        }
    }
    
    private void calcularInfoGrilla() {
        if (metaDatosActuales == null) return;
        try {
            double start = timeRangeSlider.getLowValue();
            double end = timeRangeSlider.getHighValue();
            double duracion = end - start;
            if (duracion < 0) duracion = 0;
            lblDuracionRecorte.setText(String.format("(Duración: %.2fs)", duracion));

            int fps = (int) sliderFps.getValue();
            int totalFrames = (int) Math.ceil(duracion * fps);
            int columnas = (int) Math.ceil(Math.sqrt(totalFrames));
            int filas = (int) Math.ceil((double)totalFrames / columnas);
            int alturaSprite = Integer.parseInt(txtAltura.getText());
            double ratio = (double) metaDatosActuales.width / metaDatosActuales.height;
            int anchoSprite = (int) (alturaSprite * ratio);
            int anchoFinal = columnas * anchoSprite;
            int altoFinal = filas * alturaSprite;

            lblInfoGrilla.setText(String.format("FRAMES: %d  |  GRILLA: %dx%d", totalFrames, columnas, filas));
            lblResolucionTotal.setText(String.format("IMAGEN FINAL: %d x %d px", anchoFinal, altoFinal));
            
            if (anchoFinal > 8192 || altoFinal > 8192) {
                lblResolucionTotal.setStyle("-fx-text-fill: #e06c75; -fx-font-weight: bold;"); 
                lblResolucionTotal.setText(lblResolucionTotal.getText() + " (⚠ MUY GRANDE)");
            } else {
                lblResolucionTotal.setStyle("-fx-text-fill: #e0e0e0; -fx-font-weight: bold;");
            }
        } catch (Exception e) {}
    }

    private String getColorHex() {
        if (chkChroma.isSelected()) {
            Color c = colorPicker.getValue();
            int r = (int) (c.getRed() * 255);
            int g = (int) (c.getGreen() * 255);
            int b = (int) (c.getBlue() * 255);
            return String.format("0x%02X%02X%02X", r, g, b);
        }
        return null;
    }

    @FXML
    private void onRefrescarPreview() {
        if (archivoVideoActual == null) return;

        int fps = (int) sliderFps.getValue();
        int altura;
        try { altura = Integer.parseInt(txtAltura.getText()); } 
        catch (NumberFormatException e) { return; }
        
        double start = timeRangeSlider.getLowValue();
        double tol = sliderTolerancia.getValue();
        String colorHex = getColorHex();

        Task<File> task = new Task<>() {
            @Override protected File call() throws Exception {
                return ffmpegService.generarPreview(archivoVideoActual, fps, altura, colorHex, tol, start);
            }
        };

        task.setOnSucceeded(e -> {
            Image img = new Image(task.getValue().toURI().toString());
            imgPreview.setImage(img);
            if (!btnZoomLock.isSelected()) {
                imgPreview.setFitWidth(img.getWidth() * currentZoomScale);
                imgPreview.setFitHeight(img.getHeight() * currentZoomScale);
            }
            btnPreview.setText("🔄 Frame");
        });
        btnPreview.setText("⏳ ...");
        new Thread(task).start();
    }

    @FXML
    private void onVerMiniatura() {
        if (archivoVideoActual == null) return;
        
        Task<File> task = new Task<>() {
            @Override protected File call() throws Exception {
                File temp = File.createTempFile("sheet_preview", ".png");
                int fps = (int) sliderFps.getValue();
                int altura = Integer.parseInt(txtAltura.getText());
                double tol = sliderTolerancia.getValue();
                double start = timeRangeSlider.getLowValue();
                double end = timeRangeSlider.getHighValue();
                String colorHex = getColorHex();
                
                ffmpegService.exportarSpriteSheet(archivoVideoActual, temp, fps, altura, colorHex, tol, start, end);
                return temp;
            }
        };
        
        task.setOnSucceeded(e -> {
            btnVerSheet.setText("👁 Ver Sheet");
            try {
                Stage stage = new Stage();
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setTitle("Vista Previa de Hoja de Sprites");
                
                ImageView iv = new ImageView(new Image(task.getValue().toURI().toString()));
                iv.setPreserveRatio(true);
                iv.setFitWidth(800);
                iv.setFitHeight(600);
                
                StackPane modalPane = new StackPane(iv);
                
                // Fondo ajedrez para modal
                int size = 20;
                WritableImage pattern = new WritableImage(size * 2, size * 2);
                PixelWriter pw = pattern.getPixelWriter();
                Color c1 = Color.web("#ffffff");
                Color c2 = Color.web("#cccccc");
                for (int x = 0; x < size * 2; x++) {
                    for (int y = 0; y < size * 2; y++) {
                        boolean isWhite = (x < size && y < size) || (x >= size && y >= size);
                        pw.setColor(x, y, isWhite ? c1 : c2);
                    }
                }
                ImagePattern imgPattern = new ImagePattern(pattern, 0, 0, size * 2, size * 2, false);
                modalPane.setBackground(new Background(new BackgroundFill(imgPattern, null, null)));

                ScrollPane sp = new ScrollPane(modalPane);
                sp.setFitToWidth(true);
                sp.setFitToHeight(true);
                
                Scene scene = new Scene(sp, 900, 700);
                stage.setScene(scene);
                stage.show();
            } catch (Exception ex) { ex.printStackTrace(); }
        });
        
        task.setOnFailed(e -> btnVerSheet.setText("❌ Error"));
        btnVerSheet.setText("⏳ Generando...");
        new Thread(task).start();
    }

    @FXML
    private void onExportar() {
        if (archivoVideoActual == null) return;
        
        int alturaFrame = Integer.parseInt(txtAltura.getText());
        String anchoTexto = txtAnchoCalc.getText().replace(" px", "").replace("Err", "");
        String dimStr = anchoTexto + "x" + alturaFrame; 
        
        String nombreBase = archivoVideoActual.getName();
        if (nombreBase.contains(".")) nombreBase = nombreBase.substring(0, nombreBase.lastIndexOf('.'));
        String nombreSugerido = nombreBase + "_sheet_" + dimStr + ".png";

        FileChooser fc = new FileChooser();
        fc.setInitialFileName(nombreSugerido);
        File dest = fc.showSaveDialog(rootPane.getScene().getWindow());
        
        if (dest != null) {
            int fps = (int) sliderFps.getValue();
            double tol = sliderTolerancia.getValue();
            double start = timeRangeSlider.getLowValue();
            double end = timeRangeSlider.getHighValue();
            String colorHex = getColorHex();

            Task<Void> task = new Task<>() {
                @Override protected Void call() throws Exception {
                    ffmpegService.exportarSpriteSheet(archivoVideoActual, dest, fps, alturaFrame, colorHex, tol, start, end);
                    return null;
                }
            };
            task.setOnSucceeded(e -> {
                 new Alert(Alert.AlertType.INFORMATION, "¡Exportación completada!").show();
                 btnExportar.setText("🚀 EXPORTAR PNG");
            });
            task.setOnFailed(e -> {
                 new Alert(Alert.AlertType.ERROR, "Error: " + task.getException().getMessage()).show();
                 btnExportar.setText("🚀 EXPORTAR PNG");
            });
            btnExportar.setText("⏳ Renderizando...");
            new Thread(task).start();
        }
    }
}