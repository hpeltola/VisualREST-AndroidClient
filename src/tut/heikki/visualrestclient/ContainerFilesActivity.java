package tut.heikki.visualrestclient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ContainerFilesActivity extends Activity {

    private int count;
    private Bitmap[] thumbnails;
    private boolean[] thumbnailsselection;
    private boolean[] fileincontainer;
    private String[] arrPath;
    private ImageAdapter imageAdapter;
    private GridView imagegrid;
    private boolean allChecked;
    private CheckBox checkbox;
    private long[] photoId;

    @Override
    public void onResume(){
        super.onResume();
        
        final Button saveBtn = (Button) findViewById(R.id.saveBtn);
        saveBtn.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                // When save button is clicked, find all checked images. 
                // Container program will send metadata of these files to the server.
                final int len = thumbnailsselection.length;
                int cnt = 0;
                
                List<String> paths = new ArrayList<String>();
                for (int i =0; i<len; i++)  
                {
                    if (thumbnailsselection[i]){
                        // Files that will be added to container
                        fileincontainer[i] = true;
                        paths.add(arrPath[i]);
                        
                        thumbnailsselection[i] = false;
                        cnt++;
                    }
                }
                if (cnt == 0){
                    Toast.makeText(getApplicationContext(), "Please select at least one image", Toast.LENGTH_LONG).show();
                } else {
                    String[] fullpaths = convert(paths);
                    intentToUploadMetadata(fullpaths);                    

                    Toast.makeText(getApplicationContext(), "Storing " +cnt + " images in container program.", Toast.LENGTH_LONG).show();
                }
            }
        });

        checkbox = (CheckBox) findViewById(R.id.allCheckBox);
        checkbox.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                AllSelected();
            }
        });
        
        final TextView tvAll = (TextView) findViewById(R.id.tvAll);
        tvAll.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                AllSelected();
            }  
        });
        
        
        // Get all images on device
        final String[] columns = { MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID };
        final String orderBy = MediaStore.Images.Media._ID + " DESC";
        Cursor imagecursor = managedQuery(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null,
                null, orderBy);
        int image_column_index = imagecursor.getColumnIndex(MediaStore.Images.Media._ID);
        
        // Initialize parameters
        this.count = imagecursor.getCount();
        this.thumbnails = new Bitmap[this.count];
        this.arrPath = new String[this.count];
        this.thumbnailsselection = new boolean[this.count];
        this.fileincontainer = new boolean[this.count];
        this.photoId = new long[this.count];
        
        try{
            // Open database adapter
            DBAdapter db = new DBAdapter(this);
            
            db.open();
            
            // Go through all photos
            for (int i = 0; i < this.count; i++) {

                imagecursor.moveToPosition(i);
                photoId[i] = imagecursor.getInt(image_column_index);
                int dataColumnIndex = imagecursor.getColumnIndex(MediaStore.Images.Media.DATA);
                
                // Get thumbnail to show
                thumbnails[i] = MediaStore.Images.Thumbnails.getThumbnail(
                        getApplicationContext().getContentResolver(), photoId[i],
                        MediaStore.Images.Thumbnails.MICRO_KIND, null);
                
                String fullpath = imagecursor.getString(dataColumnIndex);
                arrPath[i]= fullpath;
                
                int lastindex = fullpath.lastIndexOf("/") + 1;                
                String name = fullpath.substring(lastindex);
                String path = fullpath.substring(0, lastindex);
                    
                // See if the file is stored in container
                Cursor kursori = db.getFileWithNameAndPath(name, path);
                if( kursori.moveToFirst() ){
                    fileincontainer[i] = true;
                }
                else{
                    fileincontainer[i] = false;
                }                
            }
            
            // Close database connection
            db.close();
        }
        catch (Exception e){
            Log.e(this.getClass().getName(), e.toString());
        }
            
        // Create the image grid and set imageAdapter
        imagegrid = (GridView) findViewById(R.id.PhoneImageGrid);
        imageAdapter = new ImageAdapter();
        imagegrid.setAdapter(imageAdapter);
       // imagecursor.close();
        
        //---intent filter
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("UPLOAD_METADATA_MULTIPLE_FILES");
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
        setContentView(R.layout.containerfiles);        
    }

    // All photos selected/deselected
    public void AllSelected(){
        for(int i = 0 ; i<count ; i++){
            if(allChecked){
                thumbnailsselection[i] = false;
            }
            else{
                if(fileincontainer[i]){
                    thumbnailsselection[i] = false;  
                }
                else{
                    thumbnailsselection[i] = true;                                            
                }
            }
        }
        imagegrid.invalidateViews();
        allChecked = !allChecked;
        checkbox.setChecked(allChecked);
    }
    
    private void intentToUploadMetadata(String[] fullpaths){
        
        try
        {
            // Check VisualREST authentication parameters
            SharedPreferences prefs = getSharedPreferences("AccountSettings", MODE_PRIVATE);

            String username = prefs.getString("username", "");
            String password = prefs.getString("password", "");
            
            
            Intent intentUpload = new Intent(getBaseContext(), HTTPRequestService.class);
            Bundle extrasUpload = new Bundle();
            extrasUpload.putInt("action", 9); // "uploadMetadataMultipleFiles"
            extrasUpload.putString("username", username);
            extrasUpload.putString("password", password);
            
            // This part is different to the intent received from photo gallery
            Bundle extras = new Bundle();
            extras.putStringArray("fullpaths", fullpaths);
            
            extrasUpload.putBundle("extras", extras);
            
            intentUpload.putExtras(extrasUpload);

            startService(intentUpload);
            

        } catch (Exception e)
        {
            Log.e(this.getClass().getName(), e.toString());
        }
        
    }
    
    public class ImageAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        public ImageAdapter() {
            mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        // needed methods inherited from BaseAdapter
        public int getCount() {
            return count;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        // Create and get view of an image
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(
                        R.layout.galleryitem, null);
                holder.imageview = (ImageView) convertView.findViewById(R.id.thumbImage);
                holder.checkbox = (CheckBox) convertView.findViewById(R.id.itemCheckBox);

                convertView.setTag(holder);
            }
            else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.checkbox.setId(position);
            holder.imageview.setId(position);
            holder.checkbox.setOnClickListener(new OnClickListener() {

                public void onClick(View v) {
                    // When checkbox is clicked, toggle checkd value and save it also to thumbnailselection
                    CheckBox cb = (CheckBox) v;
                    int id = cb.getId();
                    if (thumbnailsselection[id]){
                        cb.setChecked(false);
                        thumbnailsselection[id] = false;
                    } else {
                        cb.setChecked(true);
                        thumbnailsselection[id] = true;
                    }
                }
            });
            
            holder.imageview.setImageBitmap(thumbnails[position]);
            holder.checkbox.setChecked(thumbnailsselection[position]);
            holder.id = position;
            
            // Mark images that are stored by virtual container
            if(fileincontainer[position] ){
                holder.checkbox.setVisibility(View.GONE);
                holder.imageview.setBackgroundColor(0xFF00AA00);

                holder.imageview.setOnClickListener(new OnClickListener() {

                    public void onClick(View v) {
                        // When the photo is pressed, send intent ACTION_VIEW
                        int id = v.getId();
                        Intent intent = new Intent();                        
                        intent.setAction("tut.heikki.editphotometadata");
                        intent.putExtra("fullpath", arrPath[id]);
                        intent.putExtra("photoId", photoId[id]);
                       // intent.setDataAndType(Uri.parse("file://" + arrPath[id]), "image/*");
                        startActivity(intent);                        
                    }
                    
                });
                
            }
            else{
                holder.checkbox.setVisibility(View.VISIBLE);
                holder.imageview.setBackgroundColor(0xFF000000);
                holder.imageview.setOnClickListener(new OnClickListener() {

                    public void onClick(View v) {
                        // When the photo is pressed, send intent ACTION_VIEW
                        int id = v.getId();
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.parse("file://" + arrPath[id]), "image/*");
                        startActivity(intent);
                    }
                });
            }

            return convertView;
        }
    }
    class ViewHolder {
        ImageView imageview;
        CheckBox checkbox;
        int id;
    }
    
    
    private BroadcastReceiver intentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            
                
            Bundle extras = intent.getExtras();
            if (extras != null){
              
                String action = intent.getAction();

                if ( action.equals("UPLOAD_METADATA_MULTIPLE_FILES") ){
                    
                    Boolean success = extras.getBoolean("UPLOAD_METADATA_MULTIPLE_FILES");             
                    if (success == true){
                        imagegrid.invalidateViews();
                        Toast.makeText(getBaseContext(), "Successfully sent metadata to VisualREST!", Toast.LENGTH_SHORT).show();
                    }
                    else if( success == false){
                        Toast.makeText(getBaseContext(), "Error uploading, problem parsing metadata?", Toast.LENGTH_LONG).show();

                    }
                }
            }
        }
    };
    
    static String[] convert(List<String> from) {
        ArrayList<String> list = new ArrayList<String>();
        for (String strings : from) {
            Collections.addAll(list, strings);
        }
        return list.toArray(new String[list.size()]);
    }

}
