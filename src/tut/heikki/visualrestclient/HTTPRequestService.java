package tut.heikki.visualrestclient;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

public class HTTPRequestService extends IntentService{
	
	  private String host = "http://visualrest.cs.tut.fi";
	
	  public HTTPRequestService() {
	      super("HTTPRequestService");
	  }


	  	  
	  /**
	   * The IntentService calls this method from the default worker thread with
	   * the intent that started the service. When this method returns, IntentService
	   * stops the service, as appropriate.
	   */
	  @Override
	  protected void onHandleIntent(Intent intent) {
		  		  
	//	  Toast.makeText(this, "In the service", Toast.LENGTH_SHORT).show();
	      Bundle extras = intent.getExtras();
	      if (extras != null){
	    	  
	    	  String username = extras.getString("username");
	    	  String password = extras.getString("password");
	    	  int action =  extras.getInt("action");
	    	  
	    	  // Make these ready for the reply
	    	  boolean success;
	    	  Intent broadcastIntent = new Intent();
	    	  Bundle extraBack = new Bundle();
	    	  Bundle extrasUpload;
	    	  String devicename;
	    	  String fullpath;
	    	  String json;
	    	  
	    	  
	    	  switch (action)
	    	  {
	    	  	case 1: //"authenticateUser"
	    	  		success = AuthenticateUser(username, password);
	    	  		
    	            //---send a broadcast to inform the activity
    	            broadcastIntent.setAction("USER_AUTH");
    	            
    	            extraBack.putBoolean("USER_AUTH", success);
    	            broadcastIntent.putExtras(extraBack);
    	            
    	            getBaseContext().sendBroadcast(broadcastIntent);
	
	    	  		break;
	    	  
	  //  	  	case 2: 
	    	  		
	    	  		
	    	  	case 3: // "uploadPhoto"
	    	  		
	    	  		extrasUpload = extras.getBundle("extras");
	    	  		
	    	  		success = UploadPhoto(username, password, extrasUpload);
	    	  		
	    	  		//---send a broadcast to inform the activity
    	            broadcastIntent.setAction("UPLOAD_SUCCESS");
    	            
    	            extraBack.putBoolean("UPLOAD_SUCCESS", success);
    	            broadcastIntent.putExtras(extraBack);
    	            
    	            getBaseContext().sendBroadcast(broadcastIntent);
	
	    	  		break;
	    	  		
	    	  	case 4: //"deviceRegister"
	    	  		devicename = extras.getString("devicename");
	    	  		success = DeviceRegister(username, password, devicename);
	    	  		
    	            //---send a broadcast to inform the activity
    	            broadcastIntent.setAction("DEVICE_REGISTER");
    	            
    	            extraBack.putBoolean("DEVICE_REGISTER", success);
    	            extraBack.putString("devicename", devicename);
    	            extraBack.putString("username", username);
    	            extraBack.putString("password", password);
    	            broadcastIntent.putExtras(extraBack);
    	            
    	            getBaseContext().sendBroadcast(broadcastIntent);
    	            
	    	  		break;
	    	  		
	    	  	case 5: //"uploadMetadata"
	    	  		devicename = GetDevicename();
	    	  		
	    	  		if (devicename.equals("")){
	    	  			success = false;
	    	  		}
	    	  		else{
	    	  		
		    	  		extrasUpload = extras.getBundle("extras");
		    	  		
		    	  		success = UploadMetadata(username, password, extrasUpload, devicename);
	    	  		}
	    	  		
    	            //---send a broadcast to inform the activity
    	            broadcastIntent.setAction("UPLOAD_METADATA");
    	            
    	            extraBack.putBoolean("UPLOAD_METADATA", success);
    	          //  extraBack.putString("devicename", devicename);
    	            broadcastIntent.putExtras(extraBack);
    	            
    	            getBaseContext().sendBroadcast(broadcastIntent);
    	            
	    	  		break;
	    	  		
	    	  	case 6: //"sendFilelist" - Does not return a broadcastIntent
	    	  		devicename = GetDevicename();
	    	  		if( devicename.equals("")){
	    	  			success = false;
	    	  		}
	    	  		else{
	    	  			boolean onlyNewFiles = false;
	    	  			success = SendFilelistToServer(username, password, devicename, onlyNewFiles);
	    	  		}
	
	    	  		break;
	    	  		
	    	  	case 7: //"uploadThumbnails" - Does not return a broadcastIntent
	    	  		String commit_hash = extras.getString("commit_hash");
	    	  		devicename = GetDevicename();
	    	  		if( devicename.equals("")){
	    	  			success = false;
	    	  		}
	    	  		else{
	    	  			success = UploadThumbnails(username, password, devicename, commit_hash);	    	  			
	    	  		}
	
	    	  		break;
	    	  		
	    	  	case 8: //"uploadEssence"
	    	  	//	String blob_hash = extras.getString("commit_hash");
	    	  		fullpath = extras.getString("fullpath");
	    	  		devicename = GetDevicename();
	    	  		if( devicename.equals("")){
	    	  			success = false;
	    	  		}
	    	  		else{
	    	  			success = UploadEssence(username, password, devicename, fullpath);
	    	  		}
	    	  		
	    	  		//---send a broadcast to inform the activity
                    broadcastIntent.setAction("UPLOAD_ESSENCE");
                    
                    extraBack.putBoolean("UPLOAD_ESSENCE", success);
                    //  extraBack.putString("devicename", devicename);
                    broadcastIntent.putExtras(extraBack);
                    
                    getBaseContext().sendBroadcast(broadcastIntent);
	
	    	  		break;
	    	  		
	    	  		
	    	  	case 9: //"uploadMetadataMultipleFiles"
                    devicename = GetDevicename();
                    
                    if (devicename.equals("")){
                        success = false;
                    }
                    else{
                    
                        extrasUpload = extras.getBundle("extras");
                        
                        success = UploadMetadataMultipleFiles(username, password, extrasUpload, devicename);
                    }
                    
                    //---send a broadcast to inform the activity
                    broadcastIntent.setAction("UPLOAD_METADATA_MULTIPLE_FILES");
                    
                    extraBack.putBoolean("UPLOAD_METADATA_MULTIPLE_FILES", success);
                  //  extraBack.putString("devicename", devicename);
                    broadcastIntent.putExtras(extraBack);
                    
                    getBaseContext().sendBroadcast(broadcastIntent);
                    
                    break;
	    	  		
	    	  	case 10: //"GetMetadata"
                    devicename = GetDevicename();
                    fullpath = extras.getString("fullpath");
                    
                    if (devicename.equals("")){
                        json = "";
                    }
                    else{
                    
                        extrasUpload = extras.getBundle("extras");
                        
                        json = GetMetadata(username, password, devicename, fullpath);
                    }
                    
                    //---send a broadcast to inform the activity
                    broadcastIntent.setAction("GET_METADATA");
                    
                    extraBack.putString("METADATA", json);
                  //  extraBack.putString("devicename", devicename);
                    broadcastIntent.putExtras(extraBack);
                    
                    getBaseContext().sendBroadcast(broadcastIntent);
                    
                    break;
                    
                case 11: //"GetMetadatatypes"

                    extrasUpload = extras.getBundle("extras");
                        
                    json = GetMetadatatypes();
                                        
                    //---send a broadcast to inform the activity
                    broadcastIntent.setAction("GET_METADATATYPES");
                    
                    extraBack.putString("METADATATYPES", json);
                  //  extraBack.putString("devicename", devicename);
                    broadcastIntent.putExtras(extraBack);
                    
                    getBaseContext().sendBroadcast(broadcastIntent);
                    
                    break;
                    
                case 12: //"SaveMetadata"
                    devicename = GetDevicename();
                    fullpath = extras.getString("fullpath");
                    String metadatatype = extras.getString("metadatatype");
                    String metadatavalue = extras.getString("metadatavalue");
                    if (devicename.equals("")){
                        success = false;
                    }
                    else{
                    
                        extrasUpload = extras.getBundle("extras");
                        
                        success = SaveMetadata(username, password, devicename, fullpath, metadatatype, metadatavalue);
                    }
                    
                    //---send a broadcast to inform the activity
                    broadcastIntent.setAction("SAVE_METADATA");
                    
                    extraBack.putBoolean("SAVE_METADATA", success);
                  //  extraBack.putString("devicename", devicename);
                    broadcastIntent.putExtras(extraBack);
                    
                    getBaseContext().sendBroadcast(broadcastIntent);
                    
                    break;
                    
                case 13: //"SendToExternalService"

                    fullpath = extras.getString("fullpath");
                    devicename = GetDevicename();
                    String service = extras.getString("service");
                    String caption = "";
                    if( extras.containsKey("caption")){
                        caption = extras.getString("caption");
                    }
                    if( devicename.equals("")){
                        success = false;
                    }
                    else{
                        success = SendToExternalService(username, password, devicename, fullpath, service, caption);
                    }
                    
                    //---send a broadcast to inform the activity
                    broadcastIntent.setAction("SEND_TO_EXTERNAL_SERVICE");
                    
                    extraBack.putBoolean("SEND_TO_EXTERNAL_SERVICE", success);
                    extraBack.putString("service", service);
                    broadcastIntent.putExtras(extraBack);
                    
                    getBaseContext().sendBroadcast(broadcastIntent);
                    
                    break;
                    
	    	  }
	    	  
	    	//  Toast.makeText(this, extras.getString("URL"), Toast.LENGTH_SHORT).show();
	      }
	  }

	  private boolean SendToExternalService(String username, String password, String devicename, String fullpath, String service, String caption){
          
          try{
              String file_uri = "file_uri=/user/" + username + "/device/" + devicename + "/files" + fullpath ;
              String path = "/user/" + username + "/";
              if( service.equals("Facebook") || service.equals("Flickr") ){
                  path += service.toLowerCase() + "PublishPhoto";
              }
              else if( service.equals("Dropbox") ){
                  path += "dropboxUpload";
                  file_uri += "&dropbox_path=visualrest_upload";
              }
              else{
                  return false;
              }
              if( !caption.equals("")){
                  file_uri += "&caption="+caption;
              }
              
              URL url = new URL(host + path + "?"+AuthParams(username, password, path)+"&"+file_uri);
              HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
              httpCon.setDoOutput(true);
              httpCon.setRequestMethod("PUT");
              httpCon.connect();
              Integer respo = httpCon.getResponseCode();

              if( respo.equals(200)){
                  return true;
              }
              else if( respo.equals(201)){
                  return true;
              }
              else{
                  return false;
              }
                
          }
          catch (Exception e){
              Log.e(this.getClass().getName(), e.toString());
              return false;
          }
      }
	  
	  
	  private boolean SaveMetadata(String username, String password, String devicename, String fullpath, String metadatatype, String metadatavalue){
          
          try{
              String path = "/user/" + username + "/device/" + devicename + "/metadata" + fullpath;
              URL url = new URL(host + path + "?"+AuthParams(username, password, path)+"&metadata_type="+metadatatype+"&metadata_value="+metadatavalue);
              HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
              httpCon.setDoOutput(true);
              httpCon.setRequestMethod("POST");
              httpCon.connect();
              Integer respo = httpCon.getResponseCode();

              if( respo.equals(200)){
                  return true;
              }
              else if( respo.equals(201)){
                  return true;
              }
              else{
                  return false;
              }
                
          }
          catch (Exception e){
              Log.e(this.getClass().getName(), e.toString());
              return false;
          }
      }
	  
	  private String GetMetadata(String username, String password, String devicename, String fullpath){
	      
	      try{
	          String path = "/user/" + username + "/device/" + devicename + "/metadatas" + fullpath;
	          URL url = new URL(host + path + "?"+AuthParams(username, password, path)+"&qoption[format]=json");
	          HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
	          httpCon.connect();
	          Integer respo = httpCon.getResponseCode();

	          if( respo.equals(200)){
	              InputStream in = httpCon.getInputStream();
	              return convertStreamToString(in);
	          }
	          else{
	              return "";
	          }
	            
	      }
	      catch (Exception e){
	          Log.e(this.getClass().getName(), e.toString());
	          return "";
	      }
	  }
	  
	  private String GetMetadatatypes(){
	      
	      try{
              String path = "/metadatatypes";
              URL url = new URL(host + path + ".json");
              HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
              httpCon.connect();
              Integer respo = httpCon.getResponseCode();

              if( respo.equals(200)){
                  InputStream in = httpCon.getInputStream();
                  return convertStreamToString(in);
              }
              else{
                  return "";
              }
                
          }
          catch (Exception e){
              Log.e(this.getClass().getName(), e.toString());
              return "";
          }
      }
	  
	  
	   public static String convertStreamToString(InputStream is) throws Exception {
	          BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	          StringBuilder sb = new StringBuilder();
	          String line = null;

	          while ((line = reader.readLine()) != null) {
	              sb.append(line);
	          }

	          is.close();

	          return sb.toString();
	      }
	  
	  
	  private boolean UploadEssence(String username, String password, String devicename, String fullpath){
		  
		  try{
			  
			  
			  // Open database adapter
			  DBAdapter db = new DBAdapter(this);
		  
			  db.open();
	        
			  String name = fullpath.substring(fullpath.lastIndexOf("/")+1 );
			  String path = fullpath.substring(0, fullpath.lastIndexOf("/")+1);
			  
			  Cursor kursori = db.getFileWithNameAndPath(name, path);
			  
			  if( kursori.moveToFirst() ){			
				  
				  String blob_hash = kursori.getString(6);
		  
				  String status = "status=uploading_file%3A%20%20"+fullpath+"%0Auploading_file_hash%3A%20%20"+blob_hash;
				  URL url = new URL(host + "/user/" + username + "/device/" + devicename + "/online?"+AuthParams(username, password, "/user/"+username+"/device/"+devicename+"/online")+"&"+status );
				  HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
				  httpCon.setDoOutput(true);
				  httpCon.setRequestMethod("POST");
				  httpCon.connect();
				  Integer respo = httpCon.getResponseCode();
					  
				  if ( !respo.equals(201) ){
					  return false;
				  }	
				  
				  File file = new File(fullpath);
		    		
				  byte[] data = FileUtils.readFileToByteArray(file);
		          		
				  HttpClient httpClient = new DefaultHttpClient();
				  HttpPut putRequest = new HttpPut(
		    				host+"/user/"+username+"/device/"+devicename+"/files"+fullpath+"?"+AuthParams(username, password, "/user/"+username+"/device/"+devicename+"/files"+fullpath)+"&blob_hash="+blob_hash);
				  ByteArrayBody bab = new ByteArrayBody(data, name);
	
				  MultipartEntity reqEntity = new MultipartEntity(
						  HttpMultipartMode.BROWSER_COMPATIBLE);
				  reqEntity.addPart("upload", bab);
			           
				  putRequest.setEntity(reqEntity);
				  HttpResponse response = httpClient.execute(putRequest);
		    
				  if( response.getStatusLine().getStatusCode() != 200 ){
					  db.close();
					  return false;
				  }
			  }
			  db.close();
			  
	    	}
	    	catch (Exception e){
	    		Log.e(this.getClass().getName(), e.toString());
	    		return false;
	    	}

		  
		  
		  return true;
	  }
	  
	  
    private boolean UploadThumbnails(String username, String password, String devicename, String commit_hash){
		  
			// Open database adapter
			DBAdapter db = new DBAdapter(this);
			
			db.open();
	        
	        Cursor kursori = db.getAllFilesWithCommitHash(commit_hash);
	        
			Integer id;
			String name;
			String filedate;
			Integer size;
			String filetype;
			String path;
			String blob_hash;
			String status;
			Integer thumb_uploaded;
			
			// Create whole filelist, including the new added file
			if (kursori.moveToFirst())
	        {
				do {
					try{
					
						id = kursori.getInt(0);
						name = kursori.getString(1);
						filedate = kursori.getString(2);
						size = kursori.getInt(3);
						filetype = kursori.getString(4);
						path = kursori.getString(5);
						blob_hash = kursori.getString(6);
						status = kursori.getString(7);
						thumb_uploaded = kursori.getInt(8);
			    				
						if( thumb_uploaded == 0 )
						{
						
				    		// Crete thumbnail
				    		byte[] imageData = null;
	
	
				    		final int THUMBNAIL_SIZE = 128;
	
				    		String filename = path+name;
				    		
				    		File file = new File(filename);
				    		
				    		FileInputStream fis = new FileInputStream(file);
				    		Bitmap imageBitmap = BitmapFactory.decodeStream(fis);
	
				    		Float width = new Float(imageBitmap.getWidth());
				    		Float height = new Float(imageBitmap.getHeight());
				    		Float ratio;
				    		if( width > height )
				    		{
				    			ratio = height/width;			    			
				    		}
				    		else{
				    			ratio = width/height;
				    		}
				    		
				    		imageBitmap = Bitmap.createScaledBitmap(imageBitmap, (int)(THUMBNAIL_SIZE * ratio), THUMBNAIL_SIZE, true);
	
				    		ByteArrayOutputStream baos = new ByteArrayOutputStream();
				    		imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
				    		imageData = baos.toByteArray();
	
				    		// Send thumbnail to server
				    		HttpClient httpClient = new DefaultHttpClient();
				    		HttpPut putRequest = new HttpPut(
				    				host+"/user/"+username+"/device/"+devicename+"/files"+filename+"?"+AuthParams(username, password, "/user/"+username+"/device/"+devicename+"/files"+filename)+"&thumbnail=true&blob_hash="+blob_hash);
				    		ByteArrayBody bab = new ByteArrayBody(imageData, filename);
	
				    		MultipartEntity reqEntity = new MultipartEntity(
				    				HttpMultipartMode.BROWSER_COMPATIBLE);
				    		reqEntity.addPart("upload", bab);
					           
				    		putRequest.setEntity(reqEntity);
				    		HttpResponse response = httpClient.execute(putRequest);
				    		
				    		if( response.getStatusLine().getStatusCode() == 200 ){
				    			// Set the thumbnail uploaded
				    			db.updateFile(id, name, filedate, size, filetype, path, blob_hash, status, 1, commit_hash);
				    		}
			    		
						}
			    		
			    		
			    		
					} catch (Exception e){
						Log.e(this.getClass().getName(), e.toString());
						return false;	
				   }
	            } while (kursori.moveToNext());
				db.close();
	        }
	        
		  
		  return true;
	  }
	  
	  

	  
	  
	  
    private boolean UploadMetadata(String username, String password,  Bundle extrasUpload, String devicename){
		
	   try{ 	
		   // Add the new file to SQLite database
		   
		   Uri uri;
		   String fullpath;
		   if( extrasUpload.containsKey("fullpath")){
			   fullpath = extrasUpload.getString("fullpath");
			   File file = new File(fullpath);
			   
			   uri = Uri.fromFile(file);
		   }
		   else{
			   uri = (Uri) extrasUpload.getParcelable(Intent.EXTRA_STREAM);			   
			   fullpath = getRealPathFromURI(uri);
		   }
		   
	
			
			ContentResolver cr = getContentResolver();
			
	  		InputStream is = cr.openInputStream(uri);
	  		// Get binary bytes for encode
	  		byte[] data = IOUtils.toByteArray(is);
			
	  		// Get all parameters that are stored to the sql database
	  		String name = fullpath.substring(fullpath.lastIndexOf("/")+1, fullpath.length());
			
			File file = new File(fullpath);
			Date date = new Date(file.lastModified());
			SimpleDateFormat formatter = new SimpleDateFormat("hh:mm:ss yyyy-MM-dd"); 
			String filedate = formatter.format(date);
			
			Integer size = (int) file.length();
			String filetype = cr.getType(uri);
			if( filetype == null ){
				filetype = "image/jpeg";
			}
			String path = fullpath.substring(0, fullpath.lastIndexOf("/"))+"/";
			
			byte[] arrayBeginning = ("blob " + size.toString() + "\0").getBytes();
			byte[] array = ConcatByteArrays(arrayBeginning, data);
			String blob_hash = Blob_hash(array);

			String status = "created";
		
			//this.deleteDatabase("VisualrestDB");
			
			// Open database adapter
			DBAdapter db = new DBAdapter(this);
			
			 //---add a new file---
	        db.open();
	        long id = db.insertFile(name, filedate, size, filetype, path, blob_hash, status, 0, "");
	        db.close();

			if( id < 0 ){
				// Adding was not successfull, if the id is -1
				return false;
			}
		
			// Send the filelist to VisualREST
			boolean onlyNewFiles = true;
			SendFilelistToServer(username, password, devicename, onlyNewFiles);
			

			
			  
	   } catch (Exception e){
			Log.e(this.getClass().getName(), e.toString());
			return false;	
	   }
		  
		  
		  
		  
		return true;
    }
	  
    
    
    
    
    private boolean UploadMetadataMultipleFiles(String username, String password,  Bundle extrasUpload, String devicename){
        
        try{     
            // Add the new file to SQLite database
            
            Uri uri;
            String[] fullpaths;
            if( extrasUpload.containsKey("fullpaths")){
                fullpaths = extrasUpload.getStringArray("fullpaths"); 
            }
            else{
                return false;
            }
            
            // Open database adapter
            DBAdapter db = new DBAdapter(this);
            
             //---add a new file---
            db.open();
            
            for(String fullpath : fullpaths)
            {
                File file = new File(fullpath);
                
                uri = Uri.fromFile(file);
                
                 
                ContentResolver cr = getContentResolver();
                 
                InputStream is = cr.openInputStream(uri);
                // Get binary bytes for encode
                byte[] data = IOUtils.toByteArray(is);
                 
                // Get all parameters that are stored to the sql database
                String name = fullpath.substring(fullpath.lastIndexOf("/")+1, fullpath.length());
                 
                //File file = new File(fullpath);
                Date date = new Date(file.lastModified());
                SimpleDateFormat formatter = new SimpleDateFormat("hh:mm:ss yyyy-MM-dd"); 
                String filedate = formatter.format(date);
                 
                Integer size = (int) file.length();
                String filetype = cr.getType(uri);
                if( filetype == null ){
                    filetype = "image/jpeg";
                }
                String path = fullpath.substring(0, fullpath.lastIndexOf("/"))+"/";
                 
                byte[] arrayBeginning = ("blob " + size.toString() + "\0").getBytes();
                byte[] array = ConcatByteArrays(arrayBeginning, data);
                String blob_hash = Blob_hash(array);
    
                String status = "created";
             
                //this.deleteDatabase("VisualrestDB");
                 
                 
                long id = db.insertFile(name, filedate, size, filetype, path, blob_hash, status, 0, "");
    
                if( id < 0 ){
                    // Adding was not successfull, if the id is -1
                    db.close();
                    removeFullPathsFromDatabase(fullpaths);
                    return false;
                }
            }
        
            db.close();
            
            // Send the filelist to VisualREST
            boolean onlyNewFiles = true;
            if( SendFilelistToServer(username, password, devicename, onlyNewFiles) ){
                return true;
            }
            else{
                // adding files to VisualREST was not successful, remove the files from local container also
                removeFullPathsFromDatabase(fullpaths);
                return false;
            }
             
     
        } catch (Exception e){
             Log.e(this.getClass().getName(), e.toString());
             return false;   
        }
     }
        
    
    private boolean SendFilelistToServer(String username, String password, String devicename, boolean onlyNewFiles){
    	
    	try{
			// Open database adapter
			DBAdapter db = new DBAdapter(this);
			
			// Get all already added files
			db.open();
			
			Cursor kursori;
			if( onlyNewFiles){
				// All files that have not yet been updated to the server
				kursori = db.getAllFilesWithCommitHash("");
			}
			else{
				kursori = db.getAllFilesWithCommitHash(GetPrevCommitHash());				
			}
			
			
			String commit_hash = "commit \0";
			String list = "--- ";
			
			// Create whole filelist, including the new added file
			if (kursori.moveToFirst())
	        {
				do {
					list += "\n" + kursori.getString(5) + kursori.getString(1) + ":";
					list += "\n  name: " + kursori.getString(1);
					list += "\n  filedate: " + kursori.getString(2);
					list += "\n  size: " + kursori.getString(3);
					list += "\n  filetype: " + kursori.getString(4);
					list += "\n  path: " + kursori.getString(5);
					list += "\n  blob_hash: " + kursori.getString(6);
					commit_hash += kursori.getString(6);
					list += "\n  status: " + kursori.getString(7);
	            } while (kursori.moveToNext());
	        }
			else
			{
				db.close();
				return false;
			}

			commit_hash = Blob_hash(commit_hash.getBytes());
			String prevCommitHash = GetPrevCommitHash();
    		  
			HttpClient httpclient = new DefaultHttpClient();
	        HttpPost httppost = new HttpPost(host + "/user/"+username+"/device/"+devicename+"/files");
	        
	        // All parameters that will be sent in the request body
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2); 
	        nameValuePairs.add(new BasicNameValuePair("contains", list));
	        nameValuePairs.add(new BasicNameValuePair("commit_hash",commit_hash));
	        nameValuePairs.add(new BasicNameValuePair("password", password));
	        nameValuePairs.add(new BasicNameValuePair("i_am_client", "true"));
	        nameValuePairs.add(new BasicNameValuePair("auth_username", username ));
	        Long tsLong = System.currentTimeMillis()/1000;
            String ts = tsLong.toString();
	        nameValuePairs.add(new BasicNameValuePair("auth_timestamp", ts));
	        nameValuePairs.add(new BasicNameValuePair("auth_hash",  CalculateAuthHash(ts, password, "/user/"+username+"/device/"+devicename+"/files")));
	        if ( !prevCommitHash.equals( "" ) ){
	            nameValuePairs.add(new BasicNameValuePair("prev_commit_hash", prevCommitHash));
	        }
	        
	        
	        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));  

	        HttpResponse response;
	        response = httpclient.execute(httppost);
	        
	        // Check response status code
    		if ( response.getStatusLine().getStatusCode() == 202 ){
    			// If returns 202 
    		    // - The server started processing the filelist and will send an XMPP message notifying how it went
    			
    			if(onlyNewFiles == true){
    				// Mark commit_hash to the files
    				if (kursori.moveToFirst())
    		        {
    					do {
    						db.updateFile(kursori.getInt(0), kursori.getString(1), kursori.getString(2), kursori.getInt(3), kursori.getString(4),
    								kursori.getString(5), kursori.getString(6), kursori.getString(7), kursori.getInt(8), commit_hash);
    		            } while (kursori.moveToNext());
    		        }
    			}
    			
    			SetPrevCommitHash(commit_hash);
    			
    			db.close();
    			return true;		    	  
    		}
    		else{
    			db.close();
    			return false;
    		}
    		
    		
    	}catch (Exception e){
			Log.e(this.getClass().getName(), e.toString());
			return false;	
	   }
    }
    
    
    private byte[] ConcatByteArrays(byte[] A, byte[] B) {
    	byte[] C= new byte[A.length+B.length];
    	System.arraycopy(A, 0, C, 0, A.length);
    	System.arraycopy(B, 0, C, A.length, B.length);

    	return C;
    }
  
    
    
    // Returns name of the device, if it has been registered to VisualREST
    private String GetDevicename(){

    	SharedPreferences prefs = getSharedPreferences("XMPPSettings", MODE_PRIVATE);
    	if (prefs.contains("devicename")){
    		String devicename = prefs.getString("devicename", "");
    		
    		return devicename;
  	      	}	
    	else{
    		return "";  	    	  
    	}
    }
    
    private String GetPrevCommitHash(){
    	
    	SharedPreferences prefs = getSharedPreferences("XMPPSettings", MODE_PRIVATE);
    	if (prefs.contains("prevcommithash")){
    		return prefs.getString("prevcommithash", "");		
    	}	
    	else{
    		return "";  	    	  
    	}
    	
    }
    
    private void SetPrevCommitHash(String commit_hash){
    	SharedPreferences prefs = getSharedPreferences("XMPPSettings", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        //---save the values in the EditText view to preferences---
        editor.putString("prevcommithash", commit_hash);
        //---saves the values---
        editor.commit();
    }
	  
	  
    private boolean DeviceRegister(String username, String password, String devicename){
    	try{
    		if( username == null || password == null || devicename == null ) {
    			return false;
    		}
    		URL url = new URL(host + "/user/" + username + "/device/" + devicename + "?dev_type=android&password=" + password );
    		HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
    		httpCon.setDoOutput(true);
    		httpCon.setRequestMethod("PUT");
    		httpCon.connect();
    		Integer respo = httpCon.getResponseCode();
			  
    		if ( respo.equals(201) ){
    			return true;		    	  
    		}
    		else{
    			return false;
    		}
    	}
    	catch (Exception e){
    		Log.e(this.getClass().getName(), e.toString());
    		return false;
    	}
		  
    }
	  
	  
    private boolean UploadPhoto(String username, String password, Bundle extrasUpload){
		  
    	try{
			  
    		String devName = "android_container";
               
			  
    		// Create virtual container named "android_container" (it doesn't matter if it already exists)
    		URL url = new URL(host + "/user/"+username+"/device/"+devName+"?"+AuthParams(username, password, "/user/"+username+"/device/"+devName)+"&dev_type=virtual_container&password="+password);
    		HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
    		httpCon.setDoOutput(true);
    		httpCon.setRequestMethod("PUT");
    		httpCon.getInputStream();
          	
          	
          	
    		// Get resource path from intent callee
    		Uri uri = (Uri) extrasUpload.getParcelable(Intent.EXTRA_STREAM);

    		String filename = getRealPathFromURI(uri);
              
    		// Query gallery for camera picture via
    		// Android ContentResolver interface
    		ContentResolver cr = getContentResolver();
    		InputStream is = cr.openInputStream(uri);
    		// Get binary bytes for encode
    		byte[] data = IOUtils.toByteArray(is);
          		
    		HttpClient httpClient = new DefaultHttpClient();
    		HttpPut putRequest = new HttpPut(
    				host+"/user/"+username+"/device/"+devName+"/files"+filename+"?"+AuthParams(username, password, "/user/"+username+"/device/"+devName+"/files"+filename));
    		ByteArrayBody bab = new ByteArrayBody(data, filename);

    		MultipartEntity reqEntity = new MultipartEntity(
    				HttpMultipartMode.BROWSER_COMPATIBLE);
    		reqEntity.addPart("upload", bab);
	           
    		putRequest.setEntity(reqEntity);
    		HttpResponse response = httpClient.execute(putRequest);
    		
    		if( response.getStatusLine().getStatusCode() == 200 ){
    			return true;
    		}

    	}
    	catch (Exception e){
    		return false;
    	}
		return false;
    }
	  
	  
    public String getRealPathFromURI(Uri contentUri) {
    	String[] proj = { MediaStore.Images.Media.DATA };
    	Cursor cursor = getBaseContext().getContentResolver().query(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }
	    
	    
	
    private boolean AuthenticateUser(String username, String password){
		  
    	try{
			  
    		URL url = new URL(host + "/authenticateUser?"+AuthParams(username, password, "/authenticateUser"));
    		HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
    		//httpCon.setDoOutput(true);
    		//httpCon.setRequestMethod("POST");
    		httpCon.connect();
    		Integer respo = httpCon.getResponseCode();
			  
			 
    		if ( respo.equals(200) ){
    			return true;
    		}
    		else{
    			return false;
    		}
			  
			  
    	}
    	catch (Exception e){
    		return false;
    	}

    }

	  
	  
	  
	  
	  
    private String AuthParams(String username, String password, String path){
		  
    	Long tsLong = System.currentTimeMillis()/1000;
    	String ts = tsLong.toString();
		  
    	String params = "i_am_client=true&auth_username="+username+"&auth_timestamp="+ts+"&auth_hash=" 
    			+ CalculateAuthHash(ts, password, path);;
				  
    			return params;
    }

	
	
	
    private String CalculateAuthHash(String timestamp, String password, String path){
    	
    	String resp = "";
    	
    	try{
    		resp = SHA1(timestamp + password + path);
    	}catch (Exception e){
    		
    	}
    	return resp;
    }
    
    private static String Blob_hash(byte[] array) throws NoSuchAlgorithmException, UnsupportedEncodingException  { 
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] sha1hash = new byte[40];
        md.update(array, 0, array.length);
        sha1hash = md.digest();
        return ConvertToHex(sha1hash);
    }
    

    private static String SHA1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException  { 
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] sha1hash = new byte[40];
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        sha1hash = md.digest();
        return ConvertToHex(sha1hash);
    } 
	
    
    private static String ConvertToHex(byte[] data) { 
    	StringBuffer buf = new StringBuffer();
    	for (int i = 0; i < data.length; i++) { 
    		int halfbyte = (data[i] >>> 4) & 0x0F;
    		int two_halfs = 0;
    		do { 
    			if ((0 <= halfbyte) && (halfbyte <= 9)) 
    				buf.append((char) ('0' + halfbyte));
    			else 
    				buf.append((char) ('a' + (halfbyte - 10)));
    			halfbyte = data[i] & 0x0F;
    		} while(two_halfs++ < 1);
    	} 
    	return buf.toString();
    }
    
    
    private void removeFullPathsFromDatabase(String[] fullpaths){

        try{
            // Open database adapter
            DBAdapter db = new DBAdapter(this);
            db.open();            

            for(String fullpath : fullpaths)
            {
                try{
                    String name = fullpath.substring(fullpath.lastIndexOf("/")+1, fullpath.length());
                    String path = fullpath.substring(0, fullpath.lastIndexOf("/"))+"/";
                    db.deleteRowNamePath(name, path); 
                    
                }catch (Exception e){
                    Log.e(this.getClass().getName(), e.toString());
                    db.close();
                    db = new DBAdapter(this);
                    db.open();
                }
            }
            db.close();
        }catch (Exception e){
            Log.e(this.getClass().getName(), e.toString());
        }
    }
    
    
    
    
  
    
}