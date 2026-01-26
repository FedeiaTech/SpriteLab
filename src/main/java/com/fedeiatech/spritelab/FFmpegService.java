package com.fedeiatech.spritelab;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FFmpegService {

    private Process activeProcess;

    private File getFFmpegExecutable() {
        try {
            String jarPath = new File(FFmpegService.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
            File executable = new File(jarPath, "bin/ffmpeg.exe");
            return executable.exists() ? executable : new File("bin/ffmpeg.exe");
        } catch (Exception e) {
            return new File("bin/ffmpeg.exe");
        }
    }

    public static class VideoMeta {

        public int width, height;
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
        Pattern pDur = Pattern.compile("Duration: (\\d{2}):(\\d{2}):(\\d{2}\\.\\d+)");
        Pattern pRes = Pattern.compile(" (\\d{2,5})x(\\d{2,5})");
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String l;
            while ((l = r.readLine()) != null) {
                Matcher mRes = pRes.matcher(l);
                if (mRes.find()) {
                    meta.width = Integer.parseInt(mRes.group(1));
                    meta.height = Integer.parseInt(mRes.group(2));
                }
                Matcher mDur = pDur.matcher(l);
                if (mDur.find()) {
                    meta.duration = Double.parseDouble(mDur.group(1)) * 3600 + Double.parseDouble(mDur.group(2)) * 60 + Double.parseDouble(mDur.group(3));
                }
            }
        }
        p.waitFor();
        return meta;
    }

    public File generarPreview(File v, int fps, int h, String c, double t, double s, String cr, boolean esModoSheet, int cols, int rows, int totalW, int totalH) throws Exception {
        File out = File.createTempFile("preview_", ".png");
        List<String> cmd = new ArrayList<>();
        cmd.add(getFFmpegExecutable().getAbsolutePath());
        cmd.add("-y");

        if (!esModoSheet) {
            cmd.add("-ss");
            cmd.add(String.format(Locale.US, "%.3f", s));
        }
        cmd.add("-i");
        cmd.add(v.getAbsolutePath());

        List<String> filtros = new ArrayList<>();
        if (esModoSheet) {
            int fw = totalW / cols;
            int fh = totalH / rows;
            int x = ((int) s % cols) * fw;
            int y = ((int) s / cols) * fh;
            filtros.add(String.format("crop=%d:%d:%d:%d", fw, fh, x, y));
        }

        if (cr != null && !cr.isEmpty()) {
            filtros.add(cr);
        }

        filtros.add("scale=-1:" + h + ":flags=fast_bilinear");

        if (c != null) {
            filtros.add(String.format(Locale.US, "colorkey=%s:%.2f:0.1", c, t));
        }

        cmd.add("-vf");
        cmd.add(String.join(",", filtros));
        cmd.add("-frames:v");
        cmd.add("1");
        cmd.add("-update");
        cmd.add("1");
        cmd.add(out.getAbsolutePath());

        ejecutarComando(cmd, 1, null);
        return out;
    }

    public void exportarSpriteSheet(File v, File out, int fps, int h, String c, double t, double start, double end, String cr, boolean esModoSheet, int cols, int rows, int totalW, int totalH, Consumer<Double> progressCallback, boolean esPreview) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add(getFFmpegExecutable().getAbsolutePath());
        cmd.add("-y");

        int totalFrames = esModoSheet ? (int) (end - start + 1) : (int) Math.max(1, Math.ceil((end - start) * fps));

        if (esModoSheet) {
            cmd.add("-f");
            cmd.add("image2");
            cmd.add("-loop");
            cmd.add("1");
        } else {
            cmd.add("-ss");
            cmd.add(String.format(Locale.US, "%.3f", start));
            cmd.add("-t");
            cmd.add(String.format(Locale.US, "%.3f", end - start));
        }
        cmd.add("-i");
        cmd.add(v.getAbsolutePath());

        List<String> filtros = new ArrayList<>();
        if (esModoSheet) {
            int fw = totalW / cols;
            int fh = totalH / rows;
            filtros.add(String.format(Locale.US, "crop=%d:%d:'mod(n+%d,%d)*%d':'floor((n+%d)/%d)*%d'", fw, fh, (int) start, cols, fw, (int) start, cols, fh));
        }
        if (cr != null && !cr.isEmpty()) {
            filtros.add(cr);
        }

        filtros.add("scale=-1:" + h + ":flags=neighbor");

        if (c != null) {
            filtros.add(String.format(Locale.US, "colorkey=%s:%.2f:0.1", c, t));
        }

        int colDest = (int) Math.ceil(Math.sqrt(totalFrames));
        filtros.add("tile=" + colDest + "x" + (int) Math.ceil((double) totalFrames / colDest));

        cmd.add("-vf");
        cmd.add(String.join(",", filtros));
        cmd.add("-frames:v");
        cmd.add("1");
        cmd.add(out.getAbsolutePath());

        ejecutarComando(cmd, (esModoSheet ? (int) (end - start + 1) : (int) Math.max(1, Math.ceil((end - start) * fps))), progressCallback);
    }

    public void generarGif(File v, File out, int fps, int h, double start, double end, String c, double t, String cr, boolean esModoSheet, int cols, int rows, int totalW, int totalH, Consumer<Double> progressCallback, boolean esPreview) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add(getFFmpegExecutable().getAbsolutePath());
        cmd.add("-y");

        int totalFrames = esModoSheet ? (int) (end - start + 1) : (int) Math.max(1, (int) ((end - start) * fps));

        if (esModoSheet) {
            cmd.add("-f");
            cmd.add("image2");
            cmd.add("-loop");
            cmd.add("1");
            cmd.add("-t");
            cmd.add(String.format(Locale.US, "%.3f", (double) totalFrames / fps));
        } else {
            cmd.add("-ss");
            cmd.add(String.format(Locale.US, "%.3f", start));
            cmd.add("-t");
            cmd.add(String.format(Locale.US, "%.3f", end - start));
        }
        cmd.add("-i");
        cmd.add(v.getAbsolutePath());

        StringBuilder filter = new StringBuilder();
        if (esModoSheet) {
            int fw = totalW / cols;
            int fh = totalH / rows;
            filter.append(String.format(Locale.US, "crop=%d:%d:'mod(n+%d,%d)*%d':'floor((n+%d)/%d)*%d'", fw, fh, (int) start, cols, fw, (int) start, cols, fh));
            if (cr != null && !cr.isEmpty()) {
                filter.append(",").append(cr);
            }
        } else {
            if (cr != null && !cr.isEmpty()) {
                filter.append(cr);
            } else {
                filter.append("scale=iw:ih");
            }
        }

        filter.append(",scale=-1:").append(h).append(esPreview ? ":flags=neighbor" : "");
        filter.append(esModoSheet ? String.format(Locale.US, ",setpts=N/(%d*TB),fps=%d", fps, fps) : ",fps=" + fps);

        if (c != null) {
            filter.append(String.format(Locale.US, ",colorkey=%s:%.2f:0.1", c, t));
        }

        if (esPreview) {
            filter.append(",split[s0][s1];[s0]palettegen=max_colors=32[p];[s1][p]paletteuse=dither=none");
        } else {
            filter.append(",split[s0][s1];[s0]palettegen=stats_mode=diff[p];[s1][p]paletteuse=dither=sierra2_4a");
        }

        cmd.add("-vf");
        cmd.add(filter.toString());
        cmd.add("-frames:v");
        cmd.add(String.valueOf(totalFrames));
        cmd.add(out.getAbsolutePath());

        ejecutarComando(cmd, totalFrames, progressCallback);
    }

    private void ejecutarComando(List<String> command, int totalFrames, Consumer<Double> progressCallback) throws Exception {
        if (activeProcess != null && activeProcess.isAlive()) {
            activeProcess.destroyForcibly();
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        activeProcess = pb.start();

        Pattern framePattern = Pattern.compile("frame=\\s*(\\d+)");

        try (BufferedReader r = new BufferedReader(new InputStreamReader(activeProcess.getInputStream()))) {
            String l;
            while ((l = r.readLine()) != null) {
                if (totalFrames > 0 && progressCallback != null) {
                    Matcher m = framePattern.matcher(l);
                    if (m.find()) {
                        progressCallback.accept(Math.min((double) Integer.parseInt(m.group(1)) / totalFrames, 1.0));
                    }
                }
            }
        } catch (Exception e) {
        }

        int exitCode = activeProcess.waitFor();
        if (exitCode != 0) {
            throw new Exception("FFmpeg finalizó con código: " + exitCode);
        }
    }

    public void detenerProcesoActivo() {
        if (activeProcess != null && activeProcess.isAlive()) {
            activeProcess.destroyForcibly();
        }
    }

}
