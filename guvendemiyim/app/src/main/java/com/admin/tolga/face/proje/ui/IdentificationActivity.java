
package com.admin.tolga.face.proje.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.admin.tolga.face.proje.helper.LogHelper;
import com.admin.tolga.face.proje.helper.SampleApp;
import com.admin.tolga.face.proje.helper.StorageHelper;
import com.admin.tolga.face.proje.log.IdentificationLogActivity;
import com.admin.tolga.face.proje.persongroupmanagement.PersonGroupListActivity;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.IdentifyResult;
import com.microsoft.projectoxford.face.contract.TrainingStatus;
import com.admin.tolga.face.proje.helper.ImageHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class IdentificationActivity extends AppCompatActivity {

    public double confidenceInt = -1;
    public String personNamePublic;

    private class IdentificationTask extends AsyncTask<UUID, String, IdentifyResult[]> {
        private boolean mSucceed = true;
        String mPersonGroupId;
        IdentificationTask(String personGroupId) {
            this.mPersonGroupId = personGroupId;
        }

        @Override
        protected IdentifyResult[] doInBackground(UUID... params) {
            String logString = "Request: Identifying faces ";
            for (UUID faceId: params) {
                logString += faceId.toString() + ", ";
            }
            logString += " in group " + mPersonGroupId;
            addLog(logString);

            // Get an instance of face service client to detect faces in image.
            FaceServiceClient faceServiceClient = SampleApp.getFaceServiceClient();
            try{
                publishProgress("Getting person group status...");

                TrainingStatus trainingStatus = faceServiceClient.getPersonGroupTrainingStatus(
                        this.mPersonGroupId);     /* personGroupId */

                if (trainingStatus.status != TrainingStatus.Status.Succeeded) {
                    publishProgress("Person group training status is " + trainingStatus.status);
                    mSucceed = false;
                    return null;
                }

                publishProgress("Identifying...");

                // Start identification.
                return faceServiceClient.identity(
                        this.mPersonGroupId,   /* personGroupId */
                        params,                  /* faceIds */
                        1);  /* maxNumOfCandidatesReturned */
            }  catch (Exception e) {
                mSucceed = false;
                publishProgress(e.getMessage());
                addLog(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            setUiBeforeBackgroundTask();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            // Show the status of background detection task on screen.a
            setUiDuringBackgroundTask(values[0]);
        }

        @Override
        protected void onPostExecute(IdentifyResult[] result) {
            // Show the result on screen when detection is done.
            setUiAfterIdentification(result, mSucceed);
        }
    }

    String mPersonGroupId;

    boolean detected;

    FaceListAdapter mFaceListAdapter;

    PersonGroupListAdapter mPersonGroupListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.admin.tolga.face.proje.R.layout.activity_identification);


        detected = false;

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(getString(com.admin.tolga.face.proje.R.string.progress_dialog_title));

        LogHelper.clearIdentificationLog();
    }

    @Override
    protected void onResume() {
        super.onResume();

        ListView listView = (ListView) findViewById(com.admin.tolga.face.proje.R.id.list_person_groups_identify);
        mPersonGroupListAdapter = new PersonGroupListAdapter();
        listView.setAdapter(mPersonGroupListAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                setPersonGroupSelected(position);
            }
        });

        if (mPersonGroupListAdapter.personGroupIdList.size() != 0) {
            setPersonGroupSelected(0);
        } else {
            setPersonGroupSelected(-1);
        }
    }

    void setPersonGroupSelected(int position) {
        TextView textView = (TextView) findViewById(com.admin.tolga.face.proje.R.id.text_person_group_selected);
        if (position > 0) {
            String personGroupIdSelected = mPersonGroupListAdapter.personGroupIdList.get(position);
            mPersonGroupListAdapter.personGroupIdList.set(
                    position, mPersonGroupListAdapter.personGroupIdList.get(0));
            mPersonGroupListAdapter.personGroupIdList.set(0, personGroupIdSelected);
            ListView listView = (ListView) findViewById(com.admin.tolga.face.proje.R.id.list_person_groups_identify);
            listView.setAdapter(mPersonGroupListAdapter);
            setPersonGroupSelected(0);
        } else if (position < 0) {
            setIdentifyButtonEnabledStatus(false);
            textView.setTextColor(Color.RED);
            textView.setText(com.admin.tolga.face.proje.R.string.no_person_group_selected_for_identification_warning);
        } else {
            mPersonGroupId = mPersonGroupListAdapter.personGroupIdList.get(0);
            String personGroupName = StorageHelper.getPersonGroupName(
                    mPersonGroupId, IdentificationActivity.this);
            refreshIdentifyButtonEnabledStatus();
            textView.setTextColor(Color.BLACK);
            textView.setText(String.format("Person group to use: %s", personGroupName));
        }
    }

    private void setUiBeforeBackgroundTask() {
        progressDialog.show();
    }

    private void setUiDuringBackgroundTask(String progress) {
        progressDialog.setMessage(progress);

        setInfo(progress);
    }

    // Show the result on screen when detection is done.
    private void setUiAfterIdentification(IdentifyResult[] result, boolean succeed) {
        progressDialog.dismiss();

        setAllButtonsEnabledStatus(true);
        setIdentifyButtonEnabledStatus(false);

        if (succeed) {
            // Set the information about the detection result.
            setInfo("Identification is done");

            if (result != null) {
                mFaceListAdapter.setIdentificationResult(result);

                String logString = "Response: Success. ";
                for (IdentifyResult identifyResult: result) {
                    logString += "Face " + identifyResult.faceId.toString() + " is identified as "
                            + (identifyResult.candidates.size() > 0
                                    ? identifyResult.candidates.get(0).personId.toString()
                                    : "Unknown Person")
                            + ". ";
                }
                addLog(logString);

                ListView listView = (ListView) findViewById(com.admin.tolga.face.proje.R.id.list_identified_faces);
                listView.setAdapter(mFaceListAdapter);
            }
        }
    }

    // Background task of face detection.
    private class DetectionTask extends AsyncTask<InputStream, String, Face[]> {
        @Override
        protected Face[] doInBackground(InputStream... params) {
            // Get an instance of face service client to detect faces in image.
            FaceServiceClient faceServiceClient = SampleApp.getFaceServiceClient();
            try{
                publishProgress("Detecting...");

                // Start detection.
                return faceServiceClient.detect(
                        params[0],  /* Input stream of image to detect */
                        true,       /* Whether to return face ID */
                        false,       /* Whether to return face landmarks */
                        /* Which face attributes to analyze, currently we support:
                           age,gender,headPose,smile,facialHair */
                        null);
            }  catch (Exception e) {
                publishProgress(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            setUiBeforeBackgroundTask();
        }

        @Override
        protected void onProgressUpdate(String... values) {

            setUiDuringBackgroundTask(values[0]);
        }

        @Override
        protected void onPostExecute(Face[] result) {
            progressDialog.dismiss();

            setAllButtonsEnabledStatus(true);

            if (result != null) {

                mFaceListAdapter = new FaceListAdapter(result);
                ListView listView = (ListView) findViewById(com.admin.tolga.face.proje.R.id.list_identified_faces);
                listView.setAdapter(mFaceListAdapter);

                if (result.length == 0) {
                    detected = false;
                    setInfo("No faces detected!");
                } else {
                    detected = true;
                    setInfo("Click on the \"Identify\" button to identify the faces in image.");
                }
            } else {
                detected = false;
            }

            refreshIdentifyButtonEnabledStatus();
        }
    }


    private static final int REQUEST_SELECT_IMAGE = 0;

    // The image selected to detect.
    private Bitmap mBitmap;


    ProgressDialog progressDialog;


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode)
        {
            case REQUEST_SELECT_IMAGE:
                if(resultCode == RESULT_OK) {
                    detected = false;


                    Uri imageUri = data.getData();
                    mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                            imageUri, getContentResolver());
                    if (mBitmap != null) {
                        ImageView imageView = (ImageView) findViewById(com.admin.tolga.face.proje.R.id.image);
                        imageView.setImageBitmap(mBitmap);
                    }


                    FaceListAdapter faceListAdapter = new FaceListAdapter(null);
                    ListView listView = (ListView) findViewById(com.admin.tolga.face.proje.R.id.list_identified_faces);
                    listView.setAdapter(faceListAdapter);


                    setInfo("");


                    detect(mBitmap);
                }
                break;
            default:
                break;
        }
    }


    private void detect(Bitmap bitmap) {

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        setAllButtonsEnabledStatus(false);

        // Start a background task to detect faces in the image.
        new DetectionTask().execute(inputStream);
    }


    public void selectImage(View view) {
        Intent intent = new Intent(this, SelectImageActivity.class);
        startActivityForResult(intent, REQUEST_SELECT_IMAGE);
    }
    // Save images to local directory
    private static void SaveImage(Bitmap finalBitmap) {

        String root = Environment.getExternalStorageDirectory().getAbsolutePath();
        File myDir = new File(root + "/DCIM/100ANDRO/GuvendeMiyimPrisoners");
        myDir.mkdirs();
        // assign random names for saved images to avoid overwriting
        // --------------timestamp eklesek daha iyi olur-----

        double o = Math.random();

        String fname = "guvendemiyim"+ o +".jpg";
        File file = new File (myDir, fname);
        if (file.exists ()) file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public void identify(View view) {

        if (detected && mPersonGroupId != null) {

            List<UUID> faceIds = new ArrayList<>();
            for (Face face:  mFaceListAdapter.faces) {
                faceIds.add(face.faceId);
            }

            setAllButtonsEnabledStatus(false);

            new IdentificationTask(mPersonGroupId).execute(
                    faceIds.toArray(new UUID[faceIds.size()]));
        } else {
            // Not detected or person group exists.
            setInfo("Please select an image and create a person group first.");
        }
    }

    public void managePersonGroups(View view) {
        Intent intent = new Intent(this, PersonGroupListActivity.class);
        startActivity(intent);

        refreshIdentifyButtonEnabledStatus();
    }

    public void viewLog(View view) {
        Intent intent = new Intent(this, IdentificationLogActivity.class);
        startActivity(intent);
    }



    private void addLog(String log) {
        LogHelper.addIdentificationLog(log);
    }

    private void setAllButtonsEnabledStatus(boolean isEnabled) {
        Button selectImageButton = (Button) findViewById(com.admin.tolga.face.proje.R.id.manage_person_groups);
        selectImageButton.setEnabled(isEnabled);

        Button groupButton = (Button) findViewById(com.admin.tolga.face.proje.R.id.select_image);
        groupButton.setEnabled(isEnabled);

        Button identifyButton = (Button) findViewById(com.admin.tolga.face.proje.R.id.identify);
        identifyButton.setEnabled(isEnabled);

        Button viewLogButton = (Button) findViewById(com.admin.tolga.face.proje.R.id.view_log);
        viewLogButton.setEnabled(isEnabled);
    }


    private void setIdentifyButtonEnabledStatus(boolean isEnabled) {
        Button button = (Button) findViewById(com.admin.tolga.face.proje.R.id.identify);
        button.setEnabled(isEnabled);
    }


    private void refreshIdentifyButtonEnabledStatus() {
        if (detected && mPersonGroupId != null) {
            setIdentifyButtonEnabledStatus(true);
        } else {
            setIdentifyButtonEnabledStatus(false);
        }
    }


    private void setInfo(String info) {
        TextView textView = (TextView) findViewById(com.admin.tolga.face.proje.R.id.info);
        textView.setText(info);
    }


    private class FaceListAdapter extends BaseAdapter {
        // The detected faces.
        List<Face> faces;

        List<IdentifyResult> mIdentifyResults;

        // The thumbnails of detected faces.
        List<Bitmap> faceThumbnails;

        // Initialize with detection result.
        FaceListAdapter(Face[] detectionResult) {
            faces = new ArrayList<>();
            faceThumbnails = new ArrayList<>();
            mIdentifyResults = new ArrayList<>();

            if (detectionResult != null) {
                faces = Arrays.asList(detectionResult);
                for (Face face: faces) {
                    try {
                        faceThumbnails.add(ImageHelper.generateFaceThumbnail(
                                mBitmap, face.faceRectangle));
                    } catch (IOException e) {
                        setInfo(e.getMessage());
                    }
                }
            }
        }

        public void setIdentificationResult(IdentifyResult[] identifyResults) {
            mIdentifyResults = Arrays.asList(identifyResults);
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
                        (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(
                        com.admin.tolga.face.proje.R.layout.item_face_with_description, parent, false);
            }
            convertView.setId(position);

            ((ImageView)convertView.findViewById(com.admin.tolga.face.proje.R.id.face_thumbnail)).setImageBitmap(
                    faceThumbnails.get(position));

            if (mIdentifyResults.size() == faces.size()) {
                DecimalFormat formatter = new DecimalFormat("#0.00");
                if (mIdentifyResults.get(position).candidates.size() > 0) {
                    String personId =
                            mIdentifyResults.get(position).candidates.get(0).personId.toString();
                    String personName = StorageHelper.getPersonName(
                            personId, mPersonGroupId, IdentificationActivity.this);
                    String identity = "Person: " + personName + "\n"
                            + "Confidence: " + formatter.format(
                            mIdentifyResults.get(position).candidates.get(0).confidence);
                    ((TextView) convertView.findViewById(com.admin.tolga.face.proje.R.id.text_detected_face)).setText(
                            identity);
                    confidenceInt = (mIdentifyResults.get(position).candidates.get(0).confidence);
                    personNamePublic = personName;



                } else {
                    ((TextView) convertView.findViewById(com.admin.tolga.face.proje.R.id.text_detected_face)).setText(
                            com.admin.tolga.face.proje.R.string.face_cannot_be_identified);
                    Intent intent2 = new Intent(IdentificationActivity.this, AccessDeniedActivity.class);
                    startActivity(intent2);
                    SaveImage(mBitmap);
                }
            }

            // 0.65 0.7 arasi bug var >0.6 olsa belki cozulur

            if(confidenceInt>0.7){
                Intent intent = new Intent(IdentificationActivity.this, OptionsActivity.class);
                intent.putExtra("data",personNamePublic);
                startActivity(intent);
            }




            return convertView;

        }



    }


    private class PersonGroupListAdapter extends BaseAdapter {
        List<String> personGroupIdList;


        PersonGroupListAdapter() {
            personGroupIdList = new ArrayList<>();

            Set<String> personGroupIds
                    = StorageHelper.getAllPersonGroupIds(IdentificationActivity.this);

            for (String personGroupId: personGroupIds) {
                personGroupIdList.add(personGroupId);
                if (mPersonGroupId != null && personGroupId.equals(mPersonGroupId)) {
                    personGroupIdList.set(
                            personGroupIdList.size() - 1,
                            mPersonGroupListAdapter.personGroupIdList.get(0));
                    mPersonGroupListAdapter.personGroupIdList.set(0, personGroupId);
                }
            }
        }

        @Override
        public int getCount() {
            return personGroupIdList.size();
        }

        @Override
        public Object getItem(int position) {
            return personGroupIdList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater layoutInflater =
                        (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(com.admin.tolga.face.proje.R.layout.item_person_group, parent, false);
            }
            convertView.setId(position);

            // set the text of the item
            String personGroupName = StorageHelper.getPersonGroupName(
                    personGroupIdList.get(position), IdentificationActivity.this);
            int personNumberInGroup = StorageHelper.getAllPersonIds(
                    personGroupIdList.get(position), IdentificationActivity.this).size();
            ((TextView)convertView.findViewById(com.admin.tolga.face.proje.R.id.text_person_group)).setText(
                    String.format(
                            "%s (Person count: %d)",
                            personGroupName,
                            personNumberInGroup));

            if (position == 0) {
                ((TextView)convertView.findViewById(com.admin.tolga.face.proje.R.id.text_person_group)).setTextColor(
                        Color.parseColor("#3399FF"));
            }

            return convertView;
        }
    }
}
