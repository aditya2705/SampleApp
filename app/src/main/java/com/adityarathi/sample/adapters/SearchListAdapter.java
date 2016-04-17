package com.adityarathi.sample.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import com.adityarathi.fastscroll.views.FastScrollRecyclerView;
import com.adityarathi.sample.R;
import com.adityarathi.sample.objects.UserObject;

import java.util.ArrayList;
import java.util.List;


public class SearchListAdapter extends HeaderFooterRecyclerViewAdapter implements FastScrollRecyclerView.SectionedAdapter {


    private final LayoutInflater mInflater;
    private final List<UserObject> mModels;
    private int filterType;
    private boolean footer;

    public SearchListAdapter(Context context, List<UserObject> models, int filterType, boolean footer) {
        mInflater = LayoutInflater.from(context);
        mModels = new ArrayList<>(models);
        this.filterType = filterType;
        this.footer = footer;
    }

    @Override
    protected SearchItemViewHolder onCreateContentItemViewHolder(ViewGroup parent, int contentViewType) {
        final View itemView = mInflater.inflate(R.layout.list_item, parent, false);
        return new SearchItemViewHolder(itemView);
    }

    @Override
    protected void onBindContentItemViewHolder(RecyclerView.ViewHolder contentViewHolder, int position) {
        SearchItemViewHolder holder = (SearchItemViewHolder)contentViewHolder;
        final UserObject model = mModels.get(position);
        holder.bind(model);
    }

    @Override
    public int getContentItemCount() {
        return mModels.size();
    }

    public void animateTo(List<UserObject> models) {
        applyAndAnimateRemovals(models);
        applyAndAnimateAdditions(models);
        applyAndAnimateMovedItems(models);
    }

    private void applyAndAnimateRemovals(List<UserObject> newModels) {
        for (int i = mModels.size() - 1; i >= 0; i--) {
            final UserObject model = mModels.get(i);
            if (!newModels.contains(model)) {
                removeItem(i);
            }
        }
    }

    private void applyAndAnimateAdditions(List<UserObject> newModels) {
        for (int i = 0, count = newModels.size(); i < count; i++) {
            final UserObject model = newModels.get(i);
            if (!mModels.contains(model)) {
                addItem(i, model);
            }
        }
    }

    private void applyAndAnimateMovedItems(List<UserObject> newModels) {
        for (int toPosition = newModels.size() - 1; toPosition >= 0; toPosition--) {
            final UserObject model = newModels.get(toPosition);
            final int fromPosition = mModels.indexOf(model);
            if (fromPosition >= 0 && fromPosition != toPosition) {
                moveItem(fromPosition, toPosition);
            }
        }
    }

    public UserObject removeItem(int position) {
        final UserObject model = mModels.remove(position);
        notifyItemRemoved(position);
        return model;
    }

    public void addItem(int position, UserObject model) {
        mModels.add(position, model);
        notifyItemInserted(position);
    }

    public void moveItem(int fromPosition, int toPosition) {
        final UserObject model = mModels.remove(fromPosition);
        mModels.add(toPosition, model);
        notifyItemMoved(fromPosition, toPosition);
    }

    public UserObject getItem(int position) {

        return mModels.get(position);
    }

    @NonNull
    @Override
    public String getSectionName(int position) {
        if(position < getContentItemCount()) {
            switch (filterType) {
                case 0:
                    return mModels.get(position).getFirst_name().substring(0, 1);
                case 1:
                    return mModels.get(position).getLast_name().substring(0, 1);
                case 2:
                    return mModels.get(position).getUser_name().substring(0, 1);
            }
        }
        return "";
    }

    @Override
    protected int getFooterItemCount() {
        return footer?1:0;
    }

    @Override
    protected RecyclerView.ViewHolder onCreateFooterItemViewHolder(ViewGroup parent, int footerViewType) {
        if(footer) {
            final View itemView = mInflater.inflate(R.layout.footer, parent, false);
            return new SearchItemViewHolder(itemView);
        }
        return null;
    }

    @Override
    protected void onBindFooterItemViewHolder(RecyclerView.ViewHolder footerViewHolder, int position) {

    }

    @Override
    protected RecyclerView.ViewHolder onCreateHeaderItemViewHolder(ViewGroup parent, int headerViewType) {
        return null;
    }

    @Override
    protected void onBindHeaderItemViewHolder(RecyclerView.ViewHolder headerViewHolder, int position) {

    }

    @Override
    protected int getHeaderItemCount() {
        return 0;
    }

}

class SearchItemViewHolder extends RecyclerView.ViewHolder {

    private final TextView profileNameText, userNameTextView;

    public SearchItemViewHolder(View itemView) {
        super(itemView);
        profileNameText = (TextView) itemView.findViewById(R.id.name);
        userNameTextView = (TextView) itemView.findViewById(R.id.username);
    }

    public void bind(UserObject model) {
        profileNameText.setText(model.getFirst_name()+" "+model.getLast_name());
        userNameTextView.setText(model.getUser_name());
    }
}