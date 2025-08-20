package com.example.artbookjava;

import android.Manifest;
// import android.content.Context; // Bu import kullanılmıyor, kaldırılabilir.
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri; // Resim URI'sini kullanmak için eklendi.
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

// import androidx.activity.EdgeToEdge; // Eğer bu aktivitede EdgeToEdge kullanılmayacaksa kaldırılabilir.
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.artbookjava.databinding.ActivityArtBinding;
import com.google.android.material.snackbar.Snackbar;

// import java.util.Map; // Map kullanılmıyor, kaldırılabilir (RequestMultiplePermissions kullanılsaydı gerekirdi).

public class ArtActivity extends AppCompatActivity {
    // View Binding kullanıyorsanız, initialize etmeyi unutmayın.
    // Örneğin: private ActivityArtBinding binding;
    // onCreate içinde: binding = ActivityArtBinding.inflate(getLayoutInflater());
    // setContentView(binding.getRoot());
    private ActivityArtBinding binding;
    Bitmap selectedImage;
    ActivityResultLauncher<Intent> activityResultLauncher;
    ActivityResultLauncher<String> permissionLauncher; // DÜZELTME: String[] yerine String

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // Bu aktivitede gerekli değilse kaldırılabilir.
        setContentView(R.layout.activity_art); // Kendi layout dosyanızın adı bu olmalı.

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.recyclerView), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        registerLauncher(); // Launcher'ları onCreate'de kaydetmek iyi bir pratiktir.
    }

    public void save(View view) {

    }

    // Bu metodun XML'deki ImageView'ın onClick özelliği ile çağrıldığını varsayıyorum.
    public void selected(View view) {
        // İzin kontrolü ve isteme
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // İzin verilmemiş
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Kullanıcıya neden izin gerektiği açıklanmalı (Snackbar ile)
                Snackbar.make(view, "Permission needed to access gallery to select an image.", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Give Permission", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // İzin iste
                                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                            }
                        }).show();
            } else {
                // Açıklama gerekmiyor, direkt izin iste
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        } else {
            // İzin zaten verilmiş
            // Galeriyi aç
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activityResultLauncher.launch(intentToGallery);
        }
    }

    private void registerLauncher() {
        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK) {
                           Intent intentFromResult = result.getData();
                           if(intentFromResult != null) {
                              Uri imageData = intentFromResult.getData();
                              //binding.imageView.setImageURI(imageData);
                               try {
                                   if(Build.VERSION.SDK_INT>=28){
                                       ImageDecoder.Source source= ImageDecoder.createSource(getContentResolver(), imageData);
                                       selectedImage =ImageDecoder.decodeBitmap(source);
                                       binding.imageView.setImageBitmap(selectedImage);
                                   } else {
                                       selectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), imageData);
                                       binding.imageView.setImageBitmap(selectedImage);
                                   }
                               }catch (Exception e){
                                   e.printStackTrace();

                               }
                           }
                        }
                    } // activityResultLauncher'ın onActivityResult metodunun kapanış parantezi
                }); // activityResultLauncher için registerForActivityResult çağrısının sonu ve noktalı virgülü

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), // Tek izin için RequestPermission
                new ActivityResultCallback<Boolean>() { // Geri dönüş tipi Boolean (verildi/verilmedi)
                    @Override
                    public void onActivityResult(Boolean isGranted) { // Parametre adı isGranted
                        if (isGranted) {
                            // Permission granted
                            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            activityResultLauncher.launch(intentToGallery); // Galeriyi activityResultLauncher ile başlat.
                        } else {
                            // Permission denied
                            Toast.makeText(ArtActivity.this, "Permission needed to access gallery!", Toast.LENGTH_LONG).show();
                        }
                    } // permissionLauncher'ın onActivityResult metodunun kapanış parantezi
                }); // permissionLauncher için registerForActivityResult çağrısının sonu ve noktalı virgülü
    }
}
