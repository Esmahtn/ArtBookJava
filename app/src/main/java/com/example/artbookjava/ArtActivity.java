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

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
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

    private ActivityArtBinding binding;
    Bitmap selectedImage;
    ActivityResultLauncher<Intent> activityResultLauncher;
    // DEĞİŞİKLİK: Birden fazla izni işlemek için String[] oldu
    ActivityResultLauncher<String[]> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArtBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        registerLauncher();
    }

    public void save(View view) {
        Toast.makeText(this, "Save clicked", Toast.LENGTH_SHORT).show();
    }

    public void selected(View view) {
        String[] permissionsToRequestArray;
        List<String> permissionsNeeded = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14 (API 34) ve üzeri
            // READ_MEDIA_IMAGES her zaman istenir
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
            // READ_MEDIA_VISUAL_USER_SELECTED her zaman istenir (sistem diyaloğu yönetir)
            // Bu izni ayrıca kontrol etmeye gerek yok, READ_MEDIA_IMAGES ile birlikte istenir.
            // Ancak, kullanıcının yalnızca belirli fotoğrafları seçmesine izin vermek için manifest'te olmalı
            // ve izin isteğine dahil edilmelidir.
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);
            }

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 (API 33)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else { // Android 12L (API 32) ve altı
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            permissionsToRequestArray = permissionsNeeded.toArray(new String[0]);
            // İzin gerekçesini göster (isteğe bağlı, ama iyi bir pratik)
            // Basitlik adına, birden fazla izin için tek bir gerekçe gösterilebilir veya her biri için ayrı ayrı.
            // Bu örnekte, herhangi bir izin için rationale gösterilecek.
            boolean showRationale = false;
            for (String perm : permissionsToRequestArray) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, perm)) {
                    showRationale = true;
                    break;
                }
            }

            if (showRationale) {
                Snackbar.make(view, "Permission needed for gallery access.", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Give Permission", v -> permissionLauncher.launch(permissionsToRequestArray))
                        .show();
            } else {
                permissionLauncher.launch(permissionsToRequestArray);
            }
        } else {
            // Tüm gerekli izinler zaten verilmiş
            openGallery();
        }
    }

    private void openGallery() {
        Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        activityResultLauncher.launch(intentToGallery);
    }

    private void registerLauncher() {
        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent intentFromResult = result.getData();
                        if (intentFromResult != null && intentFromResult.getData() != null) {
                            Uri imageData = intentFromResult.getData();
                            try {
                                if (Build.VERSION.SDK_INT >= 28) {
                                    ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), imageData);
                                    selectedImage = ImageDecoder.decodeBitmap(source);
                                } else {
                                    selectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), imageData);
                                }
                                binding.imageView.setImageBitmap(selectedImage);
                            } catch (Exception e) {
                                e.printStackTrace();
                                Toast.makeText(ArtActivity.this, "Error loading image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });

        // DEĞİŞİKLİK: RequestMultiplePermissions ve Map<String, Boolean> callback
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                (Map<String, Boolean> permissionsResultMap) -> {
                    boolean allPermissionsGranted = true;
                    // Android 14 ve üzerinde, READ_MEDIA_IMAGES veya READ_MEDIA_VISUAL_USER_SELECTED'dan
                    // en az birinin verilmesi genellikle yeterlidir (sistem seçimi yönetir).
                    // Ancak, burada tüm *istenen* izinlerin verilip verilmediğini kontrol ediyoruz.
                    // Daha karmaşık bir senaryoda, hangi izinlerin kritik olduğunu belirleyebilirsiniz.

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
                        // Kullanıcı "Fotoğrafları Seç" dediğinde, READ_MEDIA_IMAGES 'true' olmayabilir
                        // ama READ_MEDIA_VISUAL_USER_SELECTED 'true' olur.
                        // Ya da kullanıcı "Tüm fotoğraflara izin ver" dediyse READ_MEDIA_IMAGES 'true' olur.
                        boolean canAccessMedia = permissionsResultMap.getOrDefault(Manifest.permission.READ_MEDIA_IMAGES, false) ||
                                permissionsResultMap.getOrDefault(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED, false);
                        if (canAccessMedia) {
                            openGallery();
                        } else {
                            allPermissionsGranted = false; // Aslında bu değişkeni artık doğrudan kullanmıyoruz, ama mantığı koruyalım.
                        }
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13
                        if (permissionsResultMap.getOrDefault(Manifest.permission.READ_MEDIA_IMAGES, false)) {
                            openGallery();
                        } else {
                            allPermissionsGranted = false;
                        }
                    } else { // Android 12L ve altı
                        if (permissionsResultMap.getOrDefault(Manifest.permission.READ_EXTERNAL_STORAGE, false)) {
                            openGallery();
                        } else {
                            allPermissionsGranted = false;
                        }
                    }

                    if (!allPermissionsGranted && !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && (permissionsResultMap.getOrDefault(Manifest.permission.READ_MEDIA_IMAGES, false) || permissionsResultMap.getOrDefault(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED, false)) )) {
                        // Eğer Android 14+ değilse VEYA Android 14+ olup hiçbir medya erişim izni verilmediyse
                        Toast.makeText(ArtActivity.this, "Storage permission is required!", Toast.LENGTH_LONG).show();
                    }
                });
    }
}

