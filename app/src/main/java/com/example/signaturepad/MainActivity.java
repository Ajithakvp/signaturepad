package com.example.signaturepad;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.gcacace.signaturepad.views.SignaturePad;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {


    private SignaturePad signaturePad;
    private Button clearButton;
    private Button saveButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);


        signaturePad = findViewById(R.id.signature_pad);
        clearButton = findViewById(R.id.clear_button);
        saveButton = findViewById(R.id.save_button);

        signaturePad.setOnSignedListener(new SignaturePad.OnSignedListener() {
            @Override
            public void onStartSigning() {
                // Called when the user starts signing
            }

            @Override
            public void onSigned() {
                // Called when the user finishes signing
                enableButtons(true);
            }

            @Override
            public void onClear() {
                // Called when the pad is cleared
                enableButtons(false);
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signaturePad.clear();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bitmap signatureBitmap = signaturePad.getSignatureBitmap();
                saveSignatureToDownloads(MainActivity.this, signatureBitmap, "signature11.png");


            }
        });

        // Initially disable buttons since there's no signature
        enableButtons(false);
    }

    private void saveSignatureToDownloads(Context context, Bitmap bitmap, String fileName) {
        // Check if the external storage is writable
        String folderName = "MySignatures"; // Optional: Create a folder name within Downloads


        // Get the Downloads directory
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        // Optional: Create a subdirectory within Downloads
        File folder = new File(downloadsDir, folderName);
        if (!folder.exists()) {
            folder.mkdirs(); // Create the directory if it does not exist
        }

        // Save the bitmap to a file
        File file = new File(folder, fileName);
        try {
            OutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.flush();
            stream.close();

            // Insert the image into the MediaStore
            insertImageIntoMediaStore(context.getContentResolver(), file.getAbsolutePath(), fileName);

            //Toast.makeText(context, "Signature saved to Downloads folder", Toast.LENGTH_SHORT).show();

            // After saving locally, send the signature to server
            sendSignatureToServer(file);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to save signature", Toast.LENGTH_SHORT).show();
        }
    }

    private void insertImageIntoMediaStore(ContentResolver resolver, String imagePath, String imageName) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, imageName);
        values.put(MediaStore.Images.Media.DISPLAY_NAME, imageName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());

        // Insert into MediaStore without setting _data
        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            Log.e("TAG", "Failed to insert image into MediaStore");
            return;
        }

        try {
            // Open an OutputStream to write data into the newly inserted image file
            OutputStream outputStream = resolver.openOutputStream(uri);
            if (outputStream != null) {
                // Write the image data from the file into the OutputStream
                File imageFile = new File(imagePath);
                byte[] buffer = new byte[1024];
                int bytesRead;
                FileInputStream inputStream = new FileInputStream(imageFile);
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                inputStream.close();
                outputStream.close();
            } else {
                Log.e("TAG", "Failed to open OutputStream for MediaStore image URI");
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("TAG", "IOException while writing image data to MediaStore");
        }
    }


    private void sendSignatureToServer(File signatureFile) {
        Toast.makeText(this, signatureFile.getPath(), Toast.LENGTH_SHORT).show();
    }





    private void enableButtons(boolean enable) {
        clearButton.setEnabled(enable);
        saveButton.setEnabled(enable);
    }


}
