package com.shareyourproxy.api.domain.model;

import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.facebook.FacebookSdk;
import com.firebase.client.Firebase;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.shareyourproxy.BuildConfig;
import com.shareyourproxy.Constants;
import com.shareyourproxy.R;
import com.shareyourproxy.api.CommandIntentService;
import com.shareyourproxy.api.gson.UserTypeAdapter;
import com.shareyourproxy.api.rx.JustObserver;
import com.shareyourproxy.api.rx.RxBusDriver;
import com.shareyourproxy.api.rx.RxHelper;
import com.shareyourproxy.api.rx.command.AddUserMessageCommand;
import com.shareyourproxy.api.rx.command.BaseCommand;
import com.shareyourproxy.api.rx.command.eventcallback.EventCallback;
import com.shareyourproxy.api.rx.command.eventcallback.UserContactAddedEventCallback;
import com.shareyourproxy.api.rx.command.eventcallback.UserEventCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.fabric.sdk.android.Fabric;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import timber.log.Timber;

import static com.shareyourproxy.api.domain.factory.ContactFactory.createModelContact;
import static com.shareyourproxy.api.rx.RxMessageSync.deleteAllFirebaseMessages;
import static com.shareyourproxy.api.rx.RxQuery.queryUser;

/**
 * Proxy application that handles syncing the current user and handling BaseCommands.
 */
public class ProxyApplication extends Application {

    private User _currentUser;
    private RxBusDriver _bus = RxBusDriver.getInstance();
    private SharedPreferences _sharedPreferences;
    private INotificationService _notificationService;
    private Subscription _notificationSubscription;
    private NotificationManager _notificationManager;
    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection _notificationConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            _notificationService = INotificationService.Stub.asInterface(service);
            _notificationSubscription =
                getNotificationsObservable().subscribe(intervalObserver(_notificationService));
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            _notificationService = null;
            _notificationSubscription.unsubscribe();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        initialize();
    }

    public void initialize() {
        initializeBuildConfig();
        _sharedPreferences = getSharedPreferences(
            getString(R.string.shared_preferences_key), Context.MODE_PRIVATE);
        Firebase.setAndroidContext(this);
        FacebookSdk.sdkInitialize(this);
        _bus.toObserverable().subscribe(getRequest());
        RealmConfiguration config = new RealmConfiguration.Builder(this)
            .deleteRealmIfMigrationNeeded()
            .schemaVersion(BuildConfig.VERSION_CODE)
            .build();
        Realm.setDefaultConfiguration(config);
        initializeNotificationService();
    }

    public void initializeBuildConfig() {
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        } else if (BuildConfig.USE_GOOGLE_ANALYTICS) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            analytics.newTracker(BuildConfig.GA_TRACKER_ID);
            analytics.enableAdvertisingIdCollection(true);
            analytics.enableAutoActivityReports(this);
        } else if (BuildConfig.USE_CRASHLYTICS) {
            Fabric.with(this, new Crashlytics());
        }
    }

    public void initializeNotificationService() {
        _notificationManager =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(this, NotificationService.class);
        intent.setPackage("com.android.vending");
        bindService(intent, _notificationConnection, Context.BIND_AUTO_CREATE);
    }

    public Observable<Long> getNotificationsObservable() {
        return Observable.interval(3, TimeUnit.MINUTES).compose(RxHelper.<Long>applySchedulers());
    }

    public JustObserver<Long> intervalObserver(final INotificationService _notificationService) {
        return new JustObserver<Long>() {
            @Override
            public void onError() {
                Timber.e("Notification Observable Error");
            }

            @Override
            public void onNext(Long timesCalled) {
                Timber.i("Checking for notifications, attempt:" + timesCalled.intValue());
                try {
                    List<Notification> notifications =
                        _notificationService.getNotifications((AutoParcel_User) _currentUser);
                    if (notifications != null && notifications.size() > 0) {
                        for (Notification notification : notifications) {
                            _notificationManager.notify(notification.hashCode(), notification);
                        }
                        deleteAllFirebaseMessages(_currentUser).subscribe();
                    }
                } catch (RemoteException e) {
                    Timber.e(Log.getStackTraceString(e));
                }
            }
        };
    }

    public Action1<Object> getRequest() {
        return new Action1<Object>() {
            @Override
            public void call(Object event) {
                if (event instanceof BaseCommand) {
                    baseCommandEvent((BaseCommand) event);
                } else if (event instanceof UserContactAddedEventCallback) {
                    userContactAddedEvent((UserContactAddedEventCallback) event);
                }
            }
        };
    }

    private void userContactAddedEvent(UserContactAddedEventCallback event) {
        baseCommandEvent(new AddUserMessageCommand(event.contact.id().value(),
            Message.create(event.user.id(), createModelContact(event.user))));
    }

    private void baseCommandEvent(BaseCommand event) {
        Timber.i("BaseCommand:" + event);
        Intent intent = new Intent(ProxyApplication.this, CommandIntentService.class);
        intent.putExtra(CommandIntentService.ARG_COMMAND_CLASS, event);
        intent.putExtra(
            CommandIntentService.ARG_RESULT_RECEIVER, new ResultReceiver(null) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    if (resultCode == Activity.RESULT_OK) {
                        ArrayList<EventCallback> events =
                            resultData.getParcelableArrayList(
                                CommandIntentService.ARG_RESULT_BASE_EVENTS);
                        /** Querying logged in user with Realm.refresh() in RxQuery is 50/50
                         * correct when editing known user data. Addition of new data or deletion
                         * of known data works.
                         */
                        User realmUser = queryUser(
                            ProxyApplication.this, _currentUser.id().value());
                        updateUser(realmUser);
                        for (EventCallback event : events) {
                            if (event instanceof UserEventCallback) {
                                /**
                                 * Event Callback data below is always the correct user.
                                 */
                                Boolean isEqual = ((UserEventCallback) event).user
                                    .equals(realmUser);
                                Timber.e("IS REALM USER EQUAL TO EVENTCALLBACK:" +
                                    isEqual.toString());
                            }
                            Timber.i("EventCallback:" + event);
                            getRxBus().post(event);
                        }
                    } else {
                        Timber.e("Error receiving result");
                    }
                }
            });
        startService(intent);
    }

    private void updateUser(User user) {
        setCurrentUser(user);
        try {
            _sharedPreferences
                .edit()
                .putString(Constants.KEY_LOGGED_IN_USER, UserTypeAdapter.newInstance().toJson(user))
                .apply();
        } catch (IOException e) {
            Timber.e(Log.getStackTraceString(e));
        }
    }

    /**
     * Getter.
     *
     * @return currerntly logged in user
     */
    public User getCurrentUser() {
        return _currentUser;
    }

    /**
     * Setter.
     *
     * @param currentUser currently logged in user
     */
    public void setCurrentUser(User currentUser) {
        _currentUser = currentUser;
    }

    public RxBusDriver getRxBus() {
        return _bus;
    }

    public SharedPreferences getSharedPreferences() {
        return _sharedPreferences;
    }

}
