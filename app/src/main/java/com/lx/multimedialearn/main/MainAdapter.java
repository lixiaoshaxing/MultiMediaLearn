package com.lx.multimedialearn.main;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lx.multimedialearn.R;

import java.util.List;

/**
 * 主页的adapter
 *
 * @author lixiao
 * @since 2017-09-05 17:29
 */
public class MainAdapter extends Adapter<MainAdapter.TabViewHolder> {

    List<TabModel> mTabList;

    public MainAdapter(List<TabModel> tabList) {
        this.mTabList = tabList;
    }

    @Override
    public TabViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_main_tab, parent, false);
        return new TabViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TabViewHolder holder, int position) {
        holder.bind(mTabList.get(position));
    }


    @Override
    public int getItemCount() {
        return mTabList.size();
    }

    class TabViewHolder extends RecyclerView.ViewHolder {

        private TextView txtTitle;
        private TextView txtDes;
        private TabModel data;

        public TabViewHolder(final View itemView) {
            super(itemView);
            txtTitle = (TextView) itemView.findViewById(R.id.txt_main_tab_title);
            txtDes = (TextView) itemView.findViewById(R.id.txt_main_tab_des);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity();
                }
            });
        }

        public void startActivity() {
            Intent intent = new Intent(itemView.getContext(), data.aimActivity);
            itemView.getContext().startActivity(intent);
        }

        public void bind(TabModel data) {
            this.data = data;
            txtTitle.setText(data.title);
            txtDes.setText(data.des);
        }
    }

}
