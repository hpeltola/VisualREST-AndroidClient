package tut.heikki.visualrestclient;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class EditPhotoMetadataActivity extends Activity {

    private TextView tvFilename;
    private ImageView img_thumb;
    private TextView tvMetadata;
    private Spinner areaspinner;
    private EditText et_metadata;
    private Button btn_save;
    private String fullpath;
    private long photoId;
    private String[] array;
    private Button btn_uploadEssence;
    private Button btn_uploadFacebook;
    private ImageView img_facebook;
    private Button btn_uploadFlickr;
    private ImageView img_flickr;
    private Button btn_uploadDropbox;
    private ImageView img_dropbox;
    
    private boolean essence_uploaded;
    private boolean facebook_uploaded;
    private boolean flickr_uploaded;
    
    
    @Override
    public void onResume(){
        super.onResume();  
        
        //---intent filter
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("GET_METADATA");
        intentFilter.addAction("GET_METADATATYPES");
        intentFilter.addAction("SAVE_METADATA");
        intentFilter.addAction("UPLOAD_ESSENCE");
        intentFilter.addAction("SEND_TO_EXTERNAL_SERVICE");
        //---register the receiver---
        registerReceiver(intentReceiver, intentFilter); 
        
    }
    
    @Override
    public void onPause(){
        super.onPause();
        
        this.unregisterReceiver(intentReceiver);
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.editphotometadata);
        
        tvFilename = (TextView) findViewById(R.id.tv_filename);
        img_thumb = (ImageView) findViewById(R.id.img_thumb);
        areaspinner = (Spinner) findViewById(R.id.areaspinner);
        areaspinner.setEnabled(false);
        et_metadata = (EditText) findViewById(R.id.et_metadata);
        et_metadata.setEnabled(false);
        btn_save = (Button) findViewById(R.id.btnSave);
        btn_save.setEnabled(false);
        
        btn_uploadEssence = (Button) findViewById(R.id.btnUploadEssence);
        btn_uploadEssence.setEnabled(false);
        btn_uploadFacebook = (Button) findViewById(R.id.btnUploadFacebook);
        img_facebook = (ImageView) findViewById(R.id.img_facebook);
        btn_uploadFlickr = (Button) findViewById(R.id.btnUploadFlickr);
        img_flickr = (ImageView) findViewById(R.id.img_flickr);
        btn_uploadDropbox = (Button) findViewById(R.id.btnUploadDropbox);
        img_dropbox = (ImageView) findViewById(R.id.img_dropbox);
        
        essence_uploaded = false;
        facebook_uploaded = false;
        flickr_uploaded = false;
        showAndHideUploadOptions();

        // Get the file we are showing from the received intent
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        
        if( !extras.containsKey("fullpath") || !extras.containsKey("photoId")){
            tvFilename.setText("Error, intent didn't have fullpath");
            return;
        }
        fullpath = extras.getString("fullpath");
        String name = fullpath.substring(fullpath.lastIndexOf("/")+1 );
        String path = fullpath.substring(0, fullpath.lastIndexOf("/")+1);
         
        photoId = extras.getLong("photoId");
        
        // Set filename and path on top
        tvFilename.setText(Html.fromHtml("<b>Filename: </b><small>" + name + "</small><br /> " + 
                "<b>Path: </b><small>" + path + "</small>")); 
        
        // Set image thumbnail    
        Bitmap bm = MediaStore.Images.Thumbnails.getThumbnail(getContentResolver(), 
                                                              photoId, 
                                                              MediaStore.Images.Thumbnails.MINI_KIND, 
                                                              null);
        img_thumb.setImageBitmap(bm);

        img_thumb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // When the photo is pressed, send intent ACTION_VIEW
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + fullpath), "image/*");
                startActivity(intent);
            }
        });
        
        // Get metadata from VisualREST
        if( !GetMetadata() ){
            tvMetadata.setText("Error fetching metadata from VisualREST!");
        }
        
        // Get all available metadatatypes from VisualREST
        GetMetadatatypes();
        
        btn_save.setOnClickListener(new View.OnClickListener() { 
            @Override
            public void onClick(View v) {
                
                int ipos = areaspinner.getSelectedItemPosition();
                String metadata = et_metadata.getText().toString();
                SaveMetadata(array[ipos], metadata);
            }
        });
        
        btn_uploadEssence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( UploadEssence() ){
                    Toast.makeText(getBaseContext(), "Uploading essence on the background", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(getBaseContext(), "Essence uploaded failed, please try again!", Toast.LENGTH_SHORT).show();
                }
                
            }
        });
        
        btn_uploadFacebook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                
                addCaptionAndSendToExternalService("Facebook");
                      
            }
        });
        
        btn_uploadFlickr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addCaptionAndSendToExternalService("Flickr");                
            }
        });
        
        btn_uploadDropbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( sendToExternalService("Dropbox", "") ){
                    Toast.makeText(getBaseContext(), "Sending to Dropbox", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(getBaseContext(), "Send failed, please try again!", Toast.LENGTH_SHORT).show();
                }     
            }
        });
        
        SettingsActivity.showTips("showTipEditPhotoMetadata", 
                "You can add and browse metadata of a photo. Essence can be uploaded to the server. " +
                "After the essence is uploaded, you can send the photo to external services, " +
                "as long as the services are linked in VisualREST website settings.",
                this);
    }

    
    
    public void addCaptionAndSendToExternalService(final String service){

        AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(EditPhotoMetadataActivity.this);
        myAlertDialog.setTitle("Send to " + service);
        myAlertDialog.setMessage("Add caption");
        
        final EditText input = new EditText(this);
        myAlertDialog.setView(input);
        myAlertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
    
                String caption = input.getText().toString();
                // Send to external service
                if( sendToExternalService(service, caption) ){
                    Toast.makeText(getBaseContext(), "Sending to " + service, Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(getBaseContext(), "Send failed, please try again!", Toast.LENGTH_SHORT).show();
                }       
                InputMethodManager im = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                im.hideSoftInputFromWindow(input.getWindowToken(), 0);
                arg0.cancel();

            }
        });
        
        myAlertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                // Do nothing when the Cancel button is clicked
                InputMethodManager im = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                im.hideSoftInputFromWindow(input.getWindowToken(), 0);
                arg0.cancel();
            }
        });
        myAlertDialog.show();
    }
    
    
    public boolean sendToExternalService(String service, String caption){
        try{
            
            SharedPreferences prefs = getSharedPreferences("AccountSettings", MODE_PRIVATE);
            if( !prefs.contains("username") || !prefs.contains("password")){
                return false;
            }
            String username = prefs.getString("username", "");
            String password = prefs.getString("password", "");
                        
            Intent intent = new Intent(getBaseContext(), HTTPRequestService.class);
            Bundle extras = new Bundle();
            extras.putInt("action", 13); //"SendToExternalService"
            extras.putString("username", username);
            extras.putString("password", password);
           // extras.putString("commit_hash", commit_hash);
            extras.putString("fullpath", fullpath);
            extras.putString("service", service);
            if( caption != null && !caption.equals("")){
                extras.putString("caption", caption);                
            }
            
            intent.putExtras(extras);

            startService(intent);
                                
        } catch (Exception e){
             Log.e(this.getClass().getName(), e.toString());
             return false;
        }
        return true;
    }
    
    public boolean UploadEssence(){
        try{
            
            SharedPreferences prefs = getSharedPreferences("AccountSettings", MODE_PRIVATE);
            if( !prefs.contains("username") || !prefs.contains("password")){
                return false;
            }
            String username = prefs.getString("username", "");
            String password = prefs.getString("password", "");
                        
            Intent intent = new Intent(getBaseContext(), HTTPRequestService.class);
            Bundle extras = new Bundle();
            extras.putInt("action", 8); //"uploadEssence"
            extras.putString("username", username);
            extras.putString("password", password);
           // extras.putString("commit_hash", commit_hash);
            extras.putString("fullpath", fullpath);
            
            intent.putExtras(extras);

            startService(intent);
                                
        } catch (Exception e){
             Log.e(this.getClass().getName(), e.toString());
             return false;
        }
        return true;
    }
    
    public boolean SaveMetadata(String metadatatype, String value){
        try{
            
            SharedPreferences prefs = getSharedPreferences("AccountSettings", MODE_PRIVATE);
            if( !prefs.contains("username") || !prefs.contains("password")){
                return false;
            }
            String username = prefs.getString("username", "");
            String password = prefs.getString("password", "");
                        
            Intent intent = new Intent(getBaseContext(), HTTPRequestService.class);
            Bundle extras = new Bundle();
            extras.putInt("action", 12); //"SaveMetadata"
            extras.putString("username", username);
            extras.putString("password", password);
            extras.putString("fullpath", fullpath);
            extras.putString("metadatatype", metadatatype);
            extras.putString("metadatavalue", value);
            
            intent.putExtras(extras);

            startService(intent);
                                
        } catch (Exception e){
             Log.e(this.getClass().getName(), e.toString());
             return false;
        }
        return true;
    }
    
    
    public boolean GetMetadata(){
        try{
            
            SharedPreferences prefs = getSharedPreferences("AccountSettings", MODE_PRIVATE);
            if( !prefs.contains("username") || !prefs.contains("password")){
                return false;
            }
            String username = prefs.getString("username", "");
            String password = prefs.getString("password", "");
                        
            Intent intent = new Intent(getBaseContext(), HTTPRequestService.class);
            Bundle extras = new Bundle();
            extras.putInt("action", 10); //"GetMetadata"
            extras.putString("username", username);
            extras.putString("password", password);
            extras.putString("fullpath", fullpath);
            
            intent.putExtras(extras);

            startService(intent);
                                
        } catch (Exception e){
             Log.e(this.getClass().getName(), e.toString());
             return false;
        }
        return true;
    }
    
    public boolean GetMetadatatypes(){
        try{
            
            SharedPreferences prefs = getSharedPreferences("AccountSettings", MODE_PRIVATE);
            if( !prefs.contains("username") || !prefs.contains("password")){
                return false;
            }
            String username = prefs.getString("username", "");
            String password = prefs.getString("password", "");
                        
            Intent intent = new Intent(getBaseContext(), HTTPRequestService.class);
            Bundle extras = new Bundle();
            extras.putInt("action", 11); //"GetMetadatatypes"
            extras.putString("username", username);
            extras.putString("password", password);
            
            intent.putExtras(extras);

            startService(intent);
                                
        } catch (Exception e){
             Log.e(this.getClass().getName(), e.toString());
             return false;
        }
        return true;
    }
    
    private void showAndHideUploadOptions(){
        if( essence_uploaded == false ){
            btn_uploadEssence.setVisibility(View.VISIBLE);
            btn_uploadFacebook.setVisibility(View.GONE);
            btn_uploadFlickr.setVisibility(View.GONE);
            btn_uploadDropbox.setVisibility(View.GONE);
            img_facebook.setVisibility(View.GONE);
            img_flickr.setVisibility(View.GONE);
            img_dropbox.setVisibility(View.GONE);
        }
        else{
            btn_uploadEssence.setVisibility(View.GONE);
            btn_uploadDropbox.setVisibility(View.VISIBLE);
            img_dropbox.setVisibility(View.VISIBLE);
            if( facebook_uploaded == true ){
                img_facebook.setVisibility(View.GONE);
                btn_uploadFacebook.setVisibility(View.GONE);
            }else{
                img_facebook.setVisibility(View.VISIBLE);                
                btn_uploadFacebook.setVisibility(View.VISIBLE);
            }
            if( flickr_uploaded == true ){
                img_flickr.setVisibility(View.GONE);
                btn_uploadFlickr.setVisibility(View.GONE);
            }else{
                img_flickr.setVisibility(View.VISIBLE);                
                btn_uploadFlickr.setVisibility(View.VISIBLE);
            }
            
        }
    }
    
    private void addMetadataLine(String metadataType, String metadataValue){
        
        View linearLayout =  findViewById(R.id.linearLayoutMetadata);

        TextView valueTV = new TextView(getBaseContext());
        if( metadataValue == null){
            valueTV.setText(metadataType);
        }
        else{
            valueTV.setText(Html.fromHtml("<b>" + metadataType + ":</b> " + 
                    "<small>" + metadataValue + "</small>"));            
        }
        
        
        valueTV.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
        valueTV.setSingleLine();
        
        ((LinearLayout) linearLayout).addView(valueTV);

        
    }
    
    private BroadcastReceiver intentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            
                
            Bundle extras = intent.getExtras();
            if (extras != null){
              
                String action = intent.getAction();

                if ( action.equals("GET_METADATA") ){
                    
                    String json = extras.getString("METADATA");             
                    if (json.equals("")){
                        tvMetadata.setText("Error fetching metadata from VisualREST!");
                    }
                    else{
                        try{
                            // The whole json
                            JSONObject jObject = new JSONObject(json);
                            // Names of first level (there is 1)
                            JSONArray items = jObject.names();
                            if(items != null){
                                // second level object
                                JSONObject jObject2 = jObject.getJSONObject(items.getString(0));
                                // second level names (there is 1)
                                JSONArray items2 = jObject2.names();
                                
                                // third level object
                                JSONObject jObject3 = jObject2.getJSONObject(items2.getString(0));
                                
                                // Get only user added metadata
                                JSONObject metadatas = jObject3.optJSONObject("metadatas");
                                
                                // Clear old information
                                LinearLayout metadataLayout =  (LinearLayout) findViewById(R.id.linearLayoutMetadata);
                                metadataLayout.removeAllViewsInLayout();
                                
                                
                                if(metadatas == null){
                                    addMetadataLine("No user added metadata yet!", null);                                    
                                }
                                else{
                                    JSONArray metadatatypes = metadatas.names();
                                    if(metadatatypes == null){
                                        addMetadataLine("No user added metadata yet!", null);
                                    }
                                    else{
                                        for(int i = 0; i<metadatatypes.length(); i++){
                                            // Add metadata type and value
                                            addMetadataLine(metadatatypes.getString(i),metadatas.get(metadatatypes.getString(i)).toString());
                                            
                                            if( metadatatypes.getString(i).equals("facebook_id") ){
                                                facebook_uploaded = true;
                                            }
                                            else if( metadatatypes.getString(i).equals("flickr_id")){
                                                flickr_uploaded = true;
                                            }
                                        }
                                    }
                                }

                                
                                String cached = jObject3.optString("file_status");
                                if(cached != null){
                                    if( cached.toString().equals("cached")){
                                        essence_uploaded = true;                                        
                                    }
                                    else if( cached.toString().equals("not cached")){
                                        essence_uploaded = false;
                                    }
                                    btn_uploadEssence.setEnabled(true);
                                    showAndHideUploadOptions();
                                }
                                
   
                            }
                            
                        } catch (Exception e){
                            Log.e(this.getClass().getName(), e.toString());
                            tvMetadata.setText("Error parsing metadata from VisualREST!");
                       }  
                    }
                }              
                else if( action.equals("GET_METADATATYPES") ){
                    
                    try{
                        String json = extras.getString("METADATATYPES");
                        
                        // The whole json
                        JSONObject jObject = new JSONObject(json);
                        // Names of first level (there is 1)
                        JSONArray items = jObject.names();
                        
                        array = new String[items.length()];
                        for(int j = 0; j < items.length();++j) {
                            array[j] = items.getString(j);//+ " ("+jObject.getString(items.getString(j)) + ")";
                        }

                        
                        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(getBaseContext(),
                                android.R.layout.simple_spinner_item, array);  //array you are populating  
                        adapter2.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
                        areaspinner.setAdapter(adapter2);
                        areaspinner.setSelection(2);
                        
                        btn_save.setEnabled(true);
                        et_metadata.setEnabled(true);
                        areaspinner.setEnabled(true);

                    } catch (Exception e){
                        Log.e(this.getClass().getName(), e.toString());
                    }  
                }
                else if( action.equals("SAVE_METADATA") ){
                    try{
                        boolean success = extras.getBoolean("SAVE_METADATA");
                        if( success ){
                            Toast.makeText(getBaseContext(), "Metadata saved", Toast.LENGTH_SHORT).show();
                            et_metadata.setText("");
                            GetMetadata();
                        }
                        else{
                            Toast.makeText(getBaseContext(), "Error saving metadata, please try again!", Toast.LENGTH_LONG).show();
                        }
                    
                    } catch (Exception e){
                        Log.e(this.getClass().getName(), e.toString());
                    } 
                }
                else if( action.equals("UPLOAD_ESSENCE") ){
                    try{
                        boolean success = extras.getBoolean("UPLOAD_ESSENCE");
                        if( success ){
                            Toast.makeText(getBaseContext(), "Essence uploaded", Toast.LENGTH_SHORT).show();
                            GetMetadata();
                        }
                        else{
                            Toast.makeText(getBaseContext(), "Error uploading essence, please try again!", Toast.LENGTH_LONG).show();
                        }

                    } catch (Exception e){
                        Log.e(this.getClass().getName(), e.toString());
                    }
                }
                else if( action.equals("SEND_TO_EXTERNAL_SERVICE") ){
                    try{
                        boolean success = extras.getBoolean("SEND_TO_EXTERNAL_SERVICE");
                        String service = extras.getString("service");
                        if( success ){
                            Toast.makeText(getBaseContext(), "Photo sent to " + service, Toast.LENGTH_SHORT).show();
                            GetMetadata();
                        }
                        else{
                            Toast.makeText(getBaseContext(), "Error sending photo to: " + service, Toast.LENGTH_LONG).show();
                        }

                    } catch (Exception e){
                        Log.e(this.getClass().getName(), e.toString());
                    }
                }
                
            }
        }
    };
}
