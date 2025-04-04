/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.signflinger;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.apksig.ApkSignerEngine;
import com.android.apksig.DefaultApkSignerEngine;
import com.android.apksig.apk.ApkFormatException;
import com.android.apksig.internal.util.FileChannelDataSource;
import com.android.apksig.util.DataSource;
import com.android.apksig.util.DataSources;
import com.android.zipflinger.Archive;
import com.android.zipflinger.BytesSource;
import com.android.zipflinger.Source;
import com.android.zipflinger.Zip64;
import com.android.zipflinger.ZipArchive;
import com.android.zipflinger.ZipInfo;
import com.android.zipflinger.ZipSource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.Deflater;

public class SignedApk implements Archive {

    /** Signing block Id for SDK dependency block. */
    static final int DEPENDENCY_INFO_BLOCK_ID = 0x504b4453;

    private final ZipArchive archive;
    private final ApkSignerEngine signer;
    private final SignedApkOptions options;
    static final String MANIFEST_ENTRY_NAME = "META-INF/MANIFEST.MF";
    static final String MANIFEST_CREATED_BY = "Created-By";
    static final String MANIFEST_BUILT_BY = "Built-By";
    static final String MANIFEST_VERSION = "Manifest-Version";

    private static final boolean FAIL_ON_V4_ERROR = true;

    public SignedApk(@NonNull File file, @NonNull SignedApkOptions options)
            throws InvalidKeyException, IOException {
        this(file, options, Zip64.Policy.FORBID);
    }

    public SignedApk(
            @NonNull File file,
            @NonNull SignedApkOptions options,
            @NonNull Zip64.Policy zip64Policy)
            throws InvalidKeyException, IOException {
        this.options = options;
        this.archive = new ZipArchive(file.toPath(), zip64Policy);
        DefaultApkSignerEngine.SignerConfig signerConfig =
                new DefaultApkSignerEngine.SignerConfig.Builder(
                                options.name, options.privateKey, options.certificates)
                        .build();
        List<DefaultApkSignerEngine.SignerConfig> signerConfigs = new ArrayList<>();
        signerConfigs.add(signerConfig);
        signer =
                new DefaultApkSignerEngine.Builder(signerConfigs, options.minSdkVersion)
                        .setV1SigningEnabled(options.v1Enabled)
                        .setV2SigningEnabled(options.v2Enabled)
                        .setV3SigningEnabled(options.v3Enabled)
                        .setSigningCertificateLineage(options.v3SigningCertificateLineage)
                        .setCreatedBy(options.v1CreatedBy)
                        .setOtherSignersSignaturesPreserved(false)
                        .build();
        if (options.executor != null) {
            signer.setExecutor(options.executor);
        }

        initWithV1();
    }

    private void initWithV1() throws IOException {
        if (!options.v1Enabled) {
            return;
        }

        if (!options.v1TrustManifest) {
            archive.delete(MANIFEST_ENTRY_NAME);
        }

        byte[] manifestBytes;
        if (hasManifest(archive.listEntries())) {
            ByteBuffer manifestByteBuffer = archive.getContent(MANIFEST_ENTRY_NAME);
            manifestBytes = new byte[manifestByteBuffer.remaining()];
            manifestByteBuffer.get(manifestBytes);
        } else {
            manifestBytes = createDefaultManifest();
        }


        Set<String> filesToSign = new HashSet<>(archive.listEntries());
        Set<String> signedEntries = signer.initWith(manifestBytes, filesToSign);

        // If the archive contains entries that were not already signed, let the engine know
        // so it can sign then now.
        filesToSign.removeAll(signedEntries);
        for (String entryName : filesToSign) {
            ApkSignerEngine.InspectJarEntryRequest req = signer.outputJarEntry(entryName);
            processRequest(req);
        }

        // Let the signer take a look the new manifest if the archive did not contain one.
        if (!hasManifest(archive.listEntries())) {
            showNewManifestToSigner(manifestBytes);
        }
    }

    // The signer learns about META-INF/MANIFEST.MF properties (e.g: Created-By, Built-By) by
    // sending a InspectJarEntryRequest seeing our "outputJarEntry(MANIFEST_ENTRY_NAME)".
    //
    // This works if the archive already contains a META-INF/MANIFEST.MF but fails if there is none.
    // The initial approach was to create a META-INF/MANIFEST.MF, write it to the archive, and let
    // the signer discover it. The problem with this approach is that it creates a virtual
    // entry when signing ends. Delete it the first META-INF/MANIFEST.MF when we receive the final
    // META-INF/MANIFEST.MF is received (via getAdditionalJarEntries). See (b/268071371).
    //
    // The workaround is to declare a META-INF/MANIFEST.MF to the signer *without* writing it to
    // the archive. We then fulfill the signer request "manually" since it is not in the archive
    // and processRequest would fail.
    private void showNewManifestToSigner(@NonNull byte[] manifestBytes) throws IOException {
        ApkSignerEngine.InspectJarEntryRequest req = signer.outputJarEntry(MANIFEST_ENTRY_NAME);
        // The signer will immediately request to see MANIFEST_ENTRY_NAME. Since it is not in the
        // archive, we resolve the request without using "processRequest".
        if (req != null && req.getEntryName().equals(MANIFEST_ENTRY_NAME)) {
            req.getDataSink().consume(ByteBuffer.wrap(manifestBytes));
            req.done();
        }
    }

    private static boolean hasManifest(@NonNull Collection<String> entries) {
        return entries.contains(MANIFEST_ENTRY_NAME);
    }

    /** See Archive.add documentation */
    @Override
    public void add(@NonNull Source source) throws IOException {
        archive.add(source);
        if (options.v1Enabled) {
            ApkSignerEngine.InspectJarEntryRequest req = signer.outputJarEntry(source.getName());
            processRequest(req);
        }
    }

    /** See Archive.add documentation */
    @Override
    public void add(@NonNull ZipSource sources) throws IOException {
        archive.add(sources);
        if (options.v1Enabled) {
            for (Source source : sources.getSelectedEntries()) {
                ApkSignerEngine.InspectJarEntryRequest req =
                        signer.outputJarEntry(source.getName());
                processRequest(req);
            }
        }
    }

    /** See Archive.delete documentation */
    @Override
    public void delete(@NonNull String name) {
        archive.delete(name);
        if (options.v1Enabled) {
            signer.outputJarEntryRemoved(name);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            finishSigning();
            // At this point the archive has been closed.
            // V4 can be done if needed.
            signV4();
        } finally {
            signer.close();
        }
    }

    private void finishSigning() throws IOException {
        try {
            finishV1();
            finishV2andV3();
        } finally {
            if (!archive.isClosed()) {
                archive.close();
            }
        }
    }

    private void signV4() throws IOException {
        if (!options.v4Enabled) {
            return;
        }
        Path path = archive.getPath();
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            FileChannelDataSource dataSource = new FileChannelDataSource(channel);
            signer.signV4(dataSource, options.v4Output, FAIL_ON_V4_ERROR);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new IllegalStateException("V4 Signing error", e);
        }
    }

    private byte[] createDefaultManifest() throws IOException {
        Manifest manifest = new Manifest();
        Attributes mainAttributes = manifest.getMainAttributes();
        mainAttributes.putValue(MANIFEST_CREATED_BY, options.v1CreatedBy);
        mainAttributes.putValue(MANIFEST_BUILT_BY, options.v1BuiltBy);
        mainAttributes.putValue(MANIFEST_VERSION, "1.0");

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        manifest.write(os);
        return os.toByteArray();
    }

    private void finishV2andV3() throws IOException {
        if (!options.v2Enabled && !options.v3Enabled) {
            return;
        }

        ZipInfo zipInfo = archive.closeWithInfo();
        try (RandomAccessFile raf = new RandomAccessFile(archive.getPath().toFile(), "rw")) {
            byte[] sigBlock = v2andV3Sign(raf, zipInfo);
            sigBlock =
                    SigningBlockUtils.addToSigningBlock(
                            sigBlock, options.sdkDependencies, DEPENDENCY_INFO_BLOCK_ID);
            ApkSigningBlock.addToArchive(raf, sigBlock, zipInfo);
        } catch (IOException
                | NoSuchAlgorithmException
                | InvalidKeyException
                | SignatureException
                | ApkFormatException e) {
            throw new IllegalStateException(e);
        }

    }

    private void processRequest(@Nullable ApkSignerEngine.InspectJarEntryRequest req)
            throws IOException {
        if (req == null) {
            return;
        }
        String name = req.getEntryName();
        try (InputStream in = archive.getInputStream(name)) {
            if (in == null) {
                String err = String.format("Cannot find and therefore inspect entry %s.", name);
                throw new IllegalStateException(err);
            }
            // Send the whole payload into the datasink
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                req.getDataSink().consume(buffer, 0, read);
            }
            req.done();
        }
    }

    private void finishV1() throws IOException {
        if (!options.v1Enabled) {
            return;
        }

        // Check whether we need to output additional JAR entries which comprise the v1 signature
        ApkSignerEngine.OutputJarSignatureRequest addV1SignatureRequest;
        try {
            addV1SignatureRequest = signer.outputJarEntries();
        } catch (Exception e) {
            throw new IOException("Failed to generate v1 signature", e);
        }
        if (addV1SignatureRequest == null) {
            return;
        }

        for (ApkSignerEngine.OutputJarSignatureRequest.JarEntry entry :
                addV1SignatureRequest.getAdditionalJarEntries()) {
            archive.delete(entry.getName());
            BytesSource source =
                    new BytesSource(entry.getData(), entry.getName(), Deflater.BEST_SPEED);
            archive.add(source);
            ApkSignerEngine.InspectJarEntryRequest request = signer.outputJarEntry(entry.getName());
            processRequest(request);
        }
        addV1SignatureRequest.done();
    }

    @NonNull
    private byte[] v2andV3Sign(@NonNull RandomAccessFile raf, @NonNull ZipInfo zipInfo)
            throws ApkFormatException, SignatureException, NoSuchAlgorithmException,
                    InvalidKeyException, IOException {
        DataSource beforeCentralDir =
                DataSources.asDataSource(raf, zipInfo.payload.first, zipInfo.payload.size());
        DataSource centralDir = DataSources.asDataSource(raf, zipInfo.cd.first, zipInfo.cd.size());
        DataSource eocd = DataSources.asDataSource(raf, zipInfo.eocd.first, zipInfo.eocd.size());

        ApkSignerEngine.OutputApkSigningBlockRequest outputApkSigningBlockRequest =
                signer.outputZipSections(beforeCentralDir, centralDir, eocd);
        outputApkSigningBlockRequest.done();

        // Write block
        return outputApkSigningBlockRequest.getApkSigningBlock();
    }
}
