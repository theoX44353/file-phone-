paket  com . google . devsite . transformers

impor  org . jetbrains . dokka . analysis . kotlin . markdown . NAMA_FILE_ELEMENT_MARKDOWN
impor  organisasi . otak jet . dok . tautan . DRI
impor  org . jetbrains . dokka . model . CheckedExceptions
impor  org . jetbrains . dokka . model . DAnnotation
impor  org . jetbrains . dokka . model . DClass
impor  organisasi . otak jet . dok . model . Seperti Kelas D
impor  organisasi . otak jet . dok . model . DEnum
impor  org . jetbrains . dokka . model . DFunction
impor  organisasi . otak jet . dok . model . D Antarmuka
impor  organisasi . otak jet . dok . model . Modul D
impor  organisasi . otak jet . dok . model . Objek
impor  org . jetbrains . dokka . model . doc . CustomDocTag
impor  organisasi . otak jet . dok . model . dok . Node Dokumentasi
import  org . jetbrains . dokka . model . doc . Throws  sebagai ThrowsTag
impor  org . jetbrains . dokka . plugability . DokkaContext
impor  org . jetbrains . dokka . transformers . dokumentasi . DocumentableTransformer

/** Menambahkan tag dokumentasi yang mewakili pengecualian yang diperiksa dari Java. */
kelas  DocTagsForCheckedExceptionsTransformer : DocumentableTransformer {
    mengesampingkan  fun  invoke ( asli : DModule , konteks : DokkaContext ): DModule =
        asli.salinan(
            paket =
                paket.peta asli { p ->
                    p.salin(kelaslike = p.kelaslike.peta(::transformKelaslike))
                Bahasa Indonesia:
        )

    pribadi  menyenangkan  transformClasslike ( classlike : DClasslike ): DClasslike =
        ketika (seperti kelas) {
            adalah  DInterface ->
                kelaslike.salin(
                    fungsi = classlike.functions.map(::transformFunction),
                    classlike = classlike.classlike.peta(::transformClasslike),
                )
            adalah  DClass ->
                kelaslike.salin(
                    fungsi = classlike.functions.map(::transformFunction),
                    classlike = classlike.classlike.peta(::transformClasslike),
                )
            adalah  DEnum ->
                kelaslike.salin(
                    fungsi = classlike.functions.map(::transformFunction),
                    classlike = classlike.classlike.peta(::transformClasslike),
                )
            adalah  DObject ->
                kelaslike.salin(
                    fungsi = classlike.functions.map(::transformFunction),
                    classlike = classlike.classlike.peta(::transformClasslike),
                )
            adalah  DAnnotation -> seperti kelas
        }

     fungsi transformasi kesenangan  pribadi (
        fungsi : DFungsi ,
    ): Fungsi {
        val  allExceptions = fungsi.extra[CheckedExceptions]?.pengecualian
        kembalikan  jika (allExceptions.isNullOrEmpty()) {
            fungsi
        } kalau tidak {
            val  dokumen baru =
                allExceptions.entries.map { ( set , pengecualian ) ->
                    val  oldDoc = fungsi.dokumentasi[set] ?: DocumentationNode(daftarkosong())
                    diatur ke documentThrows(oldDoc, pengecualian)
                }

            fungsi.salin(dokumentasi = fungsi.dokumentasi + dokumenbaru)
        }
    }

    // Ini hacky; disalin dari hulu
    pribadi  menyenangkan  DRI . fqName (): String ? =
        " $ namapaket . $ namakelas " .takeIf { namapaket != null dan namakelas != null }

     dokumen kesenangan  pribadiMelempar (
        oldDoc : Node Dokumentasi ,
        pengecualian : Daftar < DRI >,
    ): Node Dokumentasi {
        val  dikenalMelempar =
            oldDoc.anak.filterIsInstance< MelemparTag >().mapNotNull { itu.exceptionAddress }.toSet()

        val  lemparTags =
            pengecualian.minus(lemparanyangdiketahui).peta {
                LemparTag(
                    CustomDocTag(nama = NAMA_FILE_ELEMENT_MARKDOWN),
                    itu.fqName().atauEmpty(),
                    dia,
                )
            }

        kembalikan salinan dokumen lama (anak = dokumen lama.anak + lempar tag)
    }
}
