/*
* Hak Cipta 2020 Proyek Sumber Terbuka Android
*
* Dilisensikan berdasarkan Lisensi Apache, Versi 2.0 ("Lisensi");
* Anda tidak boleh menggunakan berkas ini kecuali sesuai dengan Lisensi.
* Anda dapat memperoleh salinan Lisensi di
*
 *      http://www.apache.org/licenses/LICENSE-2.0
*
* Kecuali jika diwajibkan oleh hukum yang berlaku atau disetujui secara tertulis, perangkat lunak
* didistribusikan di bawah Lisensi didistribusikan atas dasar "SEBAGAIMANA ADANYA",
* TANPA JAMINAN ATAU KETENTUAN APAPUN, baik tersurat maupun tersirat.
* Lihat Lisensi untuk bahasa spesifik yang mengatur izin dan
* batasan berdasarkan Lisensi.
*/

paket  com . google . devsite . renderer . converters

impor  org . jetbrains . dokka . links . Dapat dipanggil
impor  organisasi . otak jet . dok . tautan . Referensi Kelas Java
impor  org . jetbrains . dokka . links . Dapat dibatalkan
impor  organisasi . otak jet . dok . tautan . Tipe Rekursif
impor  organisasi . otak jet . dok . tautan . Proyeksi Bintang
impor  organisasi . otak jet . dok . tautan . Tipe Konstruktor
impor  organisasi . otak jet . dok . tautan . TipeParam
impor  organisasi . otak jet . dok . tautan . Referensi Tipe

/**
* Mengembalikan jangkar untuk suatu simbol, tanpa awalan #.
*
* Format default dalam gaya Java 12: `foo(Foo,Bar)`.
*/
internal  menyenangkan  Dapat dipanggil . jangkar (
    terbuka : String = " ( " ,
    pemisah : String = " , " ,
    tutup : String = " ) " ,
): Rangkaian {
    val  receiverStr = jika (penerima == null ) " "  yang lain  " $ buka ${ receiver!!.nama() } $ tutup . "
    val  tanda tangan = params.joinToString(pemisah) { it.name() }
    kembalikan  " $ receiverStr $ nama $ buka $ tanda tangan $ tutup "
}

pribadi  menyenangkan  TypeReference . nama (): String =
    ketika (ini) {
        adalah  JavaClassReference -> nama
        adalah  TypeConstructor -> fullyQualifiedName
        adalah  Nullable -> wrapped.name()
        // Parameter tidak digunakan dalam tautan kdoc, jadi ini hanya relevan untuk tautan Java ke Kotlin
        // Apa pun yang memiliki beberapa batasan dalam kode Java ditautkan sebagai batasan pertama....
        // (lihat uji integrasi "sederhana" Fraggy#createType)
        adalah  TypeParam -> batas.first().name()
        adalah  RecursiveType ,
        Proyeksi Bintang -> " "
    }
