package com.bosong.sample;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.bosong.expandablelayout.ExpandableLayout;

/**
 * Created by bosong on 4/18/2017.
 */

public class MyViewHolder extends RecyclerView.ViewHolder {

    public ExpandableLayout expandableLayout;
    public TextView titleTv;

    public MyViewHolder(View itemView) {
        super(itemView);

        expandableLayout = (ExpandableLayout) itemView.findViewById(R.id.expandable_layout);
        titleTv = (TextView) itemView.findViewById(R.id.tv_title);
    }
}
