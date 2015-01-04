package com.pylm.nxgeotagger;

import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import static android.os.Environment.getExternalStoragePublicDirectory;


public class MainActivity extends ActionBarActivity {

    static Button startServiceButton;
    static Button stopServiceButton;
    static TextView folderTextView;

    public static final String FOLDER_PATH_ID = "FOLDER_PATH_ID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
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
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);


            String folderPath = getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath() + "/Camera/Samsung Smart Camera Application/AutoShare";
            final Intent intent = new Intent(getActivity(), NXGeoTaggerService.class);
            intent.putExtra(FOLDER_PATH_ID, folderPath);
            getActivity().startService(intent);

            startServiceButton  = (Button) rootView.findViewById(R.id.startServiceButton);
            startServiceButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getActivity(), "Starting Service...", Toast.LENGTH_LONG).show();
                    getActivity().startService(intent);
                }
            });
            stopServiceButton  = (Button) rootView.findViewById(R.id.stopServiceButton);
            stopServiceButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getActivity(), "Stopping Service...", Toast.LENGTH_SHORT).show();
                    getActivity().stopService(intent);
                    getActivity().finish();
                }
            });

            folderTextView = (TextView) rootView.findViewById(R.id.folderTextView);
            folderTextView.setText(folderPath);
            return rootView;
        }
    }
}
