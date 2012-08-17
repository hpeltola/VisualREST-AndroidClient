package tut.heikki.visualrestclient;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class UpdateWidgetService extends Service {

    public static final String AUTOMATIC = "automatic";
    public static final String XMPP = "xmpp";
    public static final String UPDATE = "update";

    @Override
    public void onStart(Intent intent, int startId) {

        // Get widget manager
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this
                .getApplicationContext());

        // Get all widget id:s from the intent
        int[] allWidgetIds = intent
                .getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
        
        
        // See if widget is pressed
        String command = intent.getAction();
        if( command != null ){
            // turnOn parameter determines, whether we are turning the service on/off
            boolean turnOn = intent.getBooleanExtra("turnOn", false);
            
            // Automatic photo upload is toggled
            if( command.equals(AUTOMATIC)){
                if( turnOn ){
                    Log.w("AUTOMATIC", "turn on");  
                    // Start NewFileObserver service
                    Intent fileObserverIntent = new Intent();
                    fileObserverIntent.setClassName("tut.heikki.visualrestclient", "tut.heikki.visualrestclient.NewFileObserverService");
                    getBaseContext().startService(fileObserverIntent);
                }
                else{
                    Log.w("AUTOMATIC", "turn off");
                    // Stop NewFileObserver service
                    Intent xmppintent = new Intent(getBaseContext(), NewFileObserverService.class);
                    getBaseContext().stopService(xmppintent);
                }
            }
            
            // XMPP service is turned on/off
            if( command.equals(XMPP)){
                if( turnOn ){
                    Log.w("XMPP", "turn on");
                    // Start XMPP service
                    Intent xmppIntent = new Intent();
                    xmppIntent.setClassName("tut.heikki.visualrestclient", "tut.heikki.visualrestclient.XMPPService");
                    getBaseContext().startService(xmppIntent);
                }
                else{
                    Log.w("XMPP", "turn off");
                    // Stop XMPP service
                    Intent xmppintent = new Intent(getBaseContext(), XMPPService.class);
                    getBaseContext().stopService(xmppintent);
                }
            }
        }
        
       
        
        // Go through all instances of widget
        for (int widgetId : allWidgetIds) {

            try{
            
                RemoteViews remoteViews = new RemoteViews(this.getApplicationContext().getPackageName(), R.layout.widget_layout);
                
                // Update images on the widget about running services
                // and prepare pending intents. The pending intents are
                // processed when widget is pressed.
                if( IsXMPPServiceRunning() ){
                    Log.w("tattaraa", "xmpp running");
                    remoteViews.setImageViewResource(R.id.widgetXMPPIcon, R.drawable.online_32);    
                    remoteViews.setOnClickPendingIntent(R.id.widget_xmpp,VrestWidgetProvider.makeControlPendingIntent(getApplicationContext(),XMPP,allWidgetIds, false));
                }
                else{
                    Log.w("tattaraa", "xmpp not running");
                    remoteViews.setImageViewResource(R.id.widgetXMPPIcon, R.drawable.offline_32);
                    remoteViews.setOnClickPendingIntent(R.id.widget_xmpp,VrestWidgetProvider.makeControlPendingIntent(getApplicationContext(),XMPP,allWidgetIds, true));
                }
                
                if( IsAutomaticUploadServiceRunning() ){
                    remoteViews.setImageViewResource(R.id.widgetUploadIcon, R.drawable.online_32);  
                    remoteViews.setOnClickPendingIntent(R.id.widget_upload,VrestWidgetProvider.makeControlPendingIntent(getApplicationContext(),AUTOMATIC,allWidgetIds,false));
                }
                else{
                    remoteViews.setImageViewResource(R.id.widgetUploadIcon, R.drawable.offline_32);
                    remoteViews.setOnClickPendingIntent(R.id.widget_upload,VrestWidgetProvider.makeControlPendingIntent(getApplicationContext(),AUTOMATIC,allWidgetIds,true));
                }

                appWidgetManager.updateAppWidget(widgetId, remoteViews);
                
            } catch (Exception e){
                Log.e(this.getClass().getName(), e.toString());
                
           }

        }
        stopSelf();

        super.onStart(intent, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    

    
    
    private boolean IsXMPPServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("tut.heikki.visualrestclient.XMPPService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private boolean IsAutomaticUploadServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("tut.heikki.visualrestclient.NewFileObserverService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    
    
    
}
