package com.easyvaas.sdk.ilivedemo;

import android.graphics.Rect;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * @author liya
 * @version V1.0
 * @ClassName:
 * @Package com.easyvaas.sdk.ilivedemo
 * @Description:
 * @date 2017-07-18 20:37
 */

public class SpaceItemDecoration extends RecyclerView.ItemDecoration {
    private int space;
    private int lines;
    private int orientation;

    public SpaceItemDecoration(int space, int lines, int orientation) {
        this.space = space;
        this.lines = lines;
        this.orientation = orientation;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        if (orientation == GridLayoutManager.VERTICAL) {
            //不是第一个的格子都设一个左边和底部的间距
            outRect.left = space;
            outRect.bottom = space;
            //由于每行都只有3个，所以第一个都是3的倍数，把左边距设为0
            if (parent.getChildLayoutPosition(view) % lines == 0) {
                outRect.left = 0;
            }
        } else {
            outRect.top = space;
            outRect.right = space;

            if (parent.getChildLayoutPosition(view) % lines == 0) {
                outRect.top = 0;
            }

            if (parent.getChildLayoutPosition(view) / lines == 0) {
                outRect.right = 0;
            }
        }
    }
}
