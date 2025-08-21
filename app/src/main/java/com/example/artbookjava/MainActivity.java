package com.example.artbookjava;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.artbookjava.databinding.ActivityMainBinding;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding; // View Binding nesnesi
    ArrayList<Art> artArrayList;
    ArtAdapter artAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // Bu satır kalabilir

        // 1. View Binding'i initialize et
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        // 2. binding.getRoot() ile layout'u set et
        setContentView(binding.getRoot());

        // Eğer Toolbar eklediyseniz ve activity_main.xml içinde ID'si "toolbar" ise:
        // androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar); // Eski yöntem
        // setSupportActionBar(toolbar);
        // Binding üzerinden erişim (Eğer toolbar ID'si activity_main.xml'de varsa ve binding sınıfında oluştuysa):
        if (binding.toolbar != null) { // Toolbar ID'si activity_main.xml'de "toolbar" olmalı
            setSupportActionBar(binding.toolbar);
        }


        // Geri kalan onCreate kodunuz...
        // RecyclerView'a binding üzerinden erişim
        ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerView, (v, insets) -> { // binding.recyclerView ID'si activity_main.xml'de "recyclerView" olmalı
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        artArrayList = new ArrayList<>();

        // RecyclerView'a binding üzerinden erişim
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        artAdapter = new ArtAdapter(artArrayList);
        binding.recyclerView.setAdapter(artAdapter);

        getData();
    }

    private void getData() {
        try {
            SQLiteDatabase database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null);
            Cursor cursor = database.rawQuery("SELECT * FROM arts", null);
            int nameIx = cursor.getColumnIndex("name");
            int idIx = cursor.getColumnIndex("id");
            while (cursor.moveToNext()){
                String name = cursor.getString(nameIx);
                int id = cursor.getInt(idIx);
                System.out.println("Name: " + name + " ID: " + id);
                Art art = new Art(name, id);
                artArrayList.add(art);
            }
            artAdapter.notifyDataSetChanged();
            cursor.close();
            // database.close(); // Opsiyonel: Burada veya onStop/onDestroy'da kapatılabilir. Şimdilik orijinaldeki gibi bırakıyorum.


        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.art_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item){
        if(item.getItemId() == R.id.add_art){
            Intent intent = new Intent(this, ArtActivity.class);
            intent.putExtra("info", "new");
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
