package com.example.shuftirpo.AutoML;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class FocusView extends View {
    private Paint mTransparentPaint;
    private Paint mSemiBlackPaint;
    private Path mPath = new Path();
    int ovalWidth,ovalHeight, ovalX, ovalY;

    public FocusView(Context context) {
        super(context);
        initPaints();
    }

    public FocusView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    public FocusView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaints();
    }

    private void initPaints()
    {
        mTransparentPaint = new Paint();
        mTransparentPaint.setColor(Color.TRANSPARENT);
        mTransparentPaint.setStrokeWidth(10);

        mSemiBlackPaint = new Paint();
        mSemiBlackPaint.setColor(Color.TRANSPARENT);
        mSemiBlackPaint.setStrokeWidth(10);

    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        mPath.reset();

        ovalWidth = (int) (canvas.getWidth()/1);
        ovalHeight = (int) (canvas.getHeight()/1);
        ovalX = (canvas.getWidth() - ovalWidth) / 2;
        ovalY = (canvas.getHeight() - ovalHeight) / 2;



        mPath.addOval(new RectF(ovalX, ovalY, ovalWidth + ovalX, ovalHeight + ovalY), Path.Direction.CCW);


        mPath.setFillType(Path.FillType.INVERSE_EVEN_ODD);

        canvas.drawCircle(canvas.getWidth() / 1, canvas.getHeight() / 1, 500, mTransparentPaint);



        canvas.drawPath(mPath, mSemiBlackPaint);
       canvas.clipPath(mPath);
        canvas.drawColor(Color.parseColor("#A6000000"));


//        Paint paint = new Paint();
//        paint.setColor(Color.GREEN);
//        paint.setStrokeWidth(10);
//        paint.setStyle(Paint.Style.STROKE);
//        canvas.drawOval(new RectF(ovalX, ovalY, ovalWidth + ovalX, ovalHeight + ovalY),paint);
    }
}

