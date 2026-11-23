package io.github.abdurazaaqmohammed.ui.main;

import io.github.abdurazaaqmohammed.feature.apk.ApkIconHelper;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import io.github.abdurazaaqmohammed.MPManager.R;
import io.github.abdurazaaqmohammed.ui.common.theme.ThemeRegistry;
import io.github.abdurazaaqmohammed.util.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import java.util.List;
import java.util.Locale;

/** Shared file-type artwork for pane rows and configurable bookmark shortcuts. */
public record FileIconSpec(int glyph, int background, int foreground, int label, boolean fullColour) {
    // Golden folder and blue document carry their own colours, so they skip the
    // tinted tile every other type sits on.
    public static final FileIconSpec FOLDER = new FileIconSpec(R.drawable.ic_folder_golden, 0, 0, R.string.folder, true);
    public static final FileIconSpec APK = spec(R.drawable.apk_document_24px, 0xFF2E7D32, R.string.shortcut_icon_apk);
    public static final FileIconSpec IMAGE = spec(R.drawable.image_24px, 0xFF00838F, R.string.image);
    public static final FileIconSpec VIDEO = spec(R.drawable.video_24px, 0xFFC62828, R.string.shortcut_icon_video);
    public static final FileIconSpec AUDIO = spec(R.drawable.music_24px, 0xFFAD1457, R.string.shortcut_icon_audio);
    public static final FileIconSpec ARSC = spec(R.drawable.stacks_24px, 0xFF9A6A00, R.string.shortcut_icon_resources);
    public static final FileIconSpec DEX = spec(R.drawable.code_24px, 0xFF549395, R.string.shortcut_icon_dex);
    public static final FileIconSpec ARCHIVE = spec(R.drawable.baseline_folder_zip_24, 0xFF6A1B9A, R.string.shortcut_icon_archive);
    public static final FileIconSpec STICKER = spec(R.drawable.ic_sticker_24px, 0xFF00897B, R.string.shortcut_icon_sticker);
    public static final FileIconSpec PDF = spec(R.drawable.pdf_24px, 0xFFC62828, R.string.shortcut_icon_pdf);
    public static final FileIconSpec TEXT = spec(R.drawable.baseline_text_snippet_24, 0xFF616161, R.string.shortcut_icon_text);
    // PRD code palette: markup teal, Java/HTML orange, Kotlin violet, CSS blue, JS yellow.
    public static final FileIconSpec CODE = spec(R.drawable.code_24px, 0xFF00897B, R.string.shortcut_icon_dex);
    public static final FileIconSpec CODE_ORANGE = spec(R.drawable.code_24px, 0xFFEF6C00, R.string.shortcut_icon_dex);
    public static final FileIconSpec CODE_VIOLET = spec(R.drawable.code_24px, 0xFF4527A0, R.string.shortcut_icon_dex);
    public static final FileIconSpec CODE_BLUE = spec(R.drawable.code_24px, 0xFF1565C0, R.string.shortcut_icon_dex);
    public static final FileIconSpec CODE_YELLOW = spec(R.drawable.code_24px, 0xFFF9A825, R.string.shortcut_icon_dex);
    public static final FileIconSpec ELF = spec(R.drawable.terminal_24px, 0xFF37474F, R.string.shortcut_icon_elf);
    public static final FileIconSpec DISK = spec(R.drawable.ic_disk_drive_24px, 0xFF455A64, R.string.shortcut_icon_disk);
    public static final FileIconSpec DATABASE = spec(R.drawable.ic_database_24px, 0xFF00695C, R.string.shortcut_icon_database);
    public static final FileIconSpec FONT = spec(R.drawable.ic_font_aa_24px, 0xFFD84315, R.string.shortcut_icon_font);
    public static final FileIconSpec BINARY = spec(R.drawable.baseline_settings_24, 0xFF455A64, R.string.shortcut_icon_binary);
    public static final FileIconSpec PROTO = spec(R.drawable.ic_data_proto_24px, 0xFF4527A0, R.string.shortcut_icon_data);
    public static final FileIconSpec DOCUMENT = spec(R.drawable.ic_office_doc_24px, 0xFF1565C0, R.string.shortcut_icon_document);
    public static final FileIconSpec SHEET = spec(R.drawable.ic_office_sheet_24px, 0xFF2E7D32, R.string.shortcut_icon_sheet);
    public static final FileIconSpec SLIDE = spec(R.drawable.ic_office_slide_24px, 0xFFE65100, R.string.shortcut_icon_slide);
    public static final FileIconSpec SCRIPT = spec(R.drawable.terminal_24px, 0xFF283593, R.string.shortcut_icon_script);
    public static final FileIconSpec BACKUP = spec(R.drawable.ic_history, 0xFF5D4037, R.string.shortcut_icon_backup);
    public static final FileIconSpec CERTIFICATE = spec(R.drawable.ic_certificate_seal_24px, 0xFFF57F17, R.string.shortcut_icon_certificate);
    public static final FileIconSpec FILE = new FileIconSpec(R.drawable.ic_file_blue, 0, 0, R.string.file, true);

    // Universal file-type artwork (rounded document + pictogram, own palette).
    public static final FileIconSpec TYPE_PDF = type(R.drawable.ic_type_pdf, R.string.shortcut_icon_pdf);
    public static final FileIconSpec TYPE_WORD = type(R.drawable.ic_type_word, R.string.shortcut_icon_document);
    public static final FileIconSpec TYPE_EXCEL = type(R.drawable.ic_type_excel, R.string.shortcut_icon_sheet);
    public static final FileIconSpec TYPE_PPT = type(R.drawable.ic_type_ppt, R.string.shortcut_icon_slide);
    public static final FileIconSpec TYPE_IMAGE = type(R.drawable.ic_type_image, R.string.image);
    public static final FileIconSpec TYPE_PSD = type(R.drawable.ic_type_psd, R.string.image);
    public static final FileIconSpec TYPE_AI = type(R.drawable.ic_type_ai, R.string.image);
    public static final FileIconSpec TYPE_APK = type(R.drawable.ic_type_apk, R.string.shortcut_icon_apk);
    public static final FileIconSpec TYPE_ZIP = type(R.drawable.ic_type_zip, R.string.shortcut_icon_archive);
    public static final FileIconSpec TYPE_AUDIO = type(R.drawable.ic_type_audio, R.string.shortcut_icon_audio);
    public static final FileIconSpec TYPE_VIDEO = type(R.drawable.ic_type_video, R.string.shortcut_icon_video);
    public static final FileIconSpec TYPE_JSON = type(R.drawable.ic_type_json, R.string.shortcut_icon_data);
    public static final FileIconSpec TYPE_CODE = type(R.drawable.ic_type_code, R.string.shortcut_icon_dex);
    public static final FileIconSpec TYPE_TEXT = type(R.drawable.ic_type_text, R.string.shortcut_icon_text);
    public static final FileIconSpec TYPE_FONT = type(R.drawable.ic_type_font, R.string.shortcut_icon_font);
    public static final FileIconSpec TYPE_DISK = type(R.drawable.ic_type_disk, R.string.shortcut_icon_disk);

    private static FileIconSpec type(int glyph, int label) {
        return new FileIconSpec(glyph, 0, 0, label, true);
    }

    private static FileIconSpec spec(int glyph, int color, int label) { return new FileIconSpec(glyph, color, Color.WHITE, label, false); }

    public static List<FileIconSpec> all() {
        return List.of(FOLDER, FILE, APK, IMAGE, VIDEO, AUDIO, DEX, ARSC, ARCHIVE, STICKER, PDF, TEXT,
                ELF, DISK, DATABASE, FONT, BINARY, PROTO, DOCUMENT, SHEET, SLIDE, SCRIPT, BACKUP, CERTIFICATE);
    }

    public static FileIconSpec forPath(String path, boolean directory, int theme) {
        if (directory) return FOLDER;
        String extension = "." + FilenameUtils.getExtension(path).toLowerCase(Locale.ROOT);
        if (ApkIconHelper.isApkLikeExt(extension))
            return List.of(".apks", ".xapk", ".apkm", ".aspk").contains(extension) ? ARCHIVE : APK;
        FileIconSpec typed = typeForExtension(extension);
        if (typed != null) return typed;
        if (FileUtils.matchExt(extension, FileUtils.IMAGE_EXTS)) return IMAGE;
        if (FileUtils.matchExt(extension, FileUtils.VIDEO_EXTS)) return VIDEO;
        if (FileUtils.matchExt(extension, FileUtils.AUDIO_EXTS)) return AUDIO;
        if (FileUtils.matchExt(extension, FileUtils.ARCHIVE_EXTS)) return ARCHIVE;
        if (extension.equals(".pdf")) return PDF;
        if (extension.equals(".dex")) return DEX;
        if (extension.equals(".arsc")) return ARSC;
        // PRD icon mapping: code and markup get their own colour before the
        // generic text bucket picks the file up.
        switch (extension) {
            case ".xml": case ".json": return CODE;
            case ".java": case ".html": case ".htm": return CODE_ORANGE;
            case ".kt": case ".kts": return CODE_VIOLET;
            case ".css": return CODE_BLUE;
            case ".js": case ".mjs": return CODE_YELLOW;
            default: break;
        }
        if (FileUtils.matchExt(extension, FileUtils.TEXT_EXTS)) return TEXT;
        if (FileIconLoader.isDatabaseExt(extension)) return DATABASE;
        if (FileIconLoader.isFontExt(extension)) return FONT;
        if (FileIconLoader.isBinaryExt(extension)) return BINARY;
        if (FileIconLoader.isProtoExt(extension)) return PROTO;
        if (FileIconLoader.isWordExt(extension)) return DOCUMENT;
        if (FileIconLoader.isSheetExt(extension)) return SHEET;
        if (FileIconLoader.isSlideExt(extension)) return SLIDE;
        if (FileIconLoader.isScriptExt(extension)) return SCRIPT;
        if (FileIconLoader.isBackupExt(extension)) return BACKUP;
        if (FileIconLoader.isCertExt(extension)) return CERTIFICATE;
        if (List.of(".img", ".iso", ".vdi", ".vhd", ".vhdx", ".vmdk", ".qcow2", ".ext4", ".wad", ".payload").contains(extension)
                || "payload.bin".equalsIgnoreCase(FilenameUtils.getName(path))) return DISK;
        if (FileUtils.isElf(path)) return ELF;
        return FILE;
    }

    public Drawable drawable(Context context) {
        if (fullColour) {
            // No tile and no tint: the artwork already carries its palette.
            Drawable plain = ResourcesCompat.getDrawable(context.getResources(), glyph, context.getTheme());
            return plain == null ? null : plain.mutate();
        }
        float density = context.getResources().getDisplayMetrics().density;
        GradientDrawable backgroundDrawable = new GradientDrawable();
        backgroundDrawable.setCornerRadius(6 * density);
        int tile = background;
        if (glyph == R.drawable.ic_folder_golden) {
            tile = ThemeRegistry.packColor(context, "folder_icon_bg", tile);
        } else if (glyph == R.drawable.ic_file_blue) {
            tile = ThemeRegistry.packColor(context, "file_icon_bg", tile);
        }
        backgroundDrawable.setColor(tile);
        Drawable icon = ResourcesCompat.getDrawable(context.getResources(), glyph, context.getTheme()).mutate();
        DrawableCompat.setTint(icon, foreground);
        LayerDrawable layer = new LayerDrawable(new Drawable[]{backgroundDrawable, icon});
        int inset = Math.round(6 * density);
        layer.setLayerInset(1, inset, inset, inset, inset);
        return layer;
    }

    /** Universal icon catalog: extension family -> document artwork. */
    private static FileIconSpec typeForExtension(String extension) {
        switch (extension) {
            case ".pdf": return TYPE_PDF;
            case ".doc": case ".docx": case ".docm": case ".dot": case ".dotx": case ".odt":
            case ".ott": case ".rtf": case ".wps": case ".wpd": case ".pages": case ".epub":
            case ".mobi": case ".azw": case ".azw3": case ".fb2": case ".djvu": case ".chm":
            case ".xps": case ".oxps": return TYPE_WORD;
            case ".xls": case ".xlsx": case ".xlsm": case ".xlsb": case ".xltx": case ".xltm":
            case ".csv": case ".tsv": case ".ods": case ".ots": case ".numbers": case ".dif":
            case ".slk": case ".prn": return TYPE_EXCEL;
            case ".ppt": case ".pptx": case ".pptm": case ".pps": case ".ppsx": case ".pot":
            case ".potx": case ".odp": case ".otp": case ".key": return TYPE_PPT;
            case ".jpg": case ".jpeg": case ".jpe": case ".png": case ".apng": case ".gif":
            case ".webp": case ".bmp": case ".dib": case ".tiff": case ".tif": case ".svg":
            case ".svgz": case ".ico": case ".icns": case ".heic": case ".heif": case ".avif":
            case ".jxl": case ".raw": case ".cr2": case ".cr3": case ".nef": case ".nrw":
            case ".arw": case ".dng": case ".orf": case ".raf": case ".rw2": return TYPE_IMAGE;
            case ".psd": case ".psb": case ".xcf": case ".indd": case ".sketch": case ".fig":
            case ".xd": case ".fla": case ".swf": return TYPE_PSD;
            case ".ai": case ".eps": case ".ps": case ".cdr": return TYPE_AI;
            case ".mp3": case ".wav": case ".flac": case ".aac": case ".m4a": case ".m4b":
            case ".ogg": case ".opus": case ".wma": case ".amr": case ".aiff": case ".aif":
            case ".alac": case ".ape": case ".mid": case ".midi": case ".ac3": case ".dts":
            case ".caf": case ".au": case ".voc": case ".dsf": case ".dff": return TYPE_AUDIO;
            case ".mp4": case ".mkv": case ".avi": case ".mov": case ".wmv": case ".flv":
            case ".webm": case ".m4v": case ".mpeg": case ".mpg": case ".m2ts":
            case ".vob": case ".3gp": case ".3g2": case ".ogv": case ".divx": case ".mxf":
            case ".asf": case ".rm": case ".rmvb": case ".f4v": case ".mts": return TYPE_VIDEO;
            case ".zip": case ".rar": case ".7z": case ".tar": case ".gz": case ".tgz":
            case ".bz2": case ".xz": case ".zst": case ".z": case ".cab": case ".deb":
            case ".rpm": case ".aab": case ".jar": case ".war": case ".ear": case ".cpio":
            case ".ar": case ".lz": case ".lzh": case ".lzma": case ".ace": case ".arc":
            case ".wim": return TYPE_ZIP;
            case ".iso": case ".img": case ".dmg": case ".vhd": case ".vhdx": case ".vdi":
            case ".vmdk": case ".qcow2": return TYPE_DISK;
            case ".apk": case ".apks": case ".xapk": case ".apkm": case ".aspk": return TYPE_APK;
            case ".json": case ".xml": case ".yaml": case ".yml": case ".toml": case ".ini":
                return TYPE_JSON;
            case ".java": case ".kt": case ".kts": case ".py": case ".pyw": case ".js":
            case ".mjs": case ".cjs": case ".jsx": case ".ts": case ".tsx": case ".c":
            case ".h": case ".cpp": case ".hpp": case ".cc": case ".cxx": case ".cs":
            case ".swift": case ".dart": case ".php": case ".phtml": case ".rb": case ".go":
            case ".rs": case ".sql": case ".sh": case ".smali": case ".gradle": case ".html":
            case ".htm": case ".css": case ".scss": case ".sass": case ".less": return TYPE_CODE;
            case ".txt": case ".md": case ".markdown": case ".log": case ".tex": case ".latex":
                return TYPE_TEXT;
            case ".ttf": case ".otf": case ".woff": case ".woff2": case ".ttc": return TYPE_FONT;
            default: return null;
        }
    }

}
