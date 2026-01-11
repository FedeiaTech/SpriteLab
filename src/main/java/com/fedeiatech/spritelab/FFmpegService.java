package com.fedeiatech.spritelab;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FFmpegService {

    /**
     * Método CORREGIDO: Busca el ejecutable de forma absoluta.
     * Encuentra la carpeta donde está el .jar corriendo y le añade /bin/ffmpeg.exe
     */
    private File getFFmpegExecutable() {
        try {
            // 1. Obtenemos la ruta absoluta de donde está el archivo JAR (o la clase compilada)
            String jarPath = new File(FFmpegService.class.getProtectionDomain()
                                        .getCodeSource()
                                        .getLocation()
                                        .toURI()).getParent();
            
            // 2. Construimos la ruta hacia bin/ffmpeg.exe
            File executable = new File(jarPath, "bin/ffmpeg.exe");

            // 3. Verificamos si existe en la ruta absoluta (Producción / Instalador)
            if (executable.exists()) {
                return executable;
            } else {
                // 4. Fallback: Si no existe (ej. probando en NetBeans sin build), probamos ruta relativa
                File relative = new File("bin/ffmpeg.exe");
                if (relative.exists()) return relative;
                
                // 5. Último intento: quizás está en una carpeta arriba (entorno desarrollo)
                return new File("../bin/ffmpeg.exe");
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Si todo falla, retornamos el default relativo
            return new File("bin/ffmpeg.exe"); 
        }
    }

    public static class VideoMeta {
        public int width;
        public int height;
        public double duration;
    }

    public VideoMeta obtenerMetadatos(File video) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add(getFFmpegExecutable().getAbsolutePath());
        cmd.add("-i");
        cmd.add(video.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        VideoMeta meta = new VideoMeta();
        Pattern patternDur = Pattern.compile("Duration: (\\d{2}):(\\d{2}):(\\d{2}\\.\\d+)");
        Pattern patternRes = Pattern.compile(", (\\d{2,5})x(\\d{2,5})");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Imprimimos log para depuración si es necesario
                // System.out.println("[FFmpeg Meta]: " + line); 
                
                Matcher mRes = patternRes.matcher(line);
                if (mRes.find()) {
                    meta.width = Integer.parseInt(mRes.group(1));
                    meta.height = Integer.parseInt(mRes.group(2));
                }
                Matcher mDur = patternDur.matcher(line);
                if (mDur.find()) {
                    double horas = Double.parseDouble(mDur.group(1));
                    double min = Double.parseDouble(mDur.group(2));
                    double sec = Double.parseDouble(mDur.group(3));
                    meta.duration = (horas * 3600) + (min * 60) + sec;
                }
            }
        }
        p.waitFor();
        return meta;
    }

    public File generarPreview(File videoInput, int fps, int altura, String colorHex, double tolerancia, double startTime) throws Exception {
        File tempOutput = File.createTempFile("preview_", ".png");
        List<String> cmd = new ArrayList<>();
        cmd.add(getFFmpegExecutable().getAbsolutePath());
        cmd.add("-y");
        cmd.add("-ss"); cmd.add(String.valueOf(startTime));
        cmd.add("-i"); cmd.add(videoInput.getAbsolutePath());
        
        String filtro = construirFiltros(fps, altura, colorHex, tolerancia);
        cmd.add("-vf"); cmd.add(filtro);
        cmd.add("-frames:v"); cmd.add("1");
        cmd.add(tempOutput.getAbsolutePath());

        ejecutarComando(cmd);
        return tempOutput;
    }

    public void exportarSpriteSheet(File videoInput, File output, int fps, int altura, String colorHex, double tolerancia, double startTime, double endTime) throws Exception {
        double duration = endTime - startTime;
        if (duration <= 0.1) duration = 1.0;

        int totalFrames = (int) Math.ceil(duration * fps);
        int lados = (int) Math.ceil(Math.sqrt(totalFrames)); 
        String tileGrid = lados + "x" + lados;

        List<String> cmd = new ArrayList<>();
        cmd.add(getFFmpegExecutable().getAbsolutePath());
        cmd.add("-y");
        cmd.add("-ss"); cmd.add(String.valueOf(startTime));
        cmd.add("-t"); cmd.add(String.valueOf(duration));
        cmd.add("-i"); cmd.add(videoInput.getAbsolutePath());
        
        String filtroCompleto = construirFiltros(fps, altura, colorHex, tolerancia) + ",tile=" + tileGrid;
        cmd.add("-vf"); cmd.add(filtroCompleto);
        cmd.add("-frames:v"); cmd.add("1");
        cmd.add(output.getAbsolutePath());

        ejecutarComando(cmd);
    }

    private String construirFiltros(int fps, int altura, String colorHex, double tol) {
        List<String> f = new ArrayList<>();
        f.add("fps=" + fps);
        f.add("scale=-1:" + altura);
        
        if (colorHex != null && !colorHex.isEmpty()) {
            String t = String.format("%.2f", tol).replace(",", ".");
            f.add("colorkey=" + colorHex + ":" + t + ":0.2");
        }
        return String.join(",", f);
    }

    private void ejecutarComando(List<String> command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[FFmpeg]: " + line);
            }
        }
        if (p.waitFor() != 0) {
            throw new Exception("Error FFmpeg (Exit 1). Revisa la consola para más detalles.");
        }
    }
}