package io.github.abdurazaaqmohammed.ui.main;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import io.github.abdurazaaqmohammed.ui.main.MainActivity;
import io.github.abdurazaaqmohammed.MPManager.R;
import io.github.abdurazaaqmohammed.ui.main.DialogAdapter;

/** Shared file-menu presentation, including the user's order and grid/sheet preference. */
public final class FileMenuDialog {
    private FileMenuDialog() {}

    public static void show(MainActivity activity, String title, List<FileMenuOrder.MenuItem> visible,
                            boolean inArchive, Set<String> disabled, Consumer<String> action) {
        show(activity, title, visible, inArchive, disabled, action, null);
    }

    /**
     * @param longPressAction invoked instead of {@code action} when a row is
     *                        long-pressed; the dialog is dismissed first. May
     *                        be null — rows then only respond to taps.
     */
    public static void show(MainActivity activity, String title, List<FileMenuOrder.MenuItem> visible,
                            boolean inArchive, Set<String> disabled, Consumer<String> action,
                            Consumer<String> longPressAction) {
        List<FileMenuOrder.MenuItem> items = FileMenuOrder.sortItems(activity, visible);
        activity.runOnUiThread(() -> {
            if (activity.isFinishing() || activity.isDestroyed()) return;
            boolean grid = FileMenuOrder.isTwoColumn(activity);
            View view = LayoutInflater.from(activity).inflate(R.layout.dialog_file_menu, null);
            ((TextView) view.findViewById(R.id.fileMenuTitle)).setText(title);
            RecyclerView list = view.findViewById(R.id.fileMenuList);
            Runnable[] dismiss = {() -> {}};
            view.findViewById(R.id.fileMenuEditButton).setOnClickListener(v -> {
                dismiss[0].run();
                FileMenuCustomizer.show(activity);
            });
            if (grid) {
                view.findViewById(R.id.fileMenuHandle).setVisibility(View.GONE);
                list.setLayoutManager(new GridLayoutManager(activity, 2));
                int edge = (int) (12 * activity.getResources().getDisplayMetrics().density + 0.5f);
                list.setPadding(edge, list.getPaddingTop(), edge, list.getPaddingBottom());
            } else list.setLayoutManager(new LinearLayoutManager(activity));
            DialogAdapter adapter = new DialogAdapter(activity, items, inArchive, grid, disabled, position -> {
                dismiss[0].run();
                action.accept(items.get(position).id());
            });
            if (longPressAction != null) {
                adapter.setOnItemLongClickListener(position -> {
                    dismiss[0].run();
                    longPressAction.accept(items.get(position).id());
                });
            }
            list.setAdapter(adapter);
            if (grid) {
                AlertDialog dialog = new MaterialAlertDialogBuilder(activity).setView(view).create();
                dismiss[0] = dialog::dismiss;
                dialog.show();
            } else {
                // A centred popup instead of a bottom sheet: the sheet slid in
                // from the right with its own background edges above and below
                // the list, which is what the user kept seeing.
                AlertDialog popup = new MaterialAlertDialogBuilder(activity)
                        .setView(view)
                        .create();
                dismiss[0] = popup::dismiss;
                popup.show();
                if (popup.getWindow() != null) {
                    popup.getWindow().setBackgroundDrawable(
                            new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                }
                view.setScaleX(0.92f);
                view.setScaleY(0.92f);
                view.setAlpha(0f);
                view.animate().scaleX(1f).scaleY(1f).alpha(1f)
                        .setInterpolator(new android.view.animation.OvershootInterpolator(1.6f))
                        .setDuration(220).start();
            }
        });
    }
}
