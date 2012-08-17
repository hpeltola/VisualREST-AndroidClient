package tut.heikki.visualrestclient;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class UploadMetadataActivity extends Activity {

	
	
    @Override
    public void onResume(){
    	super.onResume();
    	//---intent filter
		IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("UPLOAD_METADATA");
        //---register the receiver---
        registerReceiver(intentReceiver, intentFilter);   	
    }
    
    @Override
    public void onPause(){
    	super.onPause();
    	this.unregisterReceiver(intentReceiver);
    }
	
	
	public void onCreate(Bundle savedInstanceState)
	{
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.storeindevice);

	    
	    Intent intent = getIntent();
	    Bundle extras = intent.getExtras();
	    String action = intent.getAction();

	    // if this is from the share menu
	    if (Intent.ACTION_SEND.equals(action))
	    {
	        if (extras.containsKey(Intent.EXTRA_STREAM))
	        {
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
	                extrasUpload.putBundle("extras", extras);
	                
	                intentUpload.putExtras(extrasUpload);

	                startService(intentUpload);
	                

	            } catch (Exception e)
	            {
	                Log.e(this.getClass().getName(), e.toString());
	            }

	        } else if (extras.containsKey(Intent.EXTRA_TEXT))
	        {
	            return;
	        }
	    }

	}
	
	
	 private BroadcastReceiver intentReceiver = new BroadcastReceiver() {
	        @Override
	        public void onReceive(Context context, Intent intent) {
	        	
	            	
	        	Bundle extras = intent.getExtras();
	        	if (extras != null){
	  	    	  
	        		String action = intent.getAction();

	        		if ( action.equals("UPLOAD_METADATA") ){
	        			
	        			ProgressBar pbar = (ProgressBar)findViewById(R.id.progressBar1);
	        			pbar.setVisibility(View.GONE);
	        			
	        			TextView txt = (TextView) findViewById(R.id.metadataSaved);
	        			
	        			Button btnClose = (Button) findViewById(R.id.btnClose);
	        			btnClose.setOnClickListener(new View.OnClickListener() {
	        				public void onClick(View v) {
	        					finish();
	        				}
	        			});
	        			btnClose.setVisibility(View.VISIBLE);
	        			findViewById(R.id.progressBar1).setVisibility(View.GONE);
	        			
	        			
		  	    	    Boolean success = extras.getBoolean("UPLOAD_METADATA");	        	
		  	    	    if (success == true){
		  	    		    Toast.makeText(getBaseContext(), "Metadata uploaded!", Toast.LENGTH_SHORT).show();
		  	    	    	
		  	    		    txt.append("Uploaded succesfully");

		  	    	    }
		  	    	    else if( success == false){
		  	  	    		Toast.makeText(getBaseContext(), "Error uploading, problem with authentication?", Toast.LENGTH_LONG).show();

		  	  	    		txt.append("Error in uploading. Check username and password");
		  	  	    		
		  	  	    		Button btnVrest = (Button) findViewById(R.id.btnVrestSettings);
		  	  	    		btnVrest.setOnClickListener(new View.OnClickListener() {
		  	  	    			public void onClick(View v) {
		  	  	    				startActivity(new Intent("tut.heikki.accountsettings"));
		  	  	    			}
		  	  	    		});
		  	  	    		btnVrest.setVisibility(View.VISIBLE);
		  	    	    }
	        		}
	        		
	        	}
	  	    	  
	            
	        }
	    };
	

    
    public String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }
	
}
