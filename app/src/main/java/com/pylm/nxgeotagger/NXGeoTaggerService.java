package com.pylm.nxgeotagger;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class NXGeoTaggerService extends Service implements LocationListener {

    final static String NX_GEO_TAGGER = "NXGeoTagger";
    private static final long MIN_TIME_BW_UPDATES = 3000;
    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 3;
    protected LocationManager locationManager;
    protected Location location;
    private FileObserver fileObserver;

    private int notificationId = new Random().nextInt();

    public NXGeoTaggerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(getClass().getName(), "NXGeoTagger Service running");

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locate();
        final PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);



        final Notification notification = new Notification.Builder(this)
                .setOngoing(true)
                .setContentTitle(NX_GEO_TAGGER)
                .setContentText("NXGeoTagger service running...")
                .setSmallIcon(R.drawable.ic_stat_maps_satellite)
                .setContentIntent(pendingIntent).build();
        startForeground(1, notification);

        final String folderPath = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext()).getString(MainActivity.FOLDER_PATH_ID, "");
        fileObserver = new FileObserver(folderPath, FileObserver.CREATE | FileObserver.CLOSE_WRITE) {
            Set<String> filesProcessed = new HashSet<>();

            @Override
            public void startWatching() {
                Log.d(getClass().getName(), "FileObserver starts watching path: " + folderPath);
                super.startWatching();
            }

            @Override
            public void onEvent(final int event, final String path) {
                Log.d(getClass().getName(), "FileObserver event: " + event + " path: " + path);
                if((FileObserver.CREATE == event || FileObserver.CLOSE_WRITE == event)
                        && (path.endsWith(".jpg") || path.endsWith(".JPG"))
                        && !filesProcessed.contains(path)) {
                    filesProcessed.add(path);
                    AsyncTask<String, Integer, Long> downloadImageTask = new AsyncTask<String, Integer, Long>() {
                        @Override
                        protected Long doInBackground(String... params) {

                            final String fileName = params[0];

                            TimerTask timerTask = new TimerTask() {
                                File file = new File(folderPath + "/" + fileName);
                                long size = file.length();
                                int progress = 0;

                                @Override
                                public void run() {
                                    File tmp = new File(folderPath + "/" + fileName);
//                                    Log.d(getClass().getName(), "file size: " + size + "tmp size: " + tmp.length());
                                    if(size<tmp.length()) {
                                        size = tmp.length();
                                        onProgressUpdate(++progress);
                                    } else {
                                        file = tmp;

                                        try {
                                            locate();
                                            Log.d(getClass().getName(), "processing " + fileName + "...");
                                            ExifInterface exifInterface = new ExifInterface(file.getPath());
                                            exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE, dec2DMS(location.getLatitude()));
                                            exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE,dec2DMS(location.getLongitude()));
                                            if (location.getLatitude() > 0)
                                                exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N");
                                            else
                                                exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "S");
                                            if (location.getLongitude()>0)
                                                exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E");
                                            else
                                                exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W");
                                            exifInterface.saveAttributes();
                                            Log.d(getClass().getName(), "gps data added to " + fileName + ".");
                                        } catch (IOException e) {
                                            Log.e(getClass().getName(), "error writing metadata", e);
                                            e.printStackTrace();
                                        }

                                        this.cancel();
                                    }
                                }
                            };

                            Timer timer = new Timer();
                            timer.schedule(timerTask, 50, 50);
                            return new File(folderPath + "/" + fileName).length();
                        }

                        protected void onProgressUpdate(Integer... progress) {
                            Log.d(getClass().getName(), "download progress: " + progress[0]);
                        }


                        @Override
                        protected void onPostExecute(Long aLong) {
                            Resources res = getApplicationContext().getResources();
                            int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
                            int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);

                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.outHeight = height;
                            options.outWidth = width;
                            options.inSampleSize = 8;
                            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                            Bitmap bitmap = BitmapFactory.decodeFile(folderPath+"/"+path, options);

                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(Uri.fromFile(new File(folderPath+"/"+path)), "image/jpeg");
                            final PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                                    intent,
                                    PendingIntent.FLAG_UPDATE_CURRENT);

                            Notification.Builder notificationBuilder = new Notification.Builder(getApplicationContext())
                                    .setContentTitle(NX_GEO_TAGGER)
                                    .setContentText("added location metadata to: " + path)
                                    .setLargeIcon(bitmap)
                                    .setSmallIcon(R.drawable.ic_stat_image_photo)
//                                    .setStyle(new Notification.BigPictureStyle().bigPicture(bitmap))
                                    .setContentIntent(pendingIntent);

                            setGroup(notificationBuilder, NX_GEO_TAGGER);

                            Notification notification = notificationBuilder.build();

                            NotificationManager notificationManager =
                                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                            notificationManager.notify(++notificationId, notification);
                            super.onPostExecute(aLong);
                        }

                        @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
                        private void setGroup(Notification.Builder notificationBuilder, String group) {
                            notificationBuilder.setGroup(group);
                        }
                    };

                    downloadImageTask.execute(path);
                }
            }
        };
        fileObserver.startWatching();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(getClass().getName(), "NXGeoTagger Service stopped");
        fileObserver.stopWatching();
        super.onDestroy();
    }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    String dec2DMS(double coord) {
        coord = coord > 0 ? coord : -coord;  // -105.9876543 -> 105.9876543
        String sOut = Integer.toString((int)coord) + "/1,";   // 105/1,
        coord = (coord % 1) * 60;         // .987654321 * 60 = 59.259258
        sOut = sOut + Integer.toString((int)coord) + "/1,";   // 105/1,59/1,
        coord = (coord % 1) * 60000;             // .259258 * 60000 = 15555
        sOut = sOut + Integer.toString((int)coord) + "/1000";   // 105/1,59/1,15555/1000
        return sOut;
    }


    public void locate() {
        // getting GPS status
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        // getting network status
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (isGPSEnabled) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        } else if (!isGPSEnabled && isNetworkEnabled) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
    }
}
