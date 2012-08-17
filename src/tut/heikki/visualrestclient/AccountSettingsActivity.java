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
import android.widget.Toast;

public class AccountSettingsActivity extends Activity {

	private SharedPreferences prefs;
	private String prefName = "AccountSettings";
	private Button btnSave;
	private Button btnTest;
	private Button btnShowAllTips;
	private EditText etUsername;
	private EditText etPassword;

	
    @Override
    public void onResume(){
    	super.onResume();
    	//---intent filter
		IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("USER_AUTH");
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
	    setContentView(R.layout.accountsettings);
	    
	    etUsername = (EditText) findViewById(R.id.txt_username);
	    etPassword = (EditText) findViewById(R.id.txt_password);
	    
	    btnSave = (Button) findViewById(R.id.btnSave);
	    btnSave.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
			    
			    AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(AccountSettingsActivity.this);
                myAlertDialog.setMessage("Save and overwrite possible old values?");
                myAlertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface arg0, int arg1) {
                        // Save the username and password
                        //---get the SharedPreferences object---
                        prefs = getSharedPreferences(prefName, MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        //---save the values in the EditText view to preferences---
                        editor.putString("username", etUsername.getText().toString());
                        editor.putString("password", etPassword.getText().toString());
                        //---saves the values---
                        editor.commit();
                        //---display file saved message---
                        Toast.makeText(getBaseContext(), "Settings saved!", Toast.LENGTH_SHORT).show();
                       
                    }});
                myAlertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                      
                    public void onClick(DialogInterface arg0, int arg1) {
                        // Do nothing when the Cancel button is clicked
                    }});
                myAlertDialog.show();

			}
		});
	    
	    btnTest = (Button) findViewById(R.id.btnTest);
	    btnTest.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try{
					
					SharedPreferences prefs = getSharedPreferences(prefName, MODE_PRIVATE);
					String username = prefs.getString("username", "");
					String password = prefs.getString("password", "");
					
					Intent intent = new Intent(getBaseContext(), HTTPRequestService.class);
			        Bundle extras = new Bundle();
			        extras.putInt("action", 1); //"authenticateUser"
			        extras.putString("username", username);
			        extras.putString("password", password);
			        
			        intent.putExtras(extras);
			        startService(intent);
			        
			        Toast.makeText(getBaseContext(), "Testing authentication to VisualREST!", Toast.LENGTH_SHORT).show();
					            		
				} catch (Exception e){
					 Log.e(this.getClass().getName(), e.toString());
				}
			}
		});
	    
	    btnShowAllTips = (Button) findViewById(R.id.btnShowAllTips);
	    btnShowAllTips.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = getSharedPreferences(prefName, MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.remove("showTips");
                editor.remove("showTipSettings");
                editor.remove("showTipAccountSettings");
                editor.remove("showTipContainerFiles");
                editor.remove("showTipDeviceSettings");
                editor.remove("showTipEditPhotoMetadata");
                editor.commit();
                
                Toast.makeText(getBaseContext(), "Tips are shown again at each view!", Toast.LENGTH_SHORT).show();
            }
        });
	    
	    
	    //---load the SharedPreferences object---
        SharedPreferences prefs = getSharedPreferences(prefName, MODE_PRIVATE);
        if( prefs.contains("username") )
        {
        	etUsername.setText(prefs.getString("username", ""));
        }
        if( prefs.contains("password"))
        {
        	etPassword.setText(prefs.getString("password", ""));
        }
        
        SettingsActivity.showTips("showTipAccountSettings", 
                "You need to register at http://visualrest.cs.tut.fi and create an account. " +
                "Insert and save your username and password here. " + 
                "'Test connection' tries to authenticate with VisualREST using the username and password.",
                this);
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
		  	    		    Toast.makeText(getBaseContext(), "Authentication successful!", Toast.LENGTH_SHORT).show();		  	    	    	
		  	    	    }
		  	    	    else if( success == false){
		  	  	    		Toast.makeText(getBaseContext(), "Error authenticating user!", Toast.LENGTH_SHORT).show();
		  	    	    }
	        		}	        		
	        	}
	  	    	  
	            
	        }
	    };
	

}
