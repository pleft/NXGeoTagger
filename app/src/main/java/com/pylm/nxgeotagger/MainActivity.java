package com.pylm.nxgeotagger;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import net.rdrei.android.dirchooser.DirectoryChooserFragment;

import static android.os.Environment.getExternalStoragePublicDirectory;


public class MainActivity extends Activity implements DirectoryChooserFragment.OnFragmentInteractionListener {

    public static final String FOLDER_PATH_ID = "FOLDER_PATH_ID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment(), "PlaceholderFragment").commit();
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
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        private Button startServiceButton;
        private Button stopServiceButton;
        private Button directoryButton;
        private TextView folderTextView;
        private DirectoryChooserFragment mDialog;
        private Intent serviceIntent;
        private String folderPath;
        
        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            folderPath = getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath() + "/Camera/Samsung Smart Camera Application/AutoShare";
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            if(preferences.contains(FOLDER_PATH_ID)) {
                folderPath = preferences.getString(FOLDER_PATH_ID, folderPath);
            } else {
                preferences.edit().putString(FOLDER_PATH_ID, folderPath).commit();
            }
            serviceIntent = new Intent(getActivity(), NXGeoTaggerService.class);
            getActivity().startService(serviceIntent);

            mDialog = DirectoryChooserFragment.newInstance("AutoShare", folderPath);

            startServiceButton  = (Button) rootView.findViewById(R.id.startServiceButton);
            startServiceButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getActivity(), "Starting Service...", Toast.LENGTH_LONG).show();
                    getActivity().startService(serviceIntent);
                }
            });
            stopServiceButton  = (Button) rootView.findViewById(R.id.stopServiceButton);
            stopServiceButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getActivity(), "Stopping Service...", Toast.LENGTH_SHORT).show();
                    getActivity().stopService(serviceIntent);
                    getActivity().finish();
                }
            });
            directoryButton  = (Button) rootView.findViewById(R.id.directoryButton);
            directoryButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDialog.show(getFragmentManager(), null);
                }
            });

            folderTextView = (TextView) rootView.findViewById(R.id.folderTextView);
            folderTextView.setText(folderPath);
            return rootView;
        }
        
        public void onSelectDirectory(@NonNull String path) {
            folderPath = path;
            folderTextView.setText(folderPath);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            preferences.edit().putString(FOLDER_PATH_ID, path).commit();
            mDialog.dismiss();
        }

        public void onCancelChooser() {
            mDialog.dismiss();
        }
    }

    @Override
    public void onSelectDirectory(@NonNull String path) {
        PlaceholderFragment placeholderFragment = (PlaceholderFragment) getFragmentManager().findFragmentById(R.id.container);
        placeholderFragment.onSelectDirectory(path);
        Toast.makeText(this, "Restarting Service...", Toast.LENGTH_SHORT).show();
        Intent serviceIntent = new Intent(this, NXGeoTaggerService.class);
        if(stopService(serviceIntent)) {
            startService(serviceIntent);
        }
    }

    @Override
    public void onCancelChooser() {
        PlaceholderFragment placeholderFragment = (PlaceholderFragment) getFragmentManager().findFragmentById(R.id.container);
        placeholderFragment.onCancelChooser();
    }
}
