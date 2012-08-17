package tut.heikki.visualrestclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class DeviceSettingsActivity extends Activity {
	
		
	
	private SharedPreferences prefs;
	private String prefName = "AccountSettings";
	private TextView tvUsername;
	private TextView tvPassword;
	private TextView tvDeviceName;
	private EditText etDeviceName;
	
	private Button btnUnregister;
	private Button btnRegister;
	
    @Override
    public void onResume(){
    	super.onResume();
    	//---intent filter
    	IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("DEVICE_REGISTER");
        //---register the receiver---
        registerReceiver(intentReceiver, intentFilter);  	
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
	    setContentView(R.layout.devicesettings);
	
	    tvUsername = (TextView) findViewById(R.id.tv_username);
	    tvPassword = (TextView) findViewById(R.id.tv_password);
	    tvDeviceName = (TextView) findViewById(R.id.tv_deviceName);
	    etDeviceName = (EditText) findViewById(R.id.txt_deviceName);

	    
	    btnRegister = (Button) findViewById(R.id.btnRegister);
	    btnRegister.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try{
					String devname = etDeviceName.getText().toString();
					
					SharedPreferences xmppprefs = getSharedPreferences("XMPPSettings", MODE_PRIVATE);					
					if ( devname.equals("") ){
						Toast.makeText(getBaseContext(), "Give name for the container!", Toast.LENGTH_SHORT).show();
					}
					else if ( xmppprefs.contains("username") ){
						Toast.makeText(getBaseContext(), "You already registered: "+xmppprefs.getString("username", ""), Toast.LENGTH_SHORT).show();
					} 
					else {
					

						
						prefs = getSharedPreferences(prefName, MODE_PRIVATE);
						String username = prefs.getString("username", "");
						String password = prefs.getString("password", "");
						
						Intent intent = new Intent(getBaseContext(), HTTPRequestService.class);
				        Bundle extras = new Bundle();
				        extras.putInt("action", 4); //"deviceRegister"
				        extras.putString("username", username);
				        extras.putString("password", password);
				        extras.putString("devicename", devname);
				        
				        intent.putExtras(extras);
	
				        startService(intent);
						
					}
			        
				} catch (Exception e){
					Log.e(this.getClass().getName(), e.toString());
				}
				
			}
		});
	    
	    btnUnregister = (Button) findViewById(R.id.btnUnregister);
	    btnUnregister.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
			    
			    AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(DeviceSettingsActivity.this);
			    //myAlertDialog.setTitle("--- Title ---");
			    myAlertDialog.setMessage("Unlink this device from VisualREST?");
			    myAlertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {

			        public void onClick(DialogInterface arg0, int arg1) {
			            // Unregister from the server
		                Intent intent = new Intent(getBaseContext(), XMPPService.class);
		                getBaseContext().stopService(intent);
		                
		                // Remove database
		                getBaseContext().deleteDatabase("VisualrestDB");
		                
		                // Remove the device's XMPP settings
		                SharedPreferences tempprefs = getSharedPreferences("XMPPSettings", MODE_PRIVATE);
		                SharedPreferences.Editor tempeditor = tempprefs.edit();
		                tempeditor.clear();
		                tempeditor.commit();
		                
		                // Refresh the view
		                deviceNotRegistered();
		                
		                Toast.makeText(getBaseContext(), "Device unregistered from VisualREST", Toast.LENGTH_SHORT).show();
			        }});
			    myAlertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			          
			        public void onClick(DialogInterface arg0, int arg1) {
			            // do something when the Cancel button is clicked
			        }});
			    myAlertDialog.show();
			    

			}
	    });
	    
	
	    
	    
        // Show saved settings
	    prefs = getSharedPreferences(prefName, MODE_PRIVATE);
	    if( prefs.contains("username")){
	    	tvUsername.setText("Username: " + prefs.getString("username", ""));
	    }
	    if( prefs.contains("password")){
	    	tvPassword.setText("Password length: " + prefs.getString("password", "").length());	    	
	    }
        
	    IsDeviceRegistered();
        
	    SettingsActivity.showTips("showTipDeviceSettings", 
                "Insert a name for this container and register it to VisualREST. " +
                "The container can be unregistered.",
                this);
	}
	
	private void IsDeviceRegistered(){
		
		SharedPreferences xmppprefs = getSharedPreferences("XMPPSettings", MODE_PRIVATE);
		if ( xmppprefs.contains("devicename") ){
			deviceIsRegistered(xmppprefs.getString("devicename", ""));
		}
		else{
			deviceNotRegistered();

		}
		
		
		
	}
	
	private void deviceIsRegistered(String devicename){
		// Hide these two
		etDeviceName.setFocusable(false);
		etDeviceName.setVisibility(View.GONE);
		btnRegister.setVisibility(View.GONE);
		btnUnregister.setVisibility(View.VISIBLE);
		
		tvDeviceName.setVisibility(View.VISIBLE);
		tvDeviceName.setText("Device name: " + devicename );

	}
	
	private void deviceNotRegistered(){
		tvDeviceName.setVisibility(View.GONE);
		btnUnregister.setVisibility(View.GONE);
		
		btnRegister.setVisibility(View.VISIBLE);		
		etDeviceName.setVisibility(View.VISIBLE);
		//etDeviceName.setFocusable(true);
		etDeviceName.setFocusableInTouchMode(true);
		
	}
	

	 private BroadcastReceiver intentReceiver = new BroadcastReceiver() {
	        @Override
	        public void onReceive(Context context, Intent intent) {
	        	
	            	
	        	Bundle extras = intent.getExtras();
	        	if (extras != null){
	  	    	  
	        		String action = intent.getAction();

	        		if (action.equals("DEVICE_REGISTER") ){
	        			
	        			Boolean success = extras.getBoolean("DEVICE_REGISTER");
	        			if ( success == true ){
	        				Toast.makeText(getBaseContext(), "Registered this device succesfully", Toast.LENGTH_SHORT).show();
	        					        				
	        				String username = extras.getString("username");
	        				String devicename = extras.getString("devicename");
	        				String password = extras.getString("password");
	        				
	        		        //---get the SharedPreferences object---
	                        prefs = getSharedPreferences("XMPPSettings", MODE_PRIVATE);
	                        SharedPreferences.Editor editor = prefs.edit();
	                        //---save the values in the EditText view to preferences---
	                        editor.putString("username", username + "_" + devicename);
	                        editor.putString("devicename", devicename);
	                        editor.putString("password", password);
	                        //---saves the values---
	                        editor.commit();
	                        
	        				// Refresh the view
	                       deviceIsRegistered(devicename);
	                        
	        			}
	        			else if ( success == false ){
	        				Toast.makeText(getBaseContext(), "Error registering this device", Toast.LENGTH_SHORT).show();
	        			}
	        		}
	        		
	        	}
	  	    	  
	            
	        }
	    };
	
	    
}
