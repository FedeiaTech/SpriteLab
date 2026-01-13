# Sprite Lab (v0.2)
### Universal 2D Sprite Sheet Generator

**Sprite Lab** is a technical tool designed to optimize the workflow for 3D artists and developers using video generators. It transforms video clips (Blender renders, AI-generated clips, etc.) into **Sprite Sheets** optimized for engines such as **Godot, Unity, or Unreal**.

This version 0.2 focuses on **surgical precision** for cropping and total code transparency.

---

## Screenshots

### 1. Main Interface (Initial State)
![Main Interface](https://github.com/FedeiaTech/SpriteLab/blob/main/_img/01.png)
*Clean interface waiting for files to be loaded via Drag & Drop.*

### 2. Editor in Action (Chroma Key + Cropping)
![Editing Video](https://github.com/FedeiaTech/SpriteLab/blob/main/_img/02.png)
*Real-time preview featuring background removal, automatic grid, and zoom viewer.*

---

## 🚀 What's New in v0.2

* ** Manual Crop (Pixel-Perfect):** Direct selection tool on the viewer. Coordinate mapping has been corrected to eliminate offsets.
* ** Bilingual System:** Support for dynamic language switching (English/Spanish) across the entire interface.
* ** Linked Aspect Ratio:** Height and Width fields now visually display their proportional connection.
* ** Restore Button:** Instantly revert accidental or incorrect crops without reloading the source file.

---

## Technical Features

* **Local Processing:** All rendering is performed via **FFmpeg** on your local machine. The application makes no external connections and collects no user data.
* **Eyedropper Chroma Key:** Direct on-screen color selection with an adjustable tolerance slider for clean transparencies.
* **Zoom Viewer:** Free navigation (Panning) and automatic "Fit" mode for precise framing before exporting.
* **Automatic Grid:** Mathematical calculation of rows and columns based on FPS and clip duration.

---

## Ethical Use & Purpose
This tool is built for **creators**. It serves as the ideal bridge for:
1. **Blender/Maya Artists** who need to export their 3D animations as 2D sprites.
2. **Developers** using **generative AI video** (Runway, Sora, Grok) to create base characters and integrate them into a game engine.

---

## Installation (ZIP Release)

1. Download the `.zip` file from [Itch.io](https://fedeiatech.itch.io/spritelab).
2. Extract the contents (Ensure the `bin` folder containing `ffmpeg.exe` is in the same directory).
3. Run `SpriteLab.exe`.

---

## Development (Build)

* **Language:** Java 21 (JavaFX).
* **Dependencies:** FFmpeg (Local binaries required in `/bin`).
* **License:** MIT.

Developed by **FedeiaTech**.