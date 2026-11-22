package io.github.abdurazaaqmohammed.ui.main;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.File;
import java.util.Locale;

import io.github.abdurazaaqmohammed.MPManager.R;
import io.github.abdurazaaqmohammed.feature.apk.APKExtractorActivity;
import io.github.abdurazaaqmohammed.feature.system.DocumentsBrowserActivity;
import io.github.abdurazaaqmohammed.feature.system.ToolLauncher;
import io.github.abdurazaaqmohammed.util.misc.UpdateUtil;

/**
 * The "Open Manager" popup shown by the hamburger button.
 *
 * Replaces the left drawer with a Material 3 card: storage cards with live
 * StatFs numbers, then the directory, network and advanced-tool rows. Every row
 * calls the same code path the drawer uses, so nothing here is decorative.
 */
public class OpenManagerMenuDialog extends Dialog {

    private final MainActivity activity;

    public OpenManagerMenuDialog(@NonNull MainActivity activity) {
        super(activity);
        this.activity = activity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.omp_dialog_open_manager);

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.dimAmount = 0.45f;
            window.setAttributes(lp);
        }

        // The version line doubles as the manual update check, so the updater
        // always answers instead of staying silent.
        TextView versionLine = findViewById(R.id.omp_version_line);
        if (versionLine != null) {
            try {
                String version = activity.getPackageManager()
                        .getPackageInfo(activity.getPackageName(), 0).versionName;
                versionLine.setText("v" + version + " · tap to check for updates");
            } catch (Exception ignored) {
                // keep the layout text when the version cannot be read
            }
            versionLine.setOnClickListener(v -> {
                dismiss();
                UpdateUtil.checkForUpdates(true, activity);
            });
        }

        findViewById(R.id.omp_settings_button).setOnClickListener(v -> {
            dismiss();
            activity.startActivity(new Intent(activity,
                    io.github.ratul.topactivity.ui.SettingsActivity.class));
        });

        bindDirectory(findViewById(R.id.omp_root_directory_card), "/",
                findViewById(R.id.omp_root_progress),
                findViewById(R.id.omp_root_stats),
                findViewById(R.id.omp_root_summary));

        bindDirectory(findViewById(R.id.omp_storage_directory_card),
                Environment.getExternalStorageDirectory().getAbsolutePath(),
                findViewById(R.id.omp_storage_progress),
                findViewById(R.id.omp_storage_stats),
                findViewById(R.id.omp_storage_summary));

        findViewById(R.id.omp_documents).setOnClickListener(v -> {
            dismiss();
            activity.startActivity(new Intent(activity, DocumentsBrowserActivity.class));
        });
        findViewById(R.id.omp_providers).setOnClickListener(v -> {
            dismiss();
            activity.startActivity(new Intent(activity, DocumentsBrowserActivity.class));
        });
        findViewById(R.id.omp_recycle_bin).setOnClickListener(v -> {
            dismiss();
            activity.openBuiltInSidebarTool("recycle_bin");
        });
        findViewById(R.id.omp_extract_apk).setOnClickListener(v -> {
            dismiss();
            activity.startActivity(new Intent(activity, APKExtractorActivity.class));
        });

        findViewById(R.id.omp_ftp_server).setOnClickListener(v -> {
            dismiss();
            activity.showFtpServerDialog();
        });
        findViewById(R.id.omp_network_connect).setOnClickListener(v -> {
            dismiss();
            activity.getNetwork().showConnections();
        });
        findViewById(R.id.omp_network_connections).setOnClickListener(v -> {
            dismiss();
            activity.getNetwork().showConnections();
        });

        bindTool(R.id.omp_mcp_server, "mcp_server");
        bindTool(R.id.omp_command_shell, "command_shell");
        bindTool(R.id.omp_logcat, "logcat");
        bindTool(R.id.omp_layout_inspector, "layout_inspector");
        bindTool(R.id.omp_color_picker, "color_picker");
    }

    private void bindTool(int viewId, String toolId) {
        View view = findViewById(viewId);
        if (view == null) return;
        view.setOnClickListener(v -> {
            dismiss();
            if (!ToolLauncher.open(activity, toolId)) {
                activity.openBuiltInSidebarTool(toolId);
            }
        });
    }

    private void bindDirectory(MaterialCardView card, String path,
                               LinearProgressIndicator progress, TextView stats, TextView summary) {
        if (card == null) return;
        card.setOnClickListener(v -> {
            dismiss();
            activity.loadFolderInPaneAsync(new File(path), activity.lastPaneSelected == 1);
        });
        refreshDirectory(path, progress, stats, summary);
    }

    private void refreshDirectory(String path, LinearProgressIndicator progress,
                                  TextView stats, TextView summary) {
        try {
            StatFs fs = new StatFs(path);
            long total = fs.getTotalBytes();
            long free = fs.getAvailableBytes();
            long used = total - free;
            int percent = total <= 0 ? 0 : (int) (used * 100 / total);
            if (stats != null) {
                stats.setText(String.format(Locale.US, "%s used    %s total", gb(used), gb(total)));
            }
            if (summary != null) {
                summary.setText(String.format(Locale.US, "%d%% used · %s available",
                        percent, gb(free)));
            }
            if (progress != null) progress.setProgressCompat(percent, true);
        } catch (Exception ignored) {
            // storage not readable from here: the card still navigates
        }
    }

    private static String gb(long bytes) {
        double gb = bytes / (1024d * 1024d * 1024d);
        return String.format(Locale.US, "%.2f GB", gb);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Window window = getWindow();
        if (window != null) {
            int width = (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.92f);
            int height = (int) (getContext().getResources().getDisplayMetrics().heightPixels * 0.88f);
            window.setLayout(width, height);
        }
    }
}
