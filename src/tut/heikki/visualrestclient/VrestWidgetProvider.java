package tut.heikki.visualrestclient;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class VrestWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {

        // Get all Ids of this widget
        ComponentName thisWidget = new ComponentName(context,
                VrestWidgetProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
                
        // Build the intent to call the service
        Intent intent = new Intent(context.getApplicationContext(),
                UpdateWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);

        // Update the widgets via the service
        context.startService(intent);
    }

    // This function is initiated from UpdateWidgetService
    //  - Creates pending intent, that will be initiated when the widget is pressed
    public static PendingIntent makeControlPendingIntent(Context context, String command, int[] allWidgetIds, boolean turnOn) {
        try{
            // Create intent
            Intent active = new Intent(context,UpdateWidgetService.class);
            // Command states what part of the widget is pressed (automatic/xmpp)
            active.setAction(command);

            // If turnOn == true the service will be turned on
            // if turnOn == false the service will be turned off
            active.putExtra("turnOn", turnOn);
            active.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);
            
            // Return pending intent
            return(PendingIntent.getService(context, 1, active, PendingIntent.FLAG_UPDATE_CURRENT));
        
        } catch (Exception e){
            Log.e(context.getClass().getName(), e.toString());
            return null;
        }
    }
    
}
