# Warzone 2100 — Android Build Scaffold (arm64-v8a)

Ini BUKAN patch yang dijamin langsung sukses di percobaan pertama — sudah saya
jelaskan sebelumnya kenapa itu tidak realistis untuk engine desktop seperti ini.
Yang ada di sini adalah fondasi yang sudah saya susun berdasarkan pembacaan
langsung source code project ini (CMakeLists.txt, vcpkg.json, src/clparse.cpp),
bukan tebakan generik.

## Apa yang sudah dikonfirmasi dari source aslinya

- Dependency asli: **SDL3** (bukan SDL2), PhysFS, libsodium, Freetype,
  Harfbuzz, Fribidi, libjpeg-turbo, libogg/vorbis/theora, Opus, OpenAL-soft,
  zlib, SQLite3, libzip, QuickJS (vendored). **Tidak ada Boost atau Pango.**
- Renderer sudah punya jalur **OpenGL ES 3** (`lib/ivis_opengl/gfx_api_gl.h`
  include `<GLES3/gl3.h>` dan punya flag `gles`) — kemungkinan besar
  dari dukungan Emscripten/WebGL yang sudah ada. ini mengurangi risiko besar
  dibanding kalau renderer-nya murni desktop GL.
- Project sudah pakai vcpkg manifest (`vcpkg.json`) dengan baseline terkunci.
  vcpkg punya triplet komunitas `arm64-android` yang menangani cross-compile
  NDK untuk hampir semua dependency di atas secara otomatis.
- `src/clparse.cpp` sudah punya flag `--datadir` dan `--configdir` yang bisa
  dipakai untuk mengarahkan engine ke folder data/config custom — ini dipakai
  untuk menyiasati keterbatasan filesystem Android (lihat bagian "Data game"
  di bawah).

## Bagian yang PALING mungkin gagal di percobaan pertama (jujur, bukan tebakan)

1. **OpenAL-soft di Android** — ada backend Android (OpenSL ES/AAudio) tapi
   saya tidak bisa pastikan vcpkg port `openal-soft` versi di baseline project
   ini sudah mengaktifkannya untuk triplet `arm64-android` tanpa dicoba build.
2. **Harfbuzz/Freetype/Fribidi lewat vcpkg triplet Android** — biasanya jalan,
   tapi beberapa port punya dependency sistem (ICU, dll) yang kadang perlu
   flag tambahan di triplet overlay.
3. **Ukuran APK** — data game WZ2100 (base.wz, mp.wz, terrain, dll) bisa
   ratusan MB. Untuk sideload (bukan Play Store) ini tidak masalah, tapi build
   time di GitHub Actions bisa lama dan runner bisa kehabisan disk kalau tidak
   dibersihkan.
4. **Lokasi data saat runtime** — Android tidak punya filesystem biasa untuk
   file di dalam APK (assets terkompresi). Saya pakai pendekatan umum: extract
   assets ke `getFilesDir()` saat pertama kali run, lalu kirim `--datadir`
   dan `--configdir` ke native code. Ini pola yang biasa dipakai port
   SDL+PhysFS lain, tapi saya belum bisa jalankan untuk memastikan nama field/
   argumen Java-nya 100% cocok dengan versi SDL3 yang di-pull nanti.

**Kalau salah satu dari ini meleset di log GitHub Actions, copy-paste error-nya
ke saya — itu jauh lebih cepat diperbaiki daripada saya menebak dari sini.**

## Struktur yang disertakan

```
replace-in-repo-root/
├── CMakeLists.txt       → ganti file di root repo Anda dengan ini
└── src/CMakeLists.txt   → ganti src/CMakeLists.txt Anda dengan ini
android/                 → folder BARU, taruh persis di root repo (sejajar dengan src/, lib/, dll)
.github/workflows/
└── android-build.yml    → workflow BARU
```

## Cara pakai

1. Salin `replace-in-repo-root/CMakeLists.txt` → timpa `CMakeLists.txt` di
   root repo Anda.
2. Salin `replace-in-repo-root/src/CMakeLists.txt` → timpa `src/CMakeLists.txt`.
3. Salin folder `android/` ke root repo (baru, tidak menimpa apa pun).
4. Salin `.github/workflows/android-build.yml` ke `.github/workflows/` repo Anda.
5. Commit & push. Buka tab **Actions** di GitHub, jalankan workflow
   "Android Build" secara manual (workflow_dispatch) atau tunggu push trigger.
6. APK (debug, belum ditandatangani untuk rilis) akan muncul sebagai artifact
   di halaman run Actions tersebut — download dari sana.

## Kalau gagal

Saya tidak punya akses internet di sandbox ini, jadi saya tidak bisa
menjalankan build ini sendiri untuk memverifikasi — ini disusun murni dari
membaca source code project Anda. Kemungkinan besar akan perlu 2-5 putaran
perbaikan berdasarkan log error Actions yang sebenarnya. Itu normal untuk
porting engine desktop ke Android, bukan tanda saya asal buat.
