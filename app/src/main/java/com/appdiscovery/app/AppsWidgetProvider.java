package com.appdiscovery.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.widget.RemoteViews;

import com.appdiscovery.app.services.AppsWidgetService;
import com.appdiscovery.app.services.DiscoverApp;
import com.appdiscovery.app.services.LocationWatcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

//TODO: refactor
public class AppsWidgetProvider extends BroadcastReceiver {
    Context mContext;
    public static WebApp[] webapps = new WebApp[0];

    private void setListView(WebApp[] webapps) {
        AppsWidgetProvider.webapps = webapps;
        updateWidgetView();
    }

    private LocationWatcher mLocationWatcher = null;

    private void discoverAppByLocation(Location location) {
        DiscoverApp.byLocation(location, webapps -> {
            for (WebApp webapp : webapps) {
                new Thread(() -> {
                    try {
                        webapp.download();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
            ArrayList<WebApp> mergedWebApps = new ArrayList<>();
            for (WebApp app : this.webapps) {
                if (app.distance_in_m < 0) {
                    mergedWebApps.add(app);
                }
            }
            mergedWebApps.addAll(Arrays.asList(webapps));
            setListView(mergedWebApps.toArray(new WebApp[mergedWebApps.size()]));
        });
    }

    private void discoverAppByLan() {
        DiscoverApp.byLan(webapps -> {
            for (WebApp webapp : webapps) {
                new Thread(() -> {
                    try {
                        webapp.download();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
            ArrayList<WebApp> mergedWebApps = new ArrayList<>(Arrays.asList(webapps));
            for (WebApp app : this.webapps) {
                if (app.distance_in_m >= 0) {
                    mergedWebApps.add(app);
                }
            }
            setListView(mergedWebApps.toArray(new WebApp[mergedWebApps.size()]));
        });
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (this.mLocationWatcher == null) {
            this.mLocationWatcher = new LocationWatcher(context, this::discoverAppByLocation);
            mContext = context;
            mLocationWatcher.start();
        }
        if ("SHOW_APP".equals(intent.getAction())) {
            int position = intent.getIntExtra("position", -1);
            WebApp webapp = webapps[position];
            try {
                MainActivity.activeAppName = webapp.name;
                webapp.launch();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if ("UPDATE_WIDGET".equals(intent.getAction())) {
            WebApp.setContext(context);
            this.discoverAppByLan();
        }
    }

    private void updateWidgetView() {
        RemoteViews updateViews = new RemoteViews(mContext.getPackageName(), R.layout.app_widget_layout);
        Intent svcIntent = new Intent(mContext, AppsWidgetService.class);
        svcIntent.setData(Uri.parse(svcIntent.toUri(Intent.URI_INTENT_SCHEME)));
        updateViews.setRemoteAdapter(R.id.widget_apps_list, svcIntent);
        ComponentName thisWidget = new ComponentName(mContext, AppsWidgetProvider.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(mContext);
        ComponentName thisAppWidget = new ComponentName(mContext.getPackageName(), AppsWidgetProvider.class.getName());
        int[] appWidgetIds = manager.getAppWidgetIds(thisAppWidget);
        manager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_apps_list);
        Intent showAppIntent = new Intent(mContext, AppsWidgetProvider.class);
        showAppIntent.setAction("SHOW_APP");
        PendingIntent clickPI = PendingIntent.getBroadcast(mContext, 0, showAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        updateViews.setPendingIntentTemplate(R.id.widget_apps_list, clickPI);
        manager.updateAppWidget(thisWidget, updateViews);
    }
}
