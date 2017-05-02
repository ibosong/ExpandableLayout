package com.bosong.sample;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.bosong.expandablelayout.ExpandableLayout;

/**
 * Created by bosong on 4/18/2017.
 */

public class MyAdapter extends RecyclerView.Adapter {
    private final int ITEM_COUNT = 30;
    // Save the state(collapsed or expanded) to restore when scrolling
    private boolean[] mExpandState;

    public MyAdapter(){
        mExpandState = new boolean[ITEM_COUNT];
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_demo, parent, false);
        final MyViewHolder holder = new MyViewHolder(itemView);

        holder.expandableLayout.setCollapsedEdgeView(holder.titleTv);
        holder.expandableLayout.setExpandWithScroll(true);
        holder.expandableLayout.setCollapseWithScroll(true);
        return holder;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        final MyViewHolder myHolder = (MyViewHolder) holder;
        // Restore state
        myHolder.expandableLayout.initState(!mExpandState[position]);

        myHolder.titleTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onExpandClick(position, myHolder.expandableLayout);
            }
        });
    }

    @Override
    public int getItemCount() {
        return ITEM_COUNT;
    }


    private void onExpandClick(int position, ExpandableLayout expandableLayout) {
        mExpandState[position] = !mExpandState[position];
        expandableLayout.toggle();
    }
}
