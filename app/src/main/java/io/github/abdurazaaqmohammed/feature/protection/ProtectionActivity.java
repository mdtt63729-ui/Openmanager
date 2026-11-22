package io.github.abdurazaaqmohammed.feature.protection;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.List;

import io.github.abdurazaaqmohammed.MPManager.R;

/**
 * APK Protection screen: pick an APK, read the analysis, choose a profile,
 * apply — and, when the APK already carries this app's marker, unlock it with
 * the code that was issued at signing time.
 */
public class ProtectionActivity extends AppCompatActivity {

    private static final int REQ_PICK = 4711;

    private final Handler main = new Handler(Looper.getMainLooper());
    private TextView analysisView;
    private TextView statusView;
    private TextView reportView;
    private RadioGroup profiles;
    private Button applyButton;
    private Button unlockButton;
    private File selectedApk;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_protection);

        MaterialToolbar toolbar = findViewById(R.id.protectionToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        analysisView = findViewById(R.id.protectionAnalysis);
        statusView = findViewById(R.id.protectionStatus);
        reportView = findViewById(R.id.protectionReport);
        profiles = findViewById(R.id.protectionProfiles);
        applyButton = findViewById(R.id.protectionApply);
        unlockButton = findViewById(R.id.protectionUnlock);

        findViewById(R.id.protectionPick).setOnClickListener(v -> pickApk());
        applyButton.setOnClickListener(v -> startProtection());
        unlockButton.setOnClickListener(v -> askForCode());
        applyButton.setEnabled(false);
        unlockButton.setVisibility(View.GONE);
    }

    private void pickApk() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        try {
            startActivityForResult(intent, REQ_PICK);
        } catch (Exception e) {
            Toast.makeText(this, R.string.protection_pick_failed, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK || resultCode != Activity.RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null) return;
        File copy = new File(getCacheDir(), "protection-input.apk");
        try (InputStream in = getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(copy)) {
            if (in == null) throw new IllegalStateException("no stream");
            byte[] buf = new byte[1 << 16];
            int read;
            while ((read = in.read(buf)) > 0) out.write(buf, 0, read);
        } catch (Exception e) {
            Toast.makeText(this, R.string.protection_pick_failed, Toast.LENGTH_LONG).show();
            return;
        }
        selectedApk = copy;
        showAnalysis();
    }

    private void showAnalysis() {
        statusView.setText(R.string.protection_analyzing);
        new Thread(() -> {
            ProtectionEngine.Analysis a = ProtectionEngine.analyze(this, selectedApk);
            boolean ours = ProtectionMarker.isOurs(selectedApk);
            boolean locked = ProtectionMarker.hasCode(selectedApk);
            List<String> modules = ProtectionMarker.modulesOf(ProtectionMarker.read(selectedApk));
            main.post(() -> {
                analysisView.setText(describe(a));
                applyButton.setEnabled(a.compatibility != ProtectionEngine.State.UNSUPPORTED);
                if (ours) {
                    unlockButton.setVisibility(locked ? View.VISIBLE : View.GONE);
                    statusView.setText(getString(R.string.protection_detected, modules.size()));
                } else {
                    unlockButton.setVisibility(View.GONE);
                    statusView.setText(R.string.protection_ready);
                }
            });
        }).start();
    }

    private String describe(ProtectionEngine.Analysis a) {
        return getString(R.string.protection_package) + ": " + a.packageName + "\n"
                + getString(R.string.protection_version) + ": " + a.versionName
                + " (" + a.versionCode + ")\n"
                + "SDK: " + a.minSdk + " → " + a.targetSdk + "\n"
                + "DEX: " + a.dexCount + (a.multidex ? " (multidex)" : "") + "\n"
                + "ABI: " + (a.abis.isEmpty() ? "—" : a.abis.toString()) + "\n"
                + getString(R.string.protection_framework) + ": " + a.framework + "\n"
                + getString(R.string.protection_obfuscation) + ": " + a.obfuscation + "\n"
                + getString(R.string.protection_signing) + ": " + a.signing + "\n"
                + getString(R.string.protection_compatibility) + ": " + a.compatibility
                + " (" + a.compatibilityPercent() + "%)";
    }

    /** Owner unlock: ask for the code issued at signing, then strip the layers. */
    private void askForCode() {
        if (selectedApk == null) return;
        EditText input = new EditText(this);
        input.setHint(R.string.protection_code_hint);
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        LinearLayout box = new LinearLayout(this);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(input);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.protection_unlock_title)
                .setMessage(R.string.protection_unlock_body)
                .setView(box)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.protection_unlock_action, (d, w) -> {
                    String code = input.getText().toString();
                    if (ProtectionMarker.verifyCode(selectedApk, code)) {
                        unlockToFile();
                    } else {
                        Toast.makeText(this, R.string.protection_code_wrong, Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }

    private void unlockToFile() {
        File out = new File(selectedApk.getParentFile(),
                selectedApk.getName().replaceFirst("\\.apk$", "") + "_unlocked.apk");
        new Thread(() -> {
            boolean ok = ProtectionMarker.strip(selectedApk, out);
            main.post(() -> {
                statusView.setText(ok ? getString(R.string.protection_unlocked) + ": " + out.getName()
                        : getString(R.string.protection_failed));
                reportView.setText(ok ? out.getAbsolutePath() : "");
            });
        }).start();
    }

    private ProtectionEngine.Profile selectedProfile() {
        int id = profiles.getCheckedRadioButtonId();
        if (id == R.id.profileBasic) return ProtectionEngine.Profile.BASIC;
        if (id == R.id.profileMaximum) return ProtectionEngine.Profile.MAXIMUM;
        if (id == R.id.profileCustom) return ProtectionEngine.Profile.CUSTOM;
        return ProtectionEngine.Profile.STRONG;
    }

    private void startProtection() {
        if (selectedApk == null) return;
        applyButton.setEnabled(false);
        reportView.setText("");
        ProtectionEngine.Profile profile = selectedProfile();
        EnumSet<ProtectionEngine.Module> modules = EnumSet.of(
                ProtectionEngine.Module.SIGNATURE_CLEAN,
                ProtectionEngine.Module.DEX_INTEGRITY,
                ProtectionEngine.Module.INTEGRITY_ASSET);

        File out = new File(selectedApk.getParentFile(),
                selectedApk.getName().replaceFirst("\\.apk$", "") + "_protected.apk");

        new Thread(() -> {
            ProtectionEngine.Result result = ProtectionEngine.apply(this, selectedApk, out, profile,
                    modules, (label, done, total) -> main.post(() -> statusView.setText(
                            label + "… (" + done + "/" + total + ")")));
            String code = null;
            if (result.success && result.output != null) {
                code = ProtectionMarker.newCode();
                ProtectionMarker.embed(result.output, result.applied, code);
            }
            final String issued = code;
            main.post(() -> {
                applyButton.setEnabled(true);
                statusView.setText(result.success
                        ? getString(R.string.protection_done)
                        : getString(R.string.protection_failed) + ": " + result.error);
                reportView.setText(renderReport(result));
                if (issued != null) showCode(issued);
            });
        }).start();
    }

    private void showCode(String code) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.protection_code_title)
                .setMessage(getString(R.string.protection_code_body, code))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private String renderReport(ProtectionEngine.Result r) {
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.protection_report)).append("\n\n");
        sb.append(getString(R.string.protection_score)).append(": ").append(r.score).append("/100\n");
        if (r.output != null) sb.append(getString(R.string.protection_output)).append(": ")
                .append(r.output.getAbsolutePath()).append('\n');
        sb.append('\n').append(getString(R.string.protection_applied)).append('\n');
        for (String s : r.applied) sb.append("  ✓ ").append(s).append('\n');
        sb.append('\n').append(getString(R.string.protection_skipped)).append('\n');
        for (String s : r.skipped) sb.append("  – ").append(s).append('\n');
        sb.append('\n').append(getString(R.string.protection_sign_note));
        return sb.toString();
    }
}
