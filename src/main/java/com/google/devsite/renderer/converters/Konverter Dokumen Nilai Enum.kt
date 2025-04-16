/*
* Hak Cipta 2020 Proyek Sumber Terbuka Android
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

impor  com . google . devsite . components . DeskripsiKomponen
impor  com . google . devsite . komponen . Tautan
impor  com.google.devsite.komponen.impl.DefaultPropertySignature
impor  com.google.devsite.components.impl.DefaultSymbolDetail
impor  com.google.devsite.components.impl.DefaultTableRowSummaryItem
impor  com . google . devsite . komponen . simbol . PropertySignature
impor  com . google . devsite . komponen . simbol . DetailSimbol
impor  com.google.devsite.components.table.TableRowSummaryItem
impor  com . google . devsite . renderer . Bahasa
impor  com.google.devsite.renderer.impl.paths.FilePathProvider
impor  organisasi . otak jet . dok . model . DEnum
impor  organisasi . otak jet . dok . model . DEnumEntri
impor  organisasi . otak jet . dok . model . GenericTypeConstructor

/** Mengubah DEnumEntrys yang dapat didokumentasikan menjadi komponen EnumValue. */
 kelas  internal EnumValueDocumentableConverter (
     tampilan val  pribadiBahasa : Bahasa ,
     jalur pribadi valProvider  : FilePathProvider ,
    pribadi  val  javadocConverter : DocTagConverter ,
    pribadi  nilai  paramConverter : ParameterDocumentableConverter ,
    pribadi  val  annotationConverter : AnnotationDocumentableConverter ,
) {

    /** @mengembalikan komponen ringkasan nilai enum */
    // YANG HARUS DILAKUKAN(KMP, b/256172699)
     ringkasan menyenangkan ( enumValue : DEnumEntry ): TableRowSummaryItem < Link , DescriptionComponent > {
        kembalikan DefaultTableRowSummaryItem(
            RingkasanItemBarisTabel.Params(
                judul = penyedia jalur.linkuntukReferensi(enumValue.dri, enumValue.name),
                deskripsi = javadocConverter.summaryDescription(nilai enum),
            ),
        )
    }

    /** @mengembalikan komponen detail enum */
    // YANG HARUS DILAKUKAN(KMP, b/256172699)
     detail menyenangkan (
        dEnum : DEnum ,
        nilai enum : entri DEnum ,
        petunjuk : ModifierHints
    ): DetailSimbol < TandaTandaProperti > {
        val ( anotasi tipe , anotasi nontipe ) =
            dEnum.annotations(dEnum.getExpectOrCommonSourceSet()).partition {
                itu.milikOnReturnType()
            }
         proyeksi val =
            paramConverter.komponenUntukProyeksi(
                GenericTypeConstructor(dEnum.dri, Daftar kosong()),
                isJavaSource = dEnum.isFromJava(),
                setsumber = nilaienum.getExpectOrCommonSourceSet(),
                // Meskipun secara teknis ENUM_VALUE adalah anggota ENUM_TYPE? karena Anda selalu dapat
                // tentukan nilai enum yang `null`, ini bukan informasi yang berguna
                propagatedNullability = Nullability.DONT_CARE,
                propagatedAnnotations = typeAnnotations,
            )
        kembalikan DefaultSymbolDetail(
            Detail Simbol.Params(
                nama = enumValue.nama,
                returnType = proyeksi,
                symbolKind = DetailSimbol.JenisSimbol.PROPERTI_HANYA_BACA,
                tanda tangan = enumValue.tanda tangan(),
                jangkar = enumValue.generateAnchors(),
                metadatanya =
                    javadocConverter.metadata(
                        dapat didokumentasikan = enumValue,
                        returnType = proyeksi,
                        paramNames = daftar(),
                        deprecationAnnotation = nonTypeAnnotations.deprecationAnnotation(),
                        isFromJava = dEnum.isFromJava(),
                    ),
                bahasatampil = bahasatampil,
                pengubah =
                    enumNilai
                        .getExtraModifiers(enumValue.getExpectOrCommonSourceSet())
                        .modifiersFor(petunjuk),
                komponen anotasi =
                    konverter anotasi.komponen anotasi(
                        anotasi = nonTypeAnnotations,
                        nullability = Nullability.DONT_CARE, // Lihat di atas
                    ),
            ),
        )
    }

    // YANG HARUS DILAKUKAN(KMP, b/256172699)
    internal  menyenangkan  DEnumEntry . tanda tangan (): PropertySignature {
        kembalikan DefaultPropertySignature(
            Tanda Tangan Properti.Params(
                // TODO(b/168136770): cari tahu jalur untuk jangkar default
                nama = pathProvider.linkForReference(dri),
                penerima = null ,
            ),
        )
    }

    /** Mengembalikan jangkar untuk nilai enum ini. */
    pribadi  menyenangkan  DEnumEntry . generateAnchors (): LinkedHashSet < String > {
        kembalikan linkedSetOf(
            nama,
        )
    }
}
