package com.thunderpunch.ladderlayoutmanager.sample;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.thunderpunch.ladderlayoutmanager.R;
import com.thunderpunch.ladderlayoutmanager.sample.view.HWRatioContainer;
import com.thunderpunch.lib.layoutmanager.HzLayoutManager2;
import com.thunderpunch.lib.layoutmanager.LadderSimpleSnapHelper;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class HzActivity extends AppCompatActivity {

    HzLayoutManager2 llm;
    RecyclerView rcv;
    HSAdapter2 adapter;
    int scrollToPosition;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hz);

        llm = new HzLayoutManager2(1.5f, 0.85f, HzLayoutManager2.HORIZONTAL).
                setChildDecorateHelper(new HzLayoutManager2.DefaultChildDecorateHelper(getResources().getDimension(R.dimen.item_max_elevation)));
        llm.setChildPeekSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                60, getResources().getDisplayMetrics()));
        llm.setMaxItemLayoutCount(5);

        rcv = (RecyclerView) findViewById(R.id.rcv);
        rcv.setLayoutManager(llm);
        new LadderSimpleSnapHelper().attachToRecyclerView(rcv);
        adapter = new HSAdapter2();
        rcv.setAdapter(adapter);
    }

    public void click(View view) {
//        switch (view.getId()) {
//            case R.id.add:
//                adapter.count += adapter.imgRes.length;
//                adapter.notifyDataSetChanged();
//                break;
//            case R.id.remove:
//                adapter.count -= adapter.imgRes.length;
//                adapter.notifyDataSetChanged();
//                break;
//            case R.id.reverse:
//                llm.setReverse(!llm.isReverse());
//                break;
//            case R.id.scroll:
//                if (scrollToPosition >= adapter.count) scrollToPosition -= adapter.count;
//                rcv.smoothScrollToPosition(scrollToPosition);
//                scrollToPosition++;
//                break;
//        }
    }

    private class HSAdapter extends RecyclerView.Adapter<HzActivity.HSAdapter.VH> {
        int[] imgRes = {R.drawable.blastocyst_full, R.drawable.chub_full3, R.drawable.dukeofflies_full, R.drawable.fistula, R.drawable.gemini_full, R.drawable.larryjr_full, R.drawable.loki_full, R.drawable.monstro};
        int count = imgRes.length;

        @Override
        public HzActivity.HSAdapter.VH onCreateViewHolder(ViewGroup parent, int viewType) {
            return new HzActivity.HSAdapter.VH(LayoutInflater.from(HzActivity.this).inflate(R.layout.item_horizontal, parent, false));
        }

        @Override
        public void onBindViewHolder(HzActivity.HSAdapter.VH holder, int position) {
            holder.iv.setImageResource(imgRes[position % imgRes.length]);
            holder.tv.setText("Position - " + position);
        }

        @Override
        public int getItemCount() {
            return count;
        }

        class VH extends RecyclerView.ViewHolder {
            ImageView iv;
            TextView tv;
            HWRatioContainer ivc;

            public VH(View itemView) {
                super(itemView);
                iv = (ImageView) itemView.findViewById(R.id.iv);
                tv = (TextView) itemView.findViewById(R.id.tv);
                ivc = (HWRatioContainer) itemView.findViewById(R.id.ivc);
                ivc.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            ivc.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        } else {
                            ivc.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        }
                        ivc.setTranslationX(ivc.getWidth() >> 4);
                    }
                });

            }
        }
    }

    private class HSAdapter2 extends RecyclerView.Adapter<HSAdapter2.ViewHolder> {
        private ArrayList<MyModel> mItems = new ArrayList<>();
        private Context mContext;

        public HSAdapter2() {
            init();
        }

        private void init() {
            mItems.add(new MyModel("Goodbye! Have a nice day.", R.drawable.one));
            mItems.add(new MyModel("Hope to see you soon?", R.drawable.two));
            mItems.add(new MyModel("It was nice seeing you?", R.drawable.three));
            mItems.add(new MyModel("Where have you been?", R.drawable.four));
            mItems.add(new MyModel("What time is it?", R.drawable.five));
            mItems.add(new MyModel("How are you today?", R.drawable.six));
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            mContext = parent.getContext();
            View view = LayoutInflater.from(mContext)
                    .inflate(R.layout.item_hz_my_model, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            MyModel model = mItems.get(position);
            holder.tv.setText(model.title + " - " + position);
            holder.image.setImageResource(model.drawable);
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tv;
            CircleImageView image;

            public ViewHolder(View itemView) {
                super(itemView);
                tv = (TextView) itemView.findViewById(R.id.tv);
                image = (CircleImageView) itemView.findViewById(R.id.image);
            }
        }
    }

    class MyModel {
        String title;
        @DrawableRes int drawable;

        public MyModel(String title, int drawable) {
            this.title = title;
            this.drawable = drawable;
        }
    }
}