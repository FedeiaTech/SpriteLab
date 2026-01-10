package com.fedeiatech.spritelab;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FFmpegService {

    private File getFFmpegExecutable() {
        String[] possiblePaths = { "./bin/ffmpeg.exe", "bin/ffmpeg.exe", "../bin/ffmpeg.exe", "ffmpeg.exe" };
        for (String path : possiblePaths) {
            File f = new File(path);
            if (f.exists()) return f;
        }
        return new File("bin/ffmpeg.exe"); 
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

    // AHORA ACEPTA 'colorHex' (ej: "0xFFFFFF" o null si no se usa)
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
        
        // Si colorHex no es nulo, aplicamos colorkey
        if (colorHex != null && !colorHex.isEmpty()) {
            String t = String.format("%.2f", tol).replace(",", ".");
            // colorHex debe venir como "0xFFFFFF" o "white"
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
            throw new Exception("Error FFmpeg (Exit 1).");
        }
    }
}