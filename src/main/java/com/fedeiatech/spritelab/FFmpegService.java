package com.fedeiatech.spritelab;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FFmpegService {

    private File getFFmpegExecutable() {
        try {
            String jarPath = new File(FFmpegService.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
            File executable = new File(jarPath, "bin/ffmpeg.exe");
            if (executable.exists()) return executable;
            File relative = new File("bin/ffmpeg.exe");
            return relative.exists() ? relative : new File("../bin/ffmpeg.exe");
        } catch (Exception e) { return new File("bin/ffmpeg.exe"); }
    }

    public static class VideoMeta { public int width, height; public double duration; }

    public VideoMeta obtenerMetadatos(File video) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add(getFFmpegExecutable().getAbsolutePath()); cmd.add("-i"); cmd.add(video.getAbsolutePath());
        ProcessBuilder pb = new ProcessBuilder(cmd); pb.redirectErrorStream(true); Process p = pb.start();
        VideoMeta meta = new VideoMeta();
        Pattern pDur = Pattern.compile("Duration: (\\d{2}):(\\d{2}):(\\d{2}\\.\\d+)"), pRes = Pattern.compile(", (\\d{2,5})x(\\d{2,5})");
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String l; while ((l = r.readLine()) != null) {
                Matcher mRes = pRes.matcher(l); if (mRes.find()) { meta.width = Integer.parseInt(mRes.group(1)); meta.height = Integer.parseInt(mRes.group(2)); }
                Matcher mDur = pDur.matcher(l); if (mDur.find()) { meta.duration = Double.parseDouble(mDur.group(1))*3600 + Double.parseDouble(mDur.group(2))*60 + Double.parseDouble(mDur.group(3)); }
            }
        }
        p.waitFor(); return meta;
    }

    public File generarPreview(File v, int fps, int h, String c, double t, double s, String cr) throws Exception {
    File out = File.createTempFile("preview_", ".png");
    
    // Para la previa, creamos un segmento ficticio que solo contenga el punto actual 's'
    // Esto asegura que se use el mismo motor de construcción de filtros
    List<double[]> segmentoPrevia = new ArrayList<>();
    segmentoPrevia.add(new double[]{s, s + 0.1}); 

    List<String> cmd = new ArrayList<>();
    cmd.add(getFFmpegExecutable().getAbsolutePath()); 
    cmd.add("-y"); 
    cmd.add("-ss"); cmd.add(String.format(Locale.US, "%.3f", s)); 
    cmd.add("-i"); cmd.add(v.getAbsolutePath());
    cmd.add("-vf"); 
    cmd.add(construirFiltros(fps, h, c, t, cr, segmentoPrevia)); // Usar la versión de la lista
    cmd.add("-frames:v"); cmd.add("1"); 
    cmd.add(out.getAbsolutePath());
    
    ejecutarComando(cmd); 
    return out;
}

    public void exportarSpriteSheet(File v, File out, int fps, int h, String c, double t, List<double[]> segmentos, String cr) throws Exception {
    double duracionTotal = segmentos.stream().mapToDouble(s -> s[1] - s[0]).sum();
    int totalFrames = (int) Math.max(1, Math.ceil(duracionTotal * fps));
    int columnas = (int) Math.ceil(Math.sqrt(totalFrames));
    int filas = (int) Math.ceil((double) totalFrames / columnas);
    
    List<String> cmd = new ArrayList<>();
    cmd.add(getFFmpegExecutable().getAbsolutePath()); cmd.add("-y"); cmd.add("-i"); cmd.add(v.getAbsolutePath());
    cmd.add("-vf"); 
    cmd.add(construirFiltros(fps, h, c, t, cr, segmentos) + ",tile=" + columnas + "x" + filas);
    cmd.add("-frames:v"); cmd.add("1"); cmd.add(out.getAbsolutePath());
    ejecutarComando(cmd);
}

    private String construirFiltros(int fps, int h, String c, double t, String cr, List<double[]> segmentos) {
    List<String> f = new ArrayList<>();
    if (segmentos != null && !segmentos.isEmpty()) {
        String selection = segmentos.stream()
            .map(seg -> String.format(Locale.US, "between(t,%.3f,%.3f)", seg[0], seg[1]))
            .collect(Collectors.joining("+")); // Ahora el compilador encontrará Collectors
        f.add("select='" + selection + "'");
        f.add("setpts=N/FRAME_RATE/TB");
    }
    if (cr != null && !cr.isEmpty()) f.add(cr);
    if (c != null) f.add(String.format(Locale.US, "colorkey=%s:%.2f:0.1", c, t));
    f.add("fps=" + fps); 
    f.add("scale=-1:" + h);
    return String.join(",", f);
}
    
    public void exportarGif(File v, File out, int fps, int h, double s, double e, String cr) throws Exception {
        double dur = Math.max(0.1, e - s);
        List<String> cmd = new ArrayList<>();
        cmd.add(getFFmpegExecutable().getAbsolutePath()); 
        cmd.add("-y"); 
        cmd.add("-ss"); cmd.add(String.valueOf(s)); 
        cmd.add("-t"); cmd.add(String.valueOf(dur)); 
        cmd.add("-i"); cmd.add(v.getAbsolutePath());
        String base = (cr != null && !cr.isEmpty() ? cr + "," : "") + "fps=" + fps + ",scale=-1:" + h;
        String gifFilter = base + ",split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse";
        cmd.add("-vf"); cmd.add(gifFilter);
        cmd.add(out.getAbsolutePath());
        ejecutarComando(cmd);
    }

    private String construirFiltros(int fps, int h, String c, double t, String cr) {
        List<String> f = new ArrayList<>();
        if (c != null) f.add("colorkey=" + c + ":" + String.format("%.2f", t).replace(",", ".") + ":0.2");
        if (cr != null && !cr.isEmpty()) f.add(cr);
        f.add("fps=" + fps); f.add("scale=-1:" + h);
        return String.join(",", f);
    }

    private void ejecutarComando(List<String> command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command); pb.redirectErrorStream(true); Process p = pb.start();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) { 
            String l; while ((l = r.readLine()) != null) System.out.println("[FFmpeg]: " + l); 
        }
        if (p.waitFor() != 0) throw new Exception("Error FFmpeg.");
    }
}