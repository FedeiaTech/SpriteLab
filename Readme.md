# Sprite Lab (v0.3.1)
### Universal 2D Sprite Sheet & GIF Generator

![Main Interface](https://github.com/FedeiaTech/SpriteLab/blob/main/_img/cover.jpg)

**Sprite Lab** is a technical tool designed to optimize the workflow for 3D artists and developers using video generators. It transforms video clips (Blender renders, AI-generated clips, etc.) into **Sprite Sheets** or **Animated GIFs** optimized for engines such as **Godot, Unity, or GameMaker**.

This version 0.3 focuses on **visual feedback**, **high-quality GIF export**, and **UI polish**.

---

## Screenshots

### 1. Main Interface (Initial State)
![Main Interface](https://github.com/FedeiaTech/SpriteLab/blob/main/_img/01.png)
*Clean interface waiting for files to be loaded via Drag & Drop.*

### 2. Editor in Action (Chroma Key)
![Editing Video](https://github.com/FedeiaTech/SpriteLab/blob/main/_img/02.png)

### 3. Editor in Action (Cropping)
![Editing Video](https://github.com/FedeiaTech/SpriteLab/blob/main/_img/04.png)
*Real-time preview featuring background removal, automatic grid, and zoom viewer.*

### 4. Results (Sprite Sheet)
![Editing Video](https://github.com/FedeiaTech/SpriteLab/blob/main/_img/03.png)
*Smart Naming: Exported filenames automatically include the cropped frame dimensions (WxH) for quick reference during engine setup.*

---

## What's New in v0.3

* **High-Quality GIF Export:** Added support for animated GIF export using an internal dual-pass palette generation for professional color accuracy.
* **Enhanced Preview Windows:** GIF and Sheet previews now feature proportional scaling and auto-centering, fitting any screen size without distortion.
* **Dynamic Precision Cursors:** The mouse cursor now changes to a crosshair when using the Crop or Eyedropper tools for better visual feedback.
* **Visual Aspect Ratio Link:** Replaced the legacy emoji with a custom high-compatibility icon for the resolution link, ensuring consistent UI across all Windows versions.

---

## Fix 0.3.1 

* **Repaired sheet exports bugs**

---

## Technical Features

* **Local Processing:** All rendering is performed via **FFmpeg** on your local machine. The application makes no external connections and collects no user data.
* **Eyedropper Chroma Key:** Direct on-screen color selection with an adjustable tolerance slider for clean transparencies.
* **Zoom Viewer:** Free navigation (Panning) and automatic "Fit" mode for precise framing before exporting.
* **Bilingual System:** Support for dynamic language switching (English/Spanish) across the entire interface.

---

## Ethical Use & Purpose
This tool is built for **creators**. It serves as the ideal bridge for:
1. **Blender/Maya Artists** who need to export their 3D animations as 2D assets.
2. **Developers** using **generative AI video** (Runway, Sora, Grok) to create base characters and integrate them into a game engine.

---

## Installation (RAR Release)

1. Download the `.rar` file from [Itch.io](https://fedeiatech.itch.io/spritelab).
2. Extract the contents (Ensure the `bin` folder containing `ffmpeg.exe` is in the same directory).
3. Run `SpriteLab.exe`.

---

## Development (Build)

* **Language:** Java 21 (JavaFX).
* **Dependencies:** FFmpeg (Local binaries required in `/bin`).
* **License:** MIT.

Developed by **FedeiaTech**.