# Sprite Lab (v0.4)
### Universal 2D Sprite Sheet & GIF Generator

![Main Interface](https://github.com/FedeiaTech/SpriteLab/blob/main/_img/cover.jpg)

**Sprite Lab** is a technical tool designed to optimize the workflow for 3D artists and developers using modern video generators. It transforms video clips (Blender renders, AI-generated clips, etc.) into **Sprite Sheets** or **Animated GIFs** optimized for engines such as **Godot, Unity, or GameMaker**.

Version **0.4** introduces **Professional Process Control**, allowing users to interrupt heavy exports, track real-time progress, and keep the system clean of temporary files.

---

## Screenshots

### 1. Main Interface (v0.4 Update)
![Main Interface](https://github.com/FedeiaTech/SpriteLab/blob/main/_img/01.png)
*Clean interface featuring a dynamic status bar and Drag & Drop support.*

### 2. Editor in Action (Chroma Key)
![Editing Video](https://github.com/FedeiaTech/SpriteLab/blob/main/_img/02.png)
*Precise background removal using the integrated Eyedropper tool.*

### 3. Smart Results (Sprite Sheets)
![Editing Video](https://github.com/FedeiaTech/SpriteLab/blob/main/_img/03.png)
*Smart Naming: Exported filenames automatically include frame dimensions (WxH) for quick engine setup.*

### 4. Advanced Cropping & Grid
![Editing Video](https://github.com/FedeiaTech/SpriteLab/blob/main/_img/04.png)
*Real-time cropping with automatic grid calculation and zoom viewer.*

---

## What's New in v0.4

* **Process Abort System:** Added a dedicated emergency button (**red "X"**) in the progress bar. Instantly terminates the FFmpeg process, freeing up memory and CPU resources without freezing the application.
* **Live Progress Feedback:** Real-time progress bar with dynamic percentage tracking for all exports and GIF previews.
* **Auto-Cleanup System:** Automated temporary file management. The app now tracks every preview generated and deletes them upon closing, ensuring zero disk clutter.
* **Enhanced GIF Preview:** Previews now utilize an optimized palette and faster scaling filters for nearly instantaneous visual feedback.
* **Thread Safety:** Improved task management that prevents duplicate process execution and safely locks the UI during rendering.

---

## Technical Features

* **Local Processing:** All rendering is performed via **FFmpeg** on your local machine. No internet connection required; no user data collected.
* **Eyedropper Chroma Key:** Direct on-screen color selection with an adjustable tolerance slider for clean transparencies.
* **Zoom Viewer:** Free navigation (Panning) and automatic **"Fit"** mode for precise framing before exporting.
* **Bilingual System:** Dynamic language switching (English/Spanish) across the entire interface, including status messages and alerts.
* **System Friendly:** Efficient external process management (SIGKILL) to ensure FFmpeg never remains as a "zombie" background process.

---

## Ethical Use & Purpose
This tool is built for **creators**. It serves as the ideal bridge for:
1.  **Blender/Maya Artists** who need to export 3D animations as lightweight 2D assets.
2.  **AI Developers** using generative video (Runway, Sora, Kling) to create base characters and integrate them into game engines.

---

## Installation (RAR Release)

1.  Download the `.rar` file from [Itch.io](https://fedeiatech.itch.io/spritelab).
2.  Extract the contents (Ensure the `bin` folder containing `ffmpeg.exe` is in the same directory as the executable).
3.  Run `SpriteLab.exe`.

---

## Development (Build)

* **Language:** Java 21 (JavaFX).
* **Dependencies:** FFmpeg (Local binaries required in `/bin`).
* **UI Library:** ControlsFX (RangeSlider).
* **License:** MIT.

Developed by **FedeiaTech**.