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

#include "jdwpargs.h"

#include <algorithm>
#include <sstream>

#include "base/logging.h"  // For VLOG.

namespace adbconnection {

JdwpArgs::JdwpArgs(const std::string& opts) {
  std::stringstream ss(opts);

  // Dipisahkan berdasarkan karakter ','
  while (!ss.eof()) {
    std::string w;
    getline(ss, w, ',');

    // Pangkas spasi
    w.erase(std::remove_if(w.begin(), w.end(), ::isspace), w.end());

    // Ekstrak kunci=nilai
    auto pos = w.find('=');

    // Periksa format yang buruk seperti tidak ada '=' atau '=' di salah satu ujung
    if (pos == std::string::npos || w.back() == '=' || w.front() == '=') {
      VLOG(jdwp) << "Skipping jdwp parameters '" << opts << "', token='" << w << "'";
      melanjutkan ;
    }

    // Mengatur
    std::string key = w.substr(0, pos);
    std::string value = w.substr(pos + 1);
    put(key, value);
    VLOG(jdwp) << "Found jdwp parameters '" << key << "'='" << value << "'";
  }
}

void JdwpArgs::put(const std::string& key, const std::string& value) {
  if (store.find(key) == store.end()) {
    keys.emplace_back(key);
  }

  store[key] = value;
}

std::string JdwpArgs::join() {
  std::string opts;
  for (const auto& key : keys) {
    opts += key + "=" + store[key] + ",";
  }

  // Hapus tanda koma di belakangnya jika ada
  if (opts.length() >= 2) {
    opts = opts.substr(0, opts.length() - 1);
  }

  return opts;
}
}   // ruang nama adbconnection
