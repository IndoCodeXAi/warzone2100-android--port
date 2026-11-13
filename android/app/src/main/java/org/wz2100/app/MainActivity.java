package net.wz2100.app;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;

import org.libsdl.app.SDLActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * NOTE for whoever picks this up after a failed CI run:
 *
 * This class assumes org.libsdl.app.SDLActivity exists (vendored from the
 * SDL3 source tree by the CI workflow — see .github/workflows/android-build.yml).
 * If the native lib doesn't export the symbol SDL3's JNI glue expects, check
 * getLibraries() below first: it must list every .so that needs to be
 * dlopen'd, in dependency order, ending with "main".
 *
 * The asset-extraction-on-first-run approach below is the standard pattern
 * for SDL+PhysFS style engines on Android (PhysFS can't read Android's
 * AssetManager directly). It has NOT been runtime-tested in this environment
 * — if the game reports it can't find its data files, check logcat for the
 * exact path this class wrote to, then compare against what WZ2100's
 * main.cpp / clparse.cpp expect under --datadir.
 */
public class MainActivity extends SDLActivity {

    private static final String TAG = "WZ2100Android";

    // Must match the folder name used under android/app/src/main/assets/
    private static final String ASSET_DATA_DIR = "data";
    private static final String EXTRACTED_MARKER = ".wz2100_data_extracted_v1";

    @Override
    protected String[] getLibraries() {
        // Order matters: dependencies first, the app's own lib last.
        // "c++_shared" is only needed if ANDROID_STL=c++_shared was used
        // for the native build (see the CMake configure step in the
        // workflow). If the toolchain was built with a static STL instead,
        // remove that entry or the loader will fail with "library not found".
        return new String[]{
                "c++_shared",
                "SDL3",
                "main"
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        File dataDir = new File(getFilesDir(), "data");
        File configDir = new File(getFilesDir(), "config");
        File marker = new File(getFilesDir(), EXTRACTED_MARKER);

        if (!marker.exists()) {
            try {
                Log.i(TAG, "First run: extracting bundled game data to " + dataDir);
                extractAssetDir(ASSET_DATA_DIR, dataDir);
                //noinspection ResultOfMethodCallIgnored
                configDir.mkdirs();
                //noinspection ResultOfMethodCallIgnored
                marker.createNewFile();
            } catch (IOException e) {
                // Deliberately not swallowing this: if extraction fails the
                // game WILL fail to find its data, so fail loudly in logcat
                // rather than produce a confusing native-side crash later.
                Log.e(TAG, "Asset extraction failed", e);
            }
        }

        this.extractedDataDir = dataDir.getAbsolutePath();
        this.extractedConfigDir = configDir.getAbsolutePath();

        super.onCreate(savedInstanceState);
    }

    private String extractedDataDir;
    private String extractedConfigDir;

    @Override
    protected String[] getArguments() {
        // Maps to src/clparse.cpp's --datadir / --configdir flags, confirmed
        // present in the upstream source at the time this was written.
        return new String[]{
                "--datadir", extractedDataDir,
                "--configdir", extractedConfigDir
        };
    }

    private void extractAssetDir(String assetPath, File targetDir) throws IOException {
        AssetManager assets = getAssets();
        String[] children = assets.list(assetPath);

        if (children == null || children.length == 0) {
            // Leaf file, not a directory.
            copyAssetFile(assetPath, new File(targetDir.getParentFile(), targetDir.getName()));
            return;
        }

        //noinspection ResultOfMethodCallIgnored
        targetDir.mkdirs();
        for (String child : children) {
            String childAssetPath = assetPath + "/" + child;
            File childTarget = new File(targetDir, child);
            String[] grandChildren = assets.list(childAssetPath);
            if (grandChildren != null && grandChildren.length > 0) {
                extractAssetDir(childAssetPath, childTarget);
            } else {
                copyAssetFile(childAssetPath, childTarget);
            }
        }
    }

    private void copyAssetFile(String assetPath, File target) throws IOException {
        AssetManager assets = getAssets();
        try (InputStream in = assets.open(assetPath);
             OutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }
}
