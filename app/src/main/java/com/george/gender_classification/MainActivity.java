package com.george.gender_classification;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.UiThread;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.george.gender_classification.tflite.Classifier;
import com.george.gender_classification.tflite.ClassifierFloatMobileNet;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView resultView;
    private ImageView imageView;
    private Snackbar progressBar;
    private Classifier classifier;

    //Camera field
    private static final int CAMERA_REQUEST = 1888;
    private static final int PERMISSIONS_REQUEST = 1;
    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static final int INPUT_SIZE = 224;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Initialize views
        resultView = findViewById(R.id.textViewResult);
        imageView = findViewById(R.id.imageViewPhoto);
        progressBar = Snackbar.make(imageView, "PROCESSING IMAGE", Snackbar.LENGTH_INDEFINITE);

        //Initialize classifier
        try {
            classifier = new ClassifierFloatMobileNet(this);
        } catch (IOException e) {
            return;
        }

        //Check for camera permissions after marshmallow
        if (hasPermission()) {
            Toast.makeText(this, "Permissions Ok", Toast.LENGTH_SHORT).show();
        } else {
            requestPermission();
        }

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Initialize taking picture
                takePicture();

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(
            final int requestCode, final String[] permissions, final int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            } else {
                requestPermission();
            }
        }
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
                Toast.makeText(
                        this,
                        "Camera permission is required for this demo",
                        Toast.LENGTH_SHORT)
                        .show();
            }
            requestPermissions(new String[]{PERMISSION_CAMERA}, PERMISSIONS_REQUEST);
        }
    }


    //Start activity for result
    private void takePicture() {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST);
        }else{
            Toast.makeText(
                    this,
                    "You don't have camera!",
                    Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            Bitmap picture = (Bitmap) data.getExtras().get("data");//this is your bitmap image and now you can do whatever you want with this
            imageView.setImageBitmap(picture);
            //Proceed to inference
            processImage(picture);
        }
    }

    private void processImage(final Bitmap bitmap) {
        if (classifier == null) {
            Log.e("Classifier: ", "null");
        }

        Bitmap bitmapConvert = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
        final List<Classifier.Recognition> results = classifier.recognizeImage(bitmapConvert);


        //Show results
        showResultsInBottomSheet(results);
    }

    @UiThread
    private void showResultsInBottomSheet(List<Classifier.Recognition> results) {

        StringBuilder stringBuilder = new StringBuilder();
        if (results.size() >= 1) {

            for (int i = 0; i < results.size(); i++) {
                Classifier.Recognition recognition = results.get(1);
                if (recognition != null) {
                    if (recognition.getTitle() != null)
                        stringBuilder.append(recognition.getTitle());
                    stringBuilder.append(" ");

                    if (recognition.getConfidence() != null)
                        stringBuilder.append(String.format("%.2f", (100 * recognition.getConfidence())) + "%");
                }

                stringBuilder.append("\n");
            }

            resultView.setText(stringBuilder.toString());
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
