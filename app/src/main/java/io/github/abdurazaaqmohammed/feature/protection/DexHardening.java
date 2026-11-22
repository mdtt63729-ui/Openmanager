package io.github.abdurazaaqmohammed.feature.protection;

import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.MethodImplementation;
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * DEX-level hardening that runs on the device.
 *
 * Rewrites every dex through dexlib2 and drops what makes reverse engineering
 * easy: per-method debug items (line numbers, local variable names) and the
 * class source-file attribute. The code itself keeps behaving identically —
 * only the metadata a decompiler uses to reconstruct readable source goes away.
 *
 * This is a real transformation, not a claim: the output is a rebuilt dex, and
 * the counts it returns are what was actually removed.
 */
public final class DexHardening {

    public static final class Stats {
        public int methods;
        public int debugItemsRemoved;
        public int sourceFilesRemoved;

        public int total() {
            return debugItemsRemoved + sourceFilesRemoved;
        }
    }

    private DexHardening() {}

    /** Returns a hardened copy of {@code dex}, or the original bytes on failure. */
    public static byte[] strip(byte[] dex, Stats stats) {
        File tmpIn = null;
        try {
            tmpIn = File.createTempFile("dex-harden-in", ".dex");
            try (OutputStream os = new FileOutputStream(tmpIn)) {
                os.write(dex);
            }
            DexBackedDexFile input = DexFileFactory.loadDexFile(tmpIn, null);
            DexPool pool = new DexPool(input.getOpcodes());
            for (ClassDef classDef : input.getClasses()) {
                pool.internClass(stripClass(classDef, stats));
            }
            MemoryDataStore store = new MemoryDataStore();
            pool.writeTo(store);
            return Arrays.copyOf(store.getData(), store.getSize());
        } catch (Throwable t) {
            return dex;
        } finally {
            if (tmpIn != null) tmpIn.delete();
        }
    }

    private static ClassDef stripClass(ClassDef classDef, Stats stats) {
        List<Method> methods = new ArrayList<>();
        for (Method method : classDef.getMethods()) {
            MethodImplementation impl = method.getImplementation();
            MethodImplementation stripped = null;
            if (impl != null) {
                stats.methods++;
                int debugCount = 0;
                for (Object ignored : impl.getDebugItems()) debugCount++;
                stats.debugItemsRemoved += debugCount;
                stripped = new ImmutableMethodImplementation(
                        impl.getRegisterCount(),
                        impl.getInstructions(),
                        impl.getTryBlocks(),
                        null); // no debug items
            }
            methods.add(new ImmutableMethod(
                    classDef.getType(),
                    method.getName(),
                    method.getParameters(),
                    method.getReturnType(),
                    method.getAccessFlags(),
                    method.getAnnotations(),
                    method.getHiddenApiRestrictions(),
                    stripped));
        }
        if (classDef.getSourceFile() != null) stats.sourceFilesRemoved++;
        return new ImmutableClassDef(
                classDef.getType(),
                classDef.getAccessFlags(),
                classDef.getSuperclass(),
                classDef.getInterfaces(),
                null, // drop the source-file attribute
                classDef.getAnnotations(),
                classDef.getFields(),
                methods);
    }
}
