package com.fedeiatech.spritelab;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    @FXML
    private BorderPane rootPane;
    @FXML
    private Button btnCargar, btnPreview, btnExportar, btnVerSheet, btnReset, btnLang, btnVerGif, btnExportarGif, btnAbout;
    @FXML
    private ToggleButton btnGotero, btnZoomLock, btnManualCrop;
    @FXML
    private Label lblArchivoNombre, lblInstrucciones, lblZoomFactor, lblInfoGrilla, lblResolucionTotal, lblDuracionRecorte;
    @FXML
    private Label section1, section2, section3, section4, lblAltura, lblAncho, lblFps, lblTolerancia, lblEstado;
    @FXML
    private Slider sliderFps, sliderTolerancia;
    @FXML
    private TextField txtFps, txtAltura, txtAnchoCalc, txtStart, txtEnd;
    @FXML
    private CheckBox chkChroma;
    @FXML
    private ColorPicker colorPicker;
    @FXML
    private ImageView imgPreview, imgLinkIcon;
    @FXML
    private StackPane previewContainer;
    @FXML
    private Pane selectionPane;
    @FXML
    private ScrollPane scrollPreview;
    @FXML
    private VBox rangeSliderContainer;
    @FXML
    private Button btnImportarSheet;
    @FXML
    private HBox progressBox;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label lblPorcentaje;
    @FXML
    private Tooltip ttInterrumpir;
    @FXML
    private Button btnInterrumpir;

    private final List<File> temporalesCreados = new ArrayList<>();

    private RangeSlider timeRangeSlider;
    private File archivoVideoActual;
    private final FFmpegService ffmpegService = new FFmpegService();
    private FFmpegService.VideoMeta metaDatosActuales;
    private PauseTransition debounceTimer;

    private final List<double[]> listaSegmentos = new ArrayList<>();
    private double currentZoomScale = 1.0;
    private String cropActual = null;
    private Rectangle selectionRect;
    private boolean esEspanol = true;

    private boolean esModoSheet = false;
    private int sheetCols = 1;
    private int sheetRows = 1;

    private boolean procesandoActualmente = false;

    @FXML
    private void onImportarSheet() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File f = fc.showOpenDialog(rootPane.getScene().getWindow());
        if (f != null) {
            mostrarDialogoDimensiones(f);
        }
    }

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
        } catch (Exception e) {
        }

        debounceTimer = new PauseTransition(Duration.millis(500));
        debounceTimer.setOnFinished(e -> onRefrescarPreview());

        // Estado inicial de botones
        btnPreview.setDisable(true);
        btnExportar.setDisable(true);
        btnVerSheet.setDisable(true);
        btnVerGif.setDisable(true);
        btnExportarGif.setDisable(true);
        colorPicker.setValue(Color.WHITE);

        btnManualCrop.disableProperty().bind(btnZoomLock.selectedProperty().not().or(btnReset.disableProperty().not()));
        btnReset.setDisable(true);

        imgPreview.setPreserveRatio(true);
        imgPreview.setSmooth(true);

        // Listeners de Configuración
        txtAltura.textProperty().addListener((o, old, n) -> {
            calcularAnchoProporcional();
            calcularInfoGrilla();
        });

        sliderFps.valueProperty().addListener((o, old, n) -> {
            int fps = n.intValue();
            if (esModoSheet) {
                double ms = 1000.0 / fps;
                lblEstado.setText(String.format(esEspanol
                        ? "MODO: Sheet | %d FPS (%.0f ms/f)"
                        : "MODE: Sheet | %d FPS (%.0f ms/f)", fps, ms));
            }
            calcularInfoGrilla();
            debounceTimer.playFromStart();
        });

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
            btnImportarSheet.setText("🖼️ Importar Sheet");
            section2.setText("2. RECORTE DE TIEMPO");
            section3.setText("3. CONFIGURACIÓN SPRITE");
            lblAltura.setText("Altura (px)");
            lblAncho.setText("Ancho (px)");
            lblDuracionRecorte.setText(esModoSheet ? "(frames)" : "(s)");
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
            if (ttInterrumpir != null) {
                ttInterrumpir.setText("Interrumpir exportación");
            }
        } else {
            btnLang.setText("ES");
            section1.setText("1. SOURCE FILE");
            btnCargar.setText("📂 Load Video");
            btnImportarSheet.setText("🖼️ Import Sheet");
            section2.setText("2. TIME CLIPPING");
            section3.setText("3. SPRITE SETTINGS");
            lblAltura.setText("Height (px)");
            lblAncho.setText("Width (px)");
            lblDuracionRecorte.setText(esModoSheet ? "(frames)" : "(s)");
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
            if (ttInterrumpir != null) {
                ttInterrumpir.setText("Abort export");
            }
        }
        actualizarEstadoLabel();
        calcularInfoGrilla();
    }

    private void configurarSincronizacionFPS() {
        sliderFps.valueProperty().addListener((o, old, n) -> {
            txtFps.setText(String.valueOf(n.intValue()));
        });
    }

    private void configurarZoom() {
        btnZoomLock.selectedProperty().addListener((obs, old, isLocked) -> {
            if (isLocked) {
                btnZoomLock.setText(esEspanol ? "🔒 Ajustar" : "🔒 Fit");
            } else {
                btnZoomLock.setText(esEspanol ? "🔓 Libre" : "🔓 Free");
            }
            aplicarAjusteFit();
            limpiarCapaRecorte();
        });

        scrollPreview.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (!btnZoomLock.isSelected()) {
                event.consume();
                double delta = (event.getDeltaY() > 0) ? 1.1 : 1 / 1.1;
                currentZoomScale = Math.clamp(currentZoomScale * delta, 0.05, 15.0);
                aplicarAjusteFit();
            }
        });
    }

    private void mostrarDialogoDimensiones(File f) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(esEspanol ? "Configurar Cuadrícula" : "Grid Setup");

        Image tempImg = new Image(f.toURI().toString());
        int totalW = (int) tempImg.getWidth();
        int totalH = (int) tempImg.getHeight();

        ImageView imgMiniatura = new ImageView();
        imgMiniatura.setFitHeight(150);
        imgMiniatura.setPreserveRatio(true);

        ProgressIndicator pi = new ProgressIndicator();
        pi.setMaxSize(30, 30);
        pi.setVisible(false);

        StackPane containerMiniatura = new StackPane(imgMiniatura, pi);
        containerMiniatura.setPrefSize(200, 160);
        containerMiniatura.setStyle("-fx-background-color: #000; -fx-border-color: #444;");

        Spinner<Integer> spinCols = new Spinner<>(1, 100, 4);
        Spinner<Integer> spinRows = new Spinner<>(1, 100, 4);
        spinCols.setEditable(true);
        spinRows.setEditable(true);
        spinCols.setPrefWidth(70);
        spinRows.setPrefWidth(70);

        Runnable actualizarPreview = () -> {
            pi.setVisible(true);
            Task<File> t = new Task<>() {
                @Override
                protected File call() throws Exception {
                    return ffmpegService.generarPreview(
                            f, 1, 150, null, 0, 0, null, true,
                            spinCols.getValue(), spinRows.getValue(),
                            totalW, totalH
                    );
                }
            };
            t.setOnSucceeded(ev -> {
                imgMiniatura.setImage(new Image(t.getValue().toURI().toString()));
                pi.setVisible(false);
                t.getValue().deleteOnExit();
            });
            t.setOnFailed(ev -> pi.setVisible(false));
            new Thread(t).start();
        };

        spinCols.valueProperty().addListener((o, old, n) -> actualizarPreview.run());
        spinRows.valueProperty().addListener((o, old, n) -> actualizarPreview.run());

        actualizarPreview.run(); 

        // --- Layout 
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #2a2a2a;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);

        Label lblCols = new Label(esEspanol ? "Columnas (X):" : "Columns (X):");
        lblCols.setTextFill(Color.WHITE);
        Label lblRows = new Label(esEspanol ? "Filas (Y):" : "Rows (Y):");
        lblRows.setTextFill(Color.WHITE);

        grid.add(lblCols, 0, 0);
        grid.add(spinCols, 1, 0);
        grid.add(lblRows, 0, 1);
        grid.add(spinRows, 1, 1);

        Button btnOk = new Button(esEspanol ? "Aceptar" : "Confirm");
        btnOk.setStyle("-fx-background-color: #ffd700; -fx-text-fill: black; -fx-font-weight: bold; -fx-cursor: hand;");
        btnOk.setPrefWidth(120);

        btnOk.setOnAction(e -> {
            sheetCols = spinCols.getValue();
            sheetRows = spinRows.getValue();
            esModoSheet = true;
            configurarInterfazParaSheet(f);
            dialog.close();
        });

        layout.getChildren().addAll(containerMiniatura, grid, btnOk);
        dialog.setScene(new Scene(layout));
        dialog.show();
    }

    private void configurarInterfazParaSheet(File f) {
        limpiarConfiguracion();
        esModoSheet = true;
        archivoVideoActual = f;
        Image fullImg = new Image(f.toURI().toString());

        metaDatosActuales = new FFmpegService.VideoMeta();
        metaDatosActuales.width = (int) fullImg.getWidth();
        metaDatosActuales.height = (int) fullImg.getHeight();
        int totalFrames = sheetCols * sheetRows;
        metaDatosActuales.duration = totalFrames;

        lblDuracionRecorte.setText(esEspanol ? "frames" : "frames");
        lblEstado.setText(esEspanol ? "MODO: Sprite Sheet (" + totalFrames + " f)" : "MODE: Sprite Sheet (" + totalFrames + " f)");

        int altoCelda = metaDatosActuales.height / sheetRows;
        txtAltura.setText(String.valueOf(altoCelda));
        calcularAnchoProporcional();

        lblArchivoNombre.setText(f.getName() + " (Sheet)");
        lblInstrucciones.setVisible(false);

        timeRangeSlider.setMin(0);
        timeRangeSlider.setMax(totalFrames - 1);
        timeRangeSlider.setLowValue(0);
        timeRangeSlider.setHighValue(totalFrames - 1);
        timeRangeSlider.setMajorTickUnit(1);
        timeRangeSlider.setMinorTickCount(0);
        timeRangeSlider.setSnapToTicks(true);

        btnPreview.setDisable(false);
        btnExportar.setDisable(false);
        btnVerSheet.setDisable(false); 
        btnVerGif.setDisable(false);   
        btnExportarGif.setDisable(false); 

        onRefrescarPreview();
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
            if (isSelected) {
                btnGotero.setSelected(false);
            }
        });

        selectionPane.setOnMousePressed(e -> {
            selectionRect.setX(e.getX());
            selectionRect.setY(e.getY());
            selectionRect.setWidth(0);
            selectionRect.setHeight(0);
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
                    Image img = imgPreview.getImage();
                    Bounds b = imgPreview.getBoundsInParent();
                    double sX = img.getWidth() / b.getWidth();
                    double sY = img.getHeight() / b.getHeight();

                    int realW = (int) (selectionRect.getWidth() * sX);
                    int realH = (int) (selectionRect.getHeight() * sY);

                    txtAltura.setText(String.valueOf(realH));
                    txtAnchoCalc.setText(String.valueOf(realW));

                    btnReset.setDisable(false);
                    onRefrescarPreview();
                }
            }
            limpiarCapaRecorte();
            btnManualCrop.setSelected(false);
            rootPane.setCursor(Cursor.DEFAULT);
        });
    }

    private String calcularCropRobusto() {
        Image img = imgPreview.getImage();
        if (img == null) {
            return null;
        }

        Bounds b = imgPreview.getBoundsInParent();

        double sX = img.getWidth() / b.getWidth();
        double sY = img.getHeight() / b.getHeight();

        int x = (int) ((selectionRect.getX() - b.getMinX()) * sX);
        int y = (int) ((selectionRect.getY() - b.getMinY()) * sY);
        int w = (int) (selectionRect.getWidth() * sX);
        int h = (int) (selectionRect.getHeight() * sY);

        x = Math.max(0, x);
        y = Math.max(0, y);

        return String.format("crop=%d:%d:%d:%d", w, h, x, y);
    }

    private void limpiarCapaRecorte() {
        if (selectionRect != null) {
            selectionRect.setVisible(false);
            selectionRect.setWidth(0);
        }
    }

    private void configurarGotero() {
        btnGotero.selectedProperty().addListener((o, old, isSelected) -> {
            if (isSelected) {
                btnManualCrop.setSelected(false);
            }
            rootPane.setCursor(isSelected ? Cursor.CROSSHAIR : Cursor.DEFAULT);
        });

        previewContainer.setOnMouseClicked(e -> {
            if (!btnGotero.isSelected() || imgPreview.getImage() == null) {
                return;
            }
            try {
                Bounds b = imgPreview.getBoundsInParent();
                double sX = imgPreview.getImage().getWidth() / b.getWidth();
                double sY = imgPreview.getImage().getHeight() / b.getHeight();
                int px = (int) ((e.getX() - b.getMinX()) * sX);
                int py = (int) ((e.getY() - b.getMinY()) * sY);
                colorPicker.setValue(imgPreview.getImage().getPixelReader().getColor(px, py));
            } catch (Exception ex) {
            }
            btnGotero.setSelected(false);
            rootPane.setCursor(Cursor.DEFAULT);
            onRefrescarPreview();
        });
    }

    @FXML
    private void onResetCrop() {
        cropActual = null;
        btnReset.setDisable(true);

        if (metaDatosActuales != null) {
            if (esModoSheet) {
                txtAltura.setText(String.valueOf(metaDatosActuales.height / sheetRows));
            } else {
                txtAltura.setText(String.valueOf(metaDatosActuales.height));
            }
        }
        onRefrescarPreview();
    }

    @FXML
    private void onRefrescarPreview() {
        if (archivoVideoActual == null || txtAltura.getText().isEmpty() || procesandoActualmente) {
            return;
        }

        Task<File> task = new Task<>() {
            @Override
            protected File call() throws Exception {
                return ffmpegService.generarPreview(
                        archivoVideoActual, (int) sliderFps.getValue(),
                        Integer.parseInt(txtAltura.getText()),
                        (chkChroma.isSelected() ? getColorHex() : null),
                        sliderTolerancia.getValue(),
                        timeRangeSlider.getLowValue(),
                        cropActual, esModoSheet, sheetCols, sheetRows,
                        metaDatosActuales.width, metaDatosActuales.height
                );
            }
        };

        task.setOnSucceeded(e -> {
            imgPreview.setImage(new Image(task.getValue().toURI().toString()));
            aplicarAjusteFit();
            calcularInfoGrilla();
        });

        new Thread(task).start();
    }

    private void cargarVideo(File file) {
        limpiarConfiguracion();
        esModoSheet = false;
        archivoVideoActual = file;
        lblArchivoNombre.setText(file.getName());
        lblInstrucciones.setVisible(false);

        lblEstado.setText(esEspanol ? "MODO: Video Converter" : "MODE: Video Converter");

        Task<FFmpegService.VideoMeta> task = new Task<>() {
            @Override
            protected FFmpegService.VideoMeta call() throws Exception {
                return ffmpegService.obtenerMetadatos(file);
            }
        };
        task.setOnSucceeded(e -> {
            metaDatosActuales = task.getValue();
            txtAltura.setText(String.valueOf(metaDatosActuales.height));
            timeRangeSlider.setMax(metaDatosActuales.duration);
            timeRangeSlider.setLowValue(0);
            timeRangeSlider.setHighValue(metaDatosActuales.duration);
            btnZoomLock.setSelected(true);
            btnPreview.setDisable(false);
            btnExportar.setDisable(false);
            btnVerSheet.setDisable(false);
            btnVerGif.setDisable(false);
            btnExportarGif.setDisable(false);
            onRefrescarPreview();
        });
        new Thread(task).start();
    }

    private void aplicarAjusteFit() {
        if (imgPreview.getImage() == null) {
            return;
        }

        if (btnZoomLock.isSelected()) {
            imgPreview.fitWidthProperty().bind(previewContainer.widthProperty().subtract(20));
            imgPreview.fitHeightProperty().bind(previewContainer.heightProperty().subtract(20));
            imgPreview.setPreserveRatio(true);
            lblZoomFactor.setText("FIT");
            currentZoomScale = 1.0;
            scrollPreview.setPannable(false);
        } else {
            imgPreview.fitWidthProperty().unbind();
            imgPreview.fitHeightProperty().unbind();
            imgPreview.setFitWidth(imgPreview.getImage().getWidth() * currentZoomScale);
            imgPreview.setFitHeight(imgPreview.getImage().getHeight() * currentZoomScale);
            lblZoomFactor.setText(String.format("%.0f%%", currentZoomScale * 100));
            scrollPreview.setPannable(true);
        }
    }

    private void procesarSheetImportado(File f, int cols, int rows) {

        archivoVideoActual = f;
        lblArchivoNombre.setText(f.getName());
        lblInstrucciones.setVisible(false);

        onRefrescarPreview();
        btnPreview.setDisable(false);
        btnExportar.setDisable(false);
    }

    private void calcularAnchoProporcional() {
        if (metaDatosActuales == null || txtAltura.getText().isEmpty()) {
            return;
        }
        try {
            int hIngresada = Integer.parseInt(txtAltura.getText());
            double ratio;

            if (cropActual != null && selectionRect != null && selectionRect.getHeight() > 0) {
                ratio = selectionRect.getWidth() / selectionRect.getHeight();
            }
            else if (esModoSheet) {
                double cellW = (double) metaDatosActuales.width / sheetCols;
                double cellH = (double) metaDatosActuales.height / sheetRows;
                ratio = cellW / cellH;
            }
            else {
                ratio = (double) metaDatosActuales.width / metaDatosActuales.height;
            }

            txtAnchoCalc.setText(String.valueOf((int) Math.round(hIngresada * ratio)));
        } catch (Exception e) {
        }
    }

    private void calcularInfoGrilla() {
        if (metaDatosActuales == null) {
            return;
        }
        try {
            double dur = timeRangeSlider.getHighValue() - timeRangeSlider.getLowValue();
            int total;

            if (esModoSheet) {
                total = (int) Math.round(dur) + 1;
            } else {
                total = (int) Math.ceil(dur * (int) sliderFps.getValue());
            }

            int col = (int) Math.ceil(Math.sqrt(total));
            int filas = (int) Math.ceil((double) total / col);
            int h = Integer.parseInt(txtAltura.getText());
            int w = Integer.parseInt(txtAnchoCalc.getText());

            String labelGrilla = esEspanol
                    ? String.format("CUADROS: %d | GRILLA: %dx%d", total, col, filas)
                    : String.format("FRAMES: %d | GRID: %dx%d", total, col, filas);

            lblInfoGrilla.setText(labelGrilla);
            lblResolucionTotal.setText(String.format("%d x %d px", col * w, filas * h));

            actualizarEstadoLabel();

        } catch (Exception e) {
        }
    }

    private void actualizarEstadoLabel() {
        if (archivoVideoActual == null) {
            return;
        }

        if (esModoSheet) {
            int frames = (int) (timeRangeSlider.getHighValue() - timeRangeSlider.getLowValue() + 1);
            lblEstado.setText(esEspanol
                    ? "MODO: Sprite Sheet (" + frames + " f)"
                    : "MODE: Sprite Sheet (" + frames + " f)");
        } else {
            lblEstado.setText(esEspanol
                    ? "MODO: Video Converter"
                    : "MODE: Video Converter");
        }
    }

    private void corregirEstilosVisuales() {
        lblInstrucciones.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold; -fx-font-size: 20px;");
    }

    private void crearFondoAjedrez() {
        int sz = 20;
        WritableImage p = new WritableImage(sz * 2, sz * 2);
        PixelWriter pw = p.getPixelWriter();
        for (int x = 0; x < sz * 2; x++) {
            for (int y = 0; y < sz * 2; y++) {
                pw.setColor(x, y, ((x < sz && y < sz) || (x >= sz && y >= sz)) ? Color.WHITE : Color.web("#cccccc"));
            }
        }
        previewContainer.setBackground(new Background(new BackgroundFill(new ImagePattern(p, 0, 0, sz * 2, sz * 2, false), null, null)));
    }

    private void configurarDragAndDrop() {
        rootPane.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
            }
            e.consume();
        });
        rootPane.setOnDragDropped(e -> {
            if (e.getDragboard().hasFiles()) {
                cargarVideo(e.getDragboard().getFiles().get(0));
                e.setDropCompleted(true);
            }
            e.consume();
        });
    }

    private void inicializarRangeSlider() {
        timeRangeSlider = new RangeSlider(0, 100, 0, 100);
        rangeSliderContainer.getChildren().add(timeRangeSlider);

        timeRangeSlider.lowValueProperty().addListener((o, old, n) -> {
            if (esModoSheet) {
                txtStart.setText(String.valueOf(n.intValue()));
            } else {
                txtStart.setText(String.format(Locale.US, "%.2f", n.doubleValue()));
            }
            calcularInfoGrilla();
            debounceTimer.playFromStart();
        });

        timeRangeSlider.highValueProperty().addListener((o, old, n) -> {
            if (esModoSheet) {
                txtEnd.setText(String.valueOf(n.intValue()));
            } else {
                txtEnd.setText(String.format(Locale.US, "%.2f", n.doubleValue()));
            }
            calcularInfoGrilla();
        });
    }

    @FXML
    private void onCargarVideo() {
        FileChooser fc = new FileChooser();
        File file = fc.showOpenDialog(rootPane.getScene().getWindow());
        if (file != null) {
            cargarVideo(file);
        }
    }

    private String getColorHex() {
        Color c = colorPicker.getValue();
        return String.format("0x%02X%02X%02X", (int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255));
    }

    @FXML
    private void onVerMiniatura() {
        if (archivoVideoActual == null || metaDatosActuales == null || procesandoActualmente) {
            return;
        }

        deshabilitarBotones(true);
        progressBox.setVisible(true);
        btnVerSheet.setText("⏳...");
        rootPane.getScene().setCursor(Cursor.WAIT);

        Task<File> t = new Task<>() {
            @Override
            protected File call() throws Exception {
                File f = File.createTempFile("sheet_preview", ".png");
                temporalesCreados.add(f);
                int hPreview = Math.min(Integer.parseInt(txtAltura.getText()), 600);
                ffmpegService.exportarSpriteSheet(
                        archivoVideoActual, f, (int) sliderFps.getValue(), hPreview,
                        (chkChroma.isSelected() ? getColorHex() : null), sliderTolerancia.getValue(),
                        timeRangeSlider.getLowValue(), timeRangeSlider.getHighValue(),
                        cropActual, esModoSheet, sheetCols, sheetRows,
                        metaDatosActuales.width, metaDatosActuales.height,
                        p -> updateProgress(p, 1.0), true
                );
                return f;
            }
        };

        progressBar.progressProperty().bind(t.progressProperty());
        t.setOnSucceeded(e -> {
            mostrarVentanaSheet(t.getValue());
            finalizarTask(true, null);
        });
        t.setOnFailed(e -> finalizarTask(false, null));
        new Thread(t).start();
    }

    @FXML
    private void onVerGif() {
        if (archivoVideoActual == null || metaDatosActuales == null || procesandoActualmente) {
            return;
        }

        deshabilitarBotones(true);
        progressBox.setVisible(true);
        btnVerGif.setText("⏳...");
        rootPane.getScene().setCursor(Cursor.WAIT);

        Task<File> t = new Task<>() {
            @Override
            protected File call() throws Exception {
                File f = File.createTempFile("preview", ".gif");
                temporalesCreados.add(f);
                int previewHeight = Math.min(Integer.parseInt(txtAltura.getText()), 400);

                ffmpegService.generarGif(
                        archivoVideoActual, f, (int) sliderFps.getValue(), previewHeight,
                        timeRangeSlider.getLowValue(), timeRangeSlider.getHighValue(),
                        null,
                        sliderTolerancia.getValue(), cropActual, esModoSheet, sheetCols, sheetRows,
                        metaDatosActuales.width, metaDatosActuales.height,
                        p -> updateProgress(p, 1.0), true
                );
                return f;
            }
        };

        progressBar.progressProperty().bind(t.progressProperty());
        t.progressProperty().addListener((obs, old, val) -> {
            if (val != null && val.doubleValue() >= 0) {
                lblPorcentaje.setText(String.format("%.0f%%", val.doubleValue() * 100));
            }
        });

        t.setOnSucceeded(e -> {
            mostrarVentanaSheet(t.getValue());
            finalizarTask(true, null);
        });

        t.setOnFailed(e -> {
            Throwable ex = t.getException();
            String msgError = (ex != null) ? ex.getMessage() : "Unknown error";
            finalizarTask(false, msgError);
        });

        new Thread(t).start();
    }

    private String archivoVideoOriginalName() {
        if (archivoVideoActual == null) {
            return "output";
        }
        String n = archivoVideoActual.getName();
        return n.contains(".") ? n.substring(0, n.lastIndexOf(".")) : n;
    }

    @FXML
    private void onExportarGif() {
        if (archivoVideoActual == null || metaDatosActuales == null) {
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setInitialFileName(archivoVideoOriginalName() + "_anim.gif");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("GIF", "*.gif"));
        File dest = fc.showSaveDialog(rootPane.getScene().getWindow());

        if (dest != null) {
            btnExportarGif.setText("⏳...");
            Task<Void> t = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    ffmpegService.generarGif(
                            archivoVideoActual, dest, (int) sliderFps.getValue(), Integer.parseInt(txtAltura.getText()),
                            timeRangeSlider.getLowValue(), timeRangeSlider.getHighValue(),
                            null, sliderTolerancia.getValue(), cropActual, esModoSheet,
                            sheetCols, sheetRows, metaDatosActuales.width, metaDatosActuales.height,
                            p -> updateProgress(p, 1.0), false
                    );
                    return null;
                }
            };

            configurarComportamientoTask(t, esEspanol ? "GIF guardado" : "GIF saved");
            new Thread(t).start();
        }
    }

    @FXML
    private void onExportar() {
        if (archivoVideoActual == null || metaDatosActuales == null || procesandoActualmente) {
            return;
        }

        String nombreBase = archivoVideoOriginalName();
        String nombreSugerido = String.format("%s_sheet_%sx%s.png", nombreBase, txtAnchoCalc.getText(), txtAltura.getText());

        FileChooser fc = new FileChooser();
        fc.setInitialFileName(nombreSugerido);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        File dest = fc.showSaveDialog(rootPane.getScene().getWindow());

        if (dest != null) {
            Task<Void> t = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    ffmpegService.exportarSpriteSheet(
                            archivoVideoActual, dest, (int) sliderFps.getValue(), Integer.parseInt(txtAltura.getText()),
                            (chkChroma.isSelected() ? getColorHex() : null), sliderTolerancia.getValue(),
                            timeRangeSlider.getLowValue(), timeRangeSlider.getHighValue(),
                            cropActual, esModoSheet, sheetCols, sheetRows,
                            metaDatosActuales.width, metaDatosActuales.height,
                            p -> updateProgress(p, 1.0), false
                    );
                    return null;
                }
            };
            configurarComportamientoTask(t, esEspanol ? "PNG Exportado con éxito" : "PNG Exported successfully");
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

        String creditsText = esEspanol
                ? "Esta herramienta utiliza FFmpeg para el procesamiento de video y generación de assets.\n\n"
                + "FFmpeg es una marca registrada de Fabrice Bellard, originador del proyecto FFmpeg.\n\n"
                + "Sprite Lab es software libre bajo licencia MIT. Hecho para la comunidad de desarrolladores indie."
                : "This tool uses FFmpeg for video processing and asset generation.\n\n"
                + "FFmpeg is a trademark of Fabrice Bellard, originator of the FFmpeg project.\n\n"
                + "Sprite Lab is free software under the MIT license. Made for the indie developer community.";

        credits.setText(creditsText);

        Button btnClose = new Button(esEspanol ? "Cerrar" : "Close");
        btnClose.setOnAction(e -> s.close());

        layout.getChildren().addAll(title, dev, credits, btnClose);
        s.setScene(new Scene(layout, 400, 350));
        s.show();
    }

    @FXML
    private void onDetenerProceso() {
        ffmpegService.detenerProcesoActivo();
        lblInfoGrilla.setText(esEspanol ? "Cancelando..." : "Cancelling...");
        btnInterrumpir.setDisable(true);
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

    private void limpiarConfiguracion() {
        cropActual = null;
        currentZoomScale = 1.0;

        if (btnManualCrop != null) {
            btnManualCrop.setSelected(false);
        }
        if (btnZoomLock != null) {
            btnZoomLock.setSelected(true);
        }
        if (btnReset != null) {
            btnReset.setDisable(true);
        }
        limpiarCapaRecorte();

        chkChroma.setSelected(false);
        colorPicker.setValue(Color.WHITE);
        sliderTolerancia.setValue(0.15);

        txtAnchoCalc.setText("");
    }

    private void configurarComportamientoTask(Task<?> t, String mensajeExito) {
        progressBar.progressProperty().bind(t.progressProperty());

        t.progressProperty().addListener((obs, old, val) -> {
            if (val != null && val.doubleValue() >= 0) {
                lblPorcentaje.setText(String.format("%.0f%%", val.doubleValue() * 100));
            }
        });

        progressBox.setVisible(true);
        deshabilitarBotones(true);

        t.setOnSucceeded(e -> finalizarTask(true, mensajeExito));
        t.setOnFailed(e -> {
            t.getException().printStackTrace();
            finalizarTask(false, (esEspanol ? "Error: " : "Error: ") + t.getException().getMessage());
        });
    }

    private void finalizarTask(boolean exito, String mensaje) {
        rootPane.getScene().setCursor(Cursor.DEFAULT);
        progressBar.progressProperty().unbind();
        progressBar.setProgress(0);
        lblPorcentaje.setText("0%");
        progressBox.setVisible(false);

        if (btnInterrumpir != null) {
            btnInterrumpir.setDisable(false);
        }

        deshabilitarBotones(false);

        calcularInfoGrilla();

        btnVerSheet.setText(esEspanol ? "👁 Ver Sheet" : "👁 View Sheet");
        btnVerGif.setText(esEspanol ? "🎬 Previa GIF" : "🎬 Preview GIF");
        btnExportarGif.setText(esEspanol ? "🎁 GIF" : "🎁 GIF");

        if (mensaje != null && !mensaje.isEmpty()) {
            if (mensaje.contains("FFmpeg") && !exito) {
                mensaje = esEspanol ? "Proceso interrumpido por el usuario." : "Process interrupted by user.";
            }

            Alert alert = new Alert(exito ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING);
            alert.setTitle("Sprite Lab");
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.show();
        }
    }

    private void deshabilitarBotones(boolean deshabilitar) {
        this.procesandoActualmente = deshabilitar;

        btnExportar.setDisable(deshabilitar);
        btnExportarGif.setDisable(deshabilitar);
        btnVerSheet.setDisable(deshabilitar);
        btnVerGif.setDisable(deshabilitar);
        btnCargar.setDisable(deshabilitar);
        btnImportarSheet.setDisable(deshabilitar);
        btnPreview.setDisable(deshabilitar);

        sliderFps.setDisable(deshabilitar);
        timeRangeSlider.setDisable(deshabilitar);
    }

    public void limpiarTemporales() {
        for (File f : temporalesCreados) {
            if (f.exists()) {
                f.delete();
            }
        }
        temporalesCreados.clear();
    }
}
