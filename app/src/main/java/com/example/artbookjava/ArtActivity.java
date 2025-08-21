package com.example.artbookjava;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import java.io.ByteArrayOutputStream; // ByteArrayInputStream yerine ByteArrayOutputStream import edildi.
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArtActivity extends AppCompatActivity {

    // View Binding nesnesi: Layout'taki view'lara erişmek için.
    private ActivityArtBinding binding;
    // Kullanıcının galeriden seçtiği resmi tutacak Bitmap nesnesi.
    Bitmap selectedImage;

    // Galeri activity'sini başlatmak ve sonucunu almak için ActivityResultLauncher.
    ActivityResultLauncher<Intent> activityResultLauncher;

    // İzinleri istemek ve sonucunu almak için ActivityResultLauncher.
    ActivityResultLauncher<String[]> permissionLauncher;
    SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // activity_art.xml layout'unu inflate et ve binding nesnesini oluştur.
        binding = ActivityArtBinding.inflate(getLayoutInflater());
        // Activity'nin content view'ını binding'in root view'ı olarak ayarla.
        setContentView(binding.getRoot());

        database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null);

        // activityResultLauncher ve permissionLauncher'ı kaydet ve callback'lerini tanımla.
        registerLauncher();

        // Intent'ten bilgileri al.
        Intent intent = getIntent();
        String info = intent.getStringExtra("info");
        if (info.equals("new")) {
            binding.nameText.setText("");
            binding.artistText.setText("");
            binding.yearText.setText("");
            binding.button.setVisibility(View.VISIBLE);
            binding.imageView.setImageResource(R.drawable.selected);

            //new art

        }else{
            int artId = intent.getIntExtra("artId", 1);
            binding.button.setVisibility(View.INVISIBLE);
            //butonu gizledik
            try {
                Cursor cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?", new String[]{String.valueOf(artId)});
                int nameIx = cursor.getColumnIndex("name");
                int artistIx = cursor.getColumnIndex("artist");
                int yearIx = cursor.getColumnIndex("year");
                int imageIx = cursor.getColumnIndex("image");
                //idye göre art çekme işleme bir dizi içinde artId ile bulunup o listeleniyor
                while (cursor.moveToNext()) {
                    binding.nameText.setText(cursor.getString(nameIx));
                    binding.artistText.setText(cursor.getString(artistIx));
                    binding.yearText.setText(cursor.getString(yearIx));
                    byte[] bytes = cursor.getBlob(imageIx);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    binding.imageView.setImageBitmap(bitmap);
                }
                cursor.close();


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * "Kaydet" butonuna tıklandığında çağrılır.
     * Seçilen resmi ve diğer bilgileri (isim, sanatçı vb.) veritabanına kaydeder.
     * @param view Tıklanan view (buton).
     */
    public void save(View view) {
        // Önce seçili bir resim olup olmadığını kontrol et.
        if (selectedImage == null) {
            Toast.makeText(this, "Please select an image first.", Toast.LENGTH_LONG).show();
            return; // selectedImage null ise metottan çık, kaydetme işlemi yapma.
        }

        String name = binding.nameText.getText().toString();
        String artist = binding.artistText.getText().toString();
        String year = binding.yearText.getText().toString();

        // Resmi küçült. makeSmalllerImage null dönebilir, bu durumu da kontrol et.
        Bitmap smallImage = makeSmalllerImage(selectedImage, 300);

        if (smallImage == null) {
            Toast.makeText(this, "Error processing image.", Toast.LENGTH_SHORT).show();
            return; // Resim küçültülemediyse metottan çık.
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.PNG, 50, outputStream);
        byte[] byteArray = outputStream.toByteArray();

        try {

            database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY, name VARCHAR, artist VARCHAR, year VARCHAR, image BLOB)"); // id eklendi
            String sqlString = "INSERT INTO arts (name, artist, year, image) VALUES (?, ?, ?, ?)";
            SQLiteStatement sqlStatement = database.compileStatement(sqlString);
            sqlStatement.bindString(1, name);
            sqlStatement.bindString(2, artist);
            sqlStatement.bindString(3, year);
            sqlStatement.bindBlob(4, byteArray);
            sqlStatement.execute();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving art to database.", Toast.LENGTH_LONG).show();
            return; // Veritabanı hatası olursa da MainActivity'ye hemen dönme, kullanıcı görsün.
            // Veya sadece Toast gösterip bu return'ü kaldırabilirsiniz, hata olsa bile döner.
        }

        // Başarılı kayıttan sonra MainActivity'ye dön.
        Toast.makeText(this, "Art saved successfully!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(ArtActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Önceki activity'leri temizle
        startActivity(intent);
        finish(); // ArtActivity'yi sonlandır, geri tuşuyla tekrar gelinmesin.
    }

    /**
     * Verilen Bitmap'i belirtilen maksimum boyuta göre küçültür.
     * @param image Küçültülecek Bitmap.
     * @param maximumSize Genişlik veya yükseklik için maksimum piksel değeri.
     * @return Küçültülmüş Bitmap veya image null ise null.
     */
    public Bitmap makeSmalllerImage(Bitmap image, int maximumSize) {
        if (image == null) {
            return null; // Gelen resim null ise null dön.
        }
        int width = image.getWidth();
        int height = image.getHeight();
        float bitmapRatio = (float) width / (float) height;

        if (bitmapRatio > 1) { // Yatay resim
            width = maximumSize;
            height = (int) (width / bitmapRatio);
        } else { // Dikey veya kare resim
            height = maximumSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    /**
     * "Resim Seç" butonuna tıklandığında çağrılır. İzinleri kontrol eder ve galeriyi açar.
     * @param view Tıklanan view.
     */
    public void selected(View view) {
        String[] permissionsToRequestArray;
        List<String> permissionsNeeded = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            permissionsToRequestArray = permissionsNeeded.toArray(new String[0]);
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
            openGallery();
        }
    }

    /**
     * Cihazın galerisini açmak için bir Intent başlatır.
     */
    private void openGallery() {
        Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        activityResultLauncher.launch(intentToGallery);
    }

    /**
     * ActivityResultLauncher'ları kaydeder ve callback'lerini tanımlar.
     */
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

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                (Map<String, Boolean> permissionsResultMap) -> {
                    boolean canAccessMedia = false;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        canAccessMedia = permissionsResultMap.getOrDefault(Manifest.permission.READ_MEDIA_IMAGES, false) ||
                                permissionsResultMap.getOrDefault(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED, false);
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        canAccessMedia = permissionsResultMap.getOrDefault(Manifest.permission.READ_MEDIA_IMAGES, false);
                    } else {
                        canAccessMedia = permissionsResultMap.getOrDefault(Manifest.permission.READ_EXTERNAL_STORAGE, false);
                    }

                    if (canAccessMedia) {
                        openGallery();
                    } else {
                        Toast.makeText(ArtActivity.this, "Storage permission is required to select an image!", Toast.LENGTH_LONG).show();
                    }
                });
    }
}
