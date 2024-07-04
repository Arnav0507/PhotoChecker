package com.example.finalproject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;


import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final int CAMERA_ACTION_CODE = 1;
    ImageView picture, click, addToAlbum, showAlbum;
    Bitmap photo;
    TextView message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        picture = findViewById(R.id.imageView);
        message = findViewById(R.id.textView);
        click = findViewById(R.id.takePicture);
        addToAlbum = findViewById(R.id.saveImage);
        showAlbum = findViewById(R.id.showAlbum);
        addToAlbum.setVisibility(View.INVISIBLE);
        click.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addToAlbum.setVisibility(View.INVISIBLE);
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if(intent.resolveActivity(getPackageManager()) != null){
                    startActivityForResult(intent, CAMERA_ACTION_CODE);
                }
                else{
                    startActivityForResult(intent, CAMERA_ACTION_CODE);
                    //Toast.makeText(MainActivity.this, "This app doesn't support camera access", Toast.LENGTH_SHORT).show();
                }
            }
        });
        showAlbum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,DisplayPicture.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == CAMERA_ACTION_CODE && resultCode == RESULT_OK && data != null){
            Bundle bundle = data.getExtras();
            photo = (Bitmap)bundle.get("data");
            picture.setImageBitmap(photo);
            processEmotionDetection(photo);
        }
    }

    private void processEmotionDetection(Bitmap photo) {
        FaceDetectorOptions highAccuracyOpts =
            new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .build();
        InputImage image = InputImage.fromBitmap(photo, 0);
        FaceDetector detector = FaceDetection.getClient(highAccuracyOpts);
        Task<List<Face>> result =
                detector.process(image)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<Face>>() {
                                    @Override
                                    public void onSuccess(List<Face> faces) {
                                        // Task completed successfully
                                        Toast.makeText(MainActivity.this, "Success",Toast.LENGTH_SHORT).show();
                                        Boolean goodPicture = true;
                                        int countSmile = 0;
                                        int countEye = 0;
                                        int countFaces = 0;
                                        if(faces.size() > 0){
                                            goodPicture = true;
                                        }
                                        else{
                                            Toast.makeText(MainActivity.this, "No Faces Detected", Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        for(Face face: faces) {
                                            if (face.getSmilingProbability() != null){
                                                float smileProb = face.getSmilingProbability();
                                                float leftEyeProb = face.getLeftEyeOpenProbability();
                                                float rightEyeProb = face.getRightEyeOpenProbability();
                                                if(smileProb > 0.7){
                                                    countSmile++;
                                                }
                                                if(leftEyeProb > 0.7 && rightEyeProb > 0.7){
                                                    countEye++;
                                                }
                                                if(smileProb < 0.7 || leftEyeProb < 0.7 || rightEyeProb < 0.7){
                                                    goodPicture = false;
                                                }
                                            }
                                            countFaces++;
                                        }
                                        displayResult(goodPicture, countSmile, countEye, countFaces);
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        Toast.makeText(MainActivity.this, "Failed",Toast.LENGTH_SHORT).show();
                                    }
                                });
    }

    private void displayResult(boolean goodPicture, int countSmile, int countEye, int countFaces) {
        message.setText("Number of people detected:" + countFaces + "\nNumber of people smiling: " + countSmile + "\nNumber of people with both eyes open: " + countEye);
        if(goodPicture){
            Toast.makeText(MainActivity.this, "Automatically added to album", Toast.LENGTH_SHORT).show();
            saveData();
        }
        else{
            addToAlbum.setVisibility(View.VISIBLE);
            addToAlbum.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(MainActivity.this, "Added to Album", Toast.LENGTH_SHORT).show();
                    saveData();
                }
            });
        }
    }

    private void saveData() {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
        String dateTime = formatter.format(date);
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://facial-emotion-detection-app-default-rtdb.firebaseio.com");
        DatabaseReference myRef = database.getReference().child("Image");
        myRef.child(dateTime).setValue(encodeImage(photo));
    }

    public String encodeImage(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        String imageEncoded = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT);
        return imageEncoded;
    }
}