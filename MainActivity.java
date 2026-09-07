package com.openai.galaxyfindershortcut;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String TARGET_PACKAGE = "com.samsung.android.app.galaxyfinder";
    private static final String TARGET_ACTIVITY = "com.samsung.android.app.galaxyfinder.GalaxyFinderActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        openGalaxyFinder();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isFinishing()) {
            openGalaxyFinder();
        }
    }

    private void openGalaxyFinder() {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(TARGET_PACKAGE, TARGET_ACTIVITY));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Galaxy Finder açılamadı. Bu Samsung sürümünde etkinlik engellenmiş olabilir.", Toast.LENGTH_LONG).show();
        } finally {
            finish();
        }
    }
}
