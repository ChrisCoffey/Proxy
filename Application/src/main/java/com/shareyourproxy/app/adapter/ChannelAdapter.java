package com.shareyourproxy.app.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v7.util.SortedList;
import android.support.v7.util.SortedList.Callback;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.shareyourproxy.R;
import com.shareyourproxy.api.domain.model.Channel;
import com.shareyourproxy.api.domain.model.ChannelType;
import com.shareyourproxy.api.domain.model.User;
import com.shareyourproxy.app.adapter.BaseViewHolder.ItemClickListener;
import com.shareyourproxy.util.ObjectUtils;

import butterknife.Bind;

import static com.shareyourproxy.api.domain.factory.ChannelFactory.getEmailChannel;
import static com.shareyourproxy.api.domain.factory.ChannelFactory.getFacebookChannel;
import static com.shareyourproxy.api.domain.factory.ChannelFactory.getPhoneChannel;
import static com.shareyourproxy.api.domain.factory.ChannelFactory.getSMSChannel;
import static com.shareyourproxy.api.domain.factory.ChannelFactory.getWebChannel;
import static com.shareyourproxy.api.domain.model.ChannelType.Custom;
import static com.shareyourproxy.util.ViewUtils.getActivityIcon;

/**
 * Adapter that handles displaying channels.
 */
public class ChannelAdapter extends BaseRecyclerViewAdapter {
    private static final Channel DIALER = getPhoneChannel();
    private static final Channel HANGOUTS = getSMSChannel();
    private static final Channel EMAIL = getEmailChannel();
    private static final Channel WEB = getWebChannel();
    private static final Channel FACEBOOK = getFacebookChannel();
    private static final int TYPE_LIST_ITEM = 1;
    private Callback<Channel> _sortedListCallback;
    private SortedList<Channel> _channels = new SortedList<>(Channel.class, getSortedCallback());
    private ItemClickListener _clickListener;

    public ChannelAdapter(ItemClickListener listener) {
        _clickListener = listener;
        _channels.add(DIALER);
        _channels.add(HANGOUTS);
        _channels.add(EMAIL);
        _channels.add(WEB);
        _channels.add(FACEBOOK);
    }

    /**
     * Create a newInstance of a {@link ChannelAdapter} with blank data.
     *
     * @return an {@link ChannelAdapter} with no data
     */
    public static ChannelAdapter newInstance(
        ItemClickListener
            listener) {
        return new ChannelAdapter(listener);
    }

    public Callback<Channel> getSortedCallback() {
        if (_sortedListCallback == null) {
            _sortedListCallback = new Callback<Channel>() {

                @Override
                public int compare(Channel item1, Channel item2) {
                    int weight1 = item1.channelType().getWeight();
                    int weight2 = item2.channelType().getWeight();
                    int compareFirst = ObjectUtils.compare(weight1, weight2);
                    if (compareFirst == 0) {
                        return item1.label().compareTo(item2.label());
                    } else {
                        return compareFirst;
                    }
                }

                @Override
                public void onInserted(int position, int count) {
                    notifyItemRangeInserted(position, count);
                }

                @Override
                public void onRemoved(int position, int count) {
                    notifyItemRangeRemoved(position, count);
                }

                @Override
                public void onMoved(int fromPosition, int toPosition) {
                    notifyItemMoved(fromPosition, toPosition);
                }

                @Override
                public void onChanged(int position, int count) {
                    notifyItemRangeChanged(position, count);
                }

                @Override
                public boolean areContentsTheSame(Channel item1, Channel item2) {
                    // we dont compare resId because its probably going to be removed
                    return (item1.id().equals(item2.id())
                        && item1.label().equals(item2.label())
                        && item1.packageName().equals(item2.packageName())
                        && item1.channelSection() == item2.channelSection()
                        && item1.channelType().equals(item2.channelType()));
                }

                @Override
                public boolean areItemsTheSame(Channel item1, Channel item2) {
                    //Sections will have the same ID but different categories
                    return (item1.id().equals(item2.id())
                        && item1.channelSection() == item2.channelSection());
                }
            };
        }
        return _sortedListCallback;
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.adapter_add_channel_list_item, parent, false);
        return ItemViewHolder.newInstance(view, _clickListener);
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        bindItemViewData((ItemViewHolder) holder, getItemData(position));
    }

    /**
     * Get the desired {@link Channel} based off its position in a list.
     *
     * @param position the position in the list
     * @return the desired {@link User}
     */
    public Channel getItemData(int position) {
        return _channels.get(position);
    }

    /**
     * Set the Channel Intent link content.
     *
     * @param holder  {@link Channel} {@link BaseViewHolder}
     * @param channel {@link Channel} data
     */
    @SuppressLint("NewApi")
    private void bindItemViewData(ItemViewHolder holder, Channel channel) {
        Context context = holder._view.getContext();
        ChannelType channelType = channel.channelType();
        if (channelType.equals(Custom)) {
            holder.itemImage.setImageDrawable(getAndroidIconDrawable(
                context, getActivityIcon(context, channel.packageName())));
        } else {
            holder.itemImage.setImageDrawable(
                getSVGIconDrawable(context, channel.channelType().getResId(),
                    getChannelBackgroundColor(context, channelType)));
        }
        holder.itemLabel.setText(channel.label().toLowerCase());
    }

    @Override
    public int getItemViewType(int position) {
        return TYPE_LIST_ITEM;
    }

    @Override
    public int getItemCount() {
        return _channels.size();
    }

    /**
     * ViewHolder for the settings header.
     */
    public static final class SectionHeaderViewHolder extends BaseViewHolder {
        @Bind(R.id.adapter_add_channel_list_section_image)
        protected ImageView sectionImage;
        @Bind(R.id.adapter_add_channel_list_section_label)
        protected TextView sectionLabel;

        /**
         * Constructor for the HeaderViewHolder.
         *
         * @param view              the inflated view
         * @param itemClickListener click listener for this view
         */
        private SectionHeaderViewHolder(View view, ItemClickListener itemClickListener) {
            super(view, itemClickListener);
        }

        /**
         * Create a new Instance of the ViewHolder.
         *
         * @param view              inflated in {@link RecyclerView.Adapter#onCreateViewHolder}
         * @param itemClickListener click listener for this view
         * @return a ViewHolder instance
         */
        public static SectionHeaderViewHolder newInstance(
            View view, ItemClickListener itemClickListener) {
            return new SectionHeaderViewHolder(view, itemClickListener);
        }
    }

    /**
     * ViewHolder for the entered settings data.
     */
    public static final class ItemViewHolder extends BaseViewHolder {
        @Bind(R.id.adapter_add_channel_list_item_image)
        protected ImageView itemImage;
        @Bind(R.id.adapter_add_channel_list_item_label)
        protected TextView itemLabel;

        /**
         * Constructor for the ItemViewHolder.
         *
         * @param view              the inflated view
         * @param itemClickListener click listener for this view
         */
        private ItemViewHolder(View view, ItemClickListener itemClickListener) {
            super(view, itemClickListener);
        }

        /**
         * Create a new Instance of the ViewHolder.
         *
         * @param view              inflated in {@link RecyclerView.Adapter#onCreateViewHolder}
         * @param itemClickListener click listener for this view
         * @return a ViewHolder instance
         */
        public static ItemViewHolder newInstance(View view, ItemClickListener itemClickListener) {
            return new ItemViewHolder(view, itemClickListener);
        }
    }
}

