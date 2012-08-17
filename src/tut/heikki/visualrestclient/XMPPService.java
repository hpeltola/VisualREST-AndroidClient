package tut.heikki.visualrestclient;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class XMPPService extends Service {

	private static final String TAG = "XMPPService";
	private static final String DOMAIN = "visualrest.cs.tut.fi";
	private String username;
	private String password;

	private XMPPConnection mXmppConnection;
	
	
    private NotificationManager mNM;
    
	private String prefName = "XMPPSettings";

    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int NOTIFICATION = R.string.XMPPServiceStarted;

    @Override
    public void onCreate() {
    	
        try{
        	new Thread(new Runnable() {
        		public void run() {
        		      
    
    	    SharedPreferences prefs = getSharedPreferences(prefName, MODE_PRIVATE);
    	    username = prefs.getString("username", "");
    	    password = prefs.getString("password", "");
    	    
    	    if ( username == null || password == null){
    	//    	this.stopSelf();
    	    	return;
    	    }
        	
            mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    
            // Display a notification about us starting.  We put an icon in the status bar.
            showNotification();
            
            
            mXmppConnection = new XMPPConnection(DOMAIN);
            
    		try {
    			mXmppConnection.connect();
    			mXmppConnection.login(username, password);
    		}
    		catch (final XMPPException e) {
    			Log.e(TAG, "Could not connect to Xmpp server.", e);
    		//	Toast.makeText(this, "NO CONNECTION to XMPPP", Toast.LENGTH_SHORT).show();
    		//	this.stopSelf();
    			return;
    		}
    
    		if (!mXmppConnection.isConnected()) {
    			Log.e(TAG, "Could not connect to the Xmpp server.");
    		//	Toast.makeText(this, "CONNECTion... not", Toast.LENGTH_SHORT).show();
    			return;
    		}
    		Log.i(TAG, "Yey! We're connected to the Xmpp server!");
    		mXmppConnection.getChatManager().addChatListener(new ChatManagerListener() {
    
    			@Override
    			public void chatCreated(final Chat chat, final boolean createdLocally) {
    				if (!createdLocally) {
    					chat.addMessageListener(new MyMessageListener());
    				}
    			}
    		});
    		
    		
        		}
        	}).start();
        	
        } catch (Exception e){
            Log.e(this.getClass().getName(), e.toString());
        }
        
    }

	private class MyMessageListener implements MessageListener {

		@Override
		public void processMessage(final Chat chat, final Message message) {
			String msg = message.getBody();
			//Log.i(TAG, "Xmpp message received: '" + msg + "' on thread: " + getThreadSignature());
			
			if( msg.equals("list"))
			{
				// list
				//Log.i(TAG, "Xmpp 'list' -command" + getThreadSignature());
				
				// Send an intent to HTTPRequest, asking to send filelist to VisualREST server
				if( SendFilelist() )
				{
					Log.i(TAG, "... 'LIST' sent OK" );//+ getThreadSignature());
				}
				else {
					Log.i(TAG, "... 'LIST' Error" );//+ getThreadSignature());
				}
			}
			else if ( msg.startsWith("upload ")){
				//upload <tiedostopolku> <commit hash>
				// example: upload /harald.gif d8f7f710ee58a8590c271f059406293d63112a14
				String[] tmp = msg.split(" ");
				if (tmp[0].equals("upload") && tmp.length == 3 ){
					
					//Log.i(TAG, "Xmpp 'upload' -command" + getThreadSignature());
					
					// Send an intent to HTTPRequest, asking to send filelist to VisualREST server
					if( UploadEssence(tmp[1], tmp[2]) )
					{
						Log.i(TAG, "... 'UPLOAD' OK");// + getThreadSignature());
					}
					else{
						Log.i(TAG, "... 'UPLOAD' Error");// + getThreadSignature());
					}				
					
				}
			}
			else if ( msg.startsWith("thumbs ") && msg.length() == 47 ){ 
				
				// thumbs <commit hash>
				// example: thumbs 8b10da93353e0e27ef9bdb802f835c92ca423728

				String[] tmp = msg.split(" ");
				if (tmp[0].equals("thumbs") && tmp.length == 2 ){
				
					if( UploadThumbs(tmp[1]) )
					{
						Log.i(TAG, "... 'THUMBS' OK");// + getThreadSignature());
					}
					else{
						Log.i(TAG, "... 'THUMBS' Error" );//+ getThreadSignature());
					}				
				
				}
	
				
			}
			else if ( msg.startsWith("parse ")){
				// parse [successful <commit hash> | error]
				// parse successful 8b10da93353e0e27ef9bdb802f835c92ca423728
				if( msg.startsWith("parse successful ")){
					Log.i(TAG, "... 'PARSE' OK" );// + msg + getThreadSignature());
				}
				else if( msg.startsWith("parse error")){
					Log.i(TAG, "... 'PARSE' Error" );//+ getThreadSignature());
				}
				

			}
			
		}
	}
    
	public boolean UploadThumbs(String commit_hash){
		try{
			
			SharedPreferences prefs = getSharedPreferences("AccountSettings", MODE_PRIVATE);
			if( !prefs.contains("username") || !prefs.contains("password")){
				return false;
			}
			String username = prefs.getString("username", "");
			String password = prefs.getString("password", "");
						
			Intent intent = new Intent(getBaseContext(), HTTPRequestService.class);
	        Bundle extras = new Bundle();
	        extras.putInt("action", 7); //"UploadThumbs"
	        extras.putString("username", username);
	        extras.putString("password", password);
	        extras.putString("commit_hash", commit_hash);
	        
	        intent.putExtras(extras);

	        startService(intent);
			            		
		} catch (Exception e){
			 Log.e(this.getClass().getName(), e.toString());
			 return false;
		}
		return true;
	}
	
	public boolean UploadEssence(String fullpath, String commit_hash){
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
	        extras.putString("commit_hash", commit_hash);
	        extras.putString("fullpath", fullpath);
	        
	        intent.putExtras(extras);

	        startService(intent);
			            		
		} catch (Exception e){
			 Log.e(this.getClass().getName(), e.toString());
			 return false;
		}
		return true;
	}
	
	public boolean SendFilelist(){
		try{
			
			SharedPreferences prefs = getSharedPreferences("AccountSettings", MODE_PRIVATE);
			if( !prefs.contains("username") || !prefs.contains("password")){
				return false;
			}
			String username = prefs.getString("username", "");
			String password = prefs.getString("password", "");
			
			Intent intent = new Intent(getBaseContext(), HTTPRequestService.class);
	        Bundle extras = new Bundle();
	        extras.putInt("action", 6); //"sendFilelist"
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
	
	public static String getThreadSignature() {
		final Thread t = Thread.currentThread();
		return new StringBuilder(t.getName()).append("[id=").append(t.getId()).append(", priority=")
				.append(t.getPriority()).append("]").toString();
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
	        // Cancel the persistent notification.
	        mNM.cancel(NOTIFICATION);
	
	        // Disconnect
	        mXmppConnection.disconnect();
	        
	        // Tell the user we stopped.
	        //Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
    	} catch (Exception e){
			Log.e(this.getClass().getName(), e.toString());
    	}
    }

    @Override
    public IBinder onBind(Intent intent) {
    	return null;
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
 //   private final IBinder mBinder = new LocalBinder();

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
	    SharedPreferences prefs = getSharedPreferences(prefName, MODE_PRIVATE);
    	
        CharSequence text = prefs.getString("username", "");

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.ic_launcher, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, SettingsActivity.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.XMPPServiceStarted),
                       text, contentIntent);

        // Send the notification.
        mNM.notify(NOTIFICATION, notification);
    }

	
}
