package ufro.com.mozbiisdkandroid2;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Usagi on 2017/3/23.
 */

public class RecycleAdapter extends RecyclerView.Adapter<RecycleAdapter.ViewHolder>{
    private List<Integer> colorCodeList;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new ViewHolder(inflater.inflate(R.layout.recycle_layout, null));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.textView.setText(Integer.toString(colorCodeList.get(position)));
        holder.frameLayout.setBackgroundColor(colorCodeList.get(position));
    }

    @Override
    public int getItemCount() {
        return colorCodeList.size();
    }

    public void setNumOfDevices(int numOfDevices){
        colorCodeList = new ArrayList<>(numOfDevices);
        for(int i = 0; i < numOfDevices; i ++){
            colorCodeList.add(-1);
        }
    }

    public void setColorCode(int colorCode, int indexOfDevice){
        if(indexOfDevice < colorCodeList.size()){
            colorCodeList.remove(indexOfDevice);
            colorCodeList.add(indexOfDevice, colorCode);
        }

        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        public TextView textView;
        public FrameLayout frameLayout;

        public ViewHolder(View view){
            super(view);
            textView = (TextView)view.findViewById(R.id.color_code);
            frameLayout = (FrameLayout)view.findViewById(R.id.color);
        }
    }
}
