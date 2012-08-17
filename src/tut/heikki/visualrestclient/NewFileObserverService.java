package tut.heikki.visualrestclient;

import java.io.File;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.IBinder;
import android.util.Log;


public class NewFileObserverService extends Service {

	private static final String TAG = "NewFileObserverService";
	private String path;
	private FileObserver observer;
	
	@Override
    public void onCreate() {
		try{

			// Is the default camera directory "/mnt/sdcard/DCIM/Camera"?
			path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
			File f = new File(path+"/Camera");
			if(f.exists() && f.isDirectory()) {
				path += "/Camera";
			}
			else{
				// Or is it ".../100MEDIA" ?
				f = new File(path+"/100MEDIA");
				if(f.exists() && f.isDirectory() ){
					path += "/100MEDIA";
				}
				else{
					Log.i(TAG, "...ERROR. THE FOLDER DOES NOT EXIST");
					return;
				}
			}
			
			
			// 
			observer = new FileObserver(path) { // set up a file observer to watch this directory on sd card
	            @Override
	        public void onEvent(int event, String file) {
	            if(event == FileObserver.CREATE && !file.equals(".probe") && !file.substring((file.lastIndexOf("."))).equals(".mp4") ){ // check if its a "create" and not equal to .probe because thats created every time camera is launched
	                    
	                Log.i(TAG, "File created [" + path + file + "]");
	                
	                try
		            {
		                // Check VisualREST authentication parameters
		                SharedPreferences prefs = getSharedPreferences("AccountSettings", MODE_PRIVATE);

		                String username = prefs.getString("username", "");
		                String password = prefs.getString("password", "");
		                
		            	
		                Intent intentUpload = new Intent(getBaseContext(), HTTPRequestService.class);
		                Bundle extrasUpload = new Bundle();
		                extrasUpload.putInt("action", 5); // "uploadMetadata"
		                extrasUpload.putString("username", username);
		                extrasUpload.putString("password", password);
		                
		                // This part is different to the intent received from photo gallery
		                Bundle extras = new Bundle();
		                extras.putString("fullpath", path+"/"+file);
		                
		                extrasUpload.putBundle("extras", extras);
		                
		                intentUpload.putExtras(extrasUpload);

		                startService(intentUpload);
		                

		            } catch (Exception e)
		            {
		                Log.e(this.getClass().getName(), e.toString());
		            }
	            }
	        }
			};
			observer.startWatching(); // start the observer
		} catch (Exception e){
			Log.e(TAG, e.toString());
		}
	}
	
	
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
    	try{
    		// Close the observer
    		observer.stopWatching();
    	} catch (Exception e){
			Log.e(this.getClass().getName(), e.toString());
    	}
    }
	
    @Override
    public IBinder onBind(Intent intent) {
    	return null;
    }
}
