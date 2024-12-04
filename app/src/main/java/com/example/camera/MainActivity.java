package com.example.camera;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** @noinspection ALL*/
public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private VideoView videoView;
    private Uri photoURI;
    private Uri videoURI;

    private final ActivityResultLauncher<Intent> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    imageView.setImageURI(photoURI);
                    imageView.setVisibility(ImageView.VISIBLE);
                    videoView.setVisibility(VideoView.GONE);
                    saveMediaToGallery(photoURI, "image/jpeg");
                } else {
                    Log.e("CameraApp", "Error taking picture");
                    Toast.makeText(this, "Error taking picture", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<Intent> recordVideoLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    videoView.setVideoURI(videoURI);
                    MediaController mediaController = new MediaController(this);
                    mediaController.setAnchorView(videoView);
                    videoView.setMediaController(mediaController);
                    videoView.start();
                    videoView.setVisibility(VideoView.VISIBLE);
                    imageView.setVisibility(ImageView.GONE);
                    saveMediaToGallery(videoURI, "video/mp4");
                } else {
                    Log.e("CameraApp", "Error recording video");
                    Toast.makeText(this, "Error recording video", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<Intent> openGalleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        Uri selectedUri = data.getData();
                        String mimeType = getContentResolver().getType(selectedUri);
                        if (mimeType != null && mimeType.startsWith("image/")) {
                            imageView.setImageURI(selectedUri);
                            imageView.setVisibility(ImageView.VISIBLE);
                            videoView.setVisibility(VideoView.GONE);
                        } else if (mimeType != null && mimeType.startsWith("video/")) {
                            videoView.setVideoURI(selectedUri);
                            MediaController mediaController = new MediaController(this);
                            mediaController.setAnchorView(videoView);
                            videoView.setMediaController(mediaController);
                            videoView.start();
                            videoView.setVisibility(VideoView.VISIBLE);
                            imageView.setVisibility(ImageView.GONE);
                        }
                    }
                } else {
                    Log.e("CameraApp", "Error opening gallery");
                    Toast.makeText(this, "Error opening gallery", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnTakePhoto = findViewById(R.id.btnTakePhoto);
        Button btnRecordVideo = findViewById(R.id.btnRecordVideo);
        Button btnOpenGallery = findViewById(R.id.btnOpenGallery);
        imageView = findViewById(R.id.imageView);
        videoView = findViewById(R.id.videoView);

        btnTakePhoto.setOnClickListener(v -> dispatchTakePictureIntent());
        btnRecordVideo.setOnClickListener(v -> dispatchRecordVideoIntent());
        btnOpenGallery.setOnClickListener(v -> openGallery());
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e("CameraApp", "Error occurred while creating the File", ex);
                Toast.makeText(this, "Error occurred while creating the file", Toast.LENGTH_SHORT).show();
                return;
            }
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                takePictureLauncher.launch(takePictureIntent);
            }
        }
    }

    private void dispatchRecordVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            File videoFile = null;
            try {
                videoFile = createVideoFile();
            } catch (IOException ex) {
                Log.e("CameraApp", "Error occurred while creating the File", ex);
                Toast.makeText(this, "Error occurred while creating the file", Toast.LENGTH_SHORT).show();
                return;
            }
            if (videoFile != null) {
                videoURI = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", videoFile);
                takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoURI);
                recordVideoLauncher.launch(takeVideoIntent);
            }
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/* video/*");
        openGalleryLauncher.launch(intent);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private File createVideoFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String videoFileName = "MP4_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        return File.createTempFile(videoFileName, ".mp4", storageDir);
    }

    private void saveMediaToGallery(Uri uri, String mimeType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveMediaToGalleryApi29AndAbove(uri, mimeType);
        } else {
            saveMediaToGalleryApi28AndBelow(uri, mimeType);
        }
    }

    private void saveMediaToGalleryApi29AndAbove(Uri uri, String mimeType) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()));
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM);

        ContentResolver resolver = getContentResolver();
        Uri collection;
        if (mimeType.startsWith("image/")) {
            collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        } else if (mimeType.startsWith("video/")) {
            collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        } else {
            return;
        }

        Uri itemUri = resolver.insert(collection, values);
        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream out = resolver.openOutputStream(itemUri)) {
            if (out == null) {
                throw new IOException("Failed to open output stream.");
            }
            byte[] buf = new byte[8192];
            int length;
            while ((length = in.read(buf)) > 0) {
                out.write(buf, 0, length);
            }
        } catch (IOException e) {
            if (itemUri != null) {
                resolver.delete(itemUri, null, null);
            }
            e.printStackTrace();
        }
    }

    private void saveMediaToGalleryApi28AndBelow(Uri uri, String mimeType) {
        String filePath = uri.getPath();
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        assert filePath != null;
        File file = new File(filePath);
        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
    }
}
