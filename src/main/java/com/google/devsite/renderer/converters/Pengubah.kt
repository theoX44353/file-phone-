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

impor  com . google . devsite . renderer . Bahasa
impor  organisasi . otak jet . dok . Konfigurasi Dokka
impor  org . jetbrains . dokka . model . AdditionalModifiers
impor  organisasi . otak jet . dok . model . D Antarmuka
impor  org .jetbrains .dokka .model .DPackage
impor  org . jetbrains . dokka . model . DProperty
impor  organisasi . otak jet . dok . model . Dapat didokumentasikan
impor  org . jetbrains . dokka . model . DenganAbstraksi
impor  org . jetbrains . dokka . model . WithVisibility
impor  org . jetbrains . dokka . model . properties . WithExtraProperties

/** @mengembalikan daftar lengkap pengubah untuk tipe ini */
internal  menyenangkan  Dapat didokumentasikan . pengubah (
    sourceSet : Konfigurasi Dokka . DokkaSourceSet ,
): Daftar < String > {
    nilai  hasil = mutableListOf< String ?>()
    jika (ini adalah  WithAbstraction ) {
        hasil += listOf(pengubah[sourceSet]?.nama)
    }
    jika (ini adalah  WithVisibility ) {
        hasil += listOf(visibilitas[sourceSet]?.nama)
    }
    jika (ini adalah  WithExtraProperties <*>) {
        hasil += getExtraModifiers(sourceSet)
    }
    kembalikan hasil.filterNotNull().filter { it.isNotEmpty() }
}

/** Mengembalikan daftar pengubah yang disimpan di bidang tambahan AdditionalModifiers yaitu VarArg */
internal  menyenangkan < T : Dengan Properti Ekstra <*>> T . dapatkan Modifikasi Ekstra (
    sourceSet : Konfigurasi Dokka . DokkaSourceSet ,
Bahasa Indonesia:
    extra.allOfType< AdditionalModifiers >().flatMap { pengubah ->
        pengubah.konten[sourceSet]?.peta { itu.nama }?.filter { itu.tidakkosong() } ?: daftarkosong()
    }

/** @return true jika pengubah mewakili simbol konstan, false jika tidak */
internal  menyenangkan  DProperty . isConstant (
    pengubah : Daftar < String > = pengubah(getExpectOrCommonSourceSet()),
Bahasa Indonesia: Boolean {
    kembalikan  " const "  dalam pengubah ||
        // Properti Java `static final` umumnya adalah konstanta, tetapi karena b/241259955 bisa jadi
        // bidang pribadi, jadi jika getter ada, maka itu akan ditampilkan.
        (this.isFromJava() && " static "  di pengubah && " final "  di pengubah && this.getter == null )
}

 kelas data  internal Pengubah ( var baselist : Daftar < String >): ArrayList < String >(baselist) {
  
    konstruktor ( vararg  item : String ): ini(item.sebagaiDaftar())
}

internal  val  EmptyModifiers = Pengubah()

/** Mengembalikan daftar pengubah yang difilter dan ditulis ulang. */
internal  menyenangkan  Daftar < String > .modifiersFor (
    petunjuk : ModifierHints ,
): Pengubah {
     pengubah val = toMutableSet()

    ketika (petunjuk.displayLanguage) {
        Bahasa.JAVA -> {
            // Tulis ulang pengubah yang diketahui
            jika ( " const "  dalam pengubah) {
                jika (!hints.isProperty) lemparkan RuntimeException( " 'const' pada ${ hints.type } ? " )
                pengubah.tambahkan( " statis " )
                modifiers.add( " final " ) // ini terjadi untuk `const val`
            }

            // Apa yang dilakukan @JvmStatic?
            // Ini menyebabkan fungsi/properti-dan-aksesor yang diberi anotasi diangkat ke
            // berisi objek. Itu _tidak_ memengaruhi keberadaan kata kunci `static`. Semua
            // metode yang ada atau diangkat dari objek pendamping _selalu_ statis.
            // Oleh karena itu, @JvmStatic tidak memerlukan penanganan di sini--atau tidak akan diperlukan jika kita tidak menggunakannya sebagai
            // bendera internal untuk menunjukkan bahwa sesuatu telah diangkat. Bendera ini kemudian diubah
            // ke injectStatic. (Ini praktis karena membuat java -> kotlin mudah).

            jika (petunjuk.injectStatic) {
                pengubah.tambahkan( " statis " )
            }

            // `lateinit` pada var pendamping menyebabkan terciptanya bidang pendukung publik yang diangkat
            // tetapi _tidak_ mengangkat aksesor publik.
            // Namun, hal ini tidak memerlukan penanganan di sini, mengingat bahwa kompiler melakukan
            // pengangkatan, untuk alasan yang sama mengapa @JvmStatic tidak memerlukan penanganan khusus.

            // Metode antarmuka bersifat publik secara default; menunjukkannya tidak berguna
            jika (petunjuk.diInterface) {
                pengubah.hapus( " publik " )
            }

            // Anggota objek pendamping dan objek tingkat atas menjadi statis
            // Seperti halnya elemen tingkat atas yang dikonversi menjadi file *Kt, bahkan jika ekstensi
            jika (petunjuk.diPendamping || petunjuk.diPaket) {
                pengubah.tambahkan( " statis " )
            }

            // Java menggunakan pengubah "default" pada metode non-abstrak antarmuka
            // tetapi Dokka upstream membalikkan ini untuk membuat metode antarmuka non-default menjadi "abstrak"
            jika ( " abstrak "  !dalam pengubah && petunjuk.diInterface) {
                pengubah.tambahkan( " default " )
            }

            // Konstruktor Java tidak bisa menjadi `final`
            jika (hints.isConstructor) pengubah.hapus( " final " )

            // Pengubah ini tidak ada di Java
            pengubah.hapus( " suspend " )
            pengubah.hapus( " inline " )
            pengubah.hapus( " noinline " )
            pengubah.hapus( " crossinline " )
            pengubah.hapus( " diwujudkan " )
            pengubah.hapus( " operator " )
            pengubah.hapus( " ganti " )
            pengubah.hapus( " buka " )
            pengubah.hapus( " const " )
            pengubah.hapus( " infix " )
            pengubah.hapus( " data " )
        }
        Bahasa.KOTLIN -> {
            jika (
                " statis "  dalam pengubah &&
                    " final "  dalam pengubah &&
                    petunjuk.isProperty &&
                    petunjuk.isFromJava
            ) {
                pengubah.hapus( " statis " )
                pengubah.hapus( " final " )
                pengubah.tambahkan( " const " )
            }

            // Kami tidak melakukan ini karena biasanya tidak berguna bagi pengguna Kotlin kode Java
            // if ("final" !in pengubah) pengubah.tambahkan("buka")

            // Sejajarkan pengubah default
            pengubah.hapus( " publik " )
            jika ( " override "  !dalam pengubah) {
                pengubah.hapus( " final " )
            }
            jika (petunjuk.diInterface) {
                pengubah.hapus( " abstrak " )
            }

            // Pengubah ini tidak ada di Kotlin
            jika ( " statis "  dalam pengubah) {
                pengubah.hapus( " statis " )
                jika (petunjuk.isFromJava) pengubah.tambahkan( " java-static " )
            }

            // Tidak berguna
            pengubah.hapus( " ganti " )
        }
    }

    jika (petunjuk.isSummary) {
        pengubah.hapus( " publik " )
        pengubah.hapus( " dilindungi " )
    }

    kembalikan Pengubah(pengubah.toList().sortedBy { m -> modifierOrder.indexOf(m) })
}

/**
* Urutan pengubah Kotlin (dari
* https://kotlinlang.org/docs/reference/coding-conventions.html#modifiers )
*/
nilai  pengubahOrder =
    daftar(
        // visibilitas (salah satu)
        " publik " ,
        " dilindungi " ,
        " pribadi " ,
        " dalam " ,
        // Multi-platform (salah satu)
        " mengharapkan " ,
        " sebenarnya " ,
        // Mengandung Cakupan (salah satu)
        " statis " ,
        " java-statis " ,
        // Ekstensibilitas (salah satu)
        " akhir " ,
        " membuka " ,
        " abstrak " ,
        " disegel " ,
        " konstan " ,
        // Lainnya (bisa lebih dari satu)
        " eksternal " ,
        " mengesampingkan " ,
        " terlambat inisiasi " ,
        " ekorrec " ,
        " berisi berita " ,
        " menskors " ,
        "inner",
        // Types (one of)
        "enum",
        "annotation",
        "fun",
        // More (could be more than one)
        "companion",
        "inline",
        "infix",
        "operator",
        "data",
        "noinline",
        "crossinline",
    )

/**
 * Provides modifier hints for what should be shown in the documentation.
 *
 * Note: this is an imperfect approximation that won't be correct in all cases, but Dokka doesn't
 * give us a better solution without replicating compiler functionality. The crux of the problem is
 * that Dokka always includes modifiers even if they weren't specified in the code. Example:
 * ```
 * interface Foo { fun bar() }
 * ```
 *
 * The Dokka modifiers will include `abstract`. While technically correct, that's just noise from
 * the compiler for developers. These hints give us a way of saying "look, developers will know this
 * modifier is implicit." That said, we can only go so far without replicating too much compiler
 * functionality. For example:
 * ```
 * abstract Foo { protected abstract fun foo() }
 * class Bar { public override fun foo() }
 * ```
 *
 * Without digging through the hierarchy to see that `foo()` is actually protected by default, we
 * have no way of knowing we should keep the `public` modifier. (Note: this doesn't affect our
 * understanding that the function is public and should go in the "Public functions" section. It
 * just means the signature will be "incorrect" since it doesn't reflect the real source code.)
 *
 * Sometimes we need to inject the static modifier. For example, when we have an @JvmField hoisted
 * from a companion object, it becomes static. Upstream doesn't inject, and it's not from an Object.
 */
internal data class ModifierHints(
    val displayLanguage: Language,
    val type: Class<out Documentable>,
    val containingType: Class<out Documentable>?,
    val isFromJava: Boolean,
    val isSummary: Boolean = false,
    val injectStatic: Boolean = false,
    val  adalahConstructor : Boolean = salah ,
    val  diCompanion : Boolean = salah ,
) {
    val  di Antarmuka
        dapatkan () = berisiType == DInterface:: kelas .java

    val  dalam Paket
        dapatkan () = berisiType == DPackage:: kelas .java

    val  adalahProperti
        dapatkan () = jenis == DProperty:: kelas .java
}
