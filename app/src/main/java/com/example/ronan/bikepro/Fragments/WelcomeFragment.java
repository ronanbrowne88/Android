package com.example.ronan.bikepro.Fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.ronan.bikepro.DataModel.BikeData;
import com.example.ronan.bikepro.DataModel.UserData;
import com.example.ronan.bikepro.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.base.Strings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;


public class WelcomeFragment extends Fragment {

    private TextView registered;
    private TextView stolen;
    private TextView systemStolen;
    private TextView userHeading;
    private LinearLayout circleHolder;
    ;
    private ImageView profielPic;
    private FloatingActionButton floatingEditProfile;
    private String uniqueIdentifier = "";
    private String emailFull = "";


    long countStolen;
    long countReg;
    long thisStolen = 0;

    //Firebase variables
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mDatabase;
    private DatabaseReference stolenBikesDatabse;
    private DatabaseReference mDatabaseUsers;
    private DatabaseReference reportedStolen;
    private DatabaseReference readReportOfStolenQuery;
    // storage
    FirebaseStorage storage = FirebaseStorage.getInstance();

    ArrayList<String> registeredBikeKeys = new ArrayList<>();
    ArrayList<String> sightingBikeKeys = new ArrayList<>();
    ArrayList<BikeData> reportedSightingsList = new ArrayList<>();
    ArrayList<BikeData> registeredBikesList = new ArrayList<>();
    private ValueEventListener CountRegListener;
    private ValueEventListener CountStolenListener;
    private ValueEventListener reportedStolenListener;

    public WelcomeFragment() {
        // Required empty public constructor
    }

    private Bitmap bitmap;
    ;
    private String imageValue = "";
    //======================================================================================
    // FireBase listener to grab the specific user data
    //======================================================================================
    ValueEventListener userDataListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {

            if (dataSnapshot.getValue(UserData.class) == null) {
                Log.v("Profile_fragment", "datasnap shot returned null in userDataListener");
                return;
            }

            user = dataSnapshot.getValue(UserData.class);
            imageValue = user.getUser_image_In_Base64();

            //if no username is set use uniqueIdentifier from users email
            if (!Strings.isNullOrEmpty(user.getUsername())) {
                userHeading.setText(user.getUsername());
            } else {
                userHeading.setText(emailFull);
            }

        }

        UserData user = new UserData();

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };


    @Override
    public void onStop() {
        super.onStop();
        Log.d("*stope", "syope");

        mDatabase.removeEventListener(CountRegListener);
        stolenBikesDatabse.removeEventListener(CountStolenListener);
        reportedStolen.removeEventListener(reportedStolenListener);

    }

    //======================================================================================
    // onCreateView
    //======================================================================================
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_welcome, container, false);
        final View loadingIndicator = rootView.findViewById(R.id.loading_indicator_edit);


        //get user uniqueIdentifier
        mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (mFirebaseUser != null) {
            emailFull = mFirebaseUser.getEmail();
            uniqueIdentifier = emailFull.split("@")[0];
        }


        loadProfileImage(uniqueIdentifier);


        //seting up firebase DB refrences
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Bikes Registered By User").child(uniqueIdentifier);
        stolenBikesDatabse = FirebaseDatabase.getInstance().getReference().child("Stolen Bikes");
        mDatabaseUsers = FirebaseDatabase.getInstance().getReference().child("User Profile Data");
        readReportOfStolenQuery = FirebaseDatabase.getInstance().getReference().child("Viewing bikes Reported Stolen").child(uniqueIdentifier);
        reportedStolen = FirebaseDatabase.getInstance().getReference().child("Reported Bikes");

        mDatabaseUsers.child(uniqueIdentifier).addValueEventListener(userDataListener);

        //get IDs
        registered = (TextView) rootView.findViewById(R.id.bikesRegistered);
        stolen = (TextView) rootView.findViewById(R.id.personalStolen);
        systemStolen = (TextView) rootView.findViewById(R.id.totalStolen);
        userHeading = (TextView) rootView.findViewById(R.id.userProfile);
        floatingEditProfile = (FloatingActionButton) rootView.findViewById(R.id.floatingConfirmEdit);
        profielPic = (ImageView) rootView.findViewById(R.id.profile_image);
        circleHolder = (LinearLayout) rootView.findViewById(R.id.circleHolder);

        circleHolder.setLayerType(View.LAYER_TYPE_SOFTWARE, null);


        //Button click to launch edit profile page
        floatingEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //setFragment
                FragmentManager fm = getFragmentManager();
                fm.beginTransaction().replace(R.id.fragment_container, new Profile_Fragment()).commit();
                getActivity().getSupportFragmentManager().beginTransaction().remove(WelcomeFragment.this).commit();

            }
        });


        //event listener for checking how many bikes registered to you
         CountRegListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //cout children nodes in this DB area.
                loadingIndicator.setVisibility(View.GONE);
                countReg = dataSnapshot.getChildrenCount();
                registered.setText("My Bikes: " + countReg);
                registered.setVisibility(View.VISIBLE);
                stolen.setVisibility(View.VISIBLE);
                systemStolen.setVisibility(View.VISIBLE);

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    //grab all ket data from bikes registered, and grab al lbikes registered
                    registeredBikeKeys.add(snapshot.getKey().toString());
                    BikeData bike = snapshot.getValue(BikeData.class);
                    registeredBikesList.add(bike);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        }; //end listener


        //event listener for checking if any of your bikes are in stolrn system and how many total in DB
         CountStolenListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //show loading bar while working
                thisStolen = 0;
                countStolen = dataSnapshot.getChildrenCount();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    BikeData bike = snapshot.getValue(BikeData.class);
                    ;
                    //check field is not null
                    if (bike.getRegisteredBy() != null) {
                        //check bikes in stolen DB to see if ay were registered by curret user if so note it
                        if (bike.getRegisteredBy().equals(emailFull)) {
                            thisStolen++;
                            Log.v("**reg", bike.getRegisteredBy());
                        } else {
                            Log.v("**reg", "no user");
                        }
                    }
                }//end for
                stolen.setText("My stolen bikes: " + thisStolen);
                systemStolen.setText("Total bikes stolen in system: " + countStolen);

                //set UI

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        }; //end listener


        //event listener for checking if bike is on stolen DB used to give correct user feedback
         reportedStolenListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                countReg = dataSnapshot.getChildrenCount();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    //keys for all sightings
                    sightingBikeKeys.add(snapshot.getKey().toString());
                    BikeData bike = snapshot.getValue(BikeData.class);

                    //if a bike you have registered has been reported grab that
                    if (registeredBikeKeys.contains(snapshot.getKey().toString())) {
                        reportedSightingsList.add(bike);
                        Log.v("**rprint", Arrays.toString(reportedSightingsList.toArray()));
                        Log.v("**rprint make:", bike.getMake() + "Model: " + bike.getModel());

                        readReportOfStolenQuery.child(snapshot.getKey().toString()).setValue(bike);
                    }


                }


                reportedSightingsList.size();
                List<String> list3 = new ArrayList<>();

                for (String matches : registeredBikeKeys) {
                    if (sightingBikeKeys.contains(matches)) {
                        list3.add(matches);
                        Log.v("**size", "" + list3.size());
                    }
                }

                if (!list3.isEmpty()) {
                    // startAnim();
                }


                //   registered.setText("Bikes registered to you: " + countReg);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        }; //end listener


        //call the listeners that set UI data
        mDatabase.addValueEventListener(CountRegListener);
        stolenBikesDatabse.addValueEventListener(CountStolenListener);
        reportedStolen.addValueEventListener(reportedStolenListener);


        profielPic.setLayerType(View.LAYER_TYPE_NONE, null);


        return rootView;

    }// end onCreateView


    //fill users image to selected view
    public String loadProfileImage(final String userToLoad) {

        if (Strings.isNullOrEmpty(userToLoad)) {
            return null;
        }

        Log.d("*aaa", "load me ");
        // Create storage reference
        final StorageReference storageRef = storage.getReferenceFromUrl("gs://findmybike-1a1af.appspot.com/Profilers/");

        //set image based on user id
        StorageReference myProfilePic = storageRef.child(userToLoad);

        //set max image download size
        final long ONE_MEGABYTE = 10000 * 10000;
        myProfilePic.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {

                //decode image
                Bitmap userImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                profielPic.setImageBitmap(userImage);

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                //         reset to default image if no image is selected
                StorageReference myProfilePic = storageRef.child("default.jpg");
                myProfilePic.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        //decode image
                        Bitmap userImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                        profielPic.setImageBitmap(userImage);

                    }
                });


            }
        });

        return null;
    }


}//end class




