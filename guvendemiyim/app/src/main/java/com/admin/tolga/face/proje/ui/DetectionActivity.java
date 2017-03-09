
package com.admin.tolga.face.proje.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.admin.tolga.face.proje.helper.ImageHelper;
import com.admin.tolga.face.proje.helper.LogHelper;
import com.admin.tolga.face.proje.helper.SampleApp;
import com.admin.tolga.face.proje.log.DetectionLogActivity;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.Face;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DetectionActivity extends AppCompatActivity {

    private class DetectionTask extends AsyncTask<InputStream, String, Face[]> {
        private boolean mSucceed = true;

        @Override
        protected Face[] doInBackground(InputStream... params) {

            FaceServiceClient faceServiceClient = SampleApp.getFaceServiceClient();
            try {
                publishProgress("Detecting...");

                // Start detection.
                return faceServiceClient.detect(
                        params[0],  /* Input stream of image to detect */
                        true,       /* Whether to return face ID */
                        true,       /* Whether to return face landmarks */
                        /* Which face attributes to analyze, currently we support:
                           age,gender,headPose,smile,facialHair */
                        new FaceServiceClient.FaceAttributeType[] {
                                FaceServiceClient.FaceAttributeType.Age,
                                FaceServiceClient.FaceAttributeType.Gender,
                                FaceServiceClient.FaceAttributeType.Glasses,
                                FaceServiceClient.FaceAttributeType.Smile,
                                FaceServiceClient.FaceAttributeType.HeadPose
                        });
            } catch (Exception e) {
                mSucceed = false;
                publishProgress(e.getMessage());
                addLog(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            mProgressDialog.show();
            addLog("Request: Detecting in image " + mImageUri);
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            mProgressDialog.setMessage(progress[0]);
            setInfo(progress[0]);
        }

        @Override
        protected void onPostExecute(Face[] result) {
            if (mSucceed) {
                addLog("Response: Success. Detected " + (result == null ? 0 : result.length)
                        + " face(s) in " + mImageUri);
            }

            setUiAfterDetection(result, mSucceed);
        }
    }


    private static final int REQUEST_SELECT_IMAGE = 0;


    private Uri mImageUri;

    private Bitmap mBitmap;


    ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.admin.tolga.face.proje.R.layout.activity_detection);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(getString(com.admin.tolga.face.proje.R.string.progress_dialog_title));

        setDetectButtonEnabledStatus(false);

        LogHelper.clearDetectionLog();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("ImageUri", mImageUri);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mImageUri = savedInstanceState.getParcelable("ImageUri");
        if (mImageUri != null) {
            mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                    mImageUri, getContentResolver());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SELECT_IMAGE:
                if (resultCode == RESULT_OK) {
                    mImageUri = data.getData();
                    mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                            mImageUri, getContentResolver());
                    if (mBitmap != null) {
                        ImageView imageView = (ImageView) findViewById(com.admin.tolga.face.proje.R.id.image);
                        imageView.setImageBitmap(mBitmap);

                        addLog("Image: " + mImageUri + " resized to " + mBitmap.getWidth()
                                + "x" + mBitmap.getHeight());
                    }

                    FaceListAdapter faceListAdapter = new FaceListAdapter(null);
                    ListView listView = (ListView) findViewById(com.admin.tolga.face.proje.R.id.list_detected_faces);
                    listView.setAdapter(faceListAdapter);

                    setInfo("");

                    setDetectButtonEnabledStatus(true);
                }
                break;
            default:
                break;
        }
    }

    // Called when the "Select Image" button is clicked.
    public void selectImage(View view) {
        Intent intent = new Intent(this, SelectImageActivity.class);
        startActivityForResult(intent, REQUEST_SELECT_IMAGE);
    }

    // Called when the "Detect" button is clicked.
    public void detect(View view) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        new DetectionTask().execute(inputStream);

        setAllButtonsEnabledStatus(false);
    }

    // View the log of service calls.
    public void viewLog(View view) {
        Intent intent = new Intent(this, DetectionLogActivity.class);
        startActivity(intent);
    }

    // Show the result on screen when detection is done.
    private void setUiAfterDetection(Face[] result, boolean succeed) {
        // Detection is done, hide the progress dialog.
        mProgressDialog.dismiss();

        setAllButtonsEnabledStatus(true);

        // Disable button "detect" as the image has already been detected.
        setDetectButtonEnabledStatus(false);

        if (succeed) {
            // The information about the detection result.
            String detectionResult;
            if (result != null) {
                detectionResult = result.length + " face"
                        + (result.length != 1 ? "s" : "") + " detected";

                // Show the detected faces on original image.
                ImageView imageView = (ImageView) findViewById(com.admin.tolga.face.proje.R.id.image);
                imageView.setImageBitmap(ImageHelper.drawFaceRectanglesOnBitmap(
                        mBitmap, result, true));

                FaceListAdapter faceListAdapter = new FaceListAdapter(result);

                ListView listView = (ListView) findViewById(com.admin.tolga.face.proje.R.id.list_detected_faces);
                listView.setAdapter(faceListAdapter);
            } else {
                detectionResult = "0 face detected";
            }
            setInfo(detectionResult);
        }

        mImageUri = null;
        mBitmap = null;
    }

    // Set whether the buttons are enabled.
    private void setDetectButtonEnabledStatus(boolean isEnabled) {
        Button detectButton = (Button) findViewById(com.admin.tolga.face.proje.R.id.detect);
        detectButton.setEnabled(isEnabled);
    }

    // Set whether the buttons are enabled.
    private void setAllButtonsEnabledStatus(boolean isEnabled) {
        Button selectImageButton = (Button) findViewById(com.admin.tolga.face.proje.R.id.select_image);
        selectImageButton.setEnabled(isEnabled);

        Button detectButton = (Button) findViewById(com.admin.tolga.face.proje.R.id.detect);
        detectButton.setEnabled(isEnabled);

        Button ViewLogButton = (Button) findViewById(com.admin.tolga.face.proje.R.id.view_log);
        ViewLogButton.setEnabled(isEnabled);
    }

    // Set the information panel on screen.
    private void setInfo(String info) {
        TextView textView = (TextView) findViewById(com.admin.tolga.face.proje.R.id.info);
        textView.setText(info);
    }

    // Add a log item.
    private void addLog(String log) {
        LogHelper.addDetectionLog(log);
    }

    private class FaceListAdapter extends BaseAdapter {
        // The detected faces.
        List<Face> faces;

        List<Bitmap> faceThumbnails;

        FaceListAdapter(Face[] detectionResult) {
            faces = new ArrayList<>();
            faceThumbnails = new ArrayList<>();

            if (detectionResult != null) {
                faces = Arrays.asList(detectionResult);
                for (Face face : faces) {
                    try {
                        faceThumbnails.add(ImageHelper.generateFaceThumbnail(
                                mBitmap, face.faceRectangle));
                    } catch (IOException e) {
                        // Show the exception when generating face thumbnail fails.
                        setInfo(e.getMessage());
                    }
                }
            }
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        public int getCount() {
            return faces.size();
        }

        @Override
        public Object getItem(int position) {
            return faces.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater layoutInflater =
                        (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(com.admin.tolga.face.proje.R.layout.item_face_with_description, parent, false);
            }
            convertView.setId(position);

            // Show the face thumbnail.
            ((ImageView) convertView.findViewById(com.admin.tolga.face.proje.R.id.face_thumbnail)).setImageBitmap(
                    faceThumbnails.get(position));

            // Show the face details.
            DecimalFormat formatter = new DecimalFormat("#0.0");
            String face_description = "Age: " + formatter.format(faces.get(position).faceAttributes.age) + "\n"
                    + "Gender: " + faces.get(position).faceAttributes.gender + "\n"
                    + "Head pose(in degree): roll(" + formatter.format(faces.get(position).faceAttributes.headPose.roll) + "), "
                    + "yaw(" + formatter.format(faces.get(position).faceAttributes.headPose.yaw) + ")\n"
                    + "Glasses: " + faces.get(position).faceAttributes.glasses + "\n"
                    + "Smile: " + formatter.format(faces.get(position).faceAttributes.smile);
            ((TextView) convertView.findViewById(com.admin.tolga.face.proje.R.id.text_detected_face)).setText(face_description);

            return convertView;
        }
    }
}
