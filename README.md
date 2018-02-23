# IF 3111 Platform Based Development
## Android Application

## Deskripsi Umum Aplikasi
Aplikasi Android **Smart Home** digunakan sebagai *controller* untuk melakukan suatu *task* dari Sistem **Smart Home** dari integrasi dengan aplikasi Unity dan Arduino. 
<br />
Pada aplikasi ini dimanfaatkan 2 sensor yaitu sensor *accelerometer* dan *proximity* untuk membantu pengguna agar melakukan sebuah *task* menjadi lebih mudah. Sensor *accelerometer* digunakan untuk membuka atau menutup pintu dan sensor *proximity* digunakan untuk menyalakan atau mematikan lampu. Selain itu aplikasi Android dapat menerima notifikasi dari FCM (Firebase Cloud Messaging) dan memanfaatkan Google Location Service untuk melakukan pemantauan lokasi hp terhadap rumah pengguna.
<br />
Pada aplikasi ini, pengguna dapat dengan mudah melakukan *sign-in* dengan menggunakan **Google SSO** (Single Sign On)

## Panduan Instalasi Aplikasi
Buka project `android` yang telah di *download* dengan menggunakan `Android Studio`, selanjutnya lakukan *sync* dengan build.gradle aplikasi. Setelah itu buat sebuah *project* di `Firebase Console` dan tambahkan `Android` ke dalam `Firebase` kemudian *download* `google-services.json` agar *push notification* dan **Google SSO** dapat diterima atau dilakukan oleh aplikasi.

## Penggunaan Aplikasi
Pada saat aplikasi pertama berjalan, maka pengguna wajib melakukan *login* dengan menggunakan akun **Google** yang telah dimiliki. Selanjutnya pengguna akan dibawa ke halaman, dimana pengguna dapat merubah status rumah seperti pintu, lampu, dan alarm, merubah lokasi rumah secara manual, dan mengirimkan keluhan melalui email.

### AnjasNFriendsCabangK1
13515045 Hutama Tefotuho Hulu | 13515069 Hisham Lazuardi Yusuf | 13515147 Zacki Zulfikar Fauzi