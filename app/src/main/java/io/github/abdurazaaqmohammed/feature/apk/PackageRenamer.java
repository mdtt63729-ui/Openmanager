package io.github.abdurazaaqmohammed.feature.apk;

import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.rewriter.DexRewriter;
import com.android.tools.smali.dexlib2.rewriter.Rewriter;
import com.android.tools.smali.dexlib2.rewriter.RewriterModule;
import com.android.tools.smali.dexlib2.rewriter.Rewriters;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;

import androidx.annotation.NonNull;

/**
 * Renames the application package inside a dex file.
 *
 * Cloning an APK is not just a manifest edit: every class in every dex is
 * referenced by its full type descriptor ({@code Lcom/old/pkg/Foo;}). If only
 * the manifest changes, the app cannot find its own classes and crashes on
 * start. This rewrites the type descriptors through dexlib2 so the renamed
 * package is consistent everywhere the dex mentions it.
 */
public final class PackageRenamer {

    private PackageRenamer() {}

    /** "com.old.app" -> "Lcom/old/app" (the descriptor form used inside dex). */
    public static String descriptorPrefix(String pkg) {
        return "L" + pkg.replace('.', '/');
    }

    /**
     * Returns {@code dex} with every type descriptor under {@code oldPackage}
     * moved to {@code newPackage}. On any failure the original bytes are
     * returned untouched, so a clone never produces a half-renamed dex.
     */
    public static byte[] renameDex(byte[] dex, String oldPackage, String newPackage) {
        if (oldPackage == null || newPackage == null
                || oldPackage.isEmpty() || newPackage.isEmpty()
                || oldPackage.equals(newPackage)) {
            return dex;
        }
        File tmp = null;
        try {
            tmp = File.createTempFile("pkg-rename", ".dex");
            try (OutputStream os = new FileOutputStream(tmp)) {
                os.write(dex);
            }
            DexBackedDexFile input = DexFileFactory.loadDexFile(tmp, null);
            final String oldDescriptor = descriptorPrefix(oldPackage);
            final String newDescriptor = descriptorPrefix(newPackage);

            DexRewriter rewriter = new DexRewriter(new RewriterModule() {
                @NonNull
                @Override
                public Rewriter<String> getTypeRewriter(@NonNull Rewriters rewriters) {
                    return value -> {
                        if (value == null) return null;
                        if (value.equals(oldDescriptor)) return newDescriptor;
                        if (value.startsWith(oldDescriptor + "/")) {
                            return newDescriptor + value.substring(oldDescriptor.length());
                        }
                        return value;
                    };
                }
            });

            DexPool pool = new DexPool(input.getOpcodes());
            for (ClassDef classDef : input.getClasses()) {
                pool.internClass(rewriter.getClassDefRewriter().rewrite(classDef));
            }
            MemoryDataStore store = new MemoryDataStore();
            pool.writeTo(store);
            return Arrays.copyOf(store.getData(), store.getSize());
        } catch (Throwable t) {
            return dex;
        } finally {
            if (tmp != null) tmp.delete();
        }
    }

    /** True when this dex actually mentions the package (skips needless rewrites). */
    public static boolean mentions(byte[] dex, String pkg) {
        String needle = pkg.replace('.', '/');
        String latin = new String(dex, java.nio.charset.StandardCharsets.ISO_8859_1);
        return latin.contains(needle);
    }
}
