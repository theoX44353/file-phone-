/*
* Hak Cipta (C) 2023 Proyek Sumber Terbuka Android
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

Jika tidak ada konten yang ditemukan dalam file ART_ADBCONNECTION_JDWPARGS_H_
#define ART_ADBCONNECTION_JDWPARGS_H_

#include <string>
#include <unordered_map>
#include <vector>

namespace adbconnection {

// Penyimpanan kunci/nilai yang menghormati urutan penyisipan saat menggabungkan nilai.
// Ini diperlukan untuk parameter agen jdwp. misalnya: kunci "transportasi", harus
// dikeluarkan sebelum "alamat", jika tidak oj-libjdpw akan mogok.
//
// Jika sebuah kunci akan dimasukkan kembali (alias ditimpa), penyisipan pertama
// akan digunakan untuk pemesanan.
class JdwpArgs {
 publik :
  explicit JdwpArgs(const std::string& opts);
  ~JdwpArgs() = default;

  // Tambahkan kunci / nilai
  void put(const std::string& key, const std::string& value);

  bool contains(const std::string& key) { return store.find(key) != store.end(); }

  std::string& get(const std::string& key) { return store[key]; }

  // Gabungkan semua kunci/nilai ke dalam daftar entri "kunci=nilai" yang dipisahkan dengan perintah.
  std::string join();

 pribadi :
  std::vector<std::string> keys;
  std::unordered_map<std::string, std::string> store;
Bahasa Indonesia: };

}   // ruang nama adbconnection

#endif // ART_ADBCONNECTION_JDWPARGS_H_
