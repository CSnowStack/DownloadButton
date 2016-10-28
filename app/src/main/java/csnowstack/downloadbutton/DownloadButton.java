package csnowstack.downloadbutton;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by cqll on 2016/10/27.
 */

public class DownloadButton extends View {
    private Paint mPaint;

    private int mFlag, mCenterX, mCenterY;

    private float mFractionLine = 0/*线段的弹动比例*/, mFractionLineLast = 0/*线段的弹动比例*/, mFractionArrowFirst = 0/*箭头第一步动画的比例*/, mFractionArrowSecond = 0/*箭头第二步动画的比例*/;
    private ValueAnimator mAnimatorArrowFirst/*箭头移动到线段时的动画*/, mAnimatorLine, mAnimatorArrowSecond/*箭头压迫线段和弹回时的动画*/;

    private PathMeasure mPathMeasureLine;//用于计算向下移动时最下面的那个点的移动距离
    private float mLineWidth, mLineSize, mLineMaxMove, mDistanceLine/*线段跟原点的距离*/, mRectangleSideLength/*正方形边长*/, mAngleRight/*对号的左边与坐标的夹角*/, mRightShortSize/*对号左边边长*/;
    private Path mPath, mPathDst;
    private float mPos[] = new float[2];//线段突出的点的坐标
    private RectF mRectFRectangle;//最后方形

    private static final int INITIALIZATION = 0;//初始化到移动箭头
    private static final int TOUCH_LINE = 1;//压迫线段
    private static final int CHANGE_ARROW = 2;//改变箭头，变成对号，变成矩形，加三角形

    public DownloadButton(Context context) {
        super(context);
    }

    public DownloadButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        mFlag = INITIALIZATION;

        mLineWidth = 220;
        mLineSize = mLineWidth / 10f;
        mDistanceLine = mLineWidth / 4f;
        mRectangleSideLength = mLineWidth / 9f * 4f;
        mAngleRight = (float) (Math.PI / 4);

        mRectFRectangle = new RectF();

        mPaint = new Paint();
//        mPaint.setColor(0xffF3F3F3);
        mPaint.setColor(Color.BLACK);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(mLineSize);


        mPath = new Path();
        mPathDst = new Path();

        mPathMeasureLine = new PathMeasure();

        //箭头第一步动画
        mAnimatorArrowFirst = ValueAnimator.ofFloat(0, 1, 0, -1);//箭头的动画,向左上移动，移动回来，移到到最下面
        mAnimatorArrowFirst.setDuration(1000);

        mAnimatorArrowFirst.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mFractionArrowFirst = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        mAnimatorArrowFirst.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                //开启第二步压迫线段
                mFlag = TOUCH_LINE;
                mAnimatorLine.start();
            }

        });


        //线段相关动画
        mAnimatorLine = ValueAnimator.ofFloat(0, 1, -0.6f, 0);
        mAnimatorLine.setDuration(500);
        mAnimatorLine.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mFractionLine = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        mAnimatorLine.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mFlag = CHANGE_ARROW;
                mAnimatorArrowSecond.start();//开启第三步动画
            }
        });

        //箭头的动画,alpha，画对号，对号变矩形，画三角形
        mAnimatorArrowSecond = ValueAnimator.ofFloat(0.6f, 0, -1, -2, -3);//不要问我为什么这样写，不想用多个valueAnimator
        mAnimatorArrowSecond.setDuration(2400);
        mAnimatorArrowSecond.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mFractionArrowSecond = (float) animation.getAnimatedValue();
                invalidate();
            }
        });

        mAnimatorArrowSecond.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mFlag = INITIALIZATION;
                mFractionLine = 0;
                mFractionLineLast = 0;
                mFractionArrowFirst = 0;
                mFractionArrowSecond = 0;


            }
        });


    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mCenterX = w / 2;
        mCenterY = h / 2;
        mRightShortSize = mLineWidth / 2f * 0.85f;//对号的短边的长
        mRectFRectangle.set(mCenterX - (mLineWidth / 2 - mRectangleSideLength / 2) + mLineSize / 2, mCenterY - mLineWidth / 2 - mRectangleSideLength, mCenterX - (mLineWidth / 2 - mRectangleSideLength / 2) + mRectangleSideLength + mLineSize / 2, mCenterY - mLineWidth / 2);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //画弹跳的线
        mPath.reset();
        mPaint.setStyle(Paint.Style.STROKE);
        mPath.moveTo(mCenterX - mLineWidth / 2f, mCenterY + mDistanceLine);
        mPath.cubicTo(mCenterX - mLineWidth / 2f, mCenterY + mDistanceLine, mCenterX, mCenterY + mDistanceLine + (mDistanceLine * mFractionLine), mCenterX + mLineWidth / 2f, mCenterY + mDistanceLine);
        canvas.drawPath(mPath, mPaint);
        canvas.save();

        if (mFlag == INITIALIZATION) {//初始化

            //向右上移动并移动回来,完成第一步动画
            if (mFractionArrowFirst > 0) {
                canvas.translate(mDistanceLine * mFractionArrowFirst, -mDistanceLine * mFractionArrowFirst * 1.6f);
                canvas.rotate(mFractionArrowFirst * 20f, mCenterX, mCenterY);
            } else {//移动到线所在的位置
                canvas.translate(0, (mDistanceLine - mLineSize / 2f) * Math.abs(mFractionArrowFirst));
            }

            setArrowPath();
            canvas.drawPath(mPath, mPaint);

        } else if (mFlag == TOUCH_LINE) {//压迫线段

            if (mFractionLine > 0 || !(mFractionLineLast < 0) && (mFractionLine == 0)) {//箭头的第一步动画完成，根据线段的动画来改变,0到1到0的这段，移动,
                //计算曲线顶点的坐标
                mPathMeasureLine.setPath(mPath, false);
                mPathMeasureLine.getPosTan(mPathMeasureLine.getLength() / 2, mPos, null);
                if (mFractionLine >= mFractionLineLast) {//上到下 0-1
                    canvas.translate(0, mPos[1] - mCenterY - mLineSize / 2);
                    mLineMaxMove = mPos[1] - mCenterY - mLineSize / 2;
                } else if (mFractionLine >= 0.8f) {
                    //没什么效果，和到最下面直接弹上去没什么区别，这段位移分配的时间太少了
                    canvas.translate(0, mLineMaxMove * (1f - mFractionLine) / 0.3f);
                }
                mFractionLineLast = mFractionLine;
            }
            setArrowPath();
            canvas.drawPath(mPath, mPaint);

        } else if (mFlag == CHANGE_ARROW) {//改变箭头
            if (mFractionArrowSecond >= 0) {//改变alpha
                mPaint.setAlpha((int) ((mFractionArrowSecond/0.6f) * 255));
                setArrowPath();
                canvas.drawPath(mPath, mPaint);
                mPaint.setAlpha(255);
            } else if (mFractionArrowSecond >= -1) {//画对号
                setRightPath();
                mPathDst.reset();
                mPathMeasureLine.getSegment(0, mPathMeasureLine.getLength() * Math.abs(mFractionArrowSecond), mPathDst, true);
                canvas.drawPath(mPathDst, mPaint);
            } else if (mFractionArrowSecond >= -2) {//对号变矩形
                mPaint.setStyle(Paint.Style.STROKE);
                //旋转并缩小
                if (mFractionArrowSecond > -1.8f) {
                    canvas.rotate((float) (-180 * (Math.PI / 2 - mAngleRight) / Math.PI * (-1 - mFractionArrowSecond)), mCenterX - mLineWidth / 2f + (float) Math.sin(Math.PI / 4) * mRightShortSize, mCenterY - mLineWidth / 2f + (float) Math.cos(Math.PI / 4) * mRightShortSize);
                    mPathDst.reset();
                    mPathMeasureLine.getSegment(mPathMeasureLine.getLength() * (-mFractionArrowSecond - 1), mPathMeasureLine.getLength(), mPathDst, true);
                    canvas.drawPath(mPathDst, mPaint);

                }

                //最后的时候画方形
                if (mFractionArrowSecond < -1.9f) {
                    mPaint.setStyle(Paint.Style.FILL);
                    canvas.drawRect(mRectFRectangle, mPaint);
                }

            } else if (mFractionArrowSecond >= -3) {//画矩形,画三角形
                mPaint.setStyle(Paint.Style.FILL);
                canvas.drawRect(mRectFRectangle, mPaint);//画矩形

                setTrianglePath(1);
                canvas.drawPath(mPath, mPaint);//绘制dst
                mPaint.setColor(Color.WHITE);
                setTrianglePath(3+mFractionArrowSecond);
                canvas.drawPath(mPath, mPaint);//绘制src,全覆盖到不覆盖
                mPaint.setColor(Color.BLACK);
            }
        }
        canvas.restore();
    }

    //  箭头的path
    private void setArrowPath() {
        mPath.reset();
        mPaint.setStyle(Paint.Style.FILL);
        mPath.moveTo(mCenterX, mCenterY);
        mPath.rLineTo(-mLineWidth / 2f, -mLineWidth / 2f);
        mPath.rLineTo(mLineWidth / 2 - mRectangleSideLength / 2f, 0);
        mPath.rLineTo(0, -mRectangleSideLength);
        mPath.rLineTo(mRectangleSideLength, 0);
        mPath.rLineTo(0, mRectangleSideLength);
        mPath.rLineTo(mLineWidth / 2 - mRectangleSideLength / 2f, 0);
        mPath.close();
    }

    //对号的path
    private void setRightPath() {
        mPath.reset();
        mPaint.setStyle(Paint.Style.STROKE);

        mPath.moveTo(mCenterX - mLineWidth / 2f, mCenterY - mLineWidth / 2f);//从三角形左边的顶点画对暗号

        mPath.rLineTo((float) Math.sin(mAngleRight) * mRightShortSize, (float) Math.cos(mAngleRight) * mRightShortSize);
        mPath.rLineTo(mLineWidth - (float) Math.sin(mAngleRight) * mRightShortSize, -(float) ((mLineWidth - (float) Math.sin(mAngleRight) * mRightShortSize) * Math.tan(mAngleRight)));
        mPathMeasureLine.setPath(mPath, false);
    }

    //三角形的path
    private void setTrianglePath(float fractionLine) {
        mPath.reset();
        mPath.moveTo(mCenterX, mCenterY);
        float moveStance = mLineWidth / 2f * fractionLine;
        mPath.lineTo(mCenterX - moveStance, mCenterY - moveStance);
        mPath.lineTo(mCenterX + moveStance, mCenterY - moveStance);
        mPath.close();
    }

    public void download() {
        mAnimatorArrowFirst.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAnimatorLine.cancel();
        mAnimatorArrowFirst.cancel();
        mAnimatorArrowSecond.cancel();
    }
}
