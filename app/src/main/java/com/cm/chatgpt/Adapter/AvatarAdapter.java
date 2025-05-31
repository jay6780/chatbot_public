package com.cm.chatgpt.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.RecyclerView;


import com.cm.chatgpt.R;
import com.cm.chatgpt.Class.Avatar;
import com.cm.chatgpt.Class.GenderListener;

import java.util.List;

public class AvatarAdapter extends RecyclerView.Adapter<AvatarAdapter.ViewHolder> {
    private List<Avatar> avatarList;
    private Context context;
    private GenderListener genderListener;
    private boolean IsSelected = false;
    private int selectedPosition = -1;
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView avatar;
        LinearLayout ll_select;
        public ViewHolder(View view) {
            super(view);
            avatar = view.findViewById(R.id.avatar);
            ll_select = view.findViewById(R.id.ll_select);
        }
    }

    public AvatarAdapter(Context context,List<Avatar> avatarList,GenderListener genderListener) {
        this.context = context;
        this.avatarList = avatarList;
        this.genderListener = genderListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.avatar_item, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, @SuppressLint("RecyclerView") final int position) {
        Avatar avatar =  avatarList.get(position);
        viewHolder.avatar.setImageResource(avatar.getAvatar());

        if (selectedPosition == position) {
            viewHolder.ll_select.setBackgroundResource(R.drawable.lineborder);
        } else {
            viewHolder.ll_select.setBackground(null);
        }

        viewHolder.avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedPosition == position) {
                    selectedPosition = -1;
                    genderListener.getGender("",0);
                } else {
                    selectedPosition = position;
                    genderListener.getGender(avatar.getGender(), avatar.getAvatar());
                }
                notifyDataSetChanged();
            }
        });

    }

    @Override
    public int getItemCount() {
        return avatarList.size();
    }
}