package com.example.finalproject;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class DisplayPicture extends AppCompatActivity {
    FirebaseDatabase database;
    DatabaseReference myRef;
    ImageView albumPicture,previous,next, home,delete;
    ArrayList<Bitmap> albumPictureBitmaps;
    ArrayList<String> keys;
    int currImagePos = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_picture);
        database = FirebaseDatabase.getInstance("https://facial-emotion-detection-app-default-rtdb.firebaseio.com");
        myRef = database.getReference().child("Image");
        albumPicture = findViewById(R.id.imageView2);
        previous = findViewById(R.id.buttonPrevious);
        next = findViewById(R.id.buttonNext);
        home = findViewById(R.id.returnHome);
        delete = findViewById(R.id.deleteImage);
        albumPictureBitmaps = new ArrayList<>();
        keys = new ArrayList<>();
        makeAlbum(myRef);
        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currImagePos > 0) {
                    currImagePos--;
                    displayData(currImagePos);
                }
            }

        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currImagePos < albumPictureBitmaps.size()-1) {
                    currImagePos++;
                    displayData(currImagePos);
                }
            }
        });
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DisplayPicture.this, MainActivity.class);
                startActivity(intent);
            }
        });
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(albumPictureBitmaps.size()>0){
                    myRef.child(keys.get(currImagePos)).removeValue();
                    keys.remove(currImagePos);
                    albumPictureBitmaps.remove(currImagePos);
                    currImagePos -= 1;
                    if (currImagePos < 0) {
                        currImagePos = 0;
                    }
                    displayData(currImagePos);
                }
            }
        });


    }

    private void makeAlbum(DatabaseReference myRef) {
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                if(dataSnapshot.exists()) {
                    //Log.d("TAG", "Value is: " + dataSnapshot.toString());
                    albumPictureBitmaps.clear();
                    keys.clear();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String base64Bitmap = snapshot.getValue().toString();
                        keys.add(snapshot.getKey());
                        byte[] decodedString = Base64.decode(base64Bitmap, Base64.DEFAULT);
                        albumPictureBitmaps.add(BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
                        Log.d("TAG", keys.toString());
                    }
                    displayData(0);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w("TAG", "Failed to read value.", error.toException());
            }
        });
    }

    private void displayData(int position) {
        if(albumPictureBitmaps.size() > 0){
            //albumPicture.setImageBitmap(Bitmap.createScaledBitmap(albumPictureBitmaps.get(position),480,640,false));
            albumPicture.setImageBitmap(albumPictureBitmaps.get(position));
        }
        else{
            albumPicture.setImageBitmap(null);
        }
    }
}