package com.fedeiatech.spritelab;

public class Launcher {
    public static void main(String[] args) {
        // Esta clase engaña a la JVM para iniciar sin comprobaciones de módulos estrictas
        App.main(args);
    }
}