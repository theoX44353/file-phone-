/*
* Hak Cipta 2023 Proyek Sumber Terbuka Android
*
* Dilisensikan berdasarkan Lisensi Apache, Versi 2.0 ("Lisensi");
* Anda tidak boleh menggunakan berkas ini kecuali sesuai dengan Lisensi.
* Anda dapat memperoleh salinan Lisensi di
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
* Kecuali jika diwajibkan oleh hukum yang berlaku atau disetujui secara tertulis, perangkat lunak
* didistribusikan di bawah Lisensi didistribusikan atas dasar "SEBAGAIMANA ADANYA",
* TANPA JAMINAN ATAU KETENTUAN APAPUN, baik tersurat maupun tersirat.
* Lihat Lisensi untuk bahasa spesifik yang mengatur izin dan
* batasan berdasarkan Lisensi.
*/

paket  com . google . devsite . renderer . converters

impor  com.google.devsite.komponen.impl.DefaultMetadataComponent
impor  com.google.devsite.komponen.impl.DefaultVersionMetadataComponent
impor  com . google . devsite . komponen . simbol . MetadataComponent
impor  com.google.devsite.komponen.simbol.VersionMetadataComponent
impor  com.google.devsite.renderer.converters.ParameterDocumentableConverter.Companion.rewriteKotlinPrimitivesForJava
impor  com.google.devsite.renderer.impl.DocumentablesHolder
impor  com.google.devsite.util.LibraryMetadata
impor  java.util.concurrent.ConcurrentHashMap
impor  organisasi . otak jet . dok . Konfigurasi Dokka
impor  organisasi . otak jet . dok . tautan . DRI
impor  organisasi . otak jet . dok . model . Kontravarian
impor  org . jetbrains . dokka . model . Kovariansi
impor  organisasi . otak jet . dok . model . Seperti Kelas D
impor  org . jetbrains . dokka . model . DFunction
impor  org . jetbrains . dokka . model . DProperty
impor  organisasi . otak jet . dok . model . DTypeParameter
impor  org . jetbrains . dokka . model . DefinitelyNonNullable
impor  organisasi . otak jet . dok . model . Dapat didokumentasikan
impor  organisasi . otak jet . dok . model . Sumber yang Dapat Didokumentasikan
impor  org . jetbrains . dokka . model . Dinamis
impor  organisasi . otak jet . dok . model . Konstruktor Tipe Fungsional
impor  organisasi . otak jet . dok . model . GenericTypeConstructor
impor  org . jetbrains . dokka . model . Invariansi
impor  organisasi . otak jet . dok . model . Objek Java
impor  org . jetbrains . dokka . model . Dapat dibatalkan
impor  organisasi . otak jet . dok . model . Tipe Java Primitif
impor  organisasi . otak jet . dok . model . Proyeksi
impor  organisasi . otak jet . dok . model . Bintang
impor  organisasi . otak jet . dok . model . TipeAlias
impor  organisasi . otak jet . dok . model . TipeParameter
impor  org . jetbrains . dokka . model . UnresolvedBound
impor  org . jetbrains . dokka . model . Void
impor  org . jetbrains . dokka . model . WithSources
impor  org . jetbrains . dokka . model . isExtension

/**
* Membuat komponen metadata (bagian yang berisi informasi seperti ID artefak dan sumber
* tautan) untuk dokumen yang dapat didokumentasikan.
*/
 kelas  internal MetadataConverter (
    pribadi  val  docsHolder : DocumentablesHolder ,
) {
    /** Membuat komponen metadata untuk kelas serupa. */
    menyenangkan  getMetadataForClasslike ( classlike : DClasslike ): MetadataComponent {
        nilai  libraryMetadata = classlike.findMatchingLibraryMetadata()
        nilai  sumberUrl =
            kelaslike.createLinkToSource(
                docsHolder.baseClassSourceLink,
                classlike.dri.namalengkap,
            )
        versi  Metadata =
            classlike.temukanMatchingVersionMetadata(
                pustakaMetadata?.releaseNotesUrl,
            )

        kembalikan DefaultMetadataComponent(
            Komponen Metadata.Params(
                libraryMetadata = perpustakaanMetadata,
                sourceLinkUrl = sumberUrl,
                versiMetadata = versiMetadata,
            ),
        )
    }

    /** Membuat komponen metadata untuk [fungsi]. */
    menyenangkan  getMetadataForFunction ( fungsi : DFunction ): MetadataComponent {
        nilai  libraryMetadata = fungsi.temukanMatchingLibraryMetadata()
        nilai  versionMetadata = fungsi.findMatchingVersionMetadata(libraryMetadata?.releaseNotesUrl)

        // Menampilkan metadata perpustakaan dan tautan sumber hanya untuk fungsi tingkat atas dan ekstensi, jadi
        // mereka tidak diduplikasi dari metadata kelas untuk fungsi dalam kelas.
        nilai  includeAdditionalMetadata = fungsi.dri.isTopLevel() || fungsi.isExtension()
         sumber tautan val =
            jika (sertakanMetadataTambahan) {
                // Jika ini adalah aksesor properti, tautkan ke properti sebagai gantinya karena fungsinya
                // mungkin
                // tidak ada di sumber.
                val  sourceProperty = fungsi.ekstra[SourceProperty.PropertyKey]?.properti
                sumberProperti?.createLinkToSource(
                    docsHolder.basePropertySourceLink,
                    sumberProperti.nama,
                )
                    ?: fungsi.createLinkToSource(
                        docsHolder.baseFunctionSourceLink,
                        // Jika fungsi diubah namanya, gunakan nama dari sumber.
                        fungsi.ekstra[NamaAsli.KunciProperti]?.nama ?: fungsi.nama,
                    )
            } kalau tidak {
                batal
            }

            kembalikan DefaultMetadataComponent(
            Komponen Metadata.Params(
                libraryMetadata = jika (includeAdditionalMetadata) libraryMetadata jika tidak  null ,
                sourceLinkUrl = tautan sumber,
                versiMetadata = versiMetadata,
            ),
        )
    }

    /** Membuat komponen metadata untuk [properti]. */
    menyenangkan  getMetadataForProperty ( properti : DProperty ): MetadataComponent {
        nilai  libraryMetadata = properti.findMatchingLibraryMetadata()
         versiMetadata = properti.findMatchingVersionMetadata(libraryMetadata?.releaseNotesUrl
 )

        // Menampilkan metadata perpustakaan dan tautan sumber hanya untuk properti tingkat atas dan ekstensi, jadi
        // mereka tidak diduplikasi dari metadata kelas untuk properti di dalam kelas.
        nilai  includeAdditionalMetadata = properti.dri.isTopLevel() || properti.isExtension()
         sumber tautan val =
            jika (sertakanMetadataTambahan) {
                properti.createLinkToSource(docsHolder.basePropertySourceLink, properti.nama)
            } kalau tidak {
                batal
            }

        kembalikan DefaultMetadataComponent(
            Komponen Metadata.Params(
                libraryMetadata = jika (includeAdditionalMetadata) libraryMetadata jika tidak  null ,
                sourceLinkUrl = tautan sumber,
                versiMetadata = versiMetadata,
            ),
        )
    }

    /**
     * Ulangi melalui Peta metadata perpustakaan untuk menemukan [LibraryMetadata] yang cocok dengan metadata saat ini
     * kelas sedang diproses. Jika tidak, kembalikan null.
     */
     kesenangan pribadi < T > T . findMatchingLibraryMetadata (): LibraryMetadata ? di mana
    T : Dapat didokumentasikan ,
    T : DenganSumber {
        val  paths = getSourceFilePaths() ?: kembalikan  null
         jalur val =
            jika (ukuran jalur > 1 ) {
                // Jika ada beberapa jalur, ini mungkin KMP dan jalurnya diakhiri dengan ".kt",
                // ".jvm.kt", ".native.kt", dll. Pilih jalur ".kt".
                jalur.tunggalatauNull { it.indeks( " . " ) == it.indeksterakhir( " . " ) }
            } kalau tidak {
                jalur.tunggal()
            }

        kembalikan docsHolder.fileMetadataMap[jalur]
    }

    /**
     * Menanyakan metadata versi API Map untuk menemukan [VersionMetadataComponent] yang cocok dengan
     * kelas saat ini sedang diproses dan tambahkan URL rilis. Jika tidak, kembalikan null.
     */
    pribadi  menyenangkan  DClasslike . temukanMatchingVersionMetadata (
        releaseNotesUrl : String ?,
    ): VersionMetadataComponent ? {
        nilai  classVersionMetadata = docsHolder.versionMetadataMap[dri.namalengkap]

        kembalikan classVersionMetadata?.let {
            KomponenMetadataVersiDefault.createVersionMetadataDenganUrlBasis(
                itu.ditambahkan,
                itu.deprecatedIn,
                catatanrilisUrl,
            )
        }
    }

    /**
     * Menanyakan metadata versi API Map untuk menemukan [VersionMetadataComponent] yang cocok dengan
     * fungsi saat ini sedang diproses dan menambahkan URL rilis. Jika tidak, kembalikan null.
     */
    pribadi  menyenangkan  DFunction . temukanMatchingVersionMetadata (
        releaseNotesUrl : String ?,
    ): VersionMetadataComponent ? {
        val  classVersionMetadata = docsHolder.versionMetadataMap[berisiNamaKelas()]
         metode valVersionMetadata =
            kelasVersiMetadata
                ?.versimetode
                ?.mendapatkan(
                    apiSinceMethodSignature(ini),
                )

        kembalikan metodeVersionMetadata?.let {
            KomponenMetadataVersiDefault.createVersionMetadataDenganUrlBasis(
                itu.ditambahkan,
                itu.deprecatedIn,
                catatanrilisUrl,
            )
        }
    }

    /**
     * Menanyakan peta metadata versi API untuk menemukan [VersionMetadataComponent] yang cocok dengan
     * properti saat ini sedang diproses dan tambahkan URL rilis. Jika tidak, kembalikan null. Banyak
     * properti akan direpresentasikan dalam peta metadata versi oleh pengaksesnya, jadi ini terlihat
     * untuk metadata pengambil jika metadata tidak dapat ditemukan untuk properti itu sendiri.
     */
    pribadi  menyenangkan  DProperty . temukanMatchingVersionMetadata (
        releaseNotesUrl : String ?,
    ): VersionMetadataComponent ? {
        val  classVersionMetadata = docsHolder.versionMetadataMap[berisiNamaKelas()]
        propertiVersionMetadata  = classVersionMetadata?.fieldVersions?.get(nama)

        kembalikan propertyVersionMetadata?.let {
            KomponenMetadataVersiDefault.createVersionMetadataDenganUrlBasis(
                itu.ditambahkan,
                itu.deprecatedIn,
                catatanrilisUrl,
            )
        } ?: dapatkan?.findMatchingVersionMetadata(releaseNotesUrl)
    }

    /**
     * Membangun nama yang sepenuhnya memenuhi syarat untuk kelas yang berisi [Documentable] di dalam
     * Tampilan Java dari API. Ini berarti jika [Documentable] adalah fungsi atau properti tingkat atas,
     * nama kelas sintetis digunakan.
     */
    pribadi  menyenangkan < T > T . berisiNamaKelas (): String  di mana T : DenganSumber , T : DapatDidokumentasikan =
        " ${ dri.namapaket } . ${ dri.namakelas ?: namaUntukKelasSintetis(ini) } "

    /** Ini tidak boleh diakses di luar [getSourceFilePaths]. */
    pribadi  val  sourceFilesPaths = ConcurrentHashMap< DRI , Daftar < String >>()

    /**
     * Menemukan jalur berkas yang dikaitkan dengan entri sumber yang dapat didokumentasikan.
     *
     * Mengembalikan null jika tidak ada entri sumber, atau tidak ada entri sumber dengan jalur file, yang merupakan
     * berlaku untuk semua kelas dan fungsi sintetis.
     */
    pribadi  menyenangkan < T > T . getSourceFilePaths (): Daftar < String >? di mana T: DenganSumber , T: DapatDidokumentasikan =
        // Nilai ConcurrentHashMap tidak boleh null, jadi daftar kosong disimpan sebagai gantinya.
        sumberFileJalur
            .getOrPut(dri ini) { sumber.entri.mapNotNull { it.getSourceFilePath() } }
            .jikaKosong { null }

    /**
     * Dapatkan jalur file sumber dari [SourceEntry] relatif terhadap akar direktori sumber,
     * jika memungkinkan.
     *
     * Misalnya - ini akan mengembalikan "androidx/paging/compose/LazyPagingItems.kt" jika jalurnya
     * "/lokasi/ke/root/of/source/files/androidx/paging/compose/LazyPagingItems.kt".
     */
    pribadi  menyenangkan  SourceEntry . getSourceFilePath (): String ? {
        nilai  sourceRoots = kunci.sourceRoots.peta { it.toString() }
        val  fullFilePath = nilai.jalur
        // Temukan akar sumber tempat jalur berkas dimulai, sehingga dapat dipangkas.
        // Ini mengasumsikan jalur file lengkap selalu dimulai dengan salah satu akar sumber, jika tidak
        // entri mungkin berasal dari sumber eksternal dan ini mengembalikan null.
        nilai  relevanSourceRoot =
            sourceRoots.firstOrNull { fullFilePath.startsWith(it) } ?: kembalikan  null
        nilai  jalurberkas = jalurberkaslengkap.substringAfter(relevantSourceRoot)
        kembalikan jalurfile.removePrefix( " / " )
    }

    /**
     * Membuat tautan ke sumber dokumen yang dapat didokumentasikan menggunakan [baseLink] dan [nama] yang disediakan.
     *
     * Mengembalikan null jika [baseLink] adalah null atau dokumen yang tidak memiliki entri sumber.
     */
     kesenangan pribadi < T > T . createLinkToSource (
        baseLink : String ?,
        nama : String ,
    ): String ? di mana T : Dapat didokumentasikan , T : DenganSumber {
        baseLink ?: kembalikan  null
        val  paths = getSourceFilePaths() ?: kembalikan  null
        // Kurangi daftar jalur menjadi satu jalur dengan mengambil awalan umum dari semuanya.
        val  jalur = jalur.kurangi { currPrefix , jalurberikutnya -> currPrefix.commonPrefixWith(jalurberikutnya) }
        kembalikan baseLink.format(jalur, nama)
    }

    /**
     * Mengembalikan apakah fungsi atau properti DRI awalnya tingkat atas (baik tidak dalam
     * kelas atau berada dalam kelas sintetis).
     */
     kesenangan  pribadi DRI . isTopLevel (): Boolean =
        namakelas == null || docsHolder.isFromSyntheticClass(ini)

     objek pendamping {

        /**
         * Mengonversi tanda tangan metode ke string yang cocok dengan format di apiSince JSON
         */
        menyenangkan  apiSinceMethodSignature ( fungsi : DFunction ): String {
            // Metadata menggunakan Java API, jadi gunakan JvmName jika ada
            val  namafungsi = fungsi.namajvm() ?: nama.fungsi

            val  generik =
                jika (fungsi.generik.kosong()) {
                    " "
                } kalau tidak {
                    " < " + fungsi.generik.joinToString( " , " ) { it.metalavaName() } + " > "
                }

            // Metadata menggunakan API Java, pindahkan penerima ke parameter
             parameter nilai =
                jika (fungsi.penerima != null ) {
                    fungsi.convertReceiverForJava().parameter
                } kalau tidak {
                    fungsi.parameter
                }

            nilai  tipeparameter =
                Parameter
                    .peta { param ->
                        val  tipeNamaDasar =
                            param.tipe
                                .tulis ulangKotlinPrimitivesUntukJava(
                                    gunakanQualifiedTypes = benar ,
                                )
                                NamaMetalava()

                        // Vararg Kotlin terpisah dari representasi tipe
                        // Pengubah parameter lain yang memengaruhi tanda tangan adalah `suspend`,
                        // yang
                        // ditangani dalam [FunctionalTypeConstructor.functionalTypeMetalavaName()]
                         pengubah nilai = param.pengubah(param.getExpectOrCommonSourceSet())
                        val  tambahan = jika (pengubah.berisi( " vararg " )) " ... "  jika tidak  " "

                        " $ basicTypeName $ tambahan "
                    }
                    .daftar yang dapat diubah()

            // Fungsi `suspend` memiliki argumen kelanjutan di API Java yang tidak muncul di
            // representasi Kotlin. Pengubah fungsi lainnya tidak memengaruhi tanda tangan Java
            jika (fungsi.pengubah(fungsi.getExpectOrCommonSourceSet()).berisi( " suspend " )) {
                paramTypes += " kotlin.coroutines.Kelanjutan<? super kotlin.Unit> "
            }

            kembalikan  """ $ functionName $ generics ( ${ paramTypes.joinToString( " , " ) } ) """
        }

        /**
         * Mengubah [Proyeksi] menjadi nama tipe gaya Java, yang digunakan dalam
         * apiSince metadata.
         *
         * Ini berdasarkan [ParameterDocumentableConverter.Companion.nameForJavaArray], tetapi dimaksudkan
         * untuk mencocokkan tanda tangan yang dihasilkan oleh metalava.
         */
         Proyeksi kesenangan  pribadi .metalavaName () : String =
            ketika (ini) {
                adalah  TypeParameter -> nama
                // perhapsAsJava() diperlukan di sini karena Metalava menghasilkan tampilan Java dari tipe
                // Contoh: java.lang.String dan kotlin.String keduanya direpresentasikan sebagai
                // java.lang.String
                adalah  GenericTypeConstructor -> {
                    val  bersarang =
                        jika (proyeksi.kosong()) {
                            " "
                        } kalau tidak {
                            """ < ${ proyeksi.joinToString( " , " ) { it.metalavaName() } } > """
                        }
                    " ${ dri.possiblyAsJava().namalengkap } $ bersarang "
                }
                dapat  dibatalkan -> inner.metalavaName()
                PastinyaTidakDapatDibatalkan  -> inner.metalavaName(
 )
                adalah  TypeAliased -> inner.metalavaName()
                adalah  UnresolvedBound -> nama
                // `? extends Object` bersifat redundan, ia ada dalam metadata sebagai "?"
                adalah  Kovariansi <*> ->
                    jika (bagian dalam adalah  JavaObject ) {
                        " Apa itu? "
                    } kalau tidak {
                        " ? memperluas ${ inner.metalavaName() } "
                    }
                adalah  Kontravarian <*> -> " ? super ${ inner.metalavaName() } "
                adalah  Invariansi <*> -> inner.metalavaName()
                is PrimitiveJavaType -> name
                Void -> "void"
                Star -> "?"
                is JavaObject -> "java.lang.Object"
                is FunctionalTypeConstructor -> functionalTypeMetalavaName()
                Dynamic -> throw RuntimeException("Invalid State: trying to get name of a Dynamic")
            }

        /**
         * Converts the [FunctionalTypeConstructor] to its Java-style type name, which is what is
         * used in the apiSince metadata.
         */
        private fun FunctionalTypeConstructor.functionalTypeMetalavaName(): String {
            // `suspend` function types appear as `kotlin.coroutines.SuspendFunction<N>` in the
            // model, but in the Java signature as `kotlin.jvm.functions.Function<N+1>`
            val typeName =
                if (isSuspendable) {
                    val num = dri.classNames!!.substringAfter("SuspendFunction").toInt() + 1
                    "kotlin.jvm.functions.Function$num"
                } else {
                    dri.fullName
                }

            val paramNames =
                projections.dropLast(1).map {
                    val name = it.metalavaName()
                    // Non-object param types appear as contravariance in the metadata, while
                    // object params just appear as Object -- except for in `suspend` functions,
                    // where
                    // `? super Object` does show up.
                    if (it is Invariance<*> && (name != "java.lang.Object" || isSuspendable)) {
                        "? super $name"
                    } else {
                        name
                    }
                }
            val returnName =
                projections.last().let { type ->
                    type.metalavaName().let { name ->
                        if (isSuspendable) {
                            // `suspend` functions have their return types wrapped in a
                            // Continuation, and
                            // an extra `?` added at the end of the list of generics
                            "? super kotlin.coroutines.Continuation<? super $name>,?"
                        } else if (name == "java.lang.Object") {
                            // An object return type appears in the metadata as "?"
                            "?"
                        } else {
                            val innerType = (type as? Invariance<*>)?.inner?.unwrapNullability()
                            if (
                                innerType is PrimitiveJavaType ||
                                    (innerType is GenericTypeConstructor &&
                                        innerType.projections.isEmpty())
                            ) {
                                // Simple types appear as-is in the metadata, more complex types
                                // include
                                // "? extends" first
                                name
                            } else {
                                "? extends $name"
                            }
                        }
                    }
                }
            val nested = (paramNames + returnName).joinToString(",")

            return "$typeName<$nested>"
        }

        /**
         * Converts the [DTypeParameter] to its Java-style type name, which is what is used in the
         * apiSince metadata.
         */
        private fun DTypeParameter.metalavaName(): String {
            val boundsNames =
                bounds
                    .map { it.metalavaName() }
                    // `extends java.lang.Object` is redundant and not included in the metadata
                    // Filtering by if `it !is JavaObject` doesn't work because the `JavaObject` may
                    // be nested in a different `Projection`
                    .filter { it != "java.lang.Object" }
            val bounds =
                if (boundsNames.isEmpty()) {
                    ""
                } else {
                    // This is always "extends", even if the bound represents an interface
                    " extends " + boundsNames.joinToString(" & ")
                }
            return name + bounds
        }

        /**
         * Remove an outer nullability wrapper from the [Projection], if one exists. Does not recur
         * into nested projections.
         */
        private fun Projection.unwrapNullability(): Projection =
            ketika (ini) {
                dapat  dibatalkan -> bagian dalam
                adalah  PastiTidakDapatDibatalkan -> bagian dalam
                yang lain -> ini
            }
    }
}

typealias  SourceEntry = Peta.Entri < DokkaConfiguration.DokkaSourceSet , DocumentableSource >

  
