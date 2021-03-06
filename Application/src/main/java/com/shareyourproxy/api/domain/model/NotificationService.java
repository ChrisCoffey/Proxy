package com.shareyourproxy.api.domain.model;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import com.shareyourproxy.api.rx.command.GetUserMessagesCommand;
import com.shareyourproxy.api.rx.command.eventcallback.EventCallback;
import com.shareyourproxy.api.rx.command.eventcallback.UserMessagesDownloadedEventCallback;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Created by Evan on 6/18/15.
 */
public class NotificationService extends Service {
    private final INotificationService.Stub _binder = new INotificationService.Stub() {
        @Override
        public ArrayList<Notification> getNotifications(AutoParcel_User user)
            throws RemoteException {
            List<EventCallback> event = new GetUserMessagesCommand(user).execute
                (NotificationService.this);
            EventCallback eventData = event.get(0);

            if (eventData != null) {
                ArrayList<Notification> notifications =
                    ((UserMessagesDownloadedEventCallback) eventData).notifications;
                if (notifications != null) {
                    Timber.e("Message Downloaded");
                    return notifications;
                }
            }
            Timber.e("No Message");
            return new ArrayList<>();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return _binder;
    }
}
