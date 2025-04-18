/*
* Hak Cipta (C) 2017 Proyek Sumber Terbuka Android
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

Jika tidak ada konten yang ditemukan dalam file ART_ADBCONNECTION_ADBCONNECTION_H_
#define ART_ADBCONNECTION_ADBCONNECTION_H_

#include <jni.h>
#include <stdint.h>
#include <sys/socket.h>
#include <sys/un.h>

#include <limits>
#include <memory>
#include <string>
#include <vector>

#include "adbconnection/client.h"
#include "base/array_ref.h"
#include "base/mutex.h"
#include "runtime_callbacks.h"

namespace adbconnection {

static constexpr char kAdbConnectionThreadName[] = "ADB-JDWP Connection Control Thread";

// Nama agen jdwp default.
static constexpr char kDefaultJdwpAgentName[] = "libjdwp.so";

class AdbConnectionState;

struct AdbConnectionDebuggerController : public art::DebuggerControlCallback {
  explicit AdbConnectionDebuggerController(AdbConnectionState* connection)
      : connection_(connection) {}

  // Mulai menjalankan debugger.
  void StartDebugger() override;

  // Debugger harus mulai dimatikan karena runtime telah berakhir.
  void StopDebugger() override;

  bool IsDebuggerConfigured() override;

 pribadi :
  AdbConnectionState* connection_;
Bahasa Indonesia: };

enum class DdmPacketType : uint8_t { kReply = 0x80, kCmd = 0x00, };

struct AdbConnectionDdmCallback : public art::DdmCallback {
  explicit AdbConnectionDdmCallback(AdbConnectionState* connection) : connection_(connection) {}

  void DdmPublishChunk(uint32_t type,
                       const art::ArrayRef<const uint8_t>& data)
      REQUIRES_SHARED(art::Locks::mutator_lock_);

 pribadi :
  AdbConnectionState* connection_;
Bahasa Indonesia: };

struct AdbConnectionAppInfoCallback : public art::AppInfoCallback {
  explicit AdbConnectionAppInfoCallback(AdbConnectionState* connection) : connection_(connection) {}

  void AddApplication(const std::string& package_name) REQUIRES_SHARED(art::Locks::mutator_lock_);
  void RemoveApplication(const std::string& package_name)
      REQUIRES_SHARED(art::Locks::mutator_lock_);
  void SetCurrentProcessName(const std::string& process_name)
      REQUIRES_SHARED(art::Locks::mutator_lock_);
  void SetWaitingForDebugger(bool waiting) REQUIRES_SHARED(art::Locks::mutator_lock_);
  void SetUserId(int uid) REQUIRES_SHARED(art::Locks::mutator_lock_);

 pribadi :
  AdbConnectionState* connection_;
Bahasa Indonesia: };

class AdbConnectionState {
 publik :
  explicit AdbConnectionState(const std::string& name);
  ~AdbConnectionState();

  // Dipanggil pada thread mendengarkan untuk mulai menangani input baru. thr digunakan untuk melampirkan input baru.
  // utas ke runtime.
  void RunPollLoop(art::Thread* self);

  // Mengirim data ddms melalui soket, jika ada. Data ini dikirim bahkan jika kita belum selesai
  // berjabat tangan.
  void PublishDdmData(uint32_t type, const art::ArrayRef<const uint8_t>& data);

  // Bangunkan jajak pendapat. Panggil ini jika rangkaian peristiwa menarik telah berubah. Mereka akan
  // dihitung ulang dan polling akan dipanggil lagi melalui penulisan fdevent. Wakeup ini bergantung pada fdevent
  // dan harus diakui melalui AcknowledgeWakeup.
  void WakeupPollLoop();

  // Setelah panggilan ke WakeupPollLoop, penghitung internal fdevent harus dikurangi melalui
  // metode ini. Ini harus dipanggil setelah WakeupPollLoop dipanggil dan polling dipicu.
  void AcknowledgeWakeup();

  // Menghentikan thread debugger selama penghentian.
  void StopDebuggerThreads();

  // Jika StartDebuggerThreads berhasil dipanggil.
  bool DebuggerThreadsStarted() {
    return started_debugger_threads_;
  }

  // Harus dipanggil oleh Framework saat mengubah nama prosesnya.
  void SetCurrentProcessName(const std::string& process_name);

  // Harus dipanggil oleh Framework saat menambahkan aplikasi ke suatu proses.
  // Ini dapat dipanggil beberapa kali (lihat android:process)
  void AddApplication(const std::string& package_name);

  // Harus dipanggil oleh Framework saat menghapus aplikasi dari suatu proses.
  void RemoveApplication(const std::string& package_name);

  // Harus dipanggil oleh Framework saat status debugging-nya berubah.
  void SetWaitingForDebugger(bool waiting);

  // Harus dipanggil oleh Framework saat UserID diketahui.
  void SetUserId(int uid);

 pribadi :
  uint32_t NextDdmId();

  void StartDebuggerThreads();

  // Beritahu adbd tentang runtime yang baru.
  bool SetupAdbConnection();

  std::string MakeAgentArg();

  void SendAgentFds(bool require_handshake);

  void CloseFds();

  void HandleDataWithoutAgent(art::Thread* self);

  void PerformHandshake();

  void AttachJdwpAgent(art::Thread* self);

  void NotifyDdms(bool active);

  void SendDdmPacket(uint32_t id,
                     DdmPacketType type,
                     uint32_t ddm_type,
                     art::ArrayRef<const uint8_t> data);

  std::string agent_name_;

  AdbConnectionDebuggerController controller_;
  AdbConnectionDdmCallback ddm_callback_;
  AdbConnectionAppInfoCallback appinfo_callback_;

  // Eventfd digunakan untuk mengizinkan fungsi StopDebuggerThreads untuk membangunkan thread yang sedang tidur
  android::base::unique_fd sleep_event_fd_;

  // Konteks yang membungkus soket yang kita gunakan untuk berkomunikasi dengan adbd.
  std::unique_ptr<AdbConnectionClientContext, void(*)(AdbConnectionClientContext*)> control_ctx_;

  // Soket yang kita gunakan untuk berbicara dengan agen (jika dimuat).
  android::base::unique_fd local_agent_control_sock_;

  // Fd soket yang digunakan agen untuk berbicara dengan kita. Kita perlu menyimpannya agar dapat dibersihkan
  // mengaktifkannya saat waktu proses berakhir.
  android::base::unique_fd remote_agent_control_sock_;

  // Fd yang diteruskan melalui adb ke klien. Ini dijaga oleh
  // adb_write_event_fd_.
  android::base::unique_fd adb_connection_socket_;

  // fd yang kami kirim ke agen untuk memungkinkan kami menyinkronkan akses ke adb_connection_socket_ yang dibagikan.
  // Ini juga digunakan sebagai kunci umum untuk adb_connection_socket_ pada semua utas selain
  // utas jajak pendapat.
  android::base::unique_fd adb_write_event_fd_;

  std::atomic<bool> shutting_down_;

  // Benar jika kita telah memuat pustaka agen.
  std::atomic<bool> agent_loaded_;

  // Benar jika transportasi dt_fd_forward mendengarkan saluran komunikasi baru.
  std::atomic<bool> agent_listening_;

  // Benar jika transport dt_fd_forward memiliki soket. Jika demikian, kami tidak melakukan apa pun pada agen atau
  // soket koneksi adb hingga koneksi hilang.
  std::atomic<bool> agent_has_socket_;

  std::atomic<bool> sent_agent_fds_;

  std::atomic<bool> performed_handshake_;

  bool notified_ddm_active_;

  std::atomic<uint32_t> next_ddm_id_;

  bool started_debugger_threads_;

  friend struct AdbConnectionDebuggerController;
Bahasa Indonesia: };

}   // ruang nama adbconnection

# akhiri   // ART_ADBCONNECTION_ADBCONNECTION_H_
