package io.github.abdurazaaqmohammed.feature.protection;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.io.File;
import java.util.List;

import io.github.abdurazaaqmohammed.MPManager.R;

/**
 * Asked once per signing run: apply APK protection to the build, or sign as is.
 *
 * The switch is the only decision the user has to make. Everything the engine
 * can actually do is applied when it is on; modules that cannot run on-device
 * are reported afterwards, never silently claimed.
 *
 * When the picked APK already carries this app's marker, an extra row appears
 * offering to take the protection back off — the owner's unlock, which other
 * tools cannot verify.
 */
public class SignProtectionSheet extends BottomSheetDialogFragment {

    private static final String ARG_PATH = "path";
    private static final String PREF_APPLY = "protectionOnSign";

    public interface OnProtectionChosen {
        void onChosen(boolean applyProtection, boolean removeExisting, boolean cancelled);
    }

    private OnProtectionChosen callback;
    private File apk;
    private boolean ours;

    public static SignProtectionSheet newInstance(File apk, OnProtectionChosen callback) {
        SignProtectionSheet sheet = new SignProtectionSheet();
        Bundle args = new Bundle();
        args.putString(ARG_PATH, apk == null ? "" : apk.getAbsolutePath());
        sheet.callback = callback;
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_protection, container, false);

        Bundle args = getArguments();
        String path = args == null ? "" : args.getString(ARG_PATH, "");
        apk = path.isEmpty() ? null : new File(path);

        MaterialSwitch applySwitch = view.findViewById(R.id.protectionApplySwitch);
        MaterialSwitch unlockSwitch = view.findViewById(R.id.protectionUnlockSwitch);
        TextView unlockRow = view.findViewById(R.id.protectionUnlockRow);
        TextView summary = view.findViewById(R.id.protectionSummary);
        MaterialButton signButton = view.findViewById(R.id.protectionSignNow);
        MaterialButton cancelButton = view.findViewById(R.id.protectionSignPlain);
        ImageView icon = view.findViewById(R.id.protectionIcon);

        boolean remembered = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean(PREF_APPLY, false);
        applySwitch.setChecked(remembered);

        // Does this APK already carry our marker?
        ours = apk != null && apk.isFile() && ProtectionMarker.isOurs(apk);
        if (ours) {
            List<String> modules = ProtectionMarker.modulesOf(ProtectionMarker.read(apk));
            unlockRow.setVisibility(View.VISIBLE);
            unlockSwitch.setChecked(true);
            summary.setText(getString(R.string.protection_already, modules.size()));
        } else {
            unlockRow.setVisibility(View.GONE);
            summary.setText(R.string.protection_sign_body);
        }

        applySwitch.setOnCheckedChangeListener((b, checked) -> {
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .edit().putBoolean(PREF_APPLY, checked).apply();
            summary.setText(checked ? R.string.protection_sign_body_on : R.string.protection_sign_body);
        });

        signButton.setOnClickListener(v -> {
            if (callback != null) {
                callback.onChosen(applySwitch.isChecked(),
                        ours && unlockSwitch.isChecked(), false);
            }
            dismiss();
        });
        cancelButton.setOnClickListener(v -> {
            if (callback != null) callback.onChosen(false, false, true);
            dismiss();
        });

        // Bouncy entrance for the shield, then the body fades up.
        if (icon != null) {
            icon.setScaleX(0.6f);
            icon.setScaleY(0.6f);
            icon.animate().scaleX(1f).scaleY(1f)
                    .setInterpolator(new OvershootInterpolator(2.4f))
                    .setDuration(420).start();
        }
        View body = view.findViewById(R.id.protectionBody);
        if (body != null) {
            body.setAlpha(0f);
            body.setTranslationY(24f * getResources().getDisplayMetrics().density);
            body.animate().alpha(1f).translationY(0f).setStartDelay(90).setDuration(260).start();
        }
        return view;
    }

    @Override
    public void onDestroyView() {
        callback = null;
        super.onDestroyView();
    }
}
