package com.proxy.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.proxy.R;
import com.proxy.api.domain.model.Channel;
import com.proxy.api.domain.model.Group;
import com.proxy.api.domain.model.User;
import com.proxy.api.rx.event.GroupChannelToggled;
import com.proxy.api.rx.event.GroupDeleted;
import com.proxy.app.fragment.EditGroupFragment;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static com.proxy.Constants.ARG_SELECTED_GROUP;
import static com.proxy.Constants.ARG_USER_LOGGED_IN;
import static com.proxy.Constants.ARG_USER_SELECTED_PROFILE;
import static com.proxy.util.ViewUtils.getMenuIcon;
import static rx.android.app.AppObservable.bindActivity;

public class EditGroupActivity extends BaseActivity {

    @InjectView(R.id.common_toolbar)
    protected Toolbar toolbar;
    private CompositeSubscription _subscriptions;

    //note this may make sense to factor out into the base activity
    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra(ARG_USER_SELECTED_PROFILE, getLoggedInUser());
        intent.putExtra(ARG_USER_LOGGED_IN, true);
        setResult(Activity.RESULT_OK, intent);
        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_bottom);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.common_activity_fragment_container);
        ButterKnife.inject(this);
        initialize();
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.activity_fragment_container,
                    EditGroupFragment.newInstance()).commit();
        }
    }

    private void initialize() {
        //we'll need a set of user channels
        //and a group
        buildToolbar(toolbar, getString(R.string.edit_group), getMenuIcon(this, R.raw.clear));
    }

    @Override
    public void onResume() {
        super.onResume();
        _subscriptions = new CompositeSubscription();
        _subscriptions.add(bindActivity(this, getRxBus().toObserverable())
            .subscribe(onNextEvent()));
    }

    private Action1<Object> onNextEvent() {
        return new Action1<Object>() {
            @Override
            public void call(Object event) {
                if (event instanceof GroupDeleted) {
                    removeGroupFromUser(getLoggedInUser(), selectedGroup());
                    // todo do something with the new user
                } else if (event instanceof GroupChannelToggled) {
                    Channel channel = null;
                    for (Channel userChannel : getLoggedInUser().channels()) {
                        if (userChannel.id().equals(((GroupChannelToggled) event)
                            .channelid)) {
                            channel = userChannel;
                        }
                    }
                    if (channel == null) {
                        return;
                    }
                    toggleChannelInGroup(channel);
                }
            }
        };
    }

    @Override
    protected void onPause() {
        super.onPause();
        _subscriptions.unsubscribe();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            default:
                Timber.e("Option item selected is unknown");
        }
        return super.onOptionsItemSelected(item);
    }

    private void removeGroupFromUser(User user, Group dirtyGroup) {
        if (user.groups() != null) {
            for (Group group : user.groups()) {
                if (group.id().equals(dirtyGroup.id())) {
                    user.groups().remove(dirtyGroup);
                    break;
                }
            }
        }
    }

    private Group selectedGroup() {
        return getIntent().getExtras().getParcelable(ARG_SELECTED_GROUP);
    }

    private void toggleChannelInGroup(Channel channel) {
        for (Channel c : selectedGroup().channels()) {
            if (channel.id().equals(c.id())) {
                selectedGroup().channels().remove(c);
                return;
            }
        }
        addChannelToGroup(selectedGroup(), channel);
    }

    private void addChannelToGroup(Group grp, Channel channel) {
        grp.channels().add(channel);
    }
}
