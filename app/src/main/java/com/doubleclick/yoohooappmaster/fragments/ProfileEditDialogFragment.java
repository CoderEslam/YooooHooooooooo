package com.doubleclick.yoohooappmaster.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.doubleclick.yoohooappmaster.R;
import com.doubleclick.yoohooappmaster.activities.ImageViewerActivity;
import com.doubleclick.yoohooappmaster.models.Attachment;
import com.doubleclick.yoohooappmaster.models.AttachmentTypes;
import com.doubleclick.yoohooappmaster.models.User;
import com.doubleclick.yoohooappmaster.utils.FirebaseUploader;
import com.doubleclick.yoohooappmaster.utils.Helper;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.kbeanie.multipicker.api.CameraImagePicker;
import com.kbeanie.multipicker.api.ImagePicker;
import com.kbeanie.multipicker.api.Picker;
import com.kbeanie.multipicker.api.callbacks.ImagePickerCallback;
import com.kbeanie.multipicker.api.entity.ChosenImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProfileEditDialogFragment extends BaseFullDialogFragment implements ImagePickerCallback {
    private static final int REQUEST_CODE_MEDIA_PERMISSION = 999;
    private static final int REQUEST_CODE_PICKER = 4321;
    private EditText userPhone, userNameEdit, userStatus;
    private ImageView userImage;
    private ProgressBar userImageProgress;
    private Helper helper;
    private String[] permissionsCamera = {Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private User userMe;
    private ImagePicker imagePicker;
    private CameraImagePicker cameraPicker;
    private String pickerPath;
    private ProgressDialog progressDialog;
    private DatabaseReference usersRef;
    private boolean firstEdit;

    public static ProfileEditDialogFragment newInstance(boolean firstEdit) {
        ProfileEditDialogFragment profileEditDialogFragment = new ProfileEditDialogFragment();
        profileEditDialogFragment.firstEdit = firstEdit;
        return profileEditDialogFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        helper = new Helper(getActivity());
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        usersRef = firebaseDatabase.getReference(Helper.REF_USERS);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_edit, container);
        userImage = view.findViewById(R.id.userImage);
        userImageProgress = view.findViewById(R.id.progressBar);
        userPhone = view.findViewById(R.id.userPhone);
        userNameEdit = view.findViewById(R.id.userNameEdit);
        userStatus = view.findViewById(R.id.userStatus);
        userPhone.setEnabled(false);
        view.findViewById(R.id.changeImage).setOnClickListener(view13 -> pickProfileImage());
        view.findViewById(R.id.back).setOnClickListener(view12 -> dismiss());
        view.findViewById(R.id.done).setOnClickListener(view1 -> {
            Helper.closeKeyboard(getContext(), view1);
            updateUserNameAndStatus(userNameEdit.getText().toString().trim(), userStatus.getText().toString().trim());
        });
        userImage.setOnClickListener(v -> {
            if (userMe != null && !TextUtils.isEmpty(userMe.getImage()))
                startActivity(ImageViewerActivity.newUrlInstance(getContext(), userMe.getImage()));
        });
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        helper = new Helper(getContext());
        userMe = helper.getLoggedInUser();
        setUserDetails();
    }

    private void setUserDetails() {
        userPhone.setText(userMe.getId());
        userNameEdit.setText(userMe.getName());
        userStatus.setText(userMe.getStatus());
        Glide.with(this).load(userMe.getImage()).apply(new RequestOptions().placeholder(R.drawable.yoohoo_placeholder)).into(userImage);
    }

    private void saveAndBroadcast() {
        helper.setLoggedInUser(userMe);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getContext());
        localBroadcastManager.sendBroadcast(new Intent(Helper.BROADCAST_USER_ME));
    }

    private void updateUserNameAndStatus(String updatedName, String updatedStatus) {
        if (TextUtils.isEmpty(updatedName)) {
            Helper.presentToast(getContext(), getString(R.string.validation_req_username), false);
        } else if (TextUtils.isEmpty(updatedStatus)) {
            Toast.makeText(getContext(), R.string.validation_req_status, Toast.LENGTH_SHORT).show();
        } else if (!userMe.getName().equals(updatedName) || !userMe.getStatus().equals(updatedStatus)) {
            userMe.setName(updatedName);
            userMe.setStatus(updatedStatus);
            usersRef.child(userMe.getId()).setValue(userMe);
            saveAndBroadcast();
            if (firstEdit) {
                Toast.makeText(getContext(), R.string.all_set, Toast.LENGTH_SHORT).show();
            }
            dismiss();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_MEDIA_PERMISSION) {
            if (mediaPermissions().isEmpty()) {
                pickProfileImage();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case Picker.PICK_IMAGE_DEVICE:
                    if (imagePicker == null) {
                        imagePicker = new ImagePicker(this);
                    }
                    imagePicker.submit(data);
                    break;
                case Picker.PICK_IMAGE_CAMERA:
                    if (cameraPicker == null) {
                        cameraPicker = new CameraImagePicker(this);
                        cameraPicker.reinitialize(pickerPath);
                    }
                    cameraPicker.submit(data);
                    break;
            }
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (progressDialog != null && progressDialog.isShowing())
            progressDialog.dismiss();
    }

    private void userImageUploadTask(final File fileToUpload, @AttachmentTypes.AttachmentType final int attachmentType, final Attachment attachment) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setTitle(getString(R.string.uploading));
            progressDialog.setMessage(getString(R.string.just_moment));
            progressDialog.setCancelable(false);
        }
        if (!progressDialog.isShowing())
            progressDialog.show();
        //Toast.makeText(getContext(), R.string.uploading, Toast.LENGTH_SHORT).show();
        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(getString(R.string.app_name)).child("ProfileImage").child(userMe.getId());
        FirebaseUploader firebaseUploader = new FirebaseUploader(new FirebaseUploader.UploadListener() {
            @Override
            public void onUploadFail(String message) {
                if (userImageProgress != null)
                    userImageProgress.setVisibility(View.GONE);
                if (progressDialog != null && progressDialog.isShowing())
                    progressDialog.dismiss();
            }

            @Override
            public void onUploadSuccess(String downloadUrl) {
                if (progressDialog != null && progressDialog.isShowing())
                    progressDialog.dismiss();
                if (userImageProgress != null) {
                    userImageProgress.setVisibility(View.GONE);
                }
                if (usersRef != null && userMe != null) {
                    userMe.setImage(downloadUrl);
                    usersRef.child(userMe.getId()).child("image").setValue(userMe.getImage());
                    saveAndBroadcast();
                }
            }

            @Override
            public void onUploadProgress(int progress) {

            }

            @Override
            public void onUploadCancelled() {
                if (userImageProgress != null) {
                    userImageProgress.setVisibility(View.GONE);
                }
                if (progressDialog != null && progressDialog.isShowing())
                    progressDialog.dismiss();
            }
        }, storageReference);
        firebaseUploader.setReplace(true);
        firebaseUploader.uploadImage(getContext(), fileToUpload);
        //userImageProgress.setVisibility(View.VISIBLE);
    }

    private void pickProfileImage() {
        if (mediaPermissions().isEmpty()) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
            alertDialog.setMessage(R.string.get_image_title);
            alertDialog.setPositiveButton(R.string.get_image_camera, (dialogInterface, i) -> {
                dialogInterface.dismiss();

                cameraPicker = new CameraImagePicker(ProfileEditDialogFragment.this);
                cameraPicker.shouldGenerateMetadata(true);
                cameraPicker.shouldGenerateThumbnails(true);
                cameraPicker.setImagePickerCallback(ProfileEditDialogFragment.this);
                pickerPath = cameraPicker.pickImage();
            });
            alertDialog.setNegativeButton(R.string.get_image_gallery, (dialogInterface, i) -> {
                dialogInterface.dismiss();

                imagePicker = new ImagePicker(ProfileEditDialogFragment.this);
                imagePicker.shouldGenerateMetadata(true);
                imagePicker.shouldGenerateThumbnails(true);
                imagePicker.setImagePickerCallback(ProfileEditDialogFragment.this);
                imagePicker.pickImage();
            });
            alertDialog.create().show();
        } else {
            requestPermissions(permissionsCamera, REQUEST_CODE_MEDIA_PERMISSION);
        }
    }

    @Override
    public void onImagesChosen(List<ChosenImage> images) {
        File fileToUpload = new File(Uri.parse(images.get(0).getOriginalPath()).getPath());
        Glide.with(this).load(fileToUpload).apply(new RequestOptions().placeholder(R.drawable.yoohoo_placeholder)).into(userImage);
        userImageUploadTask(fileToUpload, AttachmentTypes.IMAGE, null);
    }

    @Override
    public void onError(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // You have to save path in case your activity is killed.
        // In such a scenario, you will need to re-initialize the CameraImagePicker
        outState.putString("picker_path", pickerPath);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("picker_path")) {
                pickerPath = savedInstanceState.getString("picker_path");
            }
        }
    }

    private List<String> mediaPermissions() {
        List<String> missingPermissions = new ArrayList<>();
        for (String permission : permissionsCamera) {
            if (ActivityCompat.checkSelfPermission(getContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        return missingPermissions;
    }
}
