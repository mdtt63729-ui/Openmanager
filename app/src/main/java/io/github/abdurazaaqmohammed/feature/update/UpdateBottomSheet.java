package io.github.abdurazaaqmohammed.feature.update;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import io.github.abdurazaaqmohammed.MPManager.R;
import io.github.abdurazaaqmohammed.util.misc.UpdateUtil;
import io.noties.markwon.Markwon;

/**
 * Bottom sheet shown when a newer release exists.
 *
 * Header: rounded app icon, app name, then "old -> new" with a green NEW chip.
 * Tools: every bundled engine that is behind upstream, each with its own icon,
 * name and version arrow. Body: the release changelog. Footer: in-app
 * download, then the system installer — no browser involved.
 */
public class UpdateBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_VERSION = "version";
    private static final String ARG_APK_NAME = "apkName";
    private static final String ARG_APK_URL = "apkUrl";
    private static final String ARG_CHANGELOG = "changelog";

    private String version;
    private String apkName;
    private String apkUrl;
    private String changelog;

    private MaterialButton downloadButton;
    private ProgressBar progress;
    private LinearLayout toolsSection;
    private LinearLayout toolList;
    private long downloadId = -1L;
    private boolean receiverRegistered;
    private Context appContext;

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
            if (downloadId == -1L || id != downloadId) return;
            downloadId = -1L;
            showProgress(false);
            installDownloaded(id);
        }
    };

    public static UpdateBottomSheet newInstance(String version, String apkName, String apkUrl, String changelog) {
        UpdateBottomSheet sheet = new UpdateBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_VERSION, version);
        args.putString(ARG_APK_NAME, apkName);
        args.putString(ARG_APK_URL, apkUrl);
        args.putString(ARG_CHANGELOG, changelog);
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            version = args.getString(ARG_VERSION, "");
            apkName = args.getString(ARG_APK_NAME, "");
            apkUrl = args.getString(ARG_APK_URL, "");
            changelog = args.getString(ARG_CHANGELOG, "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_update, container, false);

        Context context = requireContext();
        appContext = context.getApplicationContext();

        ImageView icon = view.findViewById(R.id.updateAppIcon);
        TextView name = view.findViewById(R.id.updateAppName);
        TextView oldVersion = view.findViewById(R.id.updateOldVersion);
        TextView newVersion = view.findViewById(R.id.updateNewVersion);
        TextView chip = view.findViewById(R.id.updateNewChip);
        TextView changeLog = view.findViewById(R.id.updateChangelog);
        progress = view.findViewById(R.id.updateProgress);
        downloadButton = view.findViewById(R.id.updateDownload);
        MaterialButton laterButton = view.findViewById(R.id.updateLater);
        toolsSection = view.findViewById(R.id.updateToolsSection);
        toolList = view.findViewById(R.id.updateToolList);

        // Header: the app's own icon and name, its current version and the new one.
        try {
            icon.setImageDrawable(context.getPackageManager().getApplicationIcon(context.getPackageName()));
        } catch (Exception ignored) {
            // keep whatever the layout already shows
        }
        name.setText(R.string.app_name);
        oldVersion.setText(currentVersionName(context));
        newVersion.setText(version);
        chip.setText(R.string.update_sheet_new_chip);

        // Body: what actually changed, straight from the release notes.
        if (TextUtils.isEmpty(changelog)) {
            changeLog.setText(R.string.update_sheet_no_changelog);
        } else {
            try {
                Markwon.create(context).setMarkdown(changeLog, changelog);
            } catch (Exception e) {
                changeLog.setText(changelog);
            }
        }

        downloadButton.setOnClickListener(v -> startDownload());
        laterButton.setOnClickListener(v -> dismiss());

        // Engines that are behind upstream; filled in when the lookups return.
        ToolUpdateChecker.check(this::bindTools);

        return view;
    }

    private void bindTools(List<ToolUpdate> tools) {
        if (!isAdded() || toolList == null) return;
        toolList.removeAllViews();
        if (tools == null || tools.isEmpty()) {
            toolsSection.setVisibility(View.GONE);
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (ToolUpdate tool : tools) {
            View row = inflater.inflate(R.layout.item_tool_update, toolList, false);
            TextView avatar = row.findViewById(R.id.toolIcon);
            ((TextView) row.findViewById(R.id.toolName)).setText(tool.name);
            ((TextView) row.findViewById(R.id.toolVersions)).setText(
                    getString(R.string.update_sheet_tool_versions, tool.bundled, tool.latest));
            bindAvatar(avatar, tool);
            toolList.addView(row);
        }
        toolsSection.setVisibility(View.VISIBLE);
    }

    /**
     * Uses res/drawable/tool_&lt;id&gt; when the project ships a real logo for the
     * tool, and falls back to a coloured monogram otherwise.
     */
    private void bindAvatar(TextView avatar, ToolUpdate tool) {
        int resId = getResources().getIdentifier("tool_" + tool.id, "drawable", requireContext().getPackageName());
        if (resId != 0) {
            avatar.setText("");
            avatar.setBackgroundResource(resId);
            return;
        }
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(12f * getResources().getDisplayMetrics().density);
        background.setColor(tool.color);
        avatar.setBackground(background);
        String initial = tool.name == null || tool.name.isEmpty() ? "?" : tool.name.substring(0, 1).toUpperCase();
        avatar.setText(initial);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!receiverRegistered) {
            ContextCompat.registerReceiver(requireContext(), downloadReceiver,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    ContextCompat.RECEIVER_EXPORTED);
            receiverRegistered = true;
        }
    }

    @Override
    public void onStop() {
        if (receiverRegistered) {
            try {
                requireContext().unregisterReceiver(downloadReceiver);
            } catch (Exception ignored) {
                // context already gone with the activity
            }
            receiverRegistered = false;
        }
        super.onStop();
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        // Remember the version the user was told about, so the same sheet does
        // not come back on the next launch.
        if (!TextUtils.isEmpty(version) && appContext != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
            prefs.edit().putString(UpdateUtil.PREF_LAST_VER_CHECKED, version).apply();
        }
        super.onDismiss(dialog);
    }

    private void startDownload() {
        Context context = requireContext();

        // Android 8+: the user has to allow this app to install packages.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !context.getPackageManager().canRequestPackageInstalls()) {
            Toast.makeText(context, R.string.update_sheet_allow_install, Toast.LENGTH_LONG).show();
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + context.getPackageName())));
            } catch (Exception ignored) {
                // settings screen missing on this build: the toast already explained it
            }
            return;
        }

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl))
                    .setTitle(apkName)
                    .setDescription(apkName)
                    .setMimeType("application/vnd.android.package-archive")
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, apkName)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE
                            | DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm == null) throw new IllegalStateException("no DownloadManager");
            downloadId = dm.enqueue(request);
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit().putLong(UpdateUtil.PREF_DOWNLOAD_ID, downloadId).apply();
            showProgress(true);
        } catch (Exception e) {
            showProgress(false);
            Toast.makeText(context, R.string.update_sheet_download_failed, Toast.LENGTH_LONG).show();
        }
    }

    private void installDownloaded(long id) {
        Context context = requireContext();
        try {
            DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            Uri apkUri = dm == null ? null : dm.getUriForDownloadedFile(id);
            if (apkUri == null) {
                Toast.makeText(context, R.string.update_sheet_download_failed, Toast.LENGTH_LONG).show();
                return;
            }
            Intent install = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(apkUri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(install);
        } catch (Exception e) {
            Toast.makeText(context, R.string.update_sheet_download_failed, Toast.LENGTH_LONG).show();
        }
    }

    private void showProgress(boolean running) {
        if (progress != null) progress.setVisibility(running ? View.VISIBLE : View.GONE);
        if (downloadButton != null) {
            downloadButton.setEnabled(!running);
            downloadButton.setText(running ? R.string.update_sheet_downloading
                    : R.string.update_sheet_download);
        }
    }

    private static String currentVersionName(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return TextUtils.isEmpty(info.versionName) ? "?" : info.versionName;
        } catch (Exception e) {
            return "?";
        }
    }
}
