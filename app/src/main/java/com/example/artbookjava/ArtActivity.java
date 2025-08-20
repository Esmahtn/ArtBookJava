package com.example.artbookjava;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.artbookjava.databinding.ActivityArtBinding;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArtActivity extends AppCompatActivity {

    // View Binding nesnesi: Layout'taki view'lara erişmek için.
    private ActivityArtBinding binding;
    // Kullanıcının galeriden seçtiği resmi tutacak Bitmap nesnesi.
    Bitmap selectedImage;

    // Galeri activity'sini başlatmak ve sonucunu almak için ActivityResultLauncher.
    // Intent tipinde bir girdi alır (galeriyi açmak için).
    ActivityResultLauncher<Intent> activityResultLauncher;

    // İzinleri istemek ve sonucunu almak için ActivityResultLauncher.
    // String dizisi tipinde bir girdi alır (istenecek izinler).
    ActivityResultLauncher<String[]> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // activity_art.xml layout'unu inflate et ve binding nesnesini oluştur.
        binding = ActivityArtBinding.inflate(getLayoutInflater());
        // Activity'nin content view'ını binding'in root view'ı olarak ayarla.
        setContentView(binding.getRoot());

        // activityResultLauncher ve permissionLauncher'ı kaydet ve callback'lerini tanımla.
        // Bu, Activity Result API'lerinin doğru çalışması için onCreate içinde yapılmalıdır.
        registerLauncher();
    }

    /**
     * "Kaydet" butonuna tıklandığında çağrılır.
     * Seçilen resmi ve diğer bilgileri (isim, sanatçı vb.) veritabanına kaydetme
     * işlemleri burada yapılmalıdır. Bu örnekte sadece bir Toast mesajı gösteriliyor.
     * @param view Tıklanan view (buton).
     */
    public void save(View view) {
        // TODO: Seçilen resmi ve diğer sanat eseri bilgilerini veritabanına kaydetme mantığını ekle.
        Toast.makeText(this, "Save clicked", Toast.LENGTH_SHORT).show();
    }

    /**
     * "Resim Seç" (veya benzeri) bir butona tıklandığında çağrılır.
     * Cihazın Android sürümüne göre gerekli depolama/medya izinlerini kontrol eder,
     * izin yoksa ister ve izin varsa galeriyi açar.
     * @param view Tıklanan view (buton).
     */
    public void selected(View view) {
        // Kullanıcıdan istenecek izinlerin dinamik olarak belirleneceği String dizisi.
        String[] permissionsToRequestArray;
        // Cihazın Android sürümüne göre o an gerekli olan ve henüz verilmemiş izinlerin listesi.
        List<String> permissionsNeeded = new ArrayList<>();

        // Adım 1: Cihazın Android sürümüne göre hangi izinlerin gerektiğini belirle.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14 (API 34) ve üzeri
            // Android 14 ve üzerinde, kullanıcılara "Tüm fotoğraflara erişim" veya
            // "Belirli fotoğrafları seçme" seçenekleri sunulur.
            // Bu nedenle hem READ_MEDIA_IMAGES hem de READ_MEDIA_VISUAL_USER_SELECTED istenir.

            // READ_MEDIA_IMAGES iznini kontrol et.
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
            // READ_MEDIA_VISUAL_USER_SELECTED iznini kontrol et.
            // Bu izin, kullanıcının yalnızca belirli fotoğrafları seçmesine olanak tanır.
            // Manifest'te de deklare edilmiş olmalıdır.
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);
            }

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 (API 33)
            // Android 13'te granüler medya izinleri geldi. Sadece resimler için READ_MEDIA_IMAGES istenir.
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else { // Android 12L (API 32) ve altı
            // Eski sürümlerde genel depolama izni olan READ_EXTERNAL_STORAGE istenir.
            // Bu izin manifest'te android:maxSdkVersion="32" ile sınırlandırılmalıdır.
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        // Adım 2: Eğer istenmesi gereken izinler varsa (liste boş değilse) izin isteme sürecini başlat.
        if (!permissionsNeeded.isEmpty()) {
            // İzin listesini (List<String>) bir String dizisine (String[]) çevir.
            // permissionLauncher.launch() metodu String[] bekler.
            permissionsToRequestArray = permissionsNeeded.toArray(new String[0]);

            // Kullanıcıya neden izin gerektiğini açıklayan bir mesaj (rationale) gösterilmeli mi?
            boolean showRationale = false;
            for (String perm : permissionsToRequestArray) {
                // ActivityCompat.shouldShowRequestPermissionRationale() metodu,
                // kullanıcı daha önce bu izni reddetmişse (ama "Bir daha sorma" dememişse) true döner.
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, perm)) {
                    showRationale = true;
                    break; // Herhangi bir izin için rationale gerekiyorsa döngüden çık.
                }
            }

            if (showRationale) {
                // Kullanıcıya iznin neden gerekli olduğunu bir Snackbar ile açıkla.
                // Kullanıcı "Give Permission" butonuna tıklarsa izinler istenir.
                Snackbar.make(view, "Permission needed for gallery access.", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Give Permission", v -> permissionLauncher.launch(permissionsToRequestArray))
                        .show();
            } else {
                // Rationale göstermeye gerek yok (izin ilk kez isteniyor veya kullanıcı "Bir daha sorma" demiş).
                // İzinleri doğrudan iste.
                permissionLauncher.launch(permissionsToRequestArray);
            }
        } else {
            // İstenmesi gereken yeni bir izin yok, yani tüm gerekli izinler zaten verilmiş.
            // Doğrudan galeriyi aç.
            openGallery();
        }
    }

    /**
     * Cihazın galerisini açmak için bir Intent başlatır.
     * Sonuç activityResultLauncher tarafından işlenir.
     */
    private void openGallery() {
        // Galeriden bir içerik (resim) seçmek için Intent oluştur.
        // ACTION_PICK: Bir veri öğesi seçmek için standart eylem.
        // MediaStore.Images.Media.EXTERNAL_CONTENT_URI: Cihazın dış depolamasındaki resimlere işaret eder.
        Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // Galeriyi açmak için activityResultLauncher'ı kullan.
        activityResultLauncher.launch(intentToGallery);
    }

    /**
     * ActivityResultLauncher'ları (hem galeri sonucu hem de izin sonucu için)
     * onCreate sırasında kaydeder ve callback'lerini tanımlar.
     */
    private void registerLauncher() {
        // 1. Galeriden resim seçme sonucunu işlemek için launcher:
        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), // Standart bir activity başlatma ve sonuç alma contract'ı.
                result -> { // Lambda ifadesiyle ActivityResultCallback<ActivityResult>
                    // Kullanıcı bir resim seçip "Tamam" (RESULT_OK) dediyse:
                    if (result.getResultCode() == RESULT_OK) {
                        // Seçilen resmi içeren Intent'i al.
                        Intent intentFromResult = result.getData();
                        // Intent ve içindeki veri (resim URI'si) null değilse:
                        if (intentFromResult != null && intentFromResult.getData() != null) {
                            Uri imageData = intentFromResult.getData(); // Seçilen resmin URI'sini al.
                            try {
                                // Resmi Bitmap'e çevir.
                                // Android P (API 28) ve üzeri için modern ImageDecoder sınıfını kullan.
                                if (Build.VERSION.SDK_INT >= 28) {
                                    ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), imageData);
                                    selectedImage = ImageDecoder.decodeBitmap(source);
                                } else {
                                    // Eski sürümler için MediaStore.Images.Media.getBitmap kullan.
                                    selectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), imageData);
                                }
                                // Seçilen resmi layout'taki ImageView'da göster.
                                binding.imageView.setImageBitmap(selectedImage);
                            } catch (Exception e) {
                                e.printStackTrace();
                                Toast.makeText(ArtActivity.this, "Error loading image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });

        // 2. İzin isteme sonucunu işlemek için launcher:
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), // Birden fazla izin isteme contract'ı.
                (Map<String, Boolean> permissionsResultMap) -> { // Lambda ile ActivityResultCallback<Map<String, Boolean>>
                    // permissionsResultMap: İstenen her izin (String) için
                    // verilip verilmediğini (Boolean) tutan bir harita.
                    boolean canAccessMedia = false; // Medyaya erişim izni var mı? Varsayılan olarak false.

                    // Android sürümüne göre hangi izinlerin verildiğini kontrol et:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14 (API 34) ve üzeri
                        // Android 14+'da, kullanıcı "Tüm fotoğraflara izin ver" (READ_MEDIA_IMAGES)
                        // veya "Fotoğrafları seç" (READ_MEDIA_VISUAL_USER_SELECTED) diyebilir.
                        // Bu iki izinden en az birinin verilmesi medyaya erişim için yeterlidir.
                        canAccessMedia = permissionsResultMap.getOrDefault(Manifest.permission.READ_MEDIA_IMAGES, false) ||
                                permissionsResultMap.getOrDefault(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED, false);
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 (API 33)
                        // Android 13'te sadece READ_MEDIA_IMAGES izninin verilmesi yeterlidir.
                        canAccessMedia = permissionsResultMap.getOrDefault(Manifest.permission.READ_MEDIA_IMAGES, false);
                    } else { // Android 12L (API 32) ve altı
                        // Eski sürümlerde READ_EXTERNAL_STORAGE izninin verilmesi yeterlidir.
                        canAccessMedia = permissionsResultMap.getOrDefault(Manifest.permission.READ_EXTERNAL_STORAGE, false);
                    }

                    // Sonuç: Medyaya erişim izni alınabildiyse galeriyi aç.
                    if (canAccessMedia) {
                        openGallery();
                    } else {
                        // Gerekli izin(ler) verilmemişse kullanıcıya bir Toast mesajı göster.
                        Toast.makeText(ArtActivity.this, "Storage permission is required!", Toast.LENGTH_LONG).show();
                    }
                });
    }
}
