package tut.heikki.visualrestclient;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity{
	
	IntentFilter intentFilter;
	
    private Button btnDevice;
    private Button btnVrest;
    private Button btnAutomaticMetadataUpload;
    private Button btnXMPP;
    private Button btnAddedFiles;
    
    @Override
    public void onResume(){
    	super.onResume();
    	//---intent filter
    	intentFilter = new IntentFilter();
    	intentFilter.addAction("USER_AUTH");
    	//---register the receiver---
    	registerReceiver(intentReceiver, intentFilter);    	
    	
    	
        // Check VisualREST authentication parameters
        SharedPreferences prefs = getSharedPreferences("AccountSettings", MODE_PRIVATE);

        if ( prefs.contains("username") && prefs.contains("password"))
        {
	        String username = prefs.getString("username", "");
	        String password = prefs.getString("password", "");
	        
	        
	        Intent intent = new Intent(getBaseContext(), HTTPRequestService.class);
	        Bundle extras = new Bundle();
	        extras.putInt("action", 1); //"authenticateUser"
	        extras.putString("username", username);
	        extras.putString("password", password);
	        
	        intent.putExtras(extras);
	
	        startService(intent);
        }
        
        // Check device settings
        SharedPreferences xmppprefs = getSharedPreferences("XMPPSettings", MODE_PRIVATE);
		if ( xmppprefs.contains("devicename") ){
        	ImageView icon = (ImageView) findViewById(R.id.deviceOKIcon);
            icon.setImageResource(R.drawable.online_32);
            btnXMPP.setEnabled(true);
            btnAutomaticMetadataUpload.setEnabled(true);
            btnAddedFiles.setEnabled(true);
		}
		else{
    		ImageView icon = (ImageView) findViewById(R.id.deviceOKIcon);
            icon.setImageResource(R.drawable.offline_32);
            btnXMPP.setEnabled(false);
            btnAutomaticMetadataUpload.setEnabled(false);
            btnAddedFiles.setEnabled(false);
		}
        
		// Check newFileObserver
        IsNewFileObserverRunning();
      
        // Check XMPPService
		IsXMPPServiceRunning();
      
    	
    }
    
    @Override
    public void onPause(){
    	super.onPause();
    	this.unregisterReceiver(intentReceiver);
    }
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        

        
        
        btnVrest = (Button) findViewById(R.id.btnVrestSettings);
        btnVrest.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	startActivity(new Intent("tut.heikki.accountsettings"));
            }
        });
        
        btnDevice = (Button) findViewById(R.id.btnDeviceSettings);
        btnDevice.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	startActivity(new Intent("tut.heikki.devicesettings"));
            }
        });
        
        btnAutomaticMetadataUpload = (Button) findViewById(R.id.btnAutomaticMetadataUpload);
        btnAutomaticMetadataUpload.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Change automaticMetadataUpload either on/off
				if( IsNewFileObserverRunning() ){
					// Stop the service
					Intent intent = new Intent(getBaseContext(), NewFileObserverService.class);
					getBaseContext().stopService(intent);
				}
				else{
					// Start the service
					Intent intent = new Intent();
					intent.setClassName("tut.heikki.visualrestclient", "tut.heikki.visualrestclient.NewFileObserverService");
					getBaseContext().startService(intent);
					Toast.makeText(getBaseContext(), "All new photos are automatically added to container program!", Toast.LENGTH_SHORT).show();
				}
				// Update the view
				IsNewFileObserverRunning();
				
				// Update the widget
                UpdateWidget();
			}
		});
		
        
        btnXMPP = (Button) findViewById(R.id.btnXMPPSettings);
        btnXMPP.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// Change automaticMetadataUpload either on/off
				if( IsXMPPServiceRunning() ){
					// Stop the service
					Intent intent = new Intent(getBaseContext(), XMPPService.class);
					getBaseContext().stopService(intent);
				}
				else{
					// Start the service
					Intent intent = new Intent();
					intent.setClassName("tut.heikki.visualrestclient", "tut.heikki.visualrestclient.XMPPService");
					getBaseContext().startService(intent);
					Toast.makeText(getBaseContext(), "XMPP messages can now be received from the server!", Toast.LENGTH_SHORT).show();
				}
				// Update the view
				IsXMPPServiceRunning();
				
				// Update the widget
				UpdateWidget();

			}
		});
        

        btnAddedFiles = (Button) findViewById(R.id.btnAddedFiles);
        btnAddedFiles.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent("tut.heikki.containerfiles"));
            }
        });
        
        SettingsActivity.showTips("showTipSettings", 
                "'VisualREST account settings': Save your VisualREST account username and password.\n\n" +
                "'Device settings': Link container program with VisualREST server.\n\n" +
                "'Start automatic metadata upload' Toggle whether to automatically add new photos to container program.\n\n" + 
                "'Start XMPP service': Toggle XMPP connection on/off.\n\n" + 
                "'Container files': Show photos of container program and allows you to add/manage container photos.",
                this);
    }
    
    private boolean IsNewFileObserverRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("tut.heikki.visualrestclient.NewFileObserverService".equals(service.service.getClassName())) {
            	// Set the icon
            	ImageView icon = (ImageView) findViewById(R.id.automaticMetadataUploadOKIcon);
                icon.setImageResource(R.drawable.online_32);
                
                // Set the text
                btnAutomaticMetadataUpload.setText(R.string.stopAutomaticMetadataUpload);
                
                return true;
            }
        }
        // Set the icon
		ImageView icon = (ImageView) findViewById(R.id.automaticMetadataUploadOKIcon);
        icon.setImageResource(R.drawable.offline_32);
        
        // Set the text
        btnAutomaticMetadataUpload.setText(R.string.startAutomaticMetadataUpload);
        
        return false;
    } 
    
    
    
    private boolean IsXMPPServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("tut.heikki.visualrestclient.XMPPService".equals(service.service.getClassName())) {
            	// Set the icon
            	ImageView icon = (ImageView) findViewById(R.id.XMPPOKIcon);
                icon.setImageResource(R.drawable.online_32);

                // Set the text
                btnXMPP.setText(R.string.stopXMPPService);
                
                return true;
            }
        }
		ImageView icon = (ImageView) findViewById(R.id.XMPPOKIcon);
        icon.setImageResource(R.drawable.offline_32);

        // Set the text
        btnXMPP.setText(R.string.startXMPPService);
        return false;
    }

    private void UpdateWidget(){
        ComponentName thisWidget = new ComponentName(getApplicationContext(),
            VrestWidgetProvider.class);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getBaseContext().getApplicationContext());
        int[] allWidgetIds2 = appWidgetManager.getAppWidgetIds(thisWidget);
        // Build the intent to call the service
        Intent intent = new Intent(getBaseContext().getApplicationContext(),
                UpdateWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds2);
    
        // Update the widgets via the service
        getBaseContext().startService(intent);
    }
    
    
    
    private BroadcastReceiver intentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	
            	
        	Bundle extras = intent.getExtras();
        	if (extras != null){
  	    	  
        		String action = intent.getAction();

        		if ( action.equals("USER_AUTH") ){
        			
	  	    	    Boolean success = extras.getBoolean("USER_AUTH");	        	
	  	    	    if (success == true){
	  	                ImageView icon = (ImageView) findViewById(R.id.accountOKIcon);
	  	                icon.setImageResource(R.drawable.online_32);
	  	    	    }
	  	    	    else if( success == false){
	  	  	    		Toast.makeText(getBaseContext(), "Error authenticating user!", Toast.LENGTH_SHORT).show();
	  	  	    		ImageView icon = (ImageView) findViewById(R.id.accountOKIcon);
	  	  	    		icon.setImageResource(R.drawable.offline_32);
	  	    	    }
        		}

        	}
  	    	  
            
        }
    };

    
    public static void showTips(final String tipName, String tip, Context context){

        final SharedPreferences prefs = context.getSharedPreferences("AccountSettings", MODE_PRIVATE);
        
        if( prefs.getBoolean("showTips", true) && prefs.getBoolean(tipName, true) ){
            
            final Dialog dialog = new Dialog(context);
            dialog.setContentView(R.layout.help_dialog);
            TextView t = (TextView) dialog.findViewById(R.id.tvHelp);
            t.setText(tip);
            Button b = (Button) dialog.findViewById(R.id.ok);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    
                    CheckBox c = (CheckBox) dialog.findViewById(R.id.checkNoTips);
                    if( c.isChecked() ){
                        // Do not show tips in the future
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("showTips", false);
                        editor.commit();
                    }
                    
                    c = (CheckBox) dialog.findViewById(R.id.checkNoTip);
                    if( c.isChecked() ){
                        // Do not show this tip in the future
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean(tipName, false);
                        editor.commit();
                    }
                    
                    dialog.cancel();
                }
            });
            dialog.setTitle("Tips");
            dialog.show();
        }

    }
	
}
