/*
* Hak Cipta 2024 Proyek Sumber Terbuka Android
*
* Dilisensikan berdasarkan Lisensi Apache, Versi 2.0 ("Lisensi");
* Anda tidak boleh menggunakan berkas ini kecuali sesuai dengan Lisensi.
* Anda dapat memperoleh salinan Lisensi di
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
* Kecuali jika diwajibkan oleh hukum yang berlaku atau disetujui secara tertulis, perangkat lunak
* didistribusikan di bawah Lisensi didistribusikan pada BASIS "SEBAGAIMANA ADANYA",
* TANPA JAMINAN ATAU KETENTUAN APAPUN, baik tersurat maupun tersirat.
* Lihat Lisensi untuk bahasa spesifik yang mengatur izin dan
* batasan berdasarkan Lisensi.
*/

paket  com . google . devsite . transformers

impor  com . google . devsite . renderer . converters . addAnnotations
impor  com . google . devsite . renderer . converters . annotations
impor  com . google . devsite . renderer . converters . companion
impor  com . google . devsite . renderer . converters . fullName
impor  com.google.devsite.renderer.converters.getExpectOrCommonSourceSet
impor  org . jetbrains . dokka . model . Anotasi . Anotasi
impor  org . jetbrains . dokka . model . DAnnotation
impor  org . jetbrains . dokka . model . DClass
impor  organisasi . otak jet . dok . model . Seperti Kelas D
impor  organisasi . otak jet . dok . model . DEnum
impor  organisasi . otak jet . dok . model . DEnumEntri
impor  org . jetbrains . dokka . model . DFunction
impor  organisasi . otak jet . dok . model . D Antarmuka
impor  organisasi . otak jet . dok . model . Modul D
impor  organisasi . otak jet . dok . model . Objek
impor  org .jetbrains .dokka .model .DPackage
impor  org . jetbrains . dokka . model . DProperty
impor  organisasi . otak jet . dok . model . Dapat didokumentasikan
impor  organisasi . otak jet . dok . model . Dengan Konstruktor
impor  organisasi . otak jet . dok . model . properti . Kontainer Properti
impor  org . jetbrains . dokka . model . properties . WithExtraProperties
impor  org . jetbrains . dokka . plugability . DokkaContext
impor  org . jetbrains . dokka . transformers . dokumentasi . DocumentableTransformer

/** Menyebarkan anotasi dari elemen ke anggotanya. */
kelas  PropagatedAnnotationsTransformer (
    pribadi  val  propagatingAnnotations : Daftar < String >,
) : Transformator yang dapat didokumentasikan {
    mengesampingkan  fun  invoke ( asli : DModule , konteks : DokkaContext ): DModule {
        jika (propagatingAnnotations.isEmpty()) kembalikan yang asli

        kembalikan salinan asli (paket = peta paket asli { ubah(itu) })
    }

     transformasi kesenangan  pribadi ( asli : DPackage ): DPackage {
        nilai  anotasiToPropagate = original.getPropagatingAnnotations()
        kembalikan salinan asli(
            fungsi = fungsi.peta asli { transform(it, annotationsToPropagate) },
            properti = properti.asli.peta { transform(it, annotationsToPropagate) },
            classlikes = original.classlikes.map { transform(itu, anotasiUntukDipropagasi) },
        )
    }

    @ Menekan ( " UNCHECKED_CAST " )
     transformasi kesenangan  pribadi ( asli : DClasslike , parentAnnotations : Set < Annotation >): DClasslike {
        val ( newExtra , anotasiUntukDipropagasi ) =
            ketika (asli) {
                adalah  DClass -> original.propagateAnnotations(parentAnnotations)
                adalah  DAnnotation -> original.propagateAnnotations(parentAnnotations)
                adalah  DEnum -> original.propagateAnnotations(parentAnnotations)
                adalah  DInterface -> original.propagateAnnotations(parentAnnotations)
                adalah  DObject -> original.propagateAnnotations(parentAnnotations)
            }

        // Ubah semua anggota kelas yang berlaku.
        val  newFunctions = original.functions.map { transform(it, annotationsToPropagate) }
        val  propertibaru = propertiasli.peta { transform(it, anotasiUntukDipropagasi) }
        val  newClasslikes = original.classlikes.map { transform(itu, anotasiUntukDipropagasi) }
        val  teman baru =
            original.companion()?.let { transform(it, annotationsToPropagate) sebagai  DObject }
        val  konstruktor baru =
            jika (aslinya adalah  WithConstructors ) {
                original.constructors.map { transform(it, annotationsToPropagate) }
            } kalau tidak {
                daftarkosong()
            }

        kembali  saat (asli) {
            adalah  DClass ->
                asli.salinan(
                    konstruktor = konstruktor baru,
                    fungsi = fungsibaru,
                    properti = propertiBaru,
                    classlikes = classlikes baru,
                    pendamping = newCompanion,
                    ekstra = newExtra sebagai  PropertyContainer < DClass >,
                )
            adalah  DAnnotation ->
                asli.salinan(
                    konstruktor = konstruktor baru,
                    fungsi = fungsibaru,
                    properti = propertiBaru,
                    classlikes = classlikes baru,
                    pendamping = newCompanion,
                    ekstra = newExtra sebagai  PropertyContainer < DAnnotation >,
                )
            adalah  DEnum ->
                asli.salinan(
                    entri = entri.peta asli { transform(it, annotationsToPropagate) },
                    konstruktor = konstruktor baru,
                    fungsi = fungsibaru,
                    properti = propertiBaru,
                    classlikes = classlikes baru,
                    pendamping = newCompanion,
                    ekstra = newExtra sebagai  PropertyContainer < DEnum >,
                )
            adalah  DInterface ->
                asli.salinan(
                    fungsi = fungsibaru,
                    properti = propertiBaru,
                    classlikes = classlikes baru,
                    pendamping = newCompanion,
                    ekstra = newExtra sebagai  PropertyContainer < DInterface >,
                )
            adalah  DObject ->
                asli.salinan(
                    fungsi = fungsibaru,
                    properti = propertiBaru,
                    classlikes = classlikes baru,
                    ekstra = newExtra sebagai  PropertyContainer < DObject >,
                )
        }
    }

     transformasi kesenangan  pribadi ( asli : DFunction , parentAnnotations : Set < Annotation >): DFunction {
        // Anotasi tidak disebarkan ke parameter fungsi.
        jika (parentAnnotations.isEmpty()) kembalikan yang asli
        val ( newExtra , _ ) = anotasipropagasiasli(anotasiinduk)
        kembalikan salinan asli(
            ekstra = newEkstra,
        )
    }

     transformasi kesenangan  pribadi ( asli : DProperty , parentAnnotations : Set < Annotation >): DProperty {
        val ( newExtra , annotationsToPropagate ) = original.propagateAnnotations(parentAnnotations)
        if (annotationsToPropagate.isEmpty()) return original
        return original.copy(
            getter = original.getter?.let { transform(it, annotationsToPropagate) },
            setter = original.setter?.let { transform(it, annotationsToPropagate) },
            extra = newExtra,
        )
    }

    private fun transform(original: DEnumEntry, parentAnnotations: Set<Annotation>): DEnumEntry {
        // Annotations are not propagated to members of enum entries because they aren't in docs.
        if (parentAnnotations.isEmpty()) return original
        val (newExtra, _) = original.propagateAnnotations(parentAnnotations)
        return original.copy(
            extra = newExtra,
        )
    }

    /**
     * Propagates the [parentAnnotations] to this documentable, returning the new extra properties
     * containing the annotations and a set of annotations to propagate to this documetable's
     * children -- the [parentAnnotations] plus any propagating annotations present on the
     * documentable.
     *
     * For KMP, this propagates annotations from the common source set to all source sets.
     * (b/262711247: differing deprecation status between source sets isn't handled)
     */
    private fun <T> T.propagateAnnotations(
        parentAnnotations: Set<Annotation>
    ): Pair<PropertyContainer<T>, Set<Annotation>> where
    T : Documentable,
    T : WithExtraProperties<T> {
        val elementAnnotations = getPropagatingAnnotations()
        val annotationsToAdd = parentAnnotations - elementAnnotations
        val annotationsToPropagate = parentAnnotations + elementAnnotations

        val transformed =
            if (annotationsToAdd.isNotEmpty()) {
                extra.addAnnotations(annotationsToAdd, sourceSets)
            } else {
                extra
            }
        return Pair(transformed, annotationsToPropagate)
    }

    private fun Documentable.getPropagatingAnnotations(): Set<Annotation> {
        return annotations(getExpectOrCommonSourceSet())
            .filter { propagatingAnnotations.contains(it.dri.fullName) }
            .untukMengatur()
    }
}
